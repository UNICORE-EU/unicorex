package de.fzj.unicore.uas.json;

import java.io.File;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.namespace.QName;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.ApplicationDocument;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.ApplicationType;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.CreationFlagEnumeration;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.DataStagingDocument;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.DataStagingType;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.JobDefinitionDocument;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.JobDescriptionType;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.OperatingSystemTypeEnumeration;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.RangeValueType;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.ResourcesDocument;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.ResourcesType;
import org.ggf.schemas.jsdl.x2005.x11.jsdlPosix.ArgumentType;
import org.ggf.schemas.jsdl.x2005.x11.jsdlPosix.EnvironmentType;
import org.ggf.schemas.jsdl.x2005.x11.jsdlPosix.POSIXApplicationDocument;
import org.ggf.schemas.jsdl.x2005.x11.jsdlPosix.POSIXApplicationType;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ogf.schemas.jsdl.x2009.x03.sweep.SweepDocument;
import org.unigrids.services.atomic.types.ProtocolType;

import de.fzj.unicore.uas.util.MessageWriter;
import de.fzj.unicore.uas.util.UnitParser;
import de.fzj.unicore.wsrflite.xmlbeans.WSUtilities;
import eu.unicore.jsdl.extensions.IgnoreFailureDocument;
import eu.unicore.jsdl.extensions.InlineDataDocument;
import eu.unicore.jsdl.extensions.ResourceRequestDocument;
import eu.unicore.jsdl.extensions.UserCmdType;
import eu.unicore.jsdl.extensions.UserPostCommandDocument;
import eu.unicore.jsdl.extensions.UserPreCommandDocument;

/**
 * Helper to convert a UNICORE job description in JSON to JSDL form 
 * 
 * @author schuller
 */
public class Builder {

	private static final Logger logger=Logger.getLogger(Builder.class.getName());

	protected final List<DocumentSweep> sweeps;
	
	protected JobDefinitionDocument job;
	
	protected final JSONObject json;

	protected final Set<Requirement>requirements=new HashSet<Requirement>();

	protected boolean initialised;

	protected boolean isSweepJob=false;

	protected ProtocolType.Enum[] preferredProtocols={ProtocolType.BFT};

	protected boolean convertRESTtoWSRF = false;
	
	/**
	 * reads a JSON string from the supplied File
	 * and creates the builder from it
	 * 
	 * @param jsonFile
	 * @throws Exception
	 */
	public Builder(File jsonFile)throws Exception{
		this(FileUtils.readFileToString(jsonFile, "UTF-8"));
	}

	/**
	 * Creates the builder from the supplied JSON string
	 * 
	 * @param jsonString
	 * @throws IllegalArgumentException on JSON parsing errors
	 */
	public Builder(String jsonString) {
		sweeps=new ArrayList<DocumentSweep>();
		try{
			json=JSONUtil.read(jsonString);
			isSweepJob = json.optBoolean("isSweepJob", false);
		}catch(JSONException ex){
			String message=JSONUtil.makeParseErrorMessage(jsonString, ex);
			throw new IllegalArgumentException(message);
		}
	}


	/**
	 * Creates an empty builder. All content has to be set via the API
	 * @throws Exception
	 */
	public Builder()throws Exception{
		this("{}");
		json.put("Output",".");
	}

	public void setConvertRESTtoWSRF(boolean convert){
		this.convertRESTtoWSRF = convert;
	}
	
	public JSONObject getJSON() {
		return json;
	}
	
	public String[] getTags(){
		JSONArray tags = json.optJSONArray("Tags");
		if(tags==null)tags = json.optJSONArray("tags");
		if(tags!=null){
			String[] ret = new String[tags.length()];
			for(int i=0;i<tags.length();i++){
				ret[i]=tags.optString(i);
			}
			return ret;
		}
		return null;
	}

	protected void build(){
		if(initialised)return;
		initialised=true;
		preferredProtocols=parseProtocols();
		extractRequirements();
	}

