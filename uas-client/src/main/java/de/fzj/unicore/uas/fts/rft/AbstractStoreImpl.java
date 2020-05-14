package de.fzj.unicore.uas.fts.rft;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import de.fzj.unicore.uas.client.ReliableFileTransferClient;
import de.fzj.unicore.uas.client.ReliableFileTransferClient.Chunk;

public abstract class AbstractStoreImpl implements ReliableFileTransferClient.Store{
	
	protected final long totalLength;
	protected final String target;
	protected final String partsDir;
	protected final String propertiesPath;
	protected final Properties properties=new Properties();
	
	protected final int chunkLength;
	
	protected boolean append=false;
	
	public static int minChunkLength=10*1024*1024;
	
	//the chunks directory name is built from the original filename and this suffix
	public static final String suffix=".unicore_rft.parts";

	public AbstractStoreImpl(String target, long totalLength){
		this.totalLength=totalLength;
		this.target=target;
		this.partsDir=target+suffix;
		this.propertiesPath=partsDir+"/properties";
		this.chunkLength=computeChunkLength(totalLength);
	}

	/**
	 * compute the chunk length according to the following rules<br/>
	 * <ul>
	 *  <li>not more than 100 chunks are created</li>
	 *  <li>the minumum size of a chunk is {@link #minChunkLength}</li>
	 * </ul>
	 * @param totalLength
	 */
	public static int computeChunkLength(long totalLength){
		int length=minChunkLength;
		int total=minChunkLength*100;
		if(total<totalLength){
			length+=(totalLength-total)/99;
		}
		return length;
	}
	
	@Override
	public List<Chunk> getChunks() throws Exception{
		List<Chunk>result=allChunks();
		Iterator<Chunk>iterator=result.iterator();
		while(iterator.hasNext()){
			if(!needToDownload(iterator.next()))iterator.remove();
		}
		return result;
	}

	@Override
	public void ack(Chunk chunk) throws Exception {
		//consistency check of written file
		boolean ok=checkOK(chunk);
		if(ok){
			properties.setProperty("chunk."+format(chunk.getIndex())+".status","done");
			writeProperties();
		}
		else{
			properties.setProperty("chunk."+format(chunk.getIndex())+".status","partial");
			writeProperties();
		}
	}
	
	/**
	 * check if the local file corresponding to the given chunk is OK
	 * 
	 * @param chunk
	 * @throws Exception
	 */
	protected abstract boolean checkOK(Chunk chunk)throws Exception;
	
	/**
	 * builds a String containing all the chunk file names separated 
	 * by spaces
	 */
	protected String buildFileList(){
		StringBuilder sb=new StringBuilder();
		for(String s: getFileNameList()){
			if(sb.length()>0){
				sb.append(" ");
			}
			sb.append(s);
		}
		return sb.toString();
	}
	
	protected List<String> getFileNameList(){
		List<String>result=new ArrayList<String>();
		int i=0;
		long pos=0;
		while(pos<totalLength){
			result.add("p_"+format(i));
			pos+=chunkLength;
			i++;
		}
		return result;
	}
	/**
	 * check if this chunk has to be (re-)downloaded
	 * @param chunk
	 */
	public boolean needToDownload(Chunk chunk){
		String status=properties.getProperty("chunk."+format(chunk.getIndex())+".status",null);
		if(status==null)return true;
		if("done".equals(status))return false;
		if("partial".equals(status)){
			try{
				long actualSize=getActualFileSize(chunk);
				if(actualSize>0){
					chunk.setOffset(actualSize);
					chunk.setLength(chunk.getLength()-actualSize);
					chunk.setAppend(true);
				}
			}catch(Exception ignored){}
		}
		return true;
	}
	
	/**
	 * get the actual size of the file, or -1 if file does not exist
	 * @param chunk
	 */
	protected abstract long getActualFileSize(Chunk chunk)throws Exception;
	
	/**
	 * format the given index. By default the format is four figures, zero-padded
	 *  
	 * @param index
	 */
	protected String format(int index){
		return String.format("%04d", index);
	}
	
	/**
	 * initialise persistent structure
	 * @throws Exception
	 */
	protected void init()throws Exception{
		createPartsDirIfNotExists();
		loadPropertiesFileIfExists();
	}
	
	protected List<Chunk> allChunks()throws Exception{
		init();
		List<Chunk>result=new ArrayList<Chunk>();
		int i=0;
		long pos=0;
		while(pos<totalLength){
			long length=Math.min(chunkLength, totalLength-pos);
			String path=partsDir+"/"+"p_"+format(i);
			Chunk c=createChunk(i, path, pos, length);
			result.add(c);
			pos+=chunkLength;
			i++;
		}
		return result;
	}
	
	/**
	 * create a concrete chunk implementation
	 * 
	 * @param index
	 * @param path
	 * @param offset
	 * @param length
	 */
	protected abstract Chunk createChunk(int index, String path, long offset, long length);
	
	/**
	 * create the parts dir, if it does not yet exist
	 */
	protected abstract void createPartsDirIfNotExists()throws Exception;
	
	/**
	 * load properties from persistent state, if it exists
	 * @throws Exception
	 */
	protected abstract void loadPropertiesFileIfExists()throws Exception;
	
	/**
	 * writes out the properties file
	 * @throws Exception
	 */
	protected void writeProperties()throws Exception{
		OutputStream os=getOutputStream(propertiesPath,false);
		try{
			properties.store(os, "auto-generated, DO NOT EDIT");
		}
		finally{
			os.close();
		}
	}
	
	/**
	 * create a local stream for writing data to
	 * 
	 * @param path - local path (relative to storage)
	 * @param append - whether to append
	 * @throws IOException
	 */
	protected abstract OutputStream getOutputStream(String path, boolean append)throws IOException;
	
}
