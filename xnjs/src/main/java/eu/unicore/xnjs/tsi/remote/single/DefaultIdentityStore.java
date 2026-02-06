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
import eu.unicore.util.Pair;
import jakarta.inject.Singleton;

@Singleton
public class DefaultIdentityStore implements IdentityStore {

	private static final Logger logger = Log.getLogger(Log.SECURITY, DefaultIdentityStore.class);

	// user name mapped to Pair with private&public key
	private final Map<String, Pair<byte[], byte[]>> keys = new HashMap<>();

	private final Map<String, byte[]> passphrases = new HashMap<>();

	private final Set<IdentityResolver> resolvers = new HashSet<>();
	
	public DefaultIdentityStore() {}

	@Override
	public void addIdentity(JSch jsch, Client client) throws AuthorisationException {
		String user = client.getSelectedXloginName();
		if(user==null) {
			throw new AuthorisationException("Required Unix user is null.");
		}
		Pair<byte[], byte[]> kp = keys.get(user);
		if(kp==null) {
			// try our resolvers
			runUpdate(client);
			kp = keys.get(user);
		}
		if(kp==null) {
			throw new AuthorisationException("No key available for '"+user+"'");
		}
		byte[] pf = passphrases.get(user);
		try {
			jsch.addIdentity(user, kp.getM1(), kp.getM2(), pf);
		}catch(JSchException j) {
			throw new AuthorisationException("Can't configure ssh authentication for '"
					+user+"'", j);
		}
	}

	@Override
	public void register(String user, byte[]privateKey, byte[]publicKey, byte[] passphrase) {
		keys.put(user, new Pair<>(privateKey, publicKey));
		if(passphrase!=null) {
			passphrases.put(user, passphrase);
		}
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
