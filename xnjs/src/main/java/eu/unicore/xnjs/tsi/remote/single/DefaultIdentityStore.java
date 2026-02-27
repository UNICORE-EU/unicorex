package eu.unicore.xnjs.tsi.remote.single;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Logger;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;

import eu.unicore.security.AuthorisationException;
import eu.unicore.security.Client;
import eu.unicore.util.Log;
import jakarta.inject.Singleton;

@Singleton
public class DefaultIdentityStore implements IdentityStore {

	private static final Logger logger = Log.getLogger(Log.SECURITY, DefaultIdentityStore.class);

	// user name mapped to key pair
	private final Map<String, KeyPairHolder> keys = new HashMap<>();

	private final Set<IdentityResolver> resolvers = new HashSet<>();

	public DefaultIdentityStore() {}

	@Override
	public void addIdentity(JSch jsch, Client client) throws AuthorisationException {
		String user = client.getSelectedXloginName();
		if(user==null) {
			throw new AuthorisationException("Required Unix user is null.");
		}
		KeyPairHolder kp = keys.get(user);
		if(kp==null) {
			// try our resolvers
			runUpdate(client);
			kp = keys.get(user);
		}
		if(kp==null) {
			throw new AuthorisationException("No key available for '"+user+"'");
		}
		try {
			jsch.addIdentity(user, kp.privateKey, kp.publicKey, kp.passphrase);
		}catch(JSchException j) {
			throw new AuthorisationException("Can't configure ssh authentication for '"
					+user+"'", j);
		}
	}

	@Override
	public void register(String user, KeyPairHolder keyPair) {
		keys.put(user, keyPair);
	}

	@Override
	public void registerResolver(IdentityResolver resolver) {
		resolvers.add(resolver);
	}

	private void runUpdate(Client client) {
		for(IdentityResolver r: resolvers) {
			try {
				r.updateIdentities(client, this);
			}catch(Exception e) {
				logger.debug("Error running identity resolver {}: {}", r, e);
			}
		}
	}

	Collection<IdentityResolver>getResolvers(){
		return Collections.unmodifiableCollection(resolvers);
	}
}
