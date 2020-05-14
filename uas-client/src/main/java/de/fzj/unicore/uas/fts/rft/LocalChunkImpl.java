package de.fzj.unicore.uas.fts.rft;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class LocalChunkImpl extends AbstractChunkImpl{

	private final File baseDir;
	
	public LocalChunkImpl(File baseDir, int index, String path, long offset, long length){
		super(index,path,offset,length);
		this.baseDir=baseDir;
	}

	@Override
	public OutputStream newOutputStream() throws Exception {
		return new FileOutputStream(new File(baseDir, path), append);
	}
	
}
