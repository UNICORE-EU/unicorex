package eu.unicore.uas.xnjs;

import eu.unicore.services.Kernel;
import eu.unicore.services.utils.ExternalConnectorHelper;
import eu.unicore.uas.UASProperties.TSI_MODE;
import eu.unicore.util.Log;
import eu.unicore.util.Pair;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.tsi.remote.TSIConnectionFactory;

public class TSIStatusChecker extends ExternalConnectorHelper {

	private final XNJS xnjs;

	private final TSI_MODE mode;

	public TSIStatusChecker(XNJS xnjs, TSI_MODE mode) {
		this.xnjs = xnjs;
		this.mode = mode;
		setExternalSystemName("TSI "+xnjs.getID());
		setCheckService(xnjs.get(Kernel.class).getExecutorService());
		setCheckSupplier(()->checkConnection());
	}

	private Pair<Boolean, String> checkConnection(){
		boolean ok = true;
		String msg = null;
		try{
			if(TSI_MODE.embedded.equals(mode)){
				msg = "OK [using embedded TSI]";
			}
			else if(TSI_MODE.remote.equals(mode)){
				TSIConnectionFactory tsif = xnjs.get(TSIConnectionFactory.class);
				msg = tsif.getConnectionStatus();
				ok = msg.startsWith("OK");
			}
			else{
				msg = "N/A";
			}
		}catch(Exception ex){
			ok = false;
			msg = Log.getDetailMessage(ex);
		}
		return new Pair<>(ok, msg);
	}

}
