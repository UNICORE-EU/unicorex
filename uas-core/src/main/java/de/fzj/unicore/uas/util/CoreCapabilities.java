package de.fzj.unicore.uas.util;

import de.fzj.unicore.uas.fts.FileTransferCapabilities;
import de.fzj.unicore.uas.xnjs.UFileTransferCreator;
import eu.unicore.services.Capabilities;
import eu.unicore.services.Capability;
import eu.unicore.xnjs.io.IFileTransferCreator;
import eu.unicore.xnjs.io.IOCapabilities;

/**
 * lists the capabilities provided by UAS core
 * 
 * @author schuller
 */
public class CoreCapabilities implements Capabilities, IOCapabilities{

	@Override
	public Capability[] getCapabilities() {
		return new Capability[]{
				FileTransferCapabilities.REST_BASE,
				FileTransferCapabilities.REST_UFTP,
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
