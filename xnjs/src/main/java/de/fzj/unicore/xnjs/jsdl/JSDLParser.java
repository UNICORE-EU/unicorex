package de.fzj.unicore.xnjs.jsdl;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.xmlbeans.XmlObject;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.ApplicationDocument;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.ApplicationType;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.CreationFlagEnumeration;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.DataStagingType;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.JobDefinitionDocument;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.ResourcesType;
import org.ggf.schemas.jsdl.x2005.x11.jsdlPosix.ArgumentDocument;
import org.ggf.schemas.jsdl.x2005.x11.jsdlPosix.EnvironmentDocument;

import de.fzj.unicore.xnjs.idb.ApplicationInfo;
import de.fzj.unicore.xnjs.idb.ApplicationInfoParser;
import de.fzj.unicore.xnjs.io.DataStageInInfo;
import de.fzj.unicore.xnjs.io.DataStageOutInfo;
import de.fzj.unicore.xnjs.io.DataStagingCredentials;
import de.fzj.unicore.xnjs.io.DataStagingInfoParser;
import de.fzj.unicore.xnjs.io.IFileTransfer.ImportPolicy;
import de.fzj.unicore.xnjs.io.IFileTransfer.OverwritePolicy;
import de.fzj.unicore.xnjs.resources.Resource;
import de.fzj.unicore.xnjs.resources.ResourceRequest;
import de.fzj.unicore.xnjs.util.XmlBeansUtils;
import eu.unicore.jsdl.extensions.IgnoreFailureDocument;
import eu.unicore.jsdl.extensions.InlineDataDocument;
import eu.unicore.jsdl.extensions.ReadOnlyDocument;
import eu.unicore.jsdl.extensions.UserPostCommandDocument;
import eu.unicore.jsdl.extensions.UserPreCommandDocument;

/**
 * Parses information from a JSDL job into the XNJS specific classes.<br/>
 * This handles all supported dialects (plain, posix, hpcp, spmd, ...) 
 *
 * @author schuller
 */
public class JSDLParser implements ApplicationInfoParser<JobDefinitionDocument>, DataStagingInfoParser<JobDefinitionDocument> {

	private static final String posix_ns="http://schemas.ggf.org/jsdl/2005/11/jsdl-posix";

	private static final String spmd_ns="http://schemas.ogf.org/jsdl/2007/02/jsdl-spmd";

	private static QName[]executableQNames={new QName(posix_ns,"Executable")};
	private static QName[]inputQNames={new QName(posix_ns,"Input")};
	private static QName[]outputQNames={new QName(posix_ns,"Output")};
	private static QName[]errorQNames={new QName(posix_ns,"Error")};

	public JSDLParser(){}

	public ApplicationInfo parseApplicationInfo(JobDefinitionDocument sourceInfo) throws Exception{
		ApplicationInfo res=new ApplicationInfo();
		String type = extractJobType(sourceInfo);
		if(type!=null) {
			type = type.toUpperCase();
			ApplicationInfo.JobType jobType;
			try {
				jobType = ApplicationInfo.JobType.valueOf(type);
			}catch(Exception ex) {
				throw new IllegalArgumentException("No such job type, must be one 'NORMAL', 'RAW' or 'INTERACTIVE'");
			}
			if(ApplicationInfo.JobType.INTERACTIVE.equals(jobType)) {
				res.setRunOnLoginNode(true);
				res.setPreferredLoginNode(extractPreferredLoginNode(sourceInfo));
			}
			if(ApplicationInfo.JobType.RAW.equals(jobType)) {
				String file = extractAnnotation(sourceInfo, rawJobFileTag);
				if(file==null)throw new Exception("Raw submission mode requires a batch file");
				res.setRawBatchFile(file);
			}
		}
		ApplicationType app=sourceInfo.getJobDefinition().getJobDescription().getApplication();
		if(app!=null){
			ApplicationDocument ad=ApplicationDocument.Factory.newInstance();
			ad.setApplication(app);
			app=JSDLUtils.handleHPCP(app);
			res.setName(app.getApplicationName());
			res.setVersion(app.getApplicationVersion());
			res.setExecutable(getExecutable(app));
			res.setStdin(getInput(app));
			res.setStderr(getError(app));
			res.setStdout(getOutput(app));
			parseArguments(res, app);
			parseEnvironment(res, app);
			parseUserPrePostCommands(res, app);
			res.setIgnoreNonZeroExitCode(isIgnoreNonZeroExitCode(sourceInfo));
		}
		return res;
	}

