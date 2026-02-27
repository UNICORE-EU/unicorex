package eu.unicore.xnjs.tsi.remote.single;

import com.jcraft.jsch.JSch;

import eu.unicore.security.AuthorisationException;
import eu.unicore.security.Client;

/**
 * handles SSH identities (key pairs) to be able to connect to the back-end
 *
 * @author schuller
 */
public interface IdentityStore {

	/**
	 * configure the SSH connector with the identity for the given user
	 *
	 * @param jsch
	 * @param client
	 * @throws AuthorisationException if no identity is available for the given user
	 */
	public void addIdentity(JSch jsch, Client client) throws AuthorisationException;

	/**
	 * register a key pair for the given Unix user
	 *
	 * @param user
	 * @param keyPair
	 */
	public void register(String user, KeyPairHolder keyPair); 

	/**
	 * register an identity resolver that is used to get/update identities for users
	 * @param identityResolver
	 */
	public void registerResolver(IdentityResolver identityResolver); 

	public static class KeyPairHolder {

		public final byte[] privateKey;
		public final byte[] publicKey;
		public final byte[] passphrase;
		private final ValidityCheck checker;

		public KeyPairHolder (byte[] privateKey, byte[] publicKey, byte[] passphrase, ValidityCheck checker){
			this.privateKey = privateKey;
			this.publicKey = publicKey;
			this.passphrase = passphrase;
			this.checker = checker;
		}

		public boolean isValid() {
			return checker!=null? checker.isValid(this) :  true;
		}

	}

	public static interface ValidityCheck {
		public boolean isValid(KeyPairHolder kp);
	}
}