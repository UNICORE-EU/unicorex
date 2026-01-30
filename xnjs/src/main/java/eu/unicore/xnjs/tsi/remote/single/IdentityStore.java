package eu.unicore.xnjs.tsi.remote.single;

import com.jcraft.jsch.JSch;

import eu.unicore.security.AuthorisationException;

public interface IdentityStore {

	public void addIdentity(JSch jsch, String user) throws AuthorisationException;

	public void register(String user, byte[]privateKey, byte[]publicKey, byte[] passphrase); 

}