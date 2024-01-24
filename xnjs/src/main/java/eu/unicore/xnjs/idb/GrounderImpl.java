package eu.unicore.xnjs.idb;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.logging.log4j.Logger;

import eu.unicore.security.Client;
import eu.unicore.util.Log;
import eu.unicore.xnjs.XNJSProperties;
import eu.unicore.xnjs.ems.Action;
import eu.unicore.xnjs.ems.ExecutionContext;
import eu.unicore.xnjs.ems.ExecutionException;
import eu.unicore.xnjs.resources.BooleanResource;
import eu.unicore.xnjs.resources.Resource;
import eu.unicore.xnjs.resources.Resource.Category;
import eu.unicore.xnjs.resources.ResourceRequest;
import eu.unicore.xnjs.resources.ResourceSet;
import eu.unicore.xnjs.resources.StringResource;
import eu.unicore.xnjs.tsi.TSI;
import eu.unicore.xnjs.tsi.TSIFactory;
import eu.unicore.xnjs.tsi.remote.TSIMessages;
import eu.unicore.xnjs.tsi.remote.TSIProperties;
import eu.unicore.xnjs.util.ErrorCode;
import eu.unicore.xnjs.util.LogUtil;
import eu.unicore.xnjs.util.ScriptEvaluator;

/**
 *  
 * @author schuller
 */
@Singleton
public class GrounderImpl implements Incarnation {

	protected static final Logger logger=LogUtil.getLogger(LogUtil.JOBS,GrounderImpl.class);

	protected final IDB idb;

	protected final TSIFactory tsiFactory;

	protected final XNJSProperties properties;

	@Inject
	public GrounderImpl(IDB idb, TSIFactory tsiFactory, XNJSProperties properties) {
		this.idb = idb;
		this.tsiFactory = tsiFactory;
		this.properties = properties;
	}

	@Override
	public ApplicationInfo incarnateApplication(ApplicationInfo fromUser, Client client) throws ExecutionException {
		ApplicationInfo result = null;

		String appName=fromUser.getName();
		if(appName!=null){
			String version=fromUser.getVersion();
			ApplicationInfo idbApp = idb.getApplication(appName, version, client);
			if(idbApp!=null){
				result = idbApp.clone();
			}
			else{
				throw new ExecutionException(new ErrorCode(ErrorCode.ERR_UNKNOWN_APPLICATION,"Application could not be mapped to an executable."));
			}
		}
		else{
			// app info taken only from user
			result = new ApplicationInfo();
			if(fromUser.ignoreNonZeroExitCode()) {
				result.setIgnoreNonZeroExitCode(true);
			}
			result.setPreferredLoginNode(fromUser.getPreferredLoginNode());
			result.setRawBatchFile(fromUser.getRawBatchFile());
		}

		try{
			mergeInfo(result,fromUser);
		}catch(Exception ex){
			throw new ExecutionException(ex);
		}

		return result;
	}

	@Override
	public String incarnatePath(String fileName, String fileSystem, ExecutionContext ec, Client client) throws ExecutionException {

		if(fileSystem==null) {
			return (ec.getWorkingDirectory()+File.separator+fileName).replaceAll(File.separator+File.separator,File.separator);
		}
		else{
			String base=null;
			TSI tsi = tsiFactory.createTSI(client);
			if("HOME".equalsIgnoreCase(fileSystem)){
				base=tsi.getHomePath();
			}
			else{
				base=idb.getFilespace(fileSystem);
			}
			if(base==null){
				base=tsi.getEnvironment(fileSystem);
			}
			if(base==null || base.length()==0)throw new ExecutionException(new ErrorCode(0,"Filesystem '"+fileSystem+"' not available."));
			return base+fileName;
		}
	}

