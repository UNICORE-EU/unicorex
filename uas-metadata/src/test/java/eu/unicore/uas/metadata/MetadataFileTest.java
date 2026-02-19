package eu.unicore.uas.metadata;

import eu.unicore.uas.metadata.MetadataFile.MD_State;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.Mockito.*;

/**
 *
 * @author jrybicki
 */
public class MetadataFileTest {

    public MetadataFileTest() {
    }

    @Test
    public void testGetMdStates() {
        MetadataFile file = mock(MetadataFile.class);
        file.getMdStates();
        verify(file).getMdStates();
    }

    @Test
    public void testSetMdStates() {
        MetadataFile file = new MetadataFile();
        assertNotNull(file);
        file.setMdStates(MetadataFile.MD_State.NEW);
        MD_State mdStates = file.getMdStates();
        assertEquals(mdStates, MetadataFile.MD_State.NEW);

        file.setMdStates(MetadataFile.MD_State.CONSISTENT);
        MD_State mdStates1 = file.getMdStates();
        assertEquals(mdStates1, MetadataFile.MD_State.CONSISTENT);
    }

    //testIsMetadataFileName:
    @Test
    public void metadataFileMustNotBeNullEmpty() {
        assertFalse(MetadataFile.isMetadataFileName(null));
        assertFalse(MetadataFile.isMetadataFileName(""));
    }

    @Test
    public void testMetadataExtension() {
        String properMetadataFile = MetadataFile.getMetadatafileName("resource");
        assertTrue(MetadataFile.isMetadataFileName(properMetadataFile));
        String wrongName = "somenmame";
        assertFalse(MetadataFile.isMetadataFileName(wrongName));
    }

    //testGetMetadatafileName:
    @Test
    public void metadataAtTheEnd() {
        String caseHandler = "file";
        String metadatafileName = MetadataFile.getMetadatafileName(caseHandler);
        assertNotNull(metadatafileName);
        assertFalse(metadatafileName.isEmpty());
        assertEquals(".file." + MetadataFile.MD_FILE_EXTENSION, metadatafileName);
    }

    @Test
    public void metadataShouldAddafterExtension() {
        String caseHandler = "file.jpg";
        String metadatafileName = MetadataFile.getMetadatafileName(caseHandler);
        assertNotNull(metadatafileName);
        assertFalse(metadatafileName.isEmpty());
        assertEquals(".file.jpg." + MetadataFile.MD_FILE_EXTENSION, metadatafileName);
    }

    @Test
    public void metadataIgnoresPath() {
        String caseHandler = "/var/usr/file";
        String metadatafileName = MetadataFile.getMetadatafileName(caseHandler);
        assertNotNull(metadatafileName);
        assertFalse(metadatafileName.isEmpty());
        assertEquals("/var/usr/.file." + MetadataFile.MD_FILE_EXTENSION, metadatafileName);
    }

    @Test
    public void metadataIgnoresPathAddafterExtension() {
        String caseHandler = "/var/usr/file.owr";
        String metadatafileName = MetadataFile.getMetadatafileName(caseHandler);
        assertNotNull(metadatafileName);
        assertFalse(metadatafileName.isEmpty());
        assertEquals("/var/usr/.file.owr." + MetadataFile.MD_FILE_EXTENSION, metadatafileName);
    }

    //testGetResourceName:
    @Test
    public void removeTrailingDotAndExtension() {
        String caseHandler = ".file.ext." + MetadataFile.MD_FILE_EXTENSION;
        String resourceName = MetadataFile.getResourceName(caseHandler);
        assertNotNull(resourceName);
        assertFalse(resourceName.isEmpty());
        assertEquals("file.ext", resourceName);
    }

    @Test
    public void addResourcePath() {
        String caseHandler = "/var/www/.file.ext." + MetadataFile.MD_FILE_EXTENSION;
        String resourceName = MetadataFile.getResourceName(caseHandler);
        assertNotNull(resourceName);
        assertFalse(resourceName.isEmpty());
        assertEquals("/var/www/file.ext", resourceName);
    }
    
    

    @Test
    public void testGetResourceName2() {
        String[] resourceNames = {"/var/complex/resource/resourceName", "blah", "C:\\WINDOWS\\file.txt", "/var/lib/lib32.o"};

        for (int i = 0; i < resourceNames.length; i++) {
            String resource = resourceNames[i];
            String mdFile = MetadataFile.getMetadatafileName(resource);
            assertNotNull(mdFile);
            assertFalse(mdFile.trim().isEmpty());
            assertTrue(mdFile.contains(MetadataFile.MD_FILE_EXTENSION));

            //check back
            String resource2 = MetadataFile.getResourceName(mdFile);
            assertNotNull(resource2);
            assertFalse(resource2.trim().isEmpty());
            assertEquals(resource, resource2);
        }

    }
}
