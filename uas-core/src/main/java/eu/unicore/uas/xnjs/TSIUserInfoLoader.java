package eu.unicore.uas.xnjs;

import eu.unicore.security.Client;
import eu.unicore.security.Xlogin;
import eu.unicore.services.Kernel;
import eu.unicore.services.rest.RESTUtils;
import eu.unicore.services.rest.security.UserPublicKeyCache.AttributesHolder;
import eu.unicore.services.rest.security.UserPublicKeyCache.UserInfoSource;
import eu.unicore.xnjs.tsi.TSI;
import eu.unicore.xnjs.tsi.UserInfoHolder;
import eu.unicore.xnjs.tsi.remote.RemoteTSI;

public class TSIUserInfoLoader implements UserInfoSource {

	private final Kernel kernel;

	public TSIUserInfoLoader(Kernel kernel) {
		this.kernel = kernel;
	}

	@Override
	public AttributesHolder getAttributes(String userName, String identityAssign) {
		AttributesHolder ah = new AttributesHolder(userName);
		try {
			Client c = new Client();
			c.setAnonymousClient();
			c.setXlogin(new Xlogin(new String[]{userName}));
			TSI tsi = XNJSFacade.get(null, kernel).getTSI(c);
			if(tsi instanceof RemoteTSI) {
				RemoteTSI rtsi = (RemoteTSI)tsi;
				UserInfoHolder ui = rtsi.getUserInfo();
				ah.getPublicKeys().addAll(ui.getPublicKeys());
				if(identityAssign!=null && ui.getAttributes()!=null && ui.getAttributes().size()>0) {
					ah.setDN(RESTUtils.evaluateToString(identityAssign, ui.getAttributes()));
				}
			}
		} catch(Exception ex) {}
		return ah;
	}

}