	protected ProtocolType.Enum[] parseProtocols(){
		String protocolsP=getProperty("Preferred protocols","");
		String[] protocols=protocolsP.split("[ ,]+");
		ProtocolType.Enum[] result=new ProtocolType.Enum[protocols.length];
		for(int i=0;i<result.length;i++){
			if(protocols[i].length()<1)continue;
			ProtocolType.Enum p = ProtocolType.Enum.forString(protocols[i]);
			if(p!=null)
				result[i]=ProtocolType.Enum.forString(protocols[i]);
			else {
				throw new IllegalArgumentException("Unknown protocol: '"
						+protocols[i]+"'");
			}
		}
		return result;
	}

	protected void extractRequirements(){
		if(job!=null){
			extractFromJSDL();
			return;
		}

		String appName=JSONUtil.getString(json,"ApplicationName");
		String appVersion=JSONUtil.getString(json,"ApplicationVersion");
		if(appName!=null){
			ApplicationRequirement appRequired=new ApplicationRequirement(appName,appVersion);
			requirements.add(appRequired);
		}

		JSONObject ee=json.optJSONObject("Execution environment");
		if(ee!=null){
			throw new IllegalArgumentException("Tag 'Execution environment' is no longer supported");
		}
	}

	protected void extractFromJSDL(){
		//Application
		try{
			String appName=job.getJobDefinition().getJobDescription().getApplication().getApplicationName();
			String appVersion=job.getJobDefinition().getJobDescription().getApplication().getApplicationVersion();
			if(appName!=null){
				ApplicationRequirement appRequired=new ApplicationRequirement(appName,appVersion);
				requirements.add(appRequired);
			}
		}
		catch(Exception e){}
	}

	/**
	 * creates the JSDL job definition from a json object
	 */
	protected void makeJob() throws Exception {
		JobDescriptionType jd=JobDescriptionType.Factory.newInstance();
		jd.setApplication(makeApplication(json));

		String jobName=getProperty("Name");
		if(jobName==null){
			String appName = getProperty("ApplicationName");
			if(appName==null){
				jobName="UNICORE_Job";
			}
			else{
				jobName = appName;
			}
		}
		jd.addNewJobIdentification().setJobName(jobName);

		String jobProject=getProperty("Project");
		if(jobProject!=null){
			jd.getJobIdentification().addJobProject(jobProject);
		}

		String jobDescription=getProperty("Description");
		if(jobDescription!=null){
			jd.getJobIdentification().setDescription(jobDescription);
		}
		
		String jobType = getProperty("Job type");
		if(jobType!=null){
			jd.getJobIdentification().addJobAnnotation("Job type: "+jobType);
			if("raw".equalsIgnoreCase(jobType)){
				String bssFile = getProperty("BSS file");
				if(bssFile==null)throw new IllegalArgumentException("Tob type 'raw' requires 'BSS file'");
				jd.getJobIdentification().addJobAnnotation("Batch file: "+bssFile);
			}
			else if("interactive".equalsIgnoreCase(jobType)){
				String loginNode = getProperty("Login node");
				if(loginNode!=null){
					jd.getJobIdentification().addJobAnnotation("Login node: "+loginNode);
				}
			}else if("normal".equalsIgnoreCase(jobType)) {
				// nop
			}
			else {
				throw new IllegalArgumentException("No such job type: "+jobType);
			}
		}
		
		
		String email=getProperty("User email");
		if(email!=null){
			jd.getJobIdentification().addJobAnnotation("User email: "+email);
		}
		
		String notify = getProperty("Notification");
		if(notify!=null){
			jd.getJobIdentification().addJobAnnotation("Notify: "+notify);
		}
		JSONArray notifications=json.optJSONArray("Notification");
		if(notifications!=null){
			for(int i=0; i<notifications.length();i++){
				jd.getJobIdentification().addJobAnnotation("Notify: "+notifications.getString(i));
			}
		}
		
		//"not before" for scheduling server-side processing
		String notBefore=getProperty("Not before");
		if(notBefore!=null){
			jd.getJobIdentification().addJobAnnotation("notBefore: "+notBefore);
		}
		
		//how to treat application exiting with non-zero exit code
		String ignoreExitCode = getProperty("IgnoreNonZeroExitCode");
		if(ignoreExitCode!=null){
			boolean ignore = Boolean.parseBoolean(ignoreExitCode);
			jd.getJobIdentification().addJobAnnotation("IgnoreNonZeroExitCode: "+ignore);
		}

		//other annotations
		JSONArray a=json.optJSONArray("Tags");
		if(a!=null){
			for(int i=0; i<a.length();i++){
				jd.getJobIdentification().addJobAnnotation(a.getString(i));
			}
		}

		JSONObject res = (JSONObject)json.opt("Resources");
		if(res!=null){
			jd.setResources(makeResources(res));
		}

		List<DataStagingType>staging=new ArrayList<DataStagingType>();
		addStageIn(staging,json.optJSONArray("Imports"));
		addStageIn(staging,json.optJSONArray("Stage in"));

		addStageOut(staging,json.optJSONArray("Exports"));
		addStageOut(staging,json.optJSONArray("Stage out"));

		jd.setDataStagingArray(staging.toArray(new DataStagingType[staging.size()]));

		//build the final JSDL document
		job=JobDefinitionDocument.Factory.newInstance();
		job.addNewJobDefinition().setJobDescription(jd);

		// add sweep specs
		if(sweeps.size()>0){
			isSweepJob = true;
			setProperty("isSweepJob", "true");
			SweepDocument sd=SweepDocument.Factory.newInstance();
			sd.addNewSweep();
			for(DocumentSweep as: sweeps){
				WSUtilities.append(as.render(), sd);
			}
			WSUtilities.append(sd, job);
		}
	}

