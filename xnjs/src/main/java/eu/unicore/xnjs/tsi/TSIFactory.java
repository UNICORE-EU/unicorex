package eu.unicore.xnjs.tsi;

import eu.unicore.security.Client;
import eu.unicore.xnjs.XNJS;

public class TSIFactory {
	
	private final XNJS xnjs;
	
	public TSIFactory(XNJS xnjs){
		this.xnjs = xnjs;
	}

	/**
	 * create a TSI
	 * 
	 * @param client - the client
	 */
	public TSI createTSI(Client client){
		TSI tsi = xnjs.get(TSI.class);
		tsi.setClient(client);
		return tsi;
	}
		
	/**
	 * create a TSI
	 * 
	 * @param client - the client
	 * @param preferredHost - the preferred TSI host (if applicable)
	 */
	public TSI createTSI(Client client, String preferredHost){
		TSI tsi = xnjs.get(TSI.class);
		tsi.setClient(client);
		if(tsi instanceof MultiNodeTSI){
			((MultiNodeTSI)tsi).setPreferredTSIHost(preferredHost);
		}
		return tsi;
	}
}
