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
	 * @param preferredHost - the preferred TSI host (if applicable, can be null to use any host)
	 */
	public TSI createTSI(Client client, String preferredHost){
		if(client==null)throw new NullPointerException("Client cannot be null");
		TSI tsi = xnjs.get(TSI.class);
		tsi.setClient(client);
		if(tsi instanceof MultiNodeTSI){
			((MultiNodeTSI)tsi).setPreferredTSIHost(preferredHost);
		}
		return tsi;
	}
}
