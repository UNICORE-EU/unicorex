package eu.unicore.uas.fts;

import eu.unicore.services.InitParameters;
import eu.unicore.services.Resource;
import eu.unicore.services.exceptions.ResourceNotCreatedException;
import eu.unicore.services.impl.DefaultHome;
import eu.unicore.uas.fts.http.FileServlet;

/**
 * File transfer home<br/>.
 * 
 * The actual class created by {@link #doCreateInstance()} is looked up dynamically
 * in the {@link FileTransferCapabilities}.
 *
 * @author schuller
 */
public class FileTransferHomeImpl extends DefaultHome {

	//the actual protocol to be used is passed via this thread-local
	private final ThreadLocal<String>protocolT=new ThreadLocal<String>();

	/**
	 * Creates a FileTransferImpl for the given protocol<br/>
	 * If the protocol is null, a {@link ServerToServerFileTransferImpl} is created.
     */
	@Override
	protected Resource doCreateInstance()throws Exception{
		try{
			String protocol=protocolT.get();
			if(protocol==null){
				return new ServerToServerFileTransferImpl();
			}
			else return FileTransferCapabilities.getFileTransferImpl(protocol, getKernel());
		}
		finally{
			protocolT.remove();
		}
	}

	@Override
	public String createResource(InitParameters initobjs) throws ResourceNotCreatedException{
		FiletransferInitParameters ftInit = (FiletransferInitParameters)initobjs;
		if(ftInit.protocol!=null)protocolT.set(ftInit.protocol.toString());
		return super.createResource(initobjs);
	}

	/**
	 * Called after server start
	 */
	public void run(){
		initBFT();
	}

	protected void initBFT(){
		FileServlet.initialise(getKernel());
	}

}