	public List<ResourceRequest>parseRequestedResources(JobDefinitionDocument jd){
		return parseRequestedResources(jd.getJobDefinition().getJobDescription().getResources());
	}

	public List<ResourceRequest>parseRequestedResources(ResourcesType rt){
		JSDLResourceSet parsed=new JSDLResourceSet(rt);
		List<ResourceRequest>result = new ArrayList<ResourceRequest>();
		for(Resource r: parsed.getResources()){
			result.add(new ResourceRequest(r.getName(), r.getStringValue()));
		}
		for(ResourceRequest r: parsed.getExtensionJSDLResources()){
			result.add(new ResourceRequest(r.getName(), r.getRequestedValue()));
		}
		return result;
	}

	String getError(ApplicationType app){
		return XmlBeansUtils.getElementText(app, errorQNames);
	}

	String getExecutable(ApplicationType app){
		return XmlBeansUtils.getElementText(app, executableQNames);
	}

	String getInput(ApplicationType app){
		return XmlBeansUtils.getElementText(app, inputQNames);
	}

	String getOutput(ApplicationType app){
		return XmlBeansUtils.getElementText(app, outputQNames);
	}

	void parseEnvironment(ApplicationInfo appInfo, ApplicationType app)throws Exception{
		List<EnvironmentDocument>env=XmlBeansUtils.extractAnyElements(app, EnvironmentDocument.type.getDocumentElementName(), EnvironmentDocument.class);
		for(EnvironmentDocument e: env){
			appInfo.getEnvironment().put(e.getEnvironment().getName(), e.getEnvironment().getStringValue());
		}
	}

	void parseArguments(ApplicationInfo appInfo, ApplicationType app)throws Exception{
		List<ArgumentDocument>env=XmlBeansUtils.extractAnyElements(app, ArgumentDocument.type.getDocumentElementName(), ArgumentDocument.class);
		for(ArgumentDocument a: env){
			appInfo.getArguments().add(a.getArgument().getStringValue());
		}
	}

	void parseUserPrePostCommands(ApplicationInfo appInfo, ApplicationType app)throws Exception {
		XmlObject pre = XmlBeansUtils.extractFirstAnyElement(app, UserPreCommandDocument.type.getDocumentElementName());
		if(pre!=null){
			UserPreCommandDocument cmd = UserPreCommandDocument.Factory.parse(pre.toString());
			appInfo.setUserPreCommand(cmd.getUserPreCommand().getStringValue());
			boolean onLogin = cmd.getUserPreCommand().getRunOnLoginNode();
			appInfo.setUserPreCommandOnLoginNode(onLogin);
		}
		XmlObject post = XmlBeansUtils.extractFirstAnyElement(app, UserPostCommandDocument.type.getDocumentElementName());
		if(post!=null){
			UserPostCommandDocument cmd = UserPostCommandDocument.Factory.parse(post.toString());
			appInfo.setUserPostCommand(cmd.getUserPostCommand().getStringValue());
			boolean onLogin = cmd.getUserPostCommand().getRunOnLoginNode();
			appInfo.setUserPostCommandOnLoginNode(onLogin);
		}
	}
	
	//SPMD stuff
	private static final QName SPMD_PROCESSES=new QName(spmd_ns,"NumberOfProcesses");
	private static final QName SPMD_ATTR_PROCESSES=new QName(null,"actualTotalCPUCount");
	private static final QName SPMD_PROCESSES_PER_HOST=new QName(spmd_ns,"ProcessesPerHost");
	private static final QName SPMD_ATTR_THREADS=new QName(null,"actualIndividualCPUCount");
	private static final QName SPMD_THREADS_PER_PROCESS=new QName(spmd_ns,"ThreadsPerProcess");
	private static final QName SPMD_VARIATION=new QName(spmd_ns,"SPMDVariation");