	/**
	 * merges the original request into the effective ApplicationInfo
	 * This is only info relevant for SUBMISSION, i.e. info elements, metadata, etc

	 * @param result - the application info that will be used to create the submit script
	 * @param fromUser - the user's request
	 */
	protected void mergeInfo(ApplicationInfo result, ApplicationInfo fromUser) throws Exception {
		boolean allowUserExec = properties.getBooleanValue(XNJSProperties.ALLOW_USER_EXECUTABLE);

		if(fromUser.getExecutable()!=null){
			if(!allowUserExec){
				String msg="Cannot execute <"+fromUser.getExecutable()+">. Only Applications may be executed on this site.";
				ErrorCode ec=new ErrorCode(ErrorCode.ERR_EXECUTABLE_FORBIDDEN,msg);
				throw new ExecutionException(ec);
			}
			result.setExecutable(fromUser.getExecutable());
		}
		for(Map.Entry<String,String>e: fromUser.getEnvironment().entrySet()){
			result.getEnvironment().put(e.getKey(),checkLegal(e.getValue(), "Environment"));
		}

		/*
		 * filter arguments in the incarnation
		 * if an argument ends in "?", the following happens.
		 *   - The argument name (string between '${'/'}' or '%'/'%' and '?' is extracted
		 *   - it is checked whether a corresponding environment variable has
		 *     been supplied
		 *   - if no, the argument is removed from the list
		 */
		List<String> argsFromIDB = result.getArguments();
		List<String> incarnatedArgs = new ArrayList<>();
		for(String argName: argsFromIDB){
			if(!argName.endsWith("?")){
				incarnatedArgs.add(argName);
				continue;
			}
			String env=extractArgumentName(argName.substring(0,argName.length()-1));
			for(Map.Entry<String, String> envFromIDB: result.getEnvironment().entrySet()){
				String envName=envFromIDB.getKey();
				if(envName.equals(env)){
					incarnatedArgs.add(argName.substring(0,argName.length()-1));
				}
			}
		}
		result.setArguments(incarnatedArgs.toArray(new String[incarnatedArgs.size()]));

		/*
		 * use other arguments from the request
		 */
		for(String arg: fromUser.getArguments()){
			result.getArguments().add(checkLegal(arg,"Argument"));
		}

		// in/out/err targets
		if(fromUser.getStderr()!=null){
			result.setStderr(checkLegal(fromUser.getStderr(), "stderr"));
		}
		if(fromUser.getStdin()!=null){
			result.setStdin(checkLegal(fromUser.getStdin(), "stdin"));
		}
		if(fromUser.getStdout()!=null){
			result.setStdout(checkLegal(fromUser.getStdout(), "stdout"));
		}

		// user pre/post
		boolean allowedToRunOnLogin = !Boolean.parseBoolean(
				properties.getRawProperty(TSIProperties.PREFIX+TSIProperties.BSS_NO_USER_INTERACTIVE_APPS));
		if(fromUser.getUserPreCommand()!=null){
			if(!allowUserExec){
				String msg="Cannot execute user pre-command - user executables are forbidden on this site.";
				ErrorCode ec=new ErrorCode(ErrorCode.ERR_EXECUTABLE_FORBIDDEN,msg);
				throw new ExecutionException(ec);
			}
			result.setUserPreCommand(fromUser.getUserPreCommand());
			result.setUserPreCommandOnLoginNode(allowedToRunOnLogin && fromUser.isUserPreCommandOnLoginNode());
			result.setUserPreCommandIgnoreExitCode(fromUser.isUserPreCommandIgnoreExitCode());
		}
		if(fromUser.getUserPostCommand()!=null) {
			if(!allowUserExec){
				String msg="Cannot execute user post-command - user executables are forbidden on this site.";
				ErrorCode ec=new ErrorCode(ErrorCode.ERR_EXECUTABLE_FORBIDDEN,msg);
				throw new ExecutionException(ec);
			}
			result.setUserPostCommand(fromUser.getUserPostCommand());
			result.setUserPostCommandOnLoginNode(allowedToRunOnLogin && fromUser.isUserPostCommandOnLoginNode());
			result.setUserPostCommandIgnoreExitCode(fromUser.isUserPostCommandIgnoreExitCode());
		}
		// login node / raw mode / allocate
		if(allowedToRunOnLogin && fromUser.isRunOnLoginNode()) {
			result.setRunOnLoginNode(true);
			result.setPreferredLoginNode(fromUser.getPreferredLoginNode());
		}
		if(fromUser.isAllocateOnly()) {
			result.setAllocateOnly();
		}

	}


