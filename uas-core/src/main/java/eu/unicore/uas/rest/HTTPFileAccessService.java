package eu.unicore.uas.rest;

import java.util.HashSet;
import java.util.Set;

import eu.unicore.security.OperationType;
import eu.unicore.services.Kernel;
import eu.unicore.services.rest.USERestApplication;
import eu.unicore.services.security.pdp.DefaultPDP;
import eu.unicore.services.security.pdp.DefaultPDP.Rule;
import eu.unicore.services.security.pdp.PDPResult.Decision;
import jakarta.ws.rs.core.Application;

/**
 * @author schuller
 */
public class HTTPFileAccessService extends Application implements USERestApplication {

	@Override
	public void initialize(Kernel kernel) throws Exception {
		DefaultPDP pdp = DefaultPDP.get(kernel);
		if(pdp!=null) {
			pdp.setServiceRules("files",
					DefaultPDP.PERMIT_READ, PERMIT_WRITE);
		}
		// TODO
		HTTPFileAccess.kernel = kernel;
	}

	@Override
	public Set<Class<?>> getClasses() {
		Set<Class<?>>classes  = new HashSet<>();
		classes.add(HTTPFileAccess.class);
		return classes;
	}

	
	private static final Rule PERMIT_WRITE = (c,a,d)-> {
		if(OperationType.write==a.getActionType()) {
			return Decision.PERMIT;
		}
		return Decision.UNCLEAR;
	};
}
