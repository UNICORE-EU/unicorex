package de.fzj.unicore.uas.impl.task;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.impl.BaseResourceImpl;
import eu.unicore.services.InitParameters;
import eu.unicore.services.Kernel;

/**
 * implementation of the {@link Task} service.<br/>
 * 
 * The actual process is running in the background and can
 * use the {@link #putResult(Kernel, String, XmlObject, String, int)} method to make the result
 * available to the client once it is available.<br/>
 *  
 * @author schuller
 */
public class TaskImpl extends BaseResourceImpl {

	public TaskImpl(){
		super();
	}
	
	public void cancel() {
		TaskModel m = getModel();
		String status = m.getStatus();
		if("RUNNING".equals(status)){
			m.setStatus("FAILED");
			m.setStatusMessage("Cancelled");
			getModel().setResult(new HashMap<>());
		}
	}

	@Override 
	public TaskModel getModel(){
		return (TaskModel)super.getModel();
	}
	
	@Override
	public void initialise(InitParameters initParams)
			throws Exception {
		TaskModel m = getModel();
		if(m==null){
			m = new TaskModel();
			setModel(m);
		}
		super.initialise(initParams);
		String parentService = initParams.parentServiceName;
		Calendar submissionTime=Calendar.getInstance();
		m.setStatus("RUNNING");
		m.setServiceSpec(parentService);
		m.setSubmissionTime(submissionTime);
	}

	/**
	 * put a result<br/>
	 * 
	 * TODO notify once we support notifications
	 * 
	 * @param kernel
	 * @param uuid - task instance UUID
	 * @param result - the result document
	 * @param message - status message
	 * @param exitCode
	 * @throws Exception
	 */
	public static void putResult(Kernel kernel, String uuid, Map<String, String> result, String message, int exitCode)throws Exception {
		try(TaskImpl ti=(TaskImpl)kernel.getHome(UAS.TASK).getForUpdate(uuid)){
			TaskModel model = ti.getModel();
			model.setStatus("SUCCESSFUL");
			model.setStatusMessage(message);
			model.setExitCode(exitCode);
			model.setResult(result);
		}
	}
	
	public static void failTask(Kernel kernel, String uuid, String message, int exitCode)throws Exception {
		try(TaskImpl ti=(TaskImpl)kernel.getHome(UAS.TASK).getForUpdate(uuid)){
			TaskModel model = ti.getModel();
			model.setStatus("FAILED");
			model.setStatusMessage(message);
			model.setExitCode(exitCode);
		}
	}
}
