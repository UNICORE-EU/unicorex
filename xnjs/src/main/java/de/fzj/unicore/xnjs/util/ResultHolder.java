package de.fzj.unicore.xnjs.util;

import java.io.IOException;
import java.io.InputStream;

import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.ems.Action;
import de.fzj.unicore.xnjs.ems.ActionResult;
import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.ems.IExecutionContextManager;
import de.fzj.unicore.xnjs.tsi.TSI;

/**
 * Holds the results of an execution. 
 * Allows to (lazily) get stdout and stderr.
 *  
 * @author schuller
 */
public class ResultHolder {

	private final Action a;
	private final XNJS config;
	
	//limit the sizes of files if these are read into memory fully
	public static final int LIMIT=256000;
	
	public ResultHolder(Action a, XNJS config){
		this.config=config;
		this.a=a;
	}
	
	/**
	 * remove the working directory
	 * @throws ExecutionException
	 */
	public void done()throws ExecutionException{
		config.get(IExecutionContextManager.class).destroyUSpace(a);
	}
	
	
	public Integer getExitCode(){
		return a.getExecutionContext().getExitCode();
	}
	
	public ActionResult getResult(){
		return a.getResult();
	}
	
	public String getStdErr()throws IOException, ExecutionException{
		return readOutcomeFile(a.getExecutionContext().getStderr());
	}
	
	public InputStream getInputStream(String stream)throws IOException, ExecutionException{
		TSI tsi=config.getTargetSystemInterface(a.getClient());
		return tsi.getInputStream(stream);
	}
	
	/**
	 * get the contents of a file
	 * @param file The filename (relative to the execution directory)
	 * @return file content as String
	 * @throws IOException
	 * @throws ExecutionException
	 */
	public String readFile(String file)throws IOException, ExecutionException{
		String name=a.getExecutionContext().getWorkingDirectory()+"/"+file;
		return doReadFile(name);
	}
	
	/**
	 * get the contents of a file in the outcome 
	 * @param file The filename (relative to the outcome directory)
	 * @return file content as String
	 * @throws IOException
	 * @throws ExecutionException
	 */
	public String readOutcomeFile(String file)throws IOException, ExecutionException{
		String name=a.getExecutionContext().getOutcomeDirectory()+"/"+file;
		return doReadFile(name);
	}
	
	public Action getAction(){
		return a;
	}
	
	private String doReadFile(String name)throws IOException, ExecutionException{
		TSI tsi=config.getTargetSystemInterface(a.getClient());
		return IOUtils.readTSIFile(tsi, name, LIMIT);
	}

}
