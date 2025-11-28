package eu.unicore.uas.trigger.xnjs;

import java.io.Serializable;
import java.util.Arrays;

/**
 * XNJS action description holding information related to periodic 
 * directory scans
 *
 * @author schuller
 */
public class ScanSettings implements Serializable {

	private static final long serialVersionUID = 1L;

	// can be used to temporarily disable scanning
	public boolean enabled = true;

	// storage unique ID
	public String storageUID;

	// (approximate) directory scan interval in seconds;
	public int updateInterval =  60;

	// to prevent processing a file that is still being written
	public int gracePeriod = 10;

	// base of the scan, relative to storage root
	public String baseDirectory = "/";

	// directory patterns to include from recursion
	public String[] includes = new String[0];

	// directory patterns to exclude from recursion
	public String[] excludes = new String[0];

	// limit on directory levels for recursion 
	public int maxDepth=10;

	public boolean sharedStorageMode = false;

	public boolean enableLogging = true;

	@Override
	public String toString(){
		return "Scan for storage " + storageUID + " (shared="+sharedStorageMode+")"+
				" enabled=" + enabled +
				" baseDir=" + baseDirectory +
				" updateInterval=" + updateInterval +
				" includes=" + Arrays.asList(includes) +
				" excludes=" + Arrays.asList(excludes);
	}

}