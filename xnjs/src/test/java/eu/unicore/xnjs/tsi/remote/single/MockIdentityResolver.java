package eu.unicore.xnjs.tsi.remote.single;

import eu.unicore.security.Client;
import eu.unicore.xnjs.XNJS;

public class MockIdentityResolver implements IdentityResolver {

	@Override
	public void updateIdentities(Client client, IdentityStore store) {
		// NOP
	}

	public void setXNJS(XNJS xnjs) {}
	public void setInt(int param) {}
	public void setInteger(Integer param) {}
	public void setLong(long param) {}
	public void setLong2(Long param) {}
	public void setString(String param) {}
	public void setBoolean(boolean param) {}
	public void setBoolean2(Boolean param) {}

}
