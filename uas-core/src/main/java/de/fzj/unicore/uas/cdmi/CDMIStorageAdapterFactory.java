package de.fzj.unicore.uas.cdmi;

import java.io.IOException;

import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.fts.FileTransferModel;
import de.fzj.unicore.uas.impl.BaseResourceImpl;
import de.fzj.unicore.uas.xnjs.StorageAdapterFactory;
import eu.emi.security.authn.x509.helpers.BinaryCertChainValidator;
import eu.unicore.services.Kernel;
import eu.unicore.services.Model;
import eu.unicore.services.rest.client.IAuthCallback;
import eu.unicore.util.httpclient.ClientProperties;

/**
 * Creates and configures the CDMI connector
 * 
 * @author schuller
 */
public class CDMIStorageAdapterFactory implements StorageAdapterFactory {

	@Override
	public CDMIStorageAdapter createStorageAdapter(BaseResourceImpl parent)
			throws IOException {
		// load the model of the correct SMS resource: parent can be a file transfer
		Model m = parent.getModel();
		if(m instanceof FileTransferModel){
			try{
				String uid = ((FileTransferModel)m).getParentUID();
				m = parent.getKernel().getHome(UAS.SMS).get(uid).getModel();
			}catch(Exception ex){
				throw new IOException(ex);
			}
		}
		CDMIModel model = (CDMIModel)m;
		String endpoint = model.getEndpoint();
		Kernel kernel = parent.getKernel();
		IAuthCallback authCallback = createAuthCallback(model,kernel);
		return createStorageAdapter(kernel, authCallback, endpoint);
	}

	public CDMIStorageAdapter createStorageAdapter(Kernel kernel, IAuthCallback authCallback, String endpoint)
			throws IOException {
		ClientProperties cp = kernel.getClientConfiguration().clone();
		cp.setSslAuthn(false);
		cp.setSslEnabled(endpoint.startsWith("https"));
		cp.setValidator(new BinaryCertChainValidator(true));
		CDMIClient cdmi = new CDMIClient(endpoint, cp, authCallback);
		return new CDMIStorageAdapter(cdmi);
	}

	public IAuthCallback createAuthCallback(CDMIModel model, Kernel kernel){
		String username = model.getUsername();
		String password = model.getPassword();
		String tokenEndpoint = model.getTokenEndpoint();
		IAuthCallback authCallback = tokenEndpoint!=null ? 
				new KeystoneAuth(tokenEndpoint, username, password, kernel) : 
				new BasicAuth(username, password);
		return authCallback;
	}

}
