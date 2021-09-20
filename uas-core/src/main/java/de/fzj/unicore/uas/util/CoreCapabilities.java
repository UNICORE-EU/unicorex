package de.fzj.unicore.uas.util;

import de.fzj.unicore.uas.fts.FileTransferCapabilities;
import de.fzj.unicore.uas.xnjs.UFileTransferCreator;
import de.fzj.unicore.xnjs.io.IFileTransferCreator;
import de.fzj.unicore.xnjs.io.IOCapabilities;
import eu.unicore.services.Capabilities;
import eu.unicore.services.Capability;

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
