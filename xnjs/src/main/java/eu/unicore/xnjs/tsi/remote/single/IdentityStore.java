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
	 * @param privateKey
	 * @param publicKey
	 * @param passphrase
	 */
	public void register(String user, byte[]privateKey, byte[]publicKey, byte[] passphrase); 

}