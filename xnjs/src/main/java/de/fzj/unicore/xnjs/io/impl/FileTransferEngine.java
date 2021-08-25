/*********************************************************************************
 * Copyright (c) 2006 Forschungszentrum Juelich GmbH 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * (1) Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the disclaimer at the end. Redistributions in
 * binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 * 
 * (2) Neither the name of Forschungszentrum Juelich GmbH nor the names of its 
 * contributors may be used to endorse or promote products derived from this 
 * software without specific prior written permission.
 * 
 * DISCLAIMER
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *********************************************************************************/


package de.fzj.unicore.xnjs.io.impl;

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

import com.hazelcast.topic.ITopic;
import com.hazelcast.topic.Message;
import com.hazelcast.topic.MessageListener;

import de.fzj.unicore.persist.Persist;
import de.fzj.unicore.persist.PersistenceException;
import de.fzj.unicore.persist.PersistenceFactory;
import de.fzj.unicore.persist.PersistenceProperties;
import de.fzj.unicore.persist.impl.PersistImpl;
import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.fts.FTSInfo;
import de.fzj.unicore.xnjs.fts.IFTSController;
import de.fzj.unicore.xnjs.io.DataStageInInfo;
import de.fzj.unicore.xnjs.io.DataStageOutInfo;
import de.fzj.unicore.xnjs.io.IFileTransfer;
import de.fzj.unicore.xnjs.io.IFileTransferCreator;
import de.fzj.unicore.xnjs.io.IFileTransferEngine;
import de.fzj.unicore.xnjs.io.IOCapabilities;
import de.fzj.unicore.xnjs.io.TransferInfo;
import de.fzj.unicore.xnjs.io.UnsupportedProtocolException;
import de.fzj.unicore.xnjs.util.LogUtil;
import eu.unicore.security.Client;

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
public class FileTransferEngine implements IFileTransferEngine, MessageListener<FileTransferEngine.FileTransferEvent> {

	private static final Logger logger=LogUtil.getLogger(LogUtil.IO,FileTransferEngine.class);

	private final List<IFileTransferCreator>creators;

	private final List<String>protocols;

	private final XNJS xnjs;

	private final PersistenceProperties persistenceProperties;
	
	private final Map<String,IFileTransfer> ftMap = new ConcurrentHashMap<>();
	
	private final Map<String,TransferInfo> ftInfo;

	@Inject
	public FileTransferEngine(XNJS xnjs) {
		this.xnjs = xnjs;
		this.persistenceProperties = xnjs.getPersistenceProperties();
		creators = new ArrayList<>();
		protocols = new ArrayList<>();
		ftInfo = createInfoMap();
		loadExtensions();
	}
	
	private Map<String,TransferInfo> createInfoMap(){
		if(xnjs.isClusterEnabled()){
			getHZTopic().addMessageListener(this);
			return xnjs.getCluster().getMap("XNJS.filetransferinfo", String.class, TransferInfo.class);
		}
		else{
			return new ConcurrentHashMap<>();
		}
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
	 * Creates a new file import into the action's working directory.<br/>
	 * The list of registered {@link IFileTransferCreator}s is traversed and the first
	 * non-null result is returned.
	 */
	public IFileTransfer createFileImport(Client client, String workingDirectory, DataStageInInfo info) 
	throws IOException{
		for(IFileTransferCreator c: creators){
			IFileTransfer f=c.createFileImport(client,workingDirectory,info);
			if(f!=null){
				f.setOverwritePolicy(info.getOverwritePolicy());
				f.setImportPolicy(info.getImportPolicy());
				registerFileTransfer(f);
				return f;
			}
		}
		throw new UnsupportedProtocolException("Transfer "+info+
		" uses unsupported protocol(s)");
	}

	/**
	 * Creates a new file export from the actions working directory.<br/>
	 * The list of registered {@link IFileTransferCreator}s is traversed and the first
	 * non-null result is returned.
	 * 
	 */
	public IFileTransfer createFileExport(Client client, String workingDirectory, DataStageOutInfo info) 
	throws IOException{
		for(IFileTransferCreator c: creators){
			IFileTransfer f=c.createFileExport(client,workingDirectory,info);
			if(f!=null){
				f.setOverwritePolicy(info.getOverwritePolicy());
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
				f.setOverwritePolicy(info.getOverwritePolicy());
				f.setImportPolicy(info.getImportPolicy());
				return f;
			}
		}
		throw new UnsupportedProtocolException("Transfer "+info+
				" uses unsupported protocol(s)");
	}

	@Override
	public IFTSController createFTSExport(Client client, String workingDirectory, DataStageOutInfo info) 
	throws IOException{
		for(IFileTransferCreator c: creators){
			IFTSController f = c.createFTSExport(client,workingDirectory,info);
			if(f!=null){
				f.setOverwritePolicy(info.getOverwritePolicy());
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
				logger.debug("Added <"+creator+"> for protocol "+creator.getProtocol());
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
		if(xnjs.isClusterEnabled()){
			FileTransferEvent message = new FileTransferEvent("abort", id);
			getHZTopic().publish(message);
		}
		else{
			ftMap.get(id).abort();
		}
	}
	
	protected ITopic<FileTransferEvent> getHZTopic(){
		return xnjs.getCluster().getHazelcast().getTopic("XNJS.filetransfer.events");
	}
	
	public void onMessage(Message<FileTransferEvent> message){
		FileTransferEvent event = message.getMessageObject();
		IFileTransfer ft = ftMap.get(event.id);
		if(ft!=null){
			if("abort".equals(event.type)){
				ft.abort();
			}
		}
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
