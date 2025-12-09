package eu.unicore.uas.fts.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import org.junit.jupiter.api.Test;

import eu.unicore.persist.util.UUID;
import eu.unicore.security.Client;
import eu.unicore.services.messaging.PullPoint;
import eu.unicore.uas.Base;
import eu.unicore.uas.xnjs.XNJSFacade;
import eu.unicore.xnjs.io.IStorageAdapter;

public class TestUResource extends Base {

	@Test
	public void testUResource()throws Exception {
		Client c = new Client();
		IStorageAdapter tsi = XNJSFacade.get(null, kernel).getStorageTSI("/", c, null);
		File rootFile = new File(".");
		String root = rootFile.getAbsolutePath();
		try(UResource r = new UResource("test-uresource1", root, tsi, kernel)){
			assertEquals(root, r.getName());
			assertEquals(rootFile.lastModified(), r.lastModified());
			assertEquals(rootFile.length(), r.length());
			// invoke all the dummy methods
			assertNull(r.addPath(""));
			assertFalse(r.delete());
			assertTrue(r.exists());
			assertNull(r.getAlias());
			assertNull(r.getFile());
			assertNull(r.getReadableByteChannel());
			assertNull(r.getURI());
			assertFalse(r.isContainedIn(null));
			assertFalse(r.isDirectory());
			assertNull(r.list());
			assertFalse(r.renameTo(null));
		}
	}

	@Test
	public void testErrors()throws Exception {
		Client c = new Client();
		IStorageAdapter tsi = XNJSFacade.get(null, kernel).getStorageTSI("/", c, null);

		// test read error
		File rootFile = new File("./target/no_such_file_"+UUID.newUniqueID());
		String root = rootFile.getAbsolutePath();
		String rname = "test-uresource2";
		try(UResource r = new UResource(rname, root, tsi, kernel)){
			IOException e = assertThrows(IOException.class, ()->r.getInputStream());
			assertTrue(e.getMessage().contains("no_such_file"));
			Thread.sleep(100); // message writing is async
		}
		// check message on Kernel messaging channel
		PullPoint pp = kernel.getMessaging().getPullPoint(rname);
		assertTrue(pp.hasNext());
		String msg = pp.next().getBody();
		assertTrue(msg.contains("no_such_file"));

		// test write error
		rootFile = new File("/foo/no_such_file_"+UUID.newUniqueID());
		root = rootFile.getAbsolutePath();
		try(UResource r = new UResource(rname, root, tsi, kernel)){
			IOException e = assertThrows(IOException.class, ()->{
				OutputStream o = r.getOutputStream();
				o.write("test".getBytes());
			});
			assertTrue(e.getMessage().toLowerCase().contains("no such file"));
			Thread.sleep(100); // message writing is async
		}
		// check message on Kernel messaging channel
		PullPoint pp2 = kernel.getMessaging().getPullPoint(rname);
		assertTrue(pp2.hasNext());
		msg = pp2.next().getBody();
		assertTrue(msg.toLowerCase().contains("no such file"));
	}
}