package eu.unicore.uas.metadata;

import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.security.TestConfigUtil;
import de.fzj.unicore.xnjs.io.XnjsFileWithACL;
import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.io.IStorageAdapter;
import org.apache.tika.metadata.Metadata;
import java.util.Map;
import java.util.HashMap;
import org.apache.lucene.document.Document;
import java.io.IOException;

import org.junit.Test;

import static org.mockito.Mockito.*;

import static org.junit.Assert.*;

public class TestUtils {

    private static final Map<String, String> standardMetadata = new HashMap<String, String>();

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
        assertEquals(resourceName, doc.get(Metadata.RESOURCE_NAME_KEY));

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

    @Test(expected = IllegalArgumentException.class)
    public void resourceNameCannotbeNull() throws Exception {
        LuceneMetadataManager manager = new LuceneMetadataManager(new Kernel(TestConfigUtil.getInsecureProperties()));
        manager.isProperResource(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void resourceNameCannotBeEmpty() throws Exception {
        LuceneMetadataManager manager = new LuceneMetadataManager(new Kernel(TestConfigUtil.getInsecureProperties()));
        manager.isProperResource("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void resourceNameCannotEndWithMD() throws Exception {
        LuceneMetadataManager manager = new LuceneMetadataManager(new Kernel(TestConfigUtil.getInsecureProperties()));
        String name = "ProperName." + MetadataFile.MD_FILE_EXTENSION;
        manager.isProperResource(name);        
    }

    @Test(expected=IllegalArgumentException.class)
    public void resourceMustExist() throws Exception {
    	Kernel k=new Kernel(TestConfigUtil.getInsecureProperties());
    	MetadataProperties mp=new MetadataProperties(k.getContainerProperties().getRawProperties());
    	k.addConfigurationHandler(MetadataProperties.class, mp);
        LuceneMetadataManager manager = new LuceneMetadataManager(k);
        IStorageAdapter mockAdapter = mock(IStorageAdapter.class);
        String resourceName = "NonExistingResource";
        when(mockAdapter.getProperties(resourceName)).thenThrow(new ExecutionException());
        manager.setStorageAdapter(mockAdapter, getID());
        manager.isProperResource(resourceName);
    }
    
    @Test
    public void testProperName() throws Exception {
    	Kernel k=new Kernel(TestConfigUtil.getInsecureProperties());
    	MetadataProperties mp=new MetadataProperties(k.getContainerProperties().getRawProperties());
    	k.addConfigurationHandler(MetadataProperties.class, mp);
        LuceneMetadataManager manager = new LuceneMetadataManager(k);
        IStorageAdapter mockAdapter = mock(IStorageAdapter.class);
        String resourceName = "NonExistingResource";
        XnjsFileWithACL mockedFile = mock(XnjsFileWithACL.class);
        when(mockAdapter.getProperties(resourceName)).thenReturn(mockedFile);
        manager.setStorageAdapter(mockAdapter, getID());
        boolean properResource = manager.isProperResource(resourceName);
        assertTrue(properResource);
    }

    @Test(expected = IllegalStateException.class)
    public void storageCannotBeNullByReading() throws Exception {
        LuceneMetadataManager manager = new LuceneMetadataManager(new Kernel(TestConfigUtil.getInsecureProperties()));
        manager.readFully("filename");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReadFully2() throws Exception {
        IStorageAdapter adapter = mock(IStorageAdapter.class);
        Kernel k=new Kernel(TestConfigUtil.getInsecureProperties());
    	MetadataProperties mp=new MetadataProperties(k.getContainerProperties().getRawProperties());
    	k.addConfigurationHandler(MetadataProperties.class, mp);
        LuceneMetadataManager manager = new LuceneMetadataManager(k);
        manager.setStorageAdapter(adapter, getID());
        manager.readFully(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReadFully3() throws Exception {
        IStorageAdapter adapter = mock(IStorageAdapter.class);
        Kernel k=new Kernel(TestConfigUtil.getInsecureProperties());
    	MetadataProperties mp=new MetadataProperties(k.getContainerProperties().getRawProperties());
    	k.addConfigurationHandler(MetadataProperties.class, mp);
        LuceneMetadataManager manager = new LuceneMetadataManager(k);
        manager.setStorageAdapter(adapter, getID());
        manager.readFully("");
    }

    @Test(expected = IOException.class)
    public void testReadFully4() throws Exception {
        IStorageAdapter adapter = mock(IStorageAdapter.class);
        when(adapter.getInputStream(anyString())).thenThrow(new ExecutionException());
        Kernel k=new Kernel(TestConfigUtil.getInsecureProperties());
    	MetadataProperties mp=new MetadataProperties(k.getContainerProperties().getRawProperties());
    	k.addConfigurationHandler(MetadataProperties.class, mp);
        LuceneMetadataManager manager = new LuceneMetadataManager(k);
        manager.setStorageAdapter(adapter, getID());
        manager.readFully("someString");
    }
    
    private String getID(){
    	return String.valueOf(System.currentTimeMillis());
	}
}
