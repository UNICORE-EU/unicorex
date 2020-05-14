package de.fzj.unicore.xnjs.tsi;

import de.fzj.unicore.xnjs.XNJS;
import eu.unicore.security.Client;

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
