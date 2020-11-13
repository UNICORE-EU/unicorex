package de.fzj.unicore.uas.xtreemfs;

import java.io.OutputStream;
import java.net.URI;

import org.apache.logging.log4j.Logger;
import org.unigrids.services.atomic.types.ProtocolType;

import de.fzj.unicore.uas.client.FileTransferClient;
import de.fzj.unicore.uas.client.StorageClient;
import de.fzj.unicore.uas.impl.sms.SMSUtils;
import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.io.TransferInfo.Status;
import de.fzj.unicore.xnjs.tsi.TSI;
import eu.unicore.security.Client;
import eu.unicore.util.Log;

/**
 * download data from XtreemFS
 * 
 * @author schuller
 */
public class XtreemFSDownload extends XtreemFSTransferBase{
	
	private static final Logger logger=Log.getLogger(Log.SERVICES+".xtreemfs", XtreemFSDownload.class);
	
	private final String workdir;
	private final URI source;
	private final String target;
	
	public XtreemFSDownload(XNJS configuration, Client client, String workdir, URI source, String target){
		super(configuration,client);
		this.source=source;
		this.target=target;
		this.workdir=workdir;
		info.setTarget(target);
		info.setSource(source.toString());
	
	}

	@Override
	public void run() {
		info.setStatus(Status.RUNNING);
		try{
			String baseDir=configuration.getProperty(XTREEMFS_LOCAL_MOUNT);
			if(baseDir==null){
				baseDir=xtreemProperties.getValue(XtreemProperties.XTREEMFS_LOCAL_MOUNT);
			}
			if(baseDir!=null){
				importLocally(baseDir);
			}
			else{
				downloadFromRemote();
			}
			info.setStatus(Status.DONE);
		}catch(Exception ex){
			info.setStatus(Status.FAILED, Log.createFaultMessage("File import failed.", ex));
		}
	}

	protected void importLocally(String baseDir)throws Exception{
		if(baseDir==null)throw new IllegalStateException("No local XtreemFS mountpoint defined.");
		String realSource=baseDir+SMSUtils.urlDecode(makeSource());
		String realTarget=workdir+"/"+target;
		ensureDirectoriesExist(realTarget);
		//just copy file to remote ...
		TSI tsi=configuration.getTargetSystemInterface(client);
		tsi.cp(realSource, realTarget);
		logger.info("Copied: "+realSource+" to "+realTarget);
	}
	
	String makeSource(){
		return source.getRawSchemeSpecificPart();
	}
	
	protected void downloadFromRemote()throws Exception{
		StorageClient sms=createStorageClient();
		logger.info("Downloading from remote SMS "+sms.getUrl());
		FileTransferClient ftc=sms.getExport(makeSource(), ProtocolType.BFT);
		TSI tsi=configuration.getTargetSystemInterface(client);
		tsi.setStorageRoot(workdir);
		OutputStream os=tsi.getOutputStream(target);
		try{
			ftc.readAllData(os);
		}
		finally{
			try{
				ftc.destroy();
			}catch(Exception ex){}
			try{
				os.close();
			}catch(Exception ex){}
		}
	}
	
}
