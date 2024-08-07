package eu.unicore.uas.metadata;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import eu.unicore.services.Kernel;
import eu.unicore.uas.UAS;
import eu.unicore.xnjs.ems.ExecutionException;
import eu.unicore.xnjs.io.IStorageAdapter;
import eu.unicore.xnjs.io.XnjsFileImpl;
import eu.unicore.xnjs.io.XnjsFileWithACL;

/**
 * Metadata tests: unit functionality
 *
 * @author jrybicki
 */
public class TestMetadata {

	private static final String configPath = "src/test/resources/uas.config";

	static Kernel kernel;

	@BeforeAll
	public static void init() throws Exception {
		kernel = initK();
	}

	public static Kernel initK() throws Exception {
		System.out.println("Initalizing class test");
		String lucenePath = "target/luceneData/" + System.currentTimeMillis();
		File data = new File(lucenePath);
		FileUtils.deleteQuietly(data);
		FileUtils.deleteQuietly(new File("target","data"));

		File f = new File("target/data/teststorage");
		if (!f.exists()) {
			f.mkdirs();
		}
		UAS uas=new UAS(configPath);
		uas.startSynchronous();
		kernel=uas.getKernel();
		MetadataProperties cfg = kernel.getAttribute(MetadataProperties.class);
		cfg.setProperty(MetadataProperties.LUCENE_INDEX_DIR, data.getAbsolutePath() + File.separator);
		return kernel;
	}

	@AfterAll
	public static void shutDown() throws Exception{
		shutDown(kernel);
	}

	public static void shutDown(Kernel kernel) throws Exception{
		kernel.shutdown();
		FileUtils.deleteQuietly(new File("target", "data"));
	}

	@Test
	public void testSetS() throws IOException, ExecutionException {
		assertThrows(IllegalStateException.class, ()->{
			LuceneMetadataManager manager = new LuceneMetadataManager(kernel);
			manager.copyResourceMetadata("/correctPath", "correctPath");
		});
	}

	@Test
	public void testCreateBadMetadata() throws IOException, ExecutionException {
		assertThrows(IllegalArgumentException.class, ()->{
			LuceneMetadataManager manager = getManager();
			manager.createMetadata("/resource", null);
		});
	}

	@Test
	public void testCreateBadMetadata2() throws IOException, ExecutionException {
		assertThrows(IllegalArgumentException.class, ()->{
			LuceneMetadataManager manager = getManager();
			manager.createMetadata("/resource", new HashMap<String, String>());
		});
	}

	@Test
	public void testCreateBadMetadata3() throws IOException, ExecutionException {
		assertThrows(IllegalArgumentException.class, ()->{

			LuceneMetadataManager manager = getManager();
			Map<String, String> meta = new HashMap<String, String>();
			meta.put("test", "123");
			manager.createMetadata("", meta);
		});
	}

	@Test
	public void testSetStorage() throws IOException {
		assertThrows(IllegalStateException.class, ()->{

			LuceneMetadataManager manager = new LuceneMetadataManager(kernel);
			manager.setStorageAdapter(null, null);
		});
	}

	@Test
	public void testRename1() throws IOException, ExecutionException {
		assertThrows(IllegalArgumentException.class, ()->{
			LuceneMetadataManager manager = getManager();
			manager.renameResource(null, "/bla");
		});
	}

	@Test
	public void noMovetoNull() throws IOException, ExecutionException {
		assertThrows(IllegalArgumentException.class, ()->{
			LuceneMetadataManager manager = getManager();
			manager.renameResource("/bla", null);
		});
	}

	private IStorageAdapter getMock() throws ExecutionException {
		IStorageAdapter storage = mock(IStorageAdapter.class);
		when(storage.getFileSeparator()).thenReturn("/");
		return storage;
	}

	private LuceneMetadataManager getManager() throws ExecutionException {
		LuceneMetadataManager manager = new LuceneMetadataManager(kernel);
		manager.setStorageAdapter(getMock(), null);
		return manager;
	}



