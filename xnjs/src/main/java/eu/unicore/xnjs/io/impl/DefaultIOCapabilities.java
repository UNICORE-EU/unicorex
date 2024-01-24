package eu.unicore.xnjs.io.impl;

import eu.unicore.xnjs.io.IFileTransferCreator;
import eu.unicore.xnjs.io.IOCapabilities;

public class DefaultIOCapabilities implements IOCapabilities {

	@Override
	@SuppressWarnings("unchecked")
	public Class<? extends IFileTransferCreator>[] getFileTransferCreators() {
		return new Class[]{
			DefaultTransferCreator.class,
		};
	}

}
