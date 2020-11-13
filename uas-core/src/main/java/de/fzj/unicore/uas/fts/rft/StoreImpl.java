package de.fzj.unicore.uas.fts.rft;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.uas.client.ReliableFileTransferClient.Chunk;
import de.fzj.unicore.uas.util.LogUtil;
import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.io.XnjsFile;
import de.fzj.unicore.xnjs.tsi.TSI;
import de.fzj.unicore.xnjs.util.AsyncCommandHelper;
import eu.unicore.security.Client;

public class StoreImpl extends AbstractStoreImpl {
	
	private static final Logger logger=LogUtil.getLogger(LogUtil.DATA,StoreImpl.class);
	
	final TSI storage;
	final Client client;
	final XNJS config;
	
	public StoreImpl(Client client,TSI storage, XNJS config, String target, long totalLength){
		super(target,totalLength);
		this.storage=storage;
		this.client=client;
		this.config=config;
	}

	@Override
	protected boolean checkOK(Chunk chunk) throws Exception {
		boolean ok=false;
		XnjsFile f=storage.getProperties(chunk.getPath());
		logger.debug("Ack: "+chunk+" have "+f);
		if(f!=null){
			ok=chunk.getLength()==f.getSize();
		}
		return ok;
	}

	public void finish()throws Exception{
		if(properties.getProperty("merge.complete")==null){
			String cmd="/bin/cat "+buildFileList();
			AsyncCommandHelper ach=new AsyncCommandHelper(config, cmd, "merge_files", null, client, storage.getStorageRoot()+"/"+partsDir+"/");
			ach.setStdout("../../"+target);
			ach.submit();
			while(!ach.isDone()){
				Thread.sleep(2000);
			}
			if(!ach.getResult().getResult().isSuccessful()){
				throw new Exception("Merge was not successful.");
			}
		}
		properties.setProperty("merge.complete","true");
		writeProperties();
		storage.rmdir(partsDir);
	}
	
	@Override
	protected long getActualFileSize(Chunk chunk)throws Exception{
		XnjsFile f=storage.getProperties(chunk.getPath());
		if(f!=null){
			return f.getSize();
		}
		else{
			return -1;
		}
	}
	
	@Override
	protected void createPartsDirIfNotExists()throws Exception{
		XnjsFile parts=storage.getProperties(target+suffix);
		if(parts==null){
			storage.mkdir(partsDir);
		}
	}
	
	@Override
	protected void loadPropertiesFileIfExists()throws Exception{
		XnjsFile propFile=storage.getProperties(propertiesPath);
		if(propFile!=null){
			InputStream is=storage.getInputStream(propertiesPath);
			try{
				properties.load(is);
			}finally{
				is.close();
			}
		}
	}
	
	@Override
	protected OutputStream getOutputStream(String path, boolean append)throws IOException{
		try{
			return storage.getOutputStream(path,append);
		}catch(ExecutionException ex){
			throw new IOException(ex);
		}
	}
	
	@Override
	protected Chunk createChunk(int index, String path, long offset, long length){
		return new ChunkImpl(index, storage, path, offset, length);
	}
}
