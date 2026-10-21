package eu.unicore.xnjs.tsi.remote.single;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import eu.unicore.persist.util.UUID;
import eu.unicore.security.AuthorisationException;
import eu.unicore.security.Client;
import eu.unicore.uftp.dpc.Utils;
import eu.unicore.xnjs.tsi.remote.RemoteTSI;
import eu.unicore.xnjs.tsi.remote.TSIConnectionFactory;
import eu.unicore.xnjs.tsi.remote.TSIMessages;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import net.schmizz.sshj.userauth.password.PasswordFinder;

public class TestPerUserTSI extends PerUserTSITestCase {

	@Test
	public void testSetup() throws Exception {
		assertNotNull(xnjs.get(PerUserTSIProperties.class));
		PerUserTSIConnectionFactory factory = (PerUserTSIConnectionFactory)xnjs.get(TSIConnectionFactory.class);
		assertEquals(1, factory.getTSIHosts().size());
		assertEquals(getTSIMachine(), factory.getTSIMachine());
		assertNotNull(factory.getTSIProperties());
		System.out.println(factory.getConnectionStatus());
		System.out.println(factory.getTSIConnectorStates());
		assertNotNull(factory.createNewTSIConnection(TSIMessages.createMinimalClient("nobody"), "127.0.0.1"));
		assertNotNull(makeTSI());
	}

	@Test
	public void testIdentityResolver() throws Exception {
		DefaultIdentityStore ids = (DefaultIdentityStore)xnjs.get(IdentityStore.class);
		List<IdentityResolver> resolvers = ids.getResolvers();
		assertEquals(2, resolvers.size());
		assertTrue(resolvers.get(0) instanceof FileIdentityResolver);
		assertTrue(resolvers.get(1) instanceof MockIdentityResolver);
		FileIdentityResolver fir = (FileIdentityResolver)resolvers.get(0);
		int numRead = fir.update(ids);
		assertEquals(2, numRead);

		Client c = TSIMessages.createMinimalClient("nobody");
		KeyProvider kp = ids.getIdentity(c);
		assertNotNull(kp);

		Client c1 = TSIMessages.createMinimalClient("nobody2");
		KeyProvider kp1 = ids.getIdentity(c1);
		Field f = kp1.getClass().getSuperclass().getDeclaredField("pwdf");
		f.setAccessible(true);
		PasswordFinder pf = (PasswordFinder)f.get(kp1);
		assertFalse(pf.shouldRetry(null));
		char[] pwd = pf.reqPassword(null);
		assertEquals("just-for-testing", new String(pwd));

		Client c2 = TSIMessages.createMinimalClient("unknown-user");
		AuthorisationException ae = assertThrows(AuthorisationException.class, ()->{ ids.getIdentity(c2); });
		assertTrue(ae.getMessage().contains("No key available"));
		assertTrue(ae.getMessage().contains("unknown-user"));

		Client c3 = TSIMessages.createMinimalClient(null);
		AuthorisationException ae2 = assertThrows(AuthorisationException.class, ()->{ ids.getIdentity(c3); });
		assertTrue(ae2.getMessage().contains("Required Unix user is null."));
	}

	@Test
	public void testPing() throws Exception {
		PerUserTSIConnection tC = (PerUserTSIConnection)xnjs.get(TSIConnectionFactory.class).
				getTSIConnection(TSIMessages.createMinimalClient("nobody"), null, 10, TimeUnit.SECONDS);
		String reply = TSIMessages.trim(tC.send("#TSI_PING"));
		System.out.println("TSI PING reply: " + reply);
		assertTrue(reply.contains("11."));
	}

	@Test
	public void testGetFile() throws Exception {
		RemoteTSI tsi = makeTSI();
		File x = new File("src/test/resources/tsi/conf/tsi.properties");
		String expected = Utils.md5(x);
		byte[]fileContent = new byte[(int)x.length()];
		IOUtils.readFully(tsi.getInputStream(x.getAbsolutePath()), fileContent);
		assertEquals(expected, Utils.md5(fileContent));
	}

	@Test
	public void testPutFile() throws Exception {
		RemoteTSI tsi = makeTSI();
		File x = new File("src/test/resources/tsi/conf/tsi.properties");
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		IOUtils.copy(new FileInputStream(x), os);
		byte[]fileContent = os.toByteArray();
		File target = new File(mkTmpDir(), "foo.txt");
		OutputStream remote = tsi.getOutputStream(target.getAbsolutePath());
		IOUtils.copy(new ByteArrayInputStream(fileContent), remote);
		remote.close();
		assertEquals(Utils.md5(x), Utils.md5(target));
	}

	private File mkTmpDir(){
		File f=new File("target","xnjs_test_"+UUID.newUniqueID());
		if(!f.exists())f.mkdirs();
		return f;
	}
}