package de.fzj.unicore.uas.xnjs;

import java.util.Collections;
import java.util.List;

import de.fzj.unicore.xnjs.tsi.TSI;
import de.fzj.unicore.xnjs.tsi.remote.RemoteTSI;
import eu.unicore.security.Client;
import eu.unicore.security.Xlogin;
import eu.unicore.services.Kernel;
import eu.unicore.services.KernelInjectable;
import eu.unicore.services.rest.security.UserPublicKeyCache.UserInfoSource;

public class TSIUserInfoLoader implements UserInfoSource, KernelInjectable {

	private Kernel kernel;
	
	public TSIUserInfoLoader(Kernel kernel) {
		this.kernel = kernel;
	}
	
	public void setKernel(Kernel kernel) {
		this.kernel = kernel;
	}

	@Override
	public List<String> getAcceptedKeys(String userName) {
		try {
			Client c = new Client();
			c.setAnonymousClient();
			c.setXlogin(new Xlogin(new String[]{userName}));
			TSI tsi = XNJSFacade.get(null, kernel).getTargetSystemInterface(c);
			if(tsi instanceof RemoteTSI) {
				RemoteTSI rTSI = (RemoteTSI)tsi;
				return rTSI.getUserPublicKeys();
			}
		} catch(Exception ex) {}
		return Collections.emptyList();
	}

}