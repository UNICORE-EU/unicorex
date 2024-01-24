package eu.unicore.uas.impl.tss;

import eu.unicore.services.InitParameters;
import eu.unicore.services.Resource;
import eu.unicore.services.impl.DefaultHome;

public class TargetSystemHomeImpl extends DefaultHome {
	
	private boolean jobSubmissionEnabled=true;
	
	//the default message used when job submission is disabled
	public static final String DEFAULT_MESSAGE="Job submission is currently disabled --- please try again later!";
	
	private String highMessage = "";

	@Override
	protected Resource doCreateInstance(InitParameters initObjs) throws Exception {
		String clazz = initObjs.resourceClassName;
		return(Resource)(Class.forName(clazz).getConstructor().newInstance());
	}

	@Override
	protected Resource doCreateInstance() throws Exception {
		return new TargetSystemImpl();
	}

	@Override
	public void notifyConfigurationRefresh() {
		super.notifyConfigurationRefresh();
		//TODO refresh TSS instances
	}

	public void setJobSubmissionEnabled(boolean enabled){
		this.jobSubmissionEnabled=enabled;
	}
	
	public boolean isJobSubmissionEnabled(){
		return jobSubmissionEnabled;
	}

	public String getHighMessage() {
		if(!jobSubmissionEnabled && highMessage.isEmpty()){
			return DEFAULT_MESSAGE;
		}
		return highMessage;
	}

	public void setHighMessage(String highMessage) {
		this.highMessage = highMessage;
	}
	
	

}