	protected void addStageIn(List<DataStagingType>staging, JSONArray j) throws IllegalArgumentException{
		if(j==null)return;
		for(int i=0; i<j.length();i++){
			try{
				JSONObject jObj=j.getJSONObject(i);
				String target=JSONUtil.getString(jObj,"To");
				String source = null;
				StagingSweep sweep = null;

				Object sweepSpec=jObj.get("From");
				if(sweepSpec instanceof JSONArray){
					sweep = new StagingSweep(i+1, false);
					sweeps.add(sweep);
					source = "placeholder";
				}else{
					source = JSONUtil.getString(jObj, "From");
				}
				if(sweep != null){
					JSONArray a = (JSONArray)sweepSpec;
					for(int k=0;k<a.length();k++){
						String f  = a.getString(k);
						Location l=createLocation(f);
						if(l.isLocal())throw new IllegalArgumentException("Cannot sweep over local file: "+f);
						sweep.getFiles().add(l.getEndpointURL());
					}
				}
				else{
					if(convertRESTtoWSRF && !hasCredentials(jObj)) {
						source = convertRESTToWSRF(source);
					}
					Location l=createLocation(source);
					if(l.isLocal())continue;
					source = l.getEndpointURL();
				}
				staging.add(createStageIn(source, target, jObj));
			}
			catch(IllegalArgumentException iae){
				throw iae;
			}catch(Exception e){
				throw new IllegalArgumentException("Stage-in specification invalid. Syntax: \"From: <location>, To: <uspacefile>, Mode: overwrite|append|nooverwrite\"",e);
			}
		}
	}

	protected Location createLocation(String descriptor){
		return new RawLocation(descriptor);
	}

	protected DataStagingType createStageIn(String source, String target, JSONObject jObj){
		DataStagingDocument dsd=DataStagingDocument.Factory.newInstance();
		DataStagingType d=dsd.addNewDataStaging();
		d.setFileName(target);
		d.addNewSource().setURI(source);
		if(source.toLowerCase().startsWith("inline:")){
			String data = JSONUtil.readMultiLine("Data", "", jObj);
			InlineDataDocument dataDoc = InlineDataDocument.Factory.newInstance();
			dataDoc.setInlineData(data);
			WSUtilities.append(dataDoc, d);
		}
		insertCommonDataStagingOptions(dsd, jObj);
		return d;
	}

