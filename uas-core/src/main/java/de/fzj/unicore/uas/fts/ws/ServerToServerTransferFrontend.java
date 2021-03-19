package de.fzj.unicore.uas.fts.ws;

import java.util.Calendar;

import javax.xml.namespace.QName;

import org.unigrids.x2006.x04.services.fts.FileTransferPropertiesDocument;
import org.unigrids.x2006.x04.services.fts.ScheduledStartTimeDocument;

import de.fzj.unicore.uas.fts.FileTransfer;
import de.fzj.unicore.uas.fts.ServerToServerFileTransferImpl;
import de.fzj.unicore.uas.impl.UASBaseFrontEnd;
import de.fzj.unicore.wsrflite.xmlbeans.renderers.ValueRenderer;


/**
 * WS-Resource for initiating and monitoring
 * a server-to-server file transfer
 * 
 * the source parameter is a UNICORE URI 
 * the target is the local file (relative to storage root)
 * 
 * @author schuller
 */
public class ServerToServerTransferFrontend extends UASBaseFrontEnd implements FileTransfer  {

	public static final String PARAM_SCHEDULED_START="scheduledStartTime";

	private final ServerToServerFileTransferImpl resource;
	
	public ServerToServerTransferFrontend(ServerToServerFileTransferImpl r){
		super(r);
		this.resource = r;
		addRenderer(new TransferRateResourceProperty(resource));
		addRenderer(new ValueRenderer(resource, ScheduledStartTimeDocument.type.getDocumentElementName()) {
			@Override
			protected ScheduledStartTimeDocument getValue() throws Exception {
				long scheduledStartTime = resource.getModel().getScheduledStartTime();
				if(scheduledStartTime==0)return null;
				ScheduledStartTimeDocument res=ScheduledStartTimeDocument.Factory.newInstance();
				Calendar cal=Calendar.getInstance();
				cal.setTimeInMillis(scheduledStartTime);
				res.setScheduledStartTime(cal);
				return res;
			}
		});
	}
	
	private static final QName portType=new QName("http://unigrids.org/2006/04/services/fts","FileTransfer");

	public QName getPortType(){
		return portType;
	}
	
	@Override
	public QName getResourcePropertyDocumentQName() {
		return FileTransferPropertiesDocument.type.getDocumentElementName();
	}
}
