package eu.unicore.xnjs.tsi.remote.single;

import java.util.HashMap;
import java.util.Map;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;

import eu.unicore.security.AuthorisationException;
import eu.unicore.util.Pair;
import jakarta.inject.Singleton;

@Singleton
public class DefaultidentityStore implements IdentityStore {

	// user name mapped to Pair with private&public key
	private final Map<String, Pair<byte[], byte[]>> keys = new HashMap<>();

	private final Map<String, byte[]> passphrases = new HashMap<>();

	public DefaultidentityStore() {}

	@Override
	public void addIdentity(JSch jsch, String user) throws AuthorisationException {
		Pair<byte[], byte[]> kp = keys.get(user);
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

}