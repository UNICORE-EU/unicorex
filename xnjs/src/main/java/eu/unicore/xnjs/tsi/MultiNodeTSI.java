package eu.unicore.xnjs.tsi;


/**
 * allows to get/set the preferred TSI host
 * 
 * @author schuller
 */
public interface MultiNodeTSI extends TSI{

	public void setPreferredTSIHost(String host);
	
	/**
	 * if multiple TSI nodes exist, this method will return the one that was
	 * used with the last successful command 
	 */
	public String getLastUsedTSIHost();
	
}