	protected boolean hasCredentials(JSONObject jObj){
		return jObj!=null && jObj.optJSONObject("Credentials")!=null;
	}

	protected void addStageOut(List<DataStagingType>staging,JSONArray j)throws IllegalArgumentException{
		if(j==null)return;
		for(int i=0;i<j.length();i++){
			try{
				JSONObject jObj=j.getJSONObject(i);
				String source=JSONUtil.getString(jObj,"From");
				String target=jObj.getString("To");
				Location l=createLocation(target);
				if(l.isLocal())continue;
				target = l.getEndpointURL();
				staging.add(createStageOut(source, target, jObj));
			}catch(Exception e){
				throw new IllegalArgumentException("Stage-out specification invalid. Syntax: \"From: <uspacefile>, To: <location>, Mode: overwrite|append|nooverwrite\"",e);
			}
		}
	}

	protected DataStagingType createStageOut(String source, String target, JSONObject jObj){
		DataStagingDocument dsd=DataStagingDocument.Factory.newInstance();
		DataStagingType d=dsd.addNewDataStaging();
		d.setFileName(source);
		d.addNewTarget().setURI(target);
		insertCommonDataStagingOptions(dsd, jObj);
		return d;
	}

	
	private static final Pattern restURLPattern = Pattern.compile("(https||http)://(.*)/rest/core/storages/([^/]*)/files/(.*)");
	
	/**
	 * Converts a UNICORE REST URL to a UNICORE WSRF URL
	 * (heuristically, i.e. using pattern matching). If the URL does
	 * not match the REST URL pattern, it is returned unchanged
	 */
	public static String convertRESTToWSRF(String url){
		Matcher m = restURLPattern.matcher(url);
		if(!m.matches())return url;
		String scheme=m.group(1);
		String base=m.group(2);
		String storageID=m.group(3);
		String path=m.group(4);
		String wsrfURL = "BFT:"+scheme+"://"+base+"/services/StorageManagement?res="+storageID+"#/"+path;
		if(logger.isDebugEnabled())
			logger.debug("Converted REST URL <"+url+"> to WSRF URL <"+wsrfURL+">");
		return wsrfURL;
	}
	
	/**
	 * adds creation mode, file system, credentials, ignoreFailure and readOnly
	 * 
	 * @param dsd - data staging
	 * @param spec - the JSON object describing the transfer
	 */
	protected void insertCommonDataStagingOptions(DataStagingDocument dsd, JSONObject spec){
		DataStagingType d=dsd.getDataStaging();

		//creation mode
		String creation=JSONUtil.getString(spec,"Mode","overwrite");

		if("append".equalsIgnoreCase(creation)){
			d.setCreationFlag(CreationFlagEnumeration.APPEND);
		}
		else if("nooverwrite".equalsIgnoreCase(creation)){
			d.setCreationFlag(CreationFlagEnumeration.DONT_OVERWRITE);
		}
		else d.setCreationFlag(CreationFlagEnumeration.OVERWRITE);

		//file system
		String fs=spec.optString("Filesystem", null);
		if(fs!=null){
			d.setFilesystemName(fs);
			//TODO add requirement
		}

		//credentials
		JSONObject cred=spec.optJSONObject("Credentials");
		if(cred!=null){
			try{
				XmlObject creds = makeCredentials(cred);
				if(creds!=null)WSUtilities.append(creds, d);
			}
			catch(Exception e){
				throw new IllegalArgumentException("Can't parse Credentials structure", e);
			}
		}

		//ignore failure
		Boolean failOnError=Boolean.parseBoolean(JSONUtil.getString(spec,"FailOnError","true"));
		if(!failOnError){
			IgnoreFailureDocument ifd=IgnoreFailureDocument.Factory.newInstance();
			ifd.setIgnoreFailure(true);
			WSUtilities.append(ifd, dsd);
		}

		//ReadOnly
		Boolean readOnly=spec.optBoolean("ReadOnly");
		if(readOnly!=null){
			String xdoc = "<ro:ReadOnly xmlns:ro=\"http://www.unicore.eu/unicore/jsdl-extensions\">"
					+readOnly+"</ro:ReadOnly>";
			try{		
				XmlObject ro = XmlObject.Factory.parse(xdoc);
				WSUtilities.append(ro, dsd);
			}catch(Exception e){}
		}
	}

