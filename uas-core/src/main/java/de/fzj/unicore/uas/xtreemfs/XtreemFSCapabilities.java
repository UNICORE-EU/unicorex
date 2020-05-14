package de.fzj.unicore.uas.xtreemfs;

import de.fzj.unicore.xnjs.io.IFileTransferCreator;
import de.fzj.unicore.xnjs.io.IOCapabilities;

public class XtreemFSCapabilities implements IOCapabilities{

	@Override
	@SuppressWarnings("unchecked")
	public Class<? extends IFileTransferCreator>[] getFileTransferCreators() {
		return new Class[]{
				XtreemFSFileTransferCreator.class,
		};
	}
	
}
