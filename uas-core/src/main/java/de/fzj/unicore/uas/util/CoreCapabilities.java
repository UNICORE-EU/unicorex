package de.fzj.unicore.uas.util;

import de.fzj.unicore.uas.fts.FileTransferCapabilities;
import de.fzj.unicore.uas.xnjs.UFileTransferCreator;
import de.fzj.unicore.wsrflite.Capabilities;
import de.fzj.unicore.wsrflite.Capability;
import de.fzj.unicore.xnjs.io.IFileTransferCreator;
import de.fzj.unicore.xnjs.io.IOCapabilities;

/**
 * lists the capabilities provided by UAS core
 * 
 * @author schuller
 */
public class CoreCapabilities implements Capabilities, IOCapabilities{

	@Override
	public Capability[] getCapabilities() {
		return new Capability[]{

				// REST

				FileTransferCapabilities.REST_BASE,
				FileTransferCapabilities.REST_UFTP,


				// SOAP / XML

				FileTransferCapabilities.SOAP_BFT,
				FileTransferCapabilities.SOAP_UFTP,
				// for compatibility, treat "u6" as BFT
				FileTransferCapabilities.U6,

		};
	}

	@Override
	@SuppressWarnings("unchecked")
	public Class<? extends IFileTransferCreator>[] getFileTransferCreators() {
		return new Class[]{
				UFileTransferCreator.class,
		};
	}

	
	
}
