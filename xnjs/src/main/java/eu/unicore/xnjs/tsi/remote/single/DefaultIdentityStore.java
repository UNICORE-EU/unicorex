package eu.unicore.xnjs.tsi.remote.single;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.Logger;

import com.hierynomus.sshj.userauth.keyprovider.OpenSSHKeyV1KeyFile;

import eu.unicore.security.AuthorisationException;
import eu.unicore.security.Client;
import eu.unicore.util.Log;
import jakarta.inject.Singleton;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import net.schmizz.sshj.userauth.password.PasswordFinder;
import net.schmizz.sshj.userauth.password.Resource;

@Singleton
public class DefaultIdentityStore implements IdentityStore {

	private static final Logger logger = Log.getLogger(Log.SECURITY, DefaultIdentityStore.class);

	// user name mapped to key pair
	private final Map<String, KeyPairHolder> keys = new ConcurrentHashMap<>();

	private final Set<IdentityResolver> resolvers = new HashSet<>();

	public DefaultIdentityStore() {}

	@Override
	public KeyProvider getIdentity(Client client) throws AuthorisationException {
		String user = client.getSelectedXloginName();
		if(user==null) {
			throw new AuthorisationException("Required Unix user is null.");
		}
		if(keys.get(user)==null) {
			// try our resolvers
			runUpdate(client);
		}
		final KeyPairHolder kp = keys.get(user);
		if(kp==null) {
			throw new AuthorisationException("No key available for '"+user+"'");
		}
		OpenSSHKeyV1KeyFile keyProvider = new OpenSSHKeyV1KeyFile();
		keyProvider.init(kp.privateKey, kp.publicKey, new PasswordFinder() {

			@Override
			public boolean shouldRetry(Resource<?> resource) {
				return false;
			}

			@Override
			public char[] reqPassword(Resource<?> resource) {
				return kp.passphrase;
			}
		});
		return keyProvider;
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
