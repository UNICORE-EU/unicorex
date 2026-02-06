package eu.unicore.xnjs.tsi.remote.single;

import eu.unicore.security.Client;

public interface IdentityResolver {

	public void updateIdentities(Client client, IdentityStore store);

}
