package eu.unicore.uas.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.junit.jupiter.api.Test;

import eu.unicore.services.Kernel;
import eu.unicore.services.security.TestConfigUtil;
import eu.unicore.xnjs.ems.ExecutionException;
import eu.unicore.xnjs.io.IStorageAdapter;
import eu.unicore.xnjs.io.XnjsFileWithACL;

public class TestUtils {

	private static final Map<String, String> standardMetadata = new HashMap<>();

	static {
		standardMetadata.put("StandardKey", "StandardValue");
	}

	@Test
	public void testExtractMetadata() throws Exception {
		String resourceName = "resource";
		String conents = "Some contents";

		Document doc = LuceneIndexer.createMetadataDocument(standardMetadata, resourceName, conents);
		assertNotNull(doc);
		assertFalse(doc.getFields().isEmpty());
		assertEquals(resourceName, doc.get(LuceneIndexer.RESOURCE_NAME_KEY));

		Map<String, String> meta = LuceneIndexer.extractMetadataFromDocument(doc);
		assertFalse(meta.isEmpty());
		for (String key : standardMetadata.keySet()) {
			assertTrue(meta.containsKey(key));
		}

		for (String value : standardMetadata.values()) {
			assertTrue(meta.containsValue(value));
		}

	}

	@Test
	public void testMerge() {
		Map<String, String> old = new HashMap<String, String>();
		Map<String, String> ne = new HashMap<String, String>();

		String commonKey = "Common Key";
		String oldValue = "OldValue4Common";

		old.put("OldKey", "OldValue");
		old.put(commonKey, oldValue);
		old.put("OldKey2", "OldValue2");

		assertEquals(3, old.size());
		assertTrue(old.containsKey(commonKey));
		assertTrue(old.containsValue(oldValue));

		String newValue = "NewValue4Common";
		ne.put("NewKey", "NewValue");
		ne.put("NewKey2", "NewValue2");
		ne.put(commonKey, newValue);
		ne.put("NewKey3", "NewValue3");

		assertEquals(4, ne.size());
		assertTrue(ne.containsKey(commonKey));
		assertTrue(ne.containsValue(newValue));
		assertFalse(ne.containsValue(oldValue));

		Map<String, String> merged = LuceneMetadataManager.mergeMetadata(old, ne);
		assertEquals(6, merged.size());
		assertTrue(merged.containsKey(commonKey));
		assertTrue(merged.containsValue(newValue));
		assertFalse(merged.containsValue(oldValue));
	}

	@Test
	public void resourceNameCannotbeNull() throws Exception {
		assertThrows(IllegalArgumentException.class, ()->{
			LuceneMetadataManager manager = new LuceneMetadataManager(new Kernel(TestConfigUtil.getInsecureProperties()));
			manager.isProperResource(null);
		});
	}

	@Test
	public void resourceNameCannotBeEmpty() throws Exception {
		assertThrows(IllegalArgumentException.class, ()->{
			LuceneMetadataManager manager = new LuceneMetadataManager(new Kernel(TestConfigUtil.getInsecureProperties()));
			manager.isProperResource("");
		});
	}

	@Test
	public void resourceNameCannotEndWithMD() throws Exception {
		assertThrows(IllegalArgumentException.class, ()->{
			LuceneMetadataManager manager = new LuceneMetadataManager(new Kernel(TestConfigUtil.getInsecureProperties()));
			String name = "ProperName." + MetadataFile.MD_FILE_EXTENSION;
			manager.isProperResource(name);        
		});
	}

	@Test
	public void resourceMustExist() throws Exception {
		Kernel k=new Kernel(TestConfigUtil.getInsecureProperties());
		MetadataProperties mp=new MetadataProperties(k.getContainerProperties().getRawProperties());
		k.setAttribute(MetadataProperties.class, mp);
		LuceneMetadataManager manager = new LuceneMetadataManager(k);
		IStorageAdapter mockAdapter = mock(IStorageAdapter.class);
		String resourceName = "NonExistingResource";
		when(mockAdapter.getProperties(resourceName)).thenThrow(new ExecutionException());
		manager.setStorageAdapter(mockAdapter, getID());
		assertThrows(IllegalArgumentException.class, ()->{
			manager.isProperResource(resourceName);
		});
	}

	@Test
	public void testProperName() throws Exception {
		Kernel k=new Kernel(TestConfigUtil.getInsecureProperties());
		MetadataProperties mp=new MetadataProperties(k.getContainerProperties().getRawProperties());
		k.setAttribute(MetadataProperties.class, mp);
		LuceneMetadataManager manager = new LuceneMetadataManager(k);
		IStorageAdapter mockAdapter = mock(IStorageAdapter.class);
		String resourceName = "NonExistingResource";
		XnjsFileWithACL mockedFile = mock(XnjsFileWithACL.class);
		when(mockAdapter.getProperties(resourceName)).thenReturn(mockedFile);
		manager.setStorageAdapter(mockAdapter, getID());
		boolean properResource = manager.isProperResource(resourceName);
		assertTrue(properResource);
	}

	@Test
	public void storageCannotBeNullByReading() throws Exception {
		LuceneMetadataManager manager = new LuceneMetadataManager(new Kernel(TestConfigUtil.getInsecureProperties()));
		assertThrows(IllegalStateException.class, ()->{
			manager.readFully("filename");
		});
	}

	@Test
	public void testReadFully2() throws Exception {
		IStorageAdapter adapter = mock(IStorageAdapter.class);
		Kernel k=new Kernel(TestConfigUtil.getInsecureProperties());
		MetadataProperties mp=new MetadataProperties(k.getContainerProperties().getRawProperties());
		k.setAttribute(MetadataProperties.class, mp);
		LuceneMetadataManager manager = new LuceneMetadataManager(k);
		manager.setStorageAdapter(adapter, getID());
		assertThrows(IllegalArgumentException.class, ()->{
			manager.readFully(null);
		});
	}

	@Test
	public void testReadFully3() throws Exception {
		IStorageAdapter adapter = mock(IStorageAdapter.class);
		Kernel k=new Kernel(TestConfigUtil.getInsecureProperties());
		MetadataProperties mp=new MetadataProperties(k.getContainerProperties().getRawProperties());
		k.setAttribute(MetadataProperties.class, mp);
		LuceneMetadataManager manager = new LuceneMetadataManager(k);
		manager.setStorageAdapter(adapter, getID());
		assertThrows(IllegalArgumentException.class, ()->{
			manager.readFully("");
		});
	}

	@Test
	public void testReadFully4() throws Exception {
		IStorageAdapter adapter = mock(IStorageAdapter.class);
		when(adapter.getInputStream(anyString())).thenThrow(new IOException());
		Kernel k=new Kernel(TestConfigUtil.getInsecureProperties());
		MetadataProperties mp=new MetadataProperties(k.getContainerProperties().getRawProperties());
		k.setAttribute(MetadataProperties.class, mp);
		LuceneMetadataManager manager = new LuceneMetadataManager(k);
		manager.setStorageAdapter(adapter, getID());
		assertThrows(IOException.class, ()->{
			manager.readFully("someString");
		});
	}

	private String getID(){
		return String.valueOf(System.currentTimeMillis());
	}
}
