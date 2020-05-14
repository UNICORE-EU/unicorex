package de.fzj.unicore.xnjs.io;

public interface IOCapabilities {

	Class<? extends IFileTransferCreator>[] getFileTransferCreators();
	
}
