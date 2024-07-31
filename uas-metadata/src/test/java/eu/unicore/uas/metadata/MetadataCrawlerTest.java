package eu.unicore.uas.metadata;

import eu.unicore.uas.metadata.MetadataCrawler.CombinedFilter;
import eu.unicore.uas.metadata.MetadataCrawler.PatternFilter;
import eu.unicore.uas.metadata.MetadataFile.MD_State;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author jrybicki
 */
public class MetadataCrawlerTest {

    public MetadataCrawlerTest() {
    }

    @Test
    public void testFindNew() {
        List<String> listOfFiles = new ArrayList<String>();
        listOfFiles.add("/tmp/file1");
        listOfFiles.add("/usr/lib/someFile");
        listOfFiles.add("/usr/lib/someFile4");
        listOfFiles.add("/var/lib/gcc/someFile4");

        Map<String, MD_State> checks = null;
        for (int i = 0; i < 10; i++) {
            checks = MetadataCrawler.statusCheck(listOfFiles);
            assertFalse(checks.isEmpty());
            assertEquals(listOfFiles.size(), checks.size());

            for (String file : listOfFiles) {
                assertTrue(checks.containsKey(file));
                assertEquals(MD_State.NEW, checks.get(file));
            }

            Collections.shuffle(listOfFiles);
        }
    }

    @Test
    public void testFindUpdated() {
        List<String> listOfFiles = new ArrayList<>();
        listOfFiles.add("/tmp/file1");
        listOfFiles.add("/var/lib/gcc/someFile4");
        int numberOfResources = listOfFiles.size();
        //and some metadata:
        List<String> listOfMDFiles = new ArrayList<>();
        for (String string : listOfFiles) {
            listOfMDFiles.add(MetadataFile.getMetadatafileName(string));
        }
        listOfFiles.addAll(listOfMDFiles);
        Map<String, MD_State> checks = null;
        for (int i = 0; i < 10; i++) {
            checks = MetadataCrawler.statusCheck(listOfFiles);
            assertFalse(checks.isEmpty());
            assertEquals(numberOfResources, checks.size());
            for (String file : listOfFiles) {
                String resourceName = MetadataFile.isMetadataFileName(file)?MetadataFile.getResourceName(file):file;
                assertTrue(checks.containsKey(resourceName));
                assertEquals(MD_State.CHK_CONSISTENCE, checks.get(resourceName));
            }
            Collections.shuffle(listOfFiles);
        }
    }

    @Test
    public void testFindDeleted() {
        List<String> listOfFiles = new ArrayList<>();
        String [] deletedResources = {"/var/lib/gcc/someFile5", "/tmp/fileRemoved", "/and/one/more"};
        for (int i = 0; i < deletedResources.length; i++) {
            String resource = deletedResources[i];
            listOfFiles.add(MetadataFile.getMetadatafileName(resource));
        }
        Map<String, MD_State> checks = null;
        for (int i = 0; i < 10; i++) {
            checks = MetadataCrawler.statusCheck(listOfFiles);
            assertFalse(checks.isEmpty());
            assertEquals(listOfFiles.size(), checks.size());
            assertEquals(deletedResources.length, checks.size());

            for (int j = 0; j < deletedResources.length; j++) {
                String resourceName = deletedResources[j];
                assertTrue(checks.containsKey(resourceName));
                assertEquals(MD_State.RESOURCE_DELETED, checks.get(resourceName));
            }
            Collections.shuffle(listOfFiles);
        }
    }
    
    @Test
    public void testFindDeletedAndUpdated() {
        String [] newResources = {"/var/www/newResource1", "/tmp/file1.txt", "/usr/lib/fileX", "blink"};
        String [] deletedResources = {"/var/www/deletedResource1.html", "/tmp/file2", "aaa"};
        String [] updatedResources = {"one", "two.win32", "/var/three", "/usr/four.o"};
        List<String> listOfFiles = new ArrayList<>();

        //add all new files:
        listOfFiles.addAll(Arrays.asList(newResources));

        //add all updated files:
        for (int i = 0; i < updatedResources.length; i++) {
            listOfFiles.add(updatedResources[i]);
            listOfFiles.add(MetadataFile.getMetadatafileName(updatedResources[i]));            
        }

        //add all medtadata files for removed files:
        for (int i = 0; i < deletedResources.length; i++) {
            listOfFiles.add(MetadataFile.getMetadatafileName(deletedResources[i]));            
        }

        assertFalse(listOfFiles.isEmpty());
        int completeLength=newResources.length+deletedResources.length+updatedResources.length;
        assertEquals(completeLength+updatedResources.length, listOfFiles.size());                

        Map<String, MD_State> checks = null;
        for (int i = 0; i < 10; i++) {
            checks = MetadataCrawler.statusCheck(listOfFiles);
            assertFalse(checks.isEmpty());
            for (int j = 0; j < updatedResources.length; j++) {
                assertTrue(checks.containsKey(updatedResources[j]), "should contain: "+updatedResources[j]);
                assertEquals(checks.get(updatedResources[j]), MD_State.CHK_CONSISTENCE);
            }
            for (int j = 0; j < newResources.length; j++) {
                assertTrue(checks.containsKey(newResources[j]), "should contain: "+newResources[j]);
                assertEquals(checks.get(newResources[j]), MD_State.NEW);
            }
            
            for (int j = 0; j < deletedResources.length; j++) {
                assertTrue(checks.containsKey(deletedResources[j]), "should contain: "+deletedResources[j]);
                assertEquals(checks.get(deletedResources[j]), MD_State.RESOURCE_DELETED);
            }
            //shuffle can do some damage to the unstable algorithm:
            Collections.shuffle(listOfFiles);
        }
    }

    @Test
    public void testPatternFilter(){
    	PatternFilter pf=new PatternFilter("*.a", "/x.html");
    	assertTrue(pf.accept("out1.a"));
    	assertTrue(pf.accept("/x.html"));
    	assertFalse(pf.accept("fooba"));
    }

    @Test
    public void testCombinedFilter(){
    	PatternFilter pf1=new PatternFilter("*.java", "*.xml");
    	PatternFilter pf2=new PatternFilter("/no.java");
    	CombinedFilter cf=new CombinedFilter(pf1, pf2);
    	assertTrue(cf.accept("foo.java"));
    	assertTrue(cf.accept("test.xml"));
    	assertFalse(cf.accept("/no.java"));
    }

}
