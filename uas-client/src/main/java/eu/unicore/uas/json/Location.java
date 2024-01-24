package eu.unicore.uas.json;


/**
 * A local or remote file location
 * @author schuller
 */
public interface Location {

	
	/**
	 * is this a local file?
	 *
	 * @return true if this is a local file
	 */
	public boolean isLocal();
	
	/**
	 * does this location denote a UNICORE storage
	 *  
	 * @return <code>true</code> if it is a UNICORE URL
	 */
	public boolean isUnicoreURL();

	/**
	 * does this location denote a "raw" storage, i.e. not a UNICORE storage
	 * but something like a FTP URL?
	 *  
	 * @return true if it is a raw URL
	 */
	public boolean isRaw();
	
	/**
	 * return the physical endpoint URL for accessing the file
	 */
	public String getEndpointURL();

}
