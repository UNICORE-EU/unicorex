package eu.unicore.xnjs.tsi.remote.single;

import eu.unicore.security.AuthorisationException;
import eu.unicore.security.Client;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;

/**
 * handles SSH identities (key pairs) to be able to connect to the back-end
 *
 * @author schuller
 */
public interface IdentityStore {

	/**
	 * get a valid key for the given user
	 *
	 * @param client
	 * @throws AuthorisationException if no key is available for the given user
	 */
	public KeyProvider getIdentity(Client client) throws AuthorisationException;

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

		public final String privateKey;
		public final String publicKey;
		public final char[] passphrase;
		private final ValidityCheck checker;

		public KeyPairHolder (String privateKey, String publicKey, char[] passphrase, ValidityCheck checker){
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