	@Test
	public void nonExistingCannotbeRenamed() throws ExecutionException, IOException {
		LuceneMetadataManager manager = new LuceneMetadataManager(kernel);
		IStorageAdapter storage = mock(IStorageAdapter.class);
		when(storage.getFileSeparator()).thenReturn("/");
		String soruce = "Source.file";
		String target = "Target.file";
		manager.setStorageAdapter(storage, "ID");
		assertThrows(IllegalArgumentException.class, ()->{
			manager.renameResource(soruce, target);
			fail("Moved non existing metadata");
		});
	}

	@Test
	public void testRename5() throws ExecutionException, IOException {
		LuceneMetadataManager manager = new LuceneMetadataManager(kernel);
		IStorageAdapter storage = mock(IStorageAdapter.class);
		when(storage.getFileSeparator()).thenReturn("/");
		String source = "Source.file";
		String target = "Target.file";
		manager.setStorageAdapter(storage, "ID");
		Map<String, String> map = new HashMap<String, String>();
		map.put("key", "value");
		OutputStream stream = mock(OutputStream.class);
		when(storage.getOutputStream(anyString(), anyBoolean())).thenReturn(stream);
		when(storage.getProperties(MetadataFile.getMetadatafileName(source))).thenReturn(new XnjsFileImpl());
		XnjsFileWithACL mockedFile = mock(XnjsFileWithACL.class);
		when(storage.getProperties(source)).thenReturn(mockedFile);
		manager.createMetadata(source, map);

		manager.renameResource(source, target);
		verify(storage, times(1)).rename(anyString(), anyString());
		verify(stream).write((byte[]) any());
		verify(storage).getOutputStream(anyString(), anyBoolean());
	}

	@Test
	public void testCopyResourceMetadata1() throws IOException, ExecutionException {
		assertThrows(IllegalArgumentException.class, ()->{
			LuceneMetadataManager manager = getManager();
			manager.copyResourceMetadata("", "/ProperName");
		});
	}

	@Test
	public void testCopyResourceMetadata2() throws IOException, ExecutionException {
		assertThrows(IllegalArgumentException.class, ()->{
			LuceneMetadataManager manager = getManager();
			manager.copyResourceMetadata("/ProperName", null);
		});
	}

	@Test
	public void testCopyResourceMetadata3() throws IOException, ExecutionException {
		assertThrows(IllegalArgumentException.class, ()->{
			LuceneMetadataManager manager = getManager();
			manager.copyResourceMetadata("/ProperName", "");
		});
	}

	@Test
	public void testUpdate() throws ExecutionException, IOException {
		assertThrows(IllegalStateException.class, ()->{
			LuceneMetadataManager manager = new LuceneMetadataManager(kernel);
			manager.updateMetadata("resource", new HashMap<String, String>());
		});
	}

	@Test
	public void testUpdate2() throws ExecutionException, IOException {
		assertThrows(IllegalArgumentException.class, ()->{
			LuceneMetadataManager manager = getManager();
			manager.updateMetadata(null, new HashMap<>());
		});
	}

	@Test
	public void testUpdate3() throws ExecutionException, IOException {
		assertThrows(IllegalArgumentException.class, ()->{
			LuceneMetadataManager manager = getManager();
			manager.updateMetadata("", new HashMap<>());
		});
	}

	@Test
	public void testUpdate4() throws ExecutionException, IOException {
		assertThrows(IllegalArgumentException.class, ()->{	
			LuceneMetadataManager manager = getManager();
			manager.updateMetadata("/properResource", null);
		});
	}

	@Test
	public void testUpdate5() throws ExecutionException, IOException {
		assertThrows(IllegalArgumentException.class, ()->{
			LuceneMetadataManager manager = getManager();
			manager.updateMetadata("/properResource", new HashMap<String, String>());
		});
	}
}
