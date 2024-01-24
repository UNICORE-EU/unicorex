package eu.unicore.xnjs.idb;

import java.util.List;

import eu.unicore.security.Client;
import eu.unicore.xnjs.ems.Action;
import eu.unicore.xnjs.ems.ExecutionContext;
import eu.unicore.xnjs.ems.ExecutionException;
import eu.unicore.xnjs.resources.ResourceRequest;

/**
 * An interface for realizing abstract resource definitions
 * (applications, paths, etc) onto the real ones on a target system <br/>
 * 
 * @author schuller
 */
public interface Incarnation {

	/**
	 * Generate a full {@link ApplicationInfo} object from the supplied "abstract" ApplicationInfo
	 */
	public ApplicationInfo incarnateApplication(ApplicationInfo job, Client client) throws ExecutionException;
	
	/**
	 * map a filename and a (possibly abstract) filesystem onto a concrete path
	 * 
	 * @param fileName
	 * @param fileSystem
	 * @param ec
	 * @param client
	 * @return path String denoting the absolute path
	 * @throws ExecutionException
	 */
	public String incarnatePath(String fileName, String fileSystem, ExecutionContext ec, Client client) throws ExecutionException;
		
	/**
	 * The resources requested by a job are converted to the list of resources
	 * that are actually used for job submission on the execution system<br/>
	 */
	public List<ResourceRequest> incarnateResources(Action job) throws ExecutionException;

	/**
	 * The resources requested by a client are converted to the list of resources
	 * that are actually used for job submission on the execution system<br/>
	 *  
	 * @param request - the requested resources
	 * @param client Client
	 */
	public List<ResourceRequest> incarnateResources(List<ResourceRequest> request, Client client) throws ExecutionException;

}
