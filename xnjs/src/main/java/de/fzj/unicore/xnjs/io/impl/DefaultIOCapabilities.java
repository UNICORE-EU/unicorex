package de.fzj.unicore.xnjs.io.impl;

import de.fzj.unicore.xnjs.io.IFileTransferCreator;
import de.fzj.unicore.xnjs.io.IOCapabilities;

public class DefaultIOCapabilities implements IOCapabilities {

	@Override
	@SuppressWarnings("unchecked")
	public Class<? extends IFileTransferCreator>[] getFileTransferCreators() {
		return new Class[]{
			DefaultTransferCreator.class,
		};
	}

}
