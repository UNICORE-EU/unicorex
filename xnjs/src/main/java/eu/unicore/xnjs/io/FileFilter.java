package eu.unicore.xnjs.io;

import eu.unicore.xnjs.tsi.TSI;

/**
 * this interface allows to filter a list of {@link XnjsFile}s.
 * 
 * @author schuller
 */
public interface FileFilter {

	/**
	 * should the find operation recurse into sub-directories?
	 */
	public boolean recurse();
	
	/**
	 * should the given {@link XnjsFile} be included in the result list?
	 * 
	 * @param file - the {@link XnjsFile} under consideration
	 * @param tsi -  the current TSI instance
	 */
	public boolean accept(XnjsFile file, TSI tsi);
	
}
