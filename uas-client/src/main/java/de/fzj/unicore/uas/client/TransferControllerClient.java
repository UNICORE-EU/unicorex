package de.fzj.unicore.uas.client;

import org.apache.log4j.Logger;
import org.unigrids.x2006.x04.services.fts.FileTransferPropertiesDocument;
import org.unigrids.x2006.x04.services.fts.SizeDocument;
import org.unigrids.x2006.x04.services.fts.StatusDocument;
import org.unigrids.x2006.x04.services.fts.SummaryType;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import org.unigrids.x2006.x04.services.fts.TransferredBytesDocument;

import eu.unicore.util.Log;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * monitor and manage a server-to-server file transfer
 * 
 * @author schuller
 */
public class TransferControllerClient extends BaseUASClient{
	
	private static final Logger logger=Log.getLogger(Log.CLIENT,TransferControllerClient.class);
	
	private long size=-1;
	
	public TransferControllerClient(String url, EndpointReferenceType epr, IClientConfiguration sec)throws Exception {
		super(url, epr, sec);
	}

	public TransferControllerClient(EndpointReferenceType epr, IClientConfiguration sec)throws Exception {
		super(epr.getAddress().getStringValue(), epr, sec);
	}

	/**
	 * @param url
	 * @param epr
	 */
	public TransferControllerClient(String url, EndpointReferenceType epr) throws Exception{
		this(url, epr, null);
	}

	public FileTransferPropertiesDocument getResourcePropertiesDocument()throws Exception{
		return FileTransferPropertiesDocument.Factory.parse(GetResourcePropertyDocument().getGetResourcePropertyDocumentResponse().newInputStream());
	}
	
	/**
	 * retrieve the number of bytes transferred
	 */
	public long getTransferredBytes(){
		try{
			TransferredBytesDocument rp=getSingleResourceProperty(TransferredBytesDocument.class);
			return rp.getTransferredBytes();
		}catch(Exception e){
			Log.logException("Can't get transferred bytes information.",e,logger);
		}
		return 0;
	}
	
	/**
	 * get the transfer rate as published by the server (or -1 if not known)
	 */
	public long getRate(){
		try{
			return getResourcePropertiesDocument().getFileTransferProperties().getTransferRate();
		}catch(Exception e){
			Log.logException("Can't get transfer rate information.",e,logger);
		}
		return -1;
	}

	/**
	 * retrieve the data size to be transferred
	 */
	public synchronized long getSize(){
		if(size==-1){
			try{
				SizeDocument sd=getSingleResourceProperty(SizeDocument.class);
				size=sd.getSize();
			}catch(Exception ex){
				Log.logException("Can't get size information.",ex,logger);
			}
		}
		return size;
	}
	

	/**
	 * get the (human-readable) status of this transfer
	 */
	public String getStatus(){
		try{
			StatusDocument status=getSingleResourceProperty(StatusDocument.class);
			return status.getStatus().getSummary().toString()+" ["+status.getStatus().getDescription()+"]";
		} catch (Exception e) {
			Log.logException("Could not get status.",e,logger);
		}
		return "n/a (Error getting status)";
	}
	
	/**
	 * get the file transfer status
	 * @throws Exception
	 */
	public SummaryType.Enum getStatusSummary()throws Exception{
			StatusDocument status=getSingleResourceProperty(StatusDocument.class);
			return status.getStatus().getSummary();
	}
	
	/**
	 * check whether the transfer has been completed (i.e. the status is DONE)
	 */
	public boolean isComplete(){
		try{
			return SummaryType.DONE.equals(getStatusSummary());
		}catch(Exception e){
			logger.error("Can't get information.",e);
		}
		return false;
	}
	
	public boolean hasFailed(){
		try{
			return SummaryType.FAILED.equals(getStatusSummary());
		}catch(Exception ex){
			return true;
		}
	}
	
}