	private final static String credentialTemplate="<ac:Credential xmlns:ac=\"http://schemas.ogf.org/hpcp/2007/11/ac\">"+
			"<wsse:UsernameToken xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\">"+
			"<wsse:Username>%1s</wsse:Username>\n"+
			"<wsse:Password>%2s</wsse:Password>\n"+
			"</wsse:UsernameToken>\n"+
			"</ac:Credential>";

	private final static String bearerCredentialTemplate="<ac:Credential xmlns:ac=\"http://schemas.ogf.org/hpcp/2007/11/ac\">"+
			"<unic:BearerToken xmlns:unic=\"http://www.unicore.eu/unicore/jsdl-extensions\">%1s"+
			"</unic:BearerToken>\n"+
			"</ac:Credential>";

	/**
	 * create a GFD.135 compliant Credential XML structure
	 * @param creds
	 * @return Credential XmlObject
	 * @throws IllegalArgumentException
	 */
	protected XmlObject makeCredentials(JSONObject creds)throws Exception{
		if(creds.optString("Username",null)!=null)return getUPCredentials(creds);
		String tok = creds.optString("BearerToken",null);
		if(tok==null)return null;
		return XmlObject.Factory.parse(String.format(bearerCredentialTemplate, tok));
	}

	protected XmlObject getUPCredentials(JSONObject creds) throws Exception {
		String user=creds.optString("Username",null);
		String pass=creds.optString("Password",null);
		return XmlObject.Factory.parse(String.format(credentialTemplate, user, pass));
	}
	
	@SuppressWarnings("unchecked")
	protected ResourcesType makeResources(JSONObject j) throws Exception {
		ResourcesDocument rd=ResourcesDocument.Factory.newInstance();
		ResourcesType rt=rd.addNewResources();
		Iterator<String> it=j.keys();

		while(it.hasNext()){
			String resource=it.next();
			if("Reservation".equals(resource)){
				String reservationID=j.getString("Reservation");
				insertReservationID(reservationID, rd);
			}
			else if("Operating system".equals(resource)){
				String os=JSONUtil.getString(j,"Operating system");
				if(os!=null){
					OperatingSystemTypeEnumeration.Enum osType=getOSType(os);
					if(osType!=null){
						rt.addNewOperatingSystem().addNewOperatingSystemType().setOperatingSystemName(osType);
					}else{
						throw new IllegalArgumentException("Operating system "+os+" not recognized (not defined in JSDL).");
					}
				}
			}
			else if("Runtime".equals(resource)){
				String runtime = j.getString("Runtime");
				if(runtime!=null){
					rt.setIndividualCPUTime(makeRVT(runtime,UnitParser.getTimeParser(1)));
				}
			}
			else if("Memory".equals(resource)){
				String memory = j.getString("Memory");
				if(memory!=null){
					rt.setIndividualPhysicalMemory(makeRVT(memory,UnitParser.getCapacitiesParser(1)));
				}
			}
			else if("MemoryPerNode".equals(resource)){
				String memory = j.getString("MemoryPerNode");
				if(memory!=null){
					rt.setIndividualPhysicalMemory(makeRVT(memory,UnitParser.getCapacitiesParser(1)));
				}
			}
			else if("CPUs".equals(resource)){
				String totalCPUs = j.getString("CPUs");
				if(totalCPUs!=null){
					rt.setTotalCPUCount(makeRVT(totalCPUs,null));
				}
			}
			else if("Nodes".equals(resource)){
				String nodes = j.getString("Nodes");
				if(nodes!=null){
					rt.setTotalResourceCount(makeRVT(nodes,null));

				}
			}
			else if("CPUsPerNode".equals(resource)){
				String cpus = j.getString("CPUsPerNode");
				if(cpus!=null){
					rt.setIndividualCPUCount(makeRVT(cpus,null));
				}
			}
			else{
				//generic resource
				String req=j.getString(resource);
				insertResourceRequest(resource,req,rd);
			}
		}

		return rt;
	}

