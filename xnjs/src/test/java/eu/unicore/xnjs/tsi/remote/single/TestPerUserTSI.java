package eu.unicore.xnjs.tsi.remote.single;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import eu.unicore.persist.util.UUID;
import eu.unicore.uftp.dpc.Utils;
import eu.unicore.xnjs.tsi.remote.RemoteTSI;
import eu.unicore.xnjs.tsi.remote.TSIConnectionFactory;

public class TestPerUserTSI extends PerUserTSITestCase {

	@Test
	public void testSetup() throws Exception {
		assertNotNull(xnjs.get(PerUserTSIProperties.class));
		assertNotNull(makeTSI());
	}

	@Test
	public void testPing() throws Exception {
		PerUserTSIConnection tC = (PerUserTSIConnection)xnjs.get(TSIConnectionFactory.class).
				getTSIConnection("nobody", "foo", null, -1);
		String reply = tC.send("#TSI_PING");
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