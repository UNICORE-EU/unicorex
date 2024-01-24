package eu.unicore.xnjs.io.impl;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.logging.log4j.Logger;

import eu.unicore.persist.Persist;
import eu.unicore.persist.PersistenceException;
import eu.unicore.persist.PersistenceFactory;
import eu.unicore.persist.PersistenceProperties;
import eu.unicore.persist.impl.PersistImpl;
import eu.unicore.security.Client;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.fts.FTSInfo;
import eu.unicore.xnjs.fts.IFTSController;
import eu.unicore.xnjs.io.DataStageInInfo;
import eu.unicore.xnjs.io.DataStageOutInfo;
import eu.unicore.xnjs.io.IFileTransfer;
import eu.unicore.xnjs.io.IFileTransferCreator;
import eu.unicore.xnjs.io.IFileTransferEngine;
import eu.unicore.xnjs.io.IOCapabilities;
import eu.unicore.xnjs.io.TransferInfo;
import eu.unicore.xnjs.io.UnsupportedProtocolException;
import eu.unicore.xnjs.util.LogUtil;

/**
 * Implementation of the {@link IFileTransferEngine} interface.<br/> 
 * It supports pluggable protocols.<br/>
 * It supports clustering, i.e. filetransfer threads need not be local to this XNJS instance.<br/>
 *
 * To add a protocol, it can either be registered directly via the {@link #registerFileTransferCreator(IFileTransferCreator)}
 * method, or by using the ServiceLoader mechanism. This works by adding
 * a file named 'de.fzj.unicore.xnjs.io.IOCapabilities' in the META-INF/services folder
 * in your jar file. This must contain a line with the name of a class implementing 
 * {@link IOCapabilities} 
 * 
 * @author schuller
 */
@Singleton
public class FileTransferEngine implements IFileTransferEngine{

	private static final Logger logger=LogUtil.getLogger(LogUtil.IO,FileTransferEngine.class);

	private final List<IFileTransferCreator>creators;

	private final List<String>protocols;

	private final XNJS xnjs;

	private final PersistenceProperties persistenceProperties;
	
	private final Map<String,IFileTransfer> ftMap = new ConcurrentHashMap<>();
	
	private final Map<String,TransferInfo> ftInfo = new ConcurrentHashMap<>();

	@Inject
	public FileTransferEngine(XNJS xnjs) {
		this.xnjs = xnjs;
		this.persistenceProperties = xnjs.getPersistenceProperties();
		creators = new ArrayList<>();
		protocols = new ArrayList<>();
		loadExtensions();
	}

	private synchronized void loadExtensions(){
		try{
			ServiceLoader<IOCapabilities> sl=ServiceLoader.load(IOCapabilities.class);
			Iterator<IOCapabilities>iter=sl.iterator();
			while(iter.hasNext()){
				IOCapabilities c=iter.next();
				Class<? extends IFileTransferCreator>[]classes=c.getFileTransferCreators();
				if(classes==null || classes.length==0){
					logger.warn("Plugin definition <"+c.getClass().getName()+"> does not define any file transfers.");
				}
				else{
					for(Class<? extends IFileTransferCreator> clazz: classes){
						IFileTransferCreator ftc=clazz.getConstructor(XNJS.class).newInstance(xnjs);
						registerFileTransferCreator(ftc);
					}
				}
			}
			logger.info("Loaded XNJS data staging protocol support: "+Arrays.toString(listProtocols()));
		}catch(Exception ex){
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Creates a new file import into the action's working directory
	 */
	public IFileTransfer createFileImport(Client client, String workingDirectory, DataStageInInfo info) 
	throws IOException{
		for(IFileTransferCreator c: creators){
			IFileTransfer f=c.createFileImport(client,workingDirectory,info);
			if(f!=null){
				registerFileTransfer(f);
				return f;
			}
		}
		throw new UnsupportedProtocolException("Transfer "+info+" uses unsupported protocol(s)");
	}

	/**
	 * Creates a new file export from the actions working directory
	 */
	public IFileTransfer createFileExport(Client client, String workingDirectory, DataStageOutInfo info) 
	throws IOException{
		for(IFileTransferCreator c: creators){
			IFileTransfer f=c.createFileExport(client,workingDirectory,info);
			if(f!=null){
				registerFileTransfer(f);
				return f;
			}
		}
		throw new UnsupportedProtocolException("Transfer "+info+" uses unsupported protocol(s)");
	}

	@Override
	public IFTSController createFTSImport(Client client, String workingDirectory, DataStageInInfo info) 
			throws IOException{
		for(IFileTransferCreator c: creators){
			IFTSController f = c.createFTSImport(client,workingDirectory,info);
			if(f!=null){
				return f;
			}
		}
		throw new UnsupportedProtocolException("Transfer "+info+" uses unsupported protocol(s)");
	}

	@Override
	public IFTSController createFTSExport(Client client, String workingDirectory, DataStageOutInfo info) 
	throws IOException{
		for(IFileTransferCreator c: creators){
			IFTSController f = c.createFTSExport(client,workingDirectory,info);
			if(f!=null){
				return f;
			}
		}
		throw new UnsupportedProtocolException("Transfer "+info+" uses unsupported protocol(s)");
	}

	private Persist<FTSInfo> ftsStorage;
	
	@Override
	public synchronized Persist<FTSInfo> getFTSStorage() throws PersistenceException {
		if(ftsStorage==null) {
			ftsStorage = (PersistImpl<FTSInfo>)PersistenceFactory.get(persistenceProperties).getPersist(FTSInfo.class);
		}
		return ftsStorage;
	}
	
	public synchronized void registerFileTransferCreator(IFileTransferCreator creator) {
		if(!creators.contains(creator)){
			creators.add(creator);
			String p=creator.getProtocol();
			if(!protocols.contains(p)){
				protocols.add(p);
				logger.debug("Added <{}> for protocol {}", creator, creator.getProtocol());
			}
		}
		order();
	}

	final Comparator<IFileTransferCreator> comp = new Comparator<IFileTransferCreator>(){
		@Override
		public int compare(IFileTransferCreator o1, IFileTransferCreator o2) {
			return Integer.compare(o2.getPriority(),o1.getPriority());
		}
	}; 
	
	// order ft creators by priority
	private void order() {
		Collections.sort(creators, comp);
	}

	public synchronized String[] listProtocols() {
		return (String[]) protocols.toArray(new String[0]);
	}

	public synchronized IFileTransfer getFiletransfer(String id) {
		return ftMap.get(id);
	}

	public synchronized TransferInfo getInfo(String id) {
		return ftInfo.get(id);
	}
	
	public synchronized void updateInfo(TransferInfo info) {
		ftInfo.put(info.getUniqueId(), info);
	}
	
	public synchronized void registerFileTransfer(IFileTransfer ft) {
		TransferInfo fti = ft.getInfo();
		ftMap.put(fti.getUniqueId(), ft);
		ftInfo.put(fti.getUniqueId(), fti);
		fti.setFileTransferEngine(this);
	}
	
	public synchronized void cleanup(String id) {
		ftMap.remove(id);
		ftInfo.remove(id);
	}

	public synchronized void abort(String id) {
		ftMap.get(id).abort();
	}

	public static class FileTransferEvent implements Serializable {
		
		private static final long serialVersionUID = 1L;
		
		public String type, id;
		
		public FileTransferEvent(String type, String id) {
			this.type=type;
			this.id=id;
		}
	}
}