	/**
	 * number of processes. This is taken from the corresponding NumberOfProcesses
	 * element. If this element has the boolean flag "actualTotalCPUCount" set to true,
	 * the variable name "${UC_TOTAL_PROCESSORS}" is used.
	 * @param app
	 * @throws Exception
	 */
	String getSPMDNumberOfProcesses(ApplicationType app)throws Exception{
		String attr=XmlBeansUtils.getAttributeText(app,SPMD_PROCESSES,SPMD_ATTR_PROCESSES);
		if(Boolean.parseBoolean(attr)){
			return "${UC_TOTAL_PROCESSORS}";
		}
		return XmlBeansUtils.getElementText(app,SPMD_PROCESSES);
	}
	String getSPMDProcessesPerHost(ApplicationType app)throws Exception{
		return XmlBeansUtils.getElementText(app,SPMD_PROCESSES_PER_HOST);
	}
	String getSPMDThreadsPerProcess(ApplicationType app)throws Exception{
		String attr=XmlBeansUtils.getAttributeText(app,SPMD_THREADS_PER_PROCESS,SPMD_ATTR_THREADS);
		if(Boolean.parseBoolean(attr)){
			return "${UC_PROCESSORS_PER_NODE}";
		}
		return XmlBeansUtils.getElementText(app,SPMD_THREADS_PER_PROCESS);
	}
	String getSPMDVariation(ApplicationType app)throws Exception{
		return XmlBeansUtils.getElementText(app,SPMD_VARIATION);
	}
	
	@Override
	public List<DataStageInInfo> parseImports(JobDefinitionDocument sourceInfo)
			throws Exception {
		DataStagingType[] ds=sourceInfo.getJobDefinition().getJobDescription().getDataStagingArray();
		return parseImports(ds);
	}
	
	public List<DataStageInInfo> parseImports(DataStagingType[] ds)
			throws Exception {
		List<DataStageInInfo>result=new ArrayList<DataStageInInfo>();
		if(ds!=null){
			for(DataStagingType dst: ds){
				if(isImport(dst)){
					String target=dst.getFileName();
					String fileSystem=dst.getFilesystemName();
					URI source=new URI(dst.getSource().getURI());
					DataStageInInfo dsi=new DataStageInInfo();
					dsi.setOverwritePolicy(getOverwritePolicy(dst));
					dsi.setImportPolicy(getImportPolicy(dst));
					dsi.setDeleteOnTermination(dst.getDeleteOnTermination());
					dsi.setSources(new URI[]{source});
					dsi.setFileName(target);
					dsi.setFileSystemName(fileSystem);
					dsi.setCredentials(extractCredentials(dst));
					dsi.setIgnoreFailure(isIgnoreFailure(dst));
					if(dst.getSource().getURI().startsWith("inline")){
						// parse inline data
						String data = XmlBeansUtils.getElementText(dst, 
								InlineDataDocument.type.getDocumentElementName());
						dsi.setInlineData(data);
					}
					result.add(dsi);
				}
			}
		}
		return result;
	}

	@Override
	public List<DataStageOutInfo> parseExports(JobDefinitionDocument sourceInfo)
			throws Exception {
		List<DataStageOutInfo>result=new ArrayList<DataStageOutInfo>();
		DataStagingType[] ds=sourceInfo.getJobDefinition().getJobDescription().getDataStagingArray();
		if(ds!=null){
			for(DataStagingType dst: ds){
				if(!isImport(dst)){
					String source=dst.getFileName();
					String fileSystem=dst.getFilesystemName();
					URI target=new URI(dst.getTarget().getURI());
					DataStageOutInfo dsi=new DataStageOutInfo();
					dsi.setOverwritePolicy(getOverwritePolicy(dst));
					dsi.setDeleteOnTermination(dst.getDeleteOnTermination());
					dsi.setTarget(target);
					dsi.setFileName(source);
					dsi.setFileSystemName(fileSystem);
					dsi.setCredentials(extractCredentials(dst));
					dsi.setIgnoreFailure(isIgnoreFailure(dst));
					result.add(dsi);
				}
			}
		}
		return result;
	}

	private boolean isImport(DataStagingType dst){
		return (dst.isSetSource());
	}

	private OverwritePolicy getOverwritePolicy(DataStagingType dst){
		CreationFlagEnumeration.Enum cfe=dst.getCreationFlag();

		if(CreationFlagEnumeration.OVERWRITE.equals(cfe)){
			return OverwritePolicy.OVERWRITE;
		}
		if(CreationFlagEnumeration.APPEND.equals(cfe)){
			return OverwritePolicy.APPEND;
		}
		else if(CreationFlagEnumeration.DONT_OVERWRITE.equals(cfe)){
			return OverwritePolicy.DONT_OVERWRITE;
		}
		else return OverwritePolicy.OVERWRITE;

	}