	protected void insertResourceRequest(String name, String value, ResourcesDocument target)throws Exception{
		ResourceRequestDocument rrd=ResourceRequestDocument.Factory.newInstance();
		rrd.addNewResourceRequest().setName(name);
		rrd.getResourceRequest().setValue(value);
		WSUtilities.append(rrd, target);
	}

	protected void insertReservationID(String id, ResourcesDocument target)throws Exception{
		String resID="<u6rr:ReservationReference xmlns:u6rr=\"http://www.unicore.eu/unicore/xnjs\">"+id+"</u6rr:ReservationReference>";
		XmlObject o=XmlObject.Factory.parse(resID);
		//and append to resources doc...
		WSUtilities.append(o, target);
	}

	protected UserCmdType getUserPreCommand(JSONObject src){
		UserCmdType uct = null;
		String userPost=JSONUtil.getString(src,"User precommand");
		if(userPost!=null){
			uct=UserCmdType.Factory.newInstance();
			uct.setStringValue(userPost);
			String sLogin = JSONUtil.getString(src,"RunUserPrecommandOnLoginNode");
			if(sLogin !=null){
				uct.setRunOnLoginNode(Boolean.parseBoolean(sLogin));
			}
		}
		return uct;
	}
	
	protected UserCmdType getUserPostCommand(JSONObject src){
		UserCmdType uct = null;
		String userPost=JSONUtil.getString(src,"User postcommand");
		if(userPost!=null){
			uct=UserCmdType.Factory.newInstance();
			uct.setStringValue(userPost);
			String sLogin = JSONUtil.getString(src,"RunUserPostcommandOnLoginNode");
			if(sLogin !=null){
				uct.setRunOnLoginNode(Boolean.parseBoolean(sLogin));
			}
		}
		return uct;
	}
	
