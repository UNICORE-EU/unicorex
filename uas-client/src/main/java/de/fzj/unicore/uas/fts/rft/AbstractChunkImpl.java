package de.fzj.unicore.uas.fts.rft;

import de.fzj.unicore.uas.client.ReliableFileTransferClient;

public abstract class AbstractChunkImpl implements ReliableFileTransferClient.Chunk {

	protected final String path;

	protected long offset;

	protected long length;
	
	protected boolean append;
	
	protected final int index;
	
	public AbstractChunkImpl(int index, String path, long offset, long length) {
		this.index=index;
		this.path = path;
		this.offset = offset;
		this.length = length;
	}

	@Override
	public long getLength() {
		return length;
	}

	@Override
	public long getOffset() {
		return offset;
	}

	public int getIndex(){
		return index;
	}
	
	public String toString(){
		return "Chunk["+offset+"-"+(offset+length-1)+" "+path+"]";
	}
	
	public String getPath(){
		return path;
	}
	
	public void setOffset(long offset){
		this.offset=offset;
	}
	
	public void setLength(long length){
		this.length=length;
	}

	public void setAppend(boolean append){
		this.append=append;
	}
}