	private ImportPolicy getImportPolicy(DataStagingType dst){
		boolean readonly = false;
		try{
			XmlObject xo=JSDLUtils.getElement(dst, ReadOnlyDocument.type.getDocumentElementName());
			readonly = ReadOnlyDocument.Factory.parse(xo.newInputStream()).getReadOnly();
		}catch(Exception ex){}
		if(readonly)return ImportPolicy.PREFER_LINK;
		
		return ImportPolicy.PREFER_COPY;

	}

	protected DataStagingCredentials extractCredentials(DataStagingType dst){
		DataStagingCredentials creds=null;
		try{
			creds=JSDLUtils.extractUsernamePassword(dst);
		}catch(Exception ignored){}
		if(creds==null){
			try{
				creds=JSDLUtils.extractOAuthToken(dst);
			}catch(Exception ignored){}
		}
		return creds;
	}

	protected boolean isIgnoreFailure(DataStagingType dst){
		try{
			XmlObject failure=JSDLUtils.getElement(dst, IgnoreFailureDocument.type.getDocumentElementName());
			if(failure!=null){
				return IgnoreFailureDocument.Factory.parse(failure.newInputStream()).getIgnoreFailure();
			}
		}catch(Exception ignored){}
		return false;
	}
	
	
	/**
	 * extract the email address from the job's JobAnnotation elements
	 * @param jsdl
	 */
	public static String extractEmail(JobDefinitionDocument jsdl){
		return extractAnnotation(jsdl, emailTag);
	}
	
	/**
	 * extract the job type from the job's JobAnnotation elements
	 * @param jsdl
	 */
	public static String extractJobType(JobDefinitionDocument jsdl){
		return extractAnnotation(jsdl, jobTypeTag);
	}
	
	public static String extractPreferredLoginNode(JobDefinitionDocument jsdl){
		return extractAnnotation(jsdl, loginNodeTag);
	}
	
	public static String extractAnnotation(JobDefinitionDocument jsdl, String tag){
		String value = null;
		if(jsdl.getJobDefinition().getJobDescription().getJobIdentification()!=null){
			String[] annotations=jsdl.getJobDefinition().getJobDescription().getJobIdentification().getJobAnnotationArray();
			if(annotations!=null){
				for(String annotation: annotations){
					value = getTagValue(annotation, tag);
					if(value!=null)break;
				}
			}
		}
		return value;
	}
	
	public static List<String> extractNotificationURLs(JobDefinitionDocument jsdl){
		List<String>notifications = new ArrayList<>();
		if(jsdl.getJobDefinition()==null ||
		   jsdl.getJobDefinition().getJobDescription()==null)return notifications;
		
		if(jsdl.getJobDefinition().getJobDescription().getJobIdentification()!=null){
			String[] annotations=jsdl.getJobDefinition().getJobDescription().getJobIdentification().getJobAnnotationArray();
			if(annotations!=null){
				for(String annotation: annotations){
					String url = getTagValue(annotation, notificationTag);
					if(url!=null)notifications.add(url);
				}
			}
		}
		return notifications;
	}
	
	public static boolean isIgnoreNonZeroExitCode(JobDefinitionDocument jsdl){
		String notBefore = extractAnnotation(jsdl, "IgnoreNonZeroExitCode");
		return Boolean.parseBoolean(notBefore);
	}
	
	public static String extractNotBefore(JobDefinitionDocument jsdl){
		String notBefore = extractAnnotation(jsdl, "notBefore");
		if(notBefore==null) {
			notBefore = extractAnnotation(jsdl, "scheduledStartTime");
		}
		return notBefore;
	}
	
	public static final String emailTag = "user email";
	public static final String jobTypeTag = "job type";
	public static final String loginNodeTag = "login node";
	public static final String rawJobFileTag = "batch file";
	public static final String notificationTag = "notify";
	
	/**
	 * extracts an email address from the source string
	 * @param source
	 */
	public static String getEmailAddress(String source){
		return getTagValue(source, emailTag);
	}

	public static String getTagValue(String source, String tag){
		if(source.toLowerCase().startsWith(tag.toLowerCase())){
			return source.split("[=:]",2)[1].trim();
		}
		return null;
	}
}
