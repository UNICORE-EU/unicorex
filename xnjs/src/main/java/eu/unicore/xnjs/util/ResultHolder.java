package eu.unicore.xnjs.util;

import java.io.IOException;

import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.ems.Action;
import eu.unicore.xnjs.ems.ActionResult;
import eu.unicore.xnjs.ems.ExecutionException;
import eu.unicore.xnjs.ems.IExecutionContextManager;
import eu.unicore.xnjs.tsi.TSI;

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

	public String getErrorMessage()throws IOException, ExecutionException{
		StringBuilder err = new StringBuilder();
		err.append(getResult().getErrorMessage());
		try{
			if(err.length()>0)err.append(" ");
			err.append("stderr: ");
			err.append(readOutcomeFile(a.getExecutionContext().getStderr()));
		}catch(Exception ex) {
			err.append("n/a (could not read stderr file)");
		}
		err.append("]");
		return err.toString();
	}

	/**
	 * get the contents of a file
	 * @param file The filename (relative to the execution directory)
	 * @return file content as String
	 * @throws IOException
	 * @throws ExecutionException
	 */
	public String readFile(String file)throws IOException, ExecutionException{
		return doReadFile(a.getExecutionContext().getWorkingDirectory()+"/"+file);
	}

	/**
	 * get the contents of a file in the outcome 
	 * @param file The filename (relative to the outcome directory)
	 * @return file content as String
	 * @throws IOException
	 * @throws ExecutionException
	 */
	public String readOutcomeFile(String file)throws IOException, ExecutionException{
		return doReadFile(a.getExecutionContext().getOutputDirectory()+"/"+file);
	}

	public Action getAction(){
		return a;
	}

	private String doReadFile(String name)throws IOException, ExecutionException{
		TSI tsi=config.getTargetSystemInterface(a.getClient(), getPreferredLoginNode());
		return IOUtils.readTSIFile(tsi, name, LIMIT);
	}

	private String getPreferredLoginNode() {
		return a.getExecutionContext()!=null?
				a.getExecutionContext().getPreferredExecutionHost():null;
	}
}
