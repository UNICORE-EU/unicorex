package de.fzj.unicore.uas.fts;

import de.fzj.unicore.uas.xnjs.UFileTransferCreator;
import de.fzj.unicore.xnjs.fts.IFTSController;
import de.fzj.unicore.xnjs.io.IFileTransfer;
import eu.unicore.services.Capability;

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

	public default Class<? extends IFTSController> getFTSController() { return null; }
	
}
