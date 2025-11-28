package eu.unicore.uas.jclouds;

import eu.unicore.services.Capabilities;
import eu.unicore.services.Capability;
import eu.unicore.xnjs.io.IFileTransferCreator;
import eu.unicore.xnjs.io.IOCapabilities;

/**
 * lists the capabilities provided by UAS core
 * 
 * @author schuller
 */
public class CloudCapabilities implements Capabilities, IOCapabilities{

	@Override
	public Capability[] getCapabilities() {
		return new Capability[0];
	}

	@Override
	@SuppressWarnings("unchecked")
	public Class<? extends IFileTransferCreator>[] getFileTransferCreators() {
		return new Class[]{
				CloudFileTransferCreator.class,
		};
	}

}