	protected ApplicationType makeApplication(JSONObject j)throws Exception{
		ApplicationDocument ad=ApplicationDocument.Factory.newInstance();
		ApplicationType app=ad.addNewApplication();

		String val2=JSONUtil.getString(j,"ApplicationVersion");
		if(val2!=null)app.setApplicationVersion(val2);

		String val=JSONUtil.getString(j,"ApplicationName");
		if(val!=null){
			app.setApplicationName(val);
			ApplicationRequirement appRequired=new ApplicationRequirement(val,val2);
			requirements.add(appRequired);
		}

		POSIXApplicationDocument pd=POSIXApplicationDocument.Factory.newInstance();
		POSIXApplicationType p=pd.addNewPOSIXApplication();

		val=JSONUtil.getString(j,"Executable");
		if(val!=null)p.addNewExecutable().setStringValue(val);

		JSONArray array=j.optJSONArray("Arguments");
		if(array!=null)	p.setArgumentArray(makeArgs(array));

		// support both array-like and map-like syntax for Environment
		array=j.optJSONArray("Environment");
		int offset = 0;
		if(array!=null){
			EnvironmentType[]envarray = makeEnvironment(array);
			offset = envarray.length;
			p.setEnvironmentArray(envarray);
		}
		JSONObject env=j.optJSONObject("Environment");
		if(env!=null){
			EnvironmentType[]envarray = makeParameters(env,offset);
			for(EnvironmentType e : envarray){
				String key=e.getName();
				val=e.getStringValue();
				EnvironmentType et=p.addNewEnvironment();
				et.setName(key);
				et.setStringValue(val);
			}
			offset+=envarray.length;
		}
		
		JSONObject params=j.optJSONObject("Parameters");
		if(params!=null){
			EnvironmentType[]envarray = makeParameters(params,offset);
			for(EnvironmentType e : envarray){
				String key=e.getName();
				val=e.getStringValue();
				EnvironmentType et=p.addNewEnvironment();
				et.setName(key);
				et.setStringValue(val);
			}
		}

		val=JSONUtil.getString(j,"User name");
		if(val!=null)p.addNewUserName().setStringValue(val);

		val=JSONUtil.getString(j,"Group");
		if(val!=null)p.addNewGroupName().setStringValue(val);

		val=JSONUtil.getString(j,"Stdin");
		if(val!=null)p.addNewInput().setStringValue(val);

		val=JSONUtil.getString(j,"Stdout");
		if(val!=null)p.addNewOutput().setStringValue(val);

		val=JSONUtil.getString(j,"Stderr");
		if(val!=null)p.addNewError().setStringValue(val);

		// pre/post command
		UserCmdType pre = getUserPreCommand(j);
		if(pre!=null){
			UserPreCommandDocument upcd = UserPreCommandDocument.Factory.newInstance();
			upcd.setUserPreCommand(pre);
			WSUtilities.append(upcd, ad);
		}
		UserCmdType post = getUserPostCommand(j);
		if(post!=null){
			UserPostCommandDocument upcd = UserPostCommandDocument.Factory.newInstance();
			upcd.setUserPostCommand(post);
			WSUtilities.append(upcd, ad);
		}
		
		WSUtilities.append(pd, ad);
		return app;
	}


	protected ArgumentType[] makeArgs(JSONArray j) {
		ArgumentType[] args=new ArgumentType[j.length()];
		for (int i = 0; i < args.length; i++) {
			args[i]=ArgumentType.Factory.newInstance();
			try{
				JSONObject sweep = j.optJSONObject(i);
				if(sweep!=null){
					ArgumentSweep sw=new ArgumentSweep(i+1,sweep);
					sweeps.add(sw);
					args[i].setStringValue(sweep.optString("Base","NNN"));
				}
				else{
					args[i].setStringValue(j.getString(i));
				}
			}catch(JSONException ex){
				throw new IllegalArgumentException("Error parsing argument "+i+" in argument array! ",ex);
			}
		}
		return args;
	}
	
	protected EnvironmentType[] makeParameters(JSONObject j, int offset) {
		EnvironmentType[] envarray = new EnvironmentType[j.length()];
		@SuppressWarnings("unchecked")
		Iterator<String> iter = j.keys();
		int i = 0;
		while (iter.hasNext()) {
			String key = iter.next();
			envarray[i] = EnvironmentType.Factory.newInstance();
			envarray[i].setName(key);
			try{
				JSONObject sweep = j.optJSONObject(key);
				if(sweep!=null){
					EnvironmentSweep sw = new EnvironmentSweep(offset+i+1,sweep);
					sweeps.add(sw);
					envarray[i].setStringValue(sweep.optString("Base","NNN"));
				}
				else{
					envarray[i].setStringValue(j.getString(key));
				}
				i++;
			}catch(JSONException ex){
				throw new IllegalArgumentException("Error parsing argument "+i+" in argument array! ",ex);
			}
		}
		return envarray;
	}

	protected EnvironmentType[] makeEnvironment(JSONArray j){
		EnvironmentType[] args=new EnvironmentType[j.length()];
		for (int i = 0; i < args.length; i++) {
			try{
				String val=j.getString(i);
				String[] split=val.split("=",2);
				args[i]=EnvironmentType.Factory.newInstance();
				args[i].setName(split[0].trim());
				if(split.length>1)args[i].setStringValue(split[1].trim());
				else args[i].setStringValue("");
			}catch(JSONException ex){
				throw new IllegalArgumentException("Error parsing entry "+i+" in environment array! ",ex);
			}
		}
		return args;
	}

