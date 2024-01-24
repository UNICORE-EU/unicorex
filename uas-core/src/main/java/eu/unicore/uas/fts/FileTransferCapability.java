package eu.unicore.uas.fts;

import eu.unicore.services.Capability;
import eu.unicore.uas.xnjs.UFileTransferCreator;
import eu.unicore.xnjs.fts.IFTSController;
import eu.unicore.xnjs.io.IFileTransfer;

public interface FileTransferCapability extends Capability{

	/**
	 * the protocol provided by the FT implementation
	 */
	public String getProtocol();
	
	/**
	 * return the class providing imports
	 *
	 * @see IFileTransfer
	 * @see UFileTransferCreator
	 */
	public Class<? extends IFileTransfer> getImporter();

	/**
	 * return the class providing exports
	 * 
	 * @see IFileTransfer
	 * @see UFileTransferCreator
	 */
	public Class<? extends IFileTransfer> getExporter();

	public default Class<? extends IFTSController> getFTSImportsController() { return null; }
	
	public default Class<? extends IFTSController> getFTSExportsController() { return null; }
	
}
