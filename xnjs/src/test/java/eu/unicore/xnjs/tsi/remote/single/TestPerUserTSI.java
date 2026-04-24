package eu.unicore.xnjs.tsi.remote.single;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import eu.unicore.persist.util.UUID;
import eu.unicore.security.Client;
import eu.unicore.uftp.dpc.Utils;
import eu.unicore.xnjs.tsi.remote.RemoteTSI;
import eu.unicore.xnjs.tsi.remote.TSIConnectionFactory;
import eu.unicore.xnjs.tsi.remote.TSIMessages;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;

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
	}

	@Test
	public void testPing() throws Exception {
		PerUserTSIConnection tC = (PerUserTSIConnection)xnjs.get(TSIConnectionFactory.class).
				getTSIConnection(TSIMessages.createMinimalClient("nobody"), null, -1);
		String reply = TSIMessages.trim(tC.send("#TSI_PING"));
		System.out.println("TSI PING reply: " + reply);
		assertTrue(reply.contains("10."));
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