	//list of common OSs for which we want to ignore case
	static final String[] knownOSs=new String[]{"LINUX", "MACOS", "AIX", 
		"FreeBSD", "NetBSD", "Solaris", "WINNT", "IRIX", "HPUX", "Unknown"};

	OperatingSystemTypeEnumeration.Enum getOSType(String os){
		for(String o: knownOSs){
			if(o.equalsIgnoreCase(os)){
				return OperatingSystemTypeEnumeration.Enum.forString(o);
			}
		}
		return OperatingSystemTypeEnumeration.Enum.forString(os);
	}

	/**
	 * get the job represented by this builder object
	 * 
	 * @return a JSDL job definition doc
	 */
	public JobDefinitionDocument getJob() throws Exception{
		build();
		if(job==null)makeJob();
		return job;
	}

	/**
	 * set the job 
	 */
	public void setJob(JobDefinitionDocument job) throws Exception{
		this.job=job;
	}

	public void setProperty(String key, String value) {
		try{
			if(value==null){
				json.remove(key);
			}
			else{
				json.put(key, value);
			}
		}catch(Exception e){}
	}

	/**
	 * returns the given property, or null if not found
	 * @param key - the property key 
	 */
	public String getProperty(String key) {
		return JSONUtil.getString(json, key);
	}

	public String getProperty(String key,String defaultValue) {
		return JSONUtil.getString(json, key,defaultValue);
	}

	public void writeTo(Writer os) {
		try{
			os.write(json.toString(2)+"\n");
			os.flush();
		}catch(Exception e){logger.error("",e);}
	}

	public Collection<Requirement>getRequirements(){
		build();
		return requirements;
	}
	
	public String toString(){
		try{
			return json.toString(2);
		}
		catch(Exception e){}
		return super.toString()+"<invalid JSON object>";
	}

	public boolean isSweepJob(){
		return isSweepJob;
	}

	/**
	 * validate the given JSDL job against the JSDL schema
	 * @param jdd - the JSDL document to validate
	 * @param msg - the {@link MessageWriter} to use, or <code>null</code> if no messages should be logged
	 * @return <code>true</code> if the document is valid, <code>false</code> otherwise
	 */
	public static boolean isValidJSDL(JobDefinitionDocument jdd, MessageWriter msg){
		List<String>validationErrors=new ArrayList<String>();
		XmlOptions opts=new XmlOptions();
		opts.setErrorListener(validationErrors);
		jdd.validate(opts);
		int numErrors=validationErrors.size();
		boolean isValid = numErrors==0;
		if(numErrors>0){
			if(msg!=null){
				msg.message("There were "+numErrors+" JSDL validation errors, check client log, or re-run in verbose mode " +
						"to see more details.");
				for(int i=0; i<numErrors; i++){
					msg.verbose(String.valueOf(validationErrors.get(i)));
				}
			}
		}
		return isValid;
	}

	protected RangeValueType makeRVT(String value, UnitParser parser){
		RangeValueType rvt = RangeValueType.Factory.newInstance();
		if(value.startsWith("${")){
			setExpression(rvt, value);
		}
		else {
			if(parser!=null){
				rvt.addNewExact().setDoubleValue(parser.getDoubleValue(value));
			}
			else{
				rvt.addNewExact().setStringValue(value);
			}
		}
		return rvt;
	}

	final static QName exprQname = new QName("http://www.unicore.eu/unicore/jsdl-extensions","expression");

	protected void setExpression(RangeValueType rvt, String expr){
		XmlCursor c = rvt.newCursor();
		c.toFirstContentToken();
		c.insertAttributeWithValue(exprQname,expr);
		c.dispose();
	}
}
