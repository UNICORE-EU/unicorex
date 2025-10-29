package eu.unicore.uas.xnjs;

import java.util.Collections;
import java.util.List;

import eu.unicore.security.Client;
import eu.unicore.security.Xlogin;
import eu.unicore.services.Kernel;
import eu.unicore.services.rest.security.UserPublicKeyCache.UserInfoSource;
import eu.unicore.xnjs.tsi.TSI;
import eu.unicore.xnjs.tsi.remote.RemoteTSI;

public class TSIUserInfoLoader implements UserInfoSource {

	private final Kernel kernel;

	public TSIUserInfoLoader(Kernel kernel) {
		this.kernel = kernel;
	}

	@Override
	public List<String> getAcceptedKeys(String userName) {
		try {
			Client c = new Client();
			c.setAnonymousClient();
			c.setXlogin(new Xlogin(new String[]{userName}));
			TSI tsi = XNJSFacade.get(null, kernel).getTSI(c);
			if(tsi instanceof RemoteTSI) {
				return ((RemoteTSI)tsi).getUserInfo().getPublicKeys();
			}
		} catch(Exception ex) {}
		return Collections.emptyList();
	}

}