	/**
	 * perform resource incarnation 
	 * 
	 * @param job - the job
	 * 
	 * @return the incarnated resources for submission
	 */
	@Override
	public List<ResourceRequest> incarnateResources(Action job) throws ExecutionException{
		List<ResourceRequest>fromUser = job.getExecutionContext().getResourceRequest();
		// merge in any defaults from IDB Application
		List<ResourceRequest>fromApplication = job.getApplicationInfo().getResourceRequests();
		List<ResourceRequest>requested = ResourceRequest.merge(fromApplication, fromUser);

		// evaluate any variables now
		for(ResourceRequest r: requested){
			if(ScriptEvaluator.isScript(r.getRequestedValue())){
				String script = ScriptEvaluator.extractScript(r.getRequestedValue());
				try{
					Map<String,String>env = job.getExecutionContext().getEnvironment();
					String newValue = ScriptEvaluator.evaluateAsString(script, env, null);
					job.addLogTrace("Evaluated resource "+r.getName()+" as "+newValue);
					r.setRequestedValue(newValue);
				}catch(Exception e){
					String msg = Log.createFaultMessage("Could not evaluate script <"+script+">"
							+" for resource <"+r.getName()+">", e);
					throw new ExecutionException(msg);
				}
			}
		}
		List<ResourceRequest> incarnatedResources=incarnateResources(requested, job.getClient());
		if(incarnatedResources!=null){
			job.addLogTrace("Requesting resources: "+incarnatedResources);
		}
		return incarnatedResources;
	}

	/**
	 * determine the partition name
	 */
	private String getRequestedPartition(Collection<ResourceRequest> requested, Resource partitions) {
		ResourceRequest requestedByUser = ResourceRequest.find(requested, ResourceSet.QUEUE);
		String partitionName = requestedByUser!=null? requestedByUser.getRequestedValue() : null;
		if(partitionName==null){
			partitionName = partitions.getStringValue();
		}
		return partitionName;
	}

	@Override
	public List<ResourceRequest> incarnateResources(List<ResourceRequest>userRequest, Client c) throws ExecutionException{
		List<ResourceRequest> incarnatedRequest = new ArrayList<>();

		Resource availablePartitions = idb.getAllowedPartitions(c);
		String requestedPartitionName = getRequestedPartition(userRequest, availablePartitions);
		availablePartitions.setSelectedValue(requestedPartitionName);
		if(requestedPartitionName!=null) {
			if(!availablePartitions.isInRange(requestedPartitionName)){
				throw new ExecutionException(new ErrorCode(ErrorCode.ERR_RESOURCE_OUT_OF_RANGE,"Resource request <Queue = "+requestedPartitionName+"> is out of range."));
			}
		}

		Partition selectedPartition = idb.getPartition(requestedPartitionName);
		if(selectedPartition==null){
			selectedPartition = idb.getDefaultPartition();
			Resource queue = selectedPartition.getResources().getResource(ResourceSet.QUEUE);
			if(queue==null || !queue.isInRange(requestedPartitionName)){
				throw new ExecutionException(new ErrorCode(ErrorCode.ERR_UNKNOWN_RESOURCE, 
						"Partition/Queue <"+requestedPartitionName+"> is not available at this site."));
			}
		}
		if(!IDBImpl.DEFAULT_PARTITION.equals(selectedPartition.getName())){
			availablePartitions.setSelectedValue(selectedPartition.getName());
		}
		String partition = "*".equals(selectedPartition.getName()) ?
				requestedPartitionName :
				availablePartitions.getStringValue();
		incarnatedRequest.add(new ResourceRequest(ResourceSet.QUEUE, partition));

		// verify requested resources
		ResourceSet resources = selectedPartition.getResources();
		for(ResourceRequest rr: userRequest){
			boolean doVerify = "*".equals(selectedPartition.getName())? false : true;
			String name = rr.getName();
			String value = rr.getRequestedValue();
			if(value==null)continue;

			Resource resource = resources.getResource(name);

			if(ResourceSet.RESERVATION_ID.equals(name)){
				doVerify = false;
				resource = new StringResource(ResourceSet.RESERVATION_ID, value);
			}
			if(ResourceSet.PROJECT.equals(name) && resource==null){
				doVerify = false;
				resource = new StringResource(ResourceSet.PROJECT, value);
			}
			if(ResourceSet.NODE_CONSTRAINTS.equals(name) && resource==null){
				doVerify = false;
				resource = new StringResource(ResourceSet.NODE_CONSTRAINTS, value);
			}
			if(ResourceSet.QOS.equals(name) && resource==null){
				doVerify = false;
				resource = new StringResource(ResourceSet.QOS, value);
			}
			if(ResourceSet.EXCLUSIVE.equals(name) && resource==null){
				doVerify = false;
				Boolean excl = Boolean.parseBoolean(value);
				resource = new BooleanResource(ResourceSet.EXCLUSIVE, excl, Category.QOS_ATTRIBUTE);
			}
			if(ResourceSet.QUEUE.equals(name)){
				continue;
			}

			if(doVerify){
				if(resource==null){
					throw new ExecutionException(new ErrorCode(ErrorCode.ERR_UNKNOWN_RESOURCE,"Resource <"
							+name+"> is not available for partition <"+selectedPartition.getName()+">"));
				}
				if(!resource.isInRange(value)){
					throw new ExecutionException(new ErrorCode(ErrorCode.ERR_RESOURCE_OUT_OF_RANGE,"Resource request <"
							+name+"="+value+"> is out of range."));
				}
			}

			incarnatedRequest.add(new ResourceRequest(name, value));
		}

		//apply defaults from IDB
		List<ResourceRequest> defaultResources = resources.getDefaults();

		boolean computeRequested = ResourceRequest.contains(userRequest, ResourceSet.TOTAL_CPUS) 
				|| ResourceRequest.contains(userRequest, ResourceSet.CPUS_PER_NODE)
				|| ResourceRequest.contains(userRequest, ResourceSet.NODES);
		
		for(ResourceRequest sr: defaultResources){
			String name=sr.getName();
			
			if(computeRequested && (
					ResourceSet.NODES.equals(name) || ResourceSet.CPUS_PER_NODE.equals(name) 
					|| ResourceSet.TOTAL_CPUS.equals(name)
			))
			{
				continue;
			}
			if(!ResourceRequest.contains(incarnatedRequest,name)){
				incarnatedRequest.add(sr);	
			}
		}
		return incarnatedRequest;
	}

	private static final String UNIX_VAR = "\\$\\{?(\\w+)\\}?";
	private static final String WIN_VAR = "%(\\w+?)%";
	private static final String VAR = UNIX_VAR+"|"+WIN_VAR;
	private static final Pattern ARG_PATTERN=Pattern.compile("\\s?(.*?)"+"("+VAR+")(.*?)\\s*", Pattern.DOTALL);

	/**
	 * extracts the argument name from the value of a jsdl:Argument tag</br>
	 * xxx$ARG, xxx${ARG}, %ARG% => ARG 
	 * @param argValue
	 */
	public static String extractArgumentName(String argValue){
		Matcher m=ARG_PATTERN.matcher(argValue);		
		if(m.matches()){
			return m.group(3) == null ? m.group(4) : m.group(3);
		}
		else return null;
	}

	private String checkLegal(String input, String desc){
		if(properties.getBooleanValue(XNJSProperties.STRICT_USERINPUT_CHECKING)){
			return TSIMessages.checkLegal(input, desc);
		}
		else return input;
	}
}
