package eu.unicore.uas.xnjs;

import eu.unicore.services.ExternalSystemConnector;
import eu.unicore.uas.UASProperties.TSI_MODE;
import eu.unicore.util.Log;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.tsi.remote.TSIConnectionFactory;

public class TSIStatusChecker implements ExternalSystemConnector {

	private final XNJS xnjs;
	private final TSI_MODE mode;

	private Status status=Status.UNKNOWN;

	private String statusMessage;

	private String name;

	private long lastChecked;

	public TSIStatusChecker(XNJS xnjs, TSI_MODE mode) {
		this.xnjs = xnjs;
		this.mode = mode;
	}

	@Override
	public String getConnectionStatusMessage() {
		checkConnection();
		return statusMessage;
	}

	@Override
	public Status getConnectionStatus() {
		checkConnection();
		return status;
	}

	@Override
	public String getExternalSystemName() {
		checkConnection();
		return name;
	}

	private void checkConnection(){
		if (lastChecked+2000>System.currentTimeMillis())
			return;
		name = "TSI "+xnjs.getID();
		try{
			if(TSI_MODE.embedded.equals(mode)){
				statusMessage = "OK [using embedded TSI]";
				status = Status.OK;
			}
			else if(TSI_MODE.remote.equals(mode)){
				TSIConnectionFactory tsif=xnjs.get(TSIConnectionFactory.class);
				statusMessage = tsif.getConnectionStatus();
				status = statusMessage.startsWith("OK")? Status.OK : Status.DOWN;
			}
			else{
				statusMessage = "N/A";
				status = Status.OK;
			}
		}catch(Exception ex){
			status=Status.DOWN;
			statusMessage = Log.createFaultMessage("Error! ",ex);
		}
		
		lastChecked=System.currentTimeMillis();
	}

}
