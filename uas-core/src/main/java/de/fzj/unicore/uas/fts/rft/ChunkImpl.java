package de.fzj.unicore.uas.fts.rft;

import java.io.OutputStream;

import de.fzj.unicore.xnjs.io.IStorageAdapter;

public class ChunkImpl extends AbstractChunkImpl {

	private final IStorageAdapter storage;
		
	public ChunkImpl(int index, IStorageAdapter storage, String path, long offset, long length) {
		super(index,path,offset,length);
		this.storage = storage;
	}
	
	@Override
	public OutputStream newOutputStream() throws Exception{
		return storage.getOutputStream(getPath(),append);
	}

}
