package eu.unicore.uas.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.document.Document;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests for indexer only (no metadata storage behind)
 *
 * @author jrybicki
 */
public class LuceneIndexerTest {
    
     
    private static final String directory = "target/luceneData/";
    private static final Map<String, String> standardMetadata = new HashMap<String, String>();
    private static final String localDir = "target/possibleDirectory/";
    private static final String fileName = "somefile.txt";

    static {
        standardMetadata.put("StandardKey", "StandardValue");
    }
    
    static LuceneIndexer indexer; 

    @BeforeClass
    public static void initIndexer(){
    	indexer=createNewIndexer();
    }
    
    @AfterClass
    public static void cleanUp() {
        FileUtils.deleteQuietly(new File(directory));
        FileUtils.deleteQuietly(new File(localDir));
        FileUtils.deleteQuietly(new File(fileName));
    }

    @Before
    public void cleanIndexer()throws IOException{
    	indexer.deleteAll();
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testInit() {
        LuceneIndexer luceneIndexer = new LuceneIndexer("/some/nonexisting/directory/");
        System.out.println(luceneIndexer.hashCode());
    }

    @Test
    public void testInit2() {
    	FileUtils.deleteQuietly(new File(localDir));
        LuceneIndexer indexer = new LuceneIndexer(localDir);
        assertNotNull(indexer);
        File file = new File(localDir);
        assertTrue(file.exists());
        assertTrue(file.isDirectory());
        //not sure if it works like that... the dir is locked.
        FileUtils.deleteQuietly(new File(localDir));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInit3() throws IOException {
        File file = new File(fileName);
        boolean createNewFile = file.createNewFile();
        assertTrue(createNewFile);
        LuceneIndexer indexer = new LuceneIndexer(fileName);
        assertNull("Indexer should be null as it fails to init", indexer);
    }

    @Test
    public void testUpdateMe() throws IOException {
        assertNotNull(indexer);
        String resource = "jjfile2.txt";
        String contents = "Some content";
        String someOtherContent = "Some other content";
        indexer.createMetadata(resource, standardMetadata, contents);
        indexer.commit();

        Map<String, String> updated = new HashMap<String, String>();
        updated.put("newKey", "newValue");
        updated.put("newKey2", "newValue2");

        indexer.updateMetadata(resource, updated, someOtherContent);
        indexer.commit();
        Document document = indexer.getDocument(resource);
        assertNotNull(document);
        Map<String, String> metaE = LuceneIndexer.extractMetadataFromDocument(document);
        for (String key : standardMetadata.keySet()) {
            assertTrue(metaE.containsKey(key));
            assertTrue(metaE.containsValue(standardMetadata.get(key)));
        }

        for (String key : updated.keySet()) {
            assertTrue(metaE.containsKey(key));
            assertTrue(metaE.containsValue(updated.get(key)));
        }

        //you can also use updateMetadata to create a document:
        indexer.updateMetadata("completetlyNewName", updated, "some completly new content");
        indexer.optimizeIndex();
    }

    @Test
    public void testCR() throws IOException, Exception {
        assertNotNull(indexer);
        String resource = "jjfile2.txt";
        String contents = "Some content";
        indexer.createMetadata(resource, standardMetadata, contents);

        String resource2 = "jjfile.txt";
        String conS = "Some other content";
        Map<String, String> newMetadata = new HashMap<String, String>();
        newMetadata.put("key", "value");
        newMetadata.put("otherKey", "otherValue");
        indexer.createMetadata(resource2, newMetadata, conS);
        indexer.commit();
        assertNotNull(indexer.getDocument(resource));
        System.out.println("Adding " + resource + " sucessful");
        assertNotNull(indexer.getDocument(resource2));
        System.out.println("Adding " + resource2 + " sucessful");
        
        indexer.removeMetadata(resource2);
        indexer.commit();
        System.out.println("Removing " + resource2 + " sucessful");

        indexer.removeMetadata(resource);
        indexer.commit();
        System.out.println("Removing " + resource2 + " sucessful");
    }

    @Test
    public void testUpdate() throws IOException {
        assertNotNull(indexer);
        
        String resource = "jjfile.txt";
        String contents = "Some content";
        indexer.createMetadata(resource, standardMetadata, contents);
        System.out.println("Adding " + resource + " sucessful");
        indexer.commit();
        
        //case1: rename existing to nonexisting:
        String newName = "jjfile2.txt";
        indexer.moveMetadata(resource, newName);
        indexer.commit();
        Document document = indexer.getDocument(resource);
        assertNull(document);
        document = indexer.getDocument(newName);
        assertNotNull(document);
        System.out.printf("Renaming resource %s to %s sucessful\n", resource, newName);
        
        Map<String, String> metadata = LuceneIndexer.extractMetadataFromDocument(document);
        assertFalse(metadata.isEmpty());
        System.out.println("Metadata:");
        for (String key : standardMetadata.keySet()) {
            assertTrue(metadata.containsKey(key));
            System.out.println("=>" + key);
        }

        for (String value : standardMetadata.values()) {
            assertTrue(metadata.containsValue(value));
            System.out.println("=>" + value);
        }

        //case2: rename existing to existing:
        String thirdResource = "3file.t";
        String thirdContent = "Some other other content";
        Map<String, String> meta = new HashMap<String, String>();
        meta.put("jjKey", "jjValue");
        meta.put("jjKey2", "jjValue2");
        meta.put("jjKey3", "jjValue3");
        indexer.createMetadata(thirdResource, meta, thirdContent);
        indexer.moveMetadata(newName, thirdResource);
        indexer.commit();
        System.out.printf("Renaming resource %s to %s sucessful\n", newName, thirdResource);

        document = indexer.getDocument(newName);
        assertNull(document);
        document = indexer.getDocument(thirdResource);
        assertNotNull(document);
        Map<String, String> metadata2 = LuceneIndexer.extractMetadataFromDocument(document);
        assertNotNull(metadata2);
        assertFalse(metadata2.isEmpty());


        //old metadata for the resource should be overwritten
        for (String key : meta.keySet()) {
            assertFalse(metadata2.containsKey(key));
        }

        for (String value : meta.values()) {
            assertFalse(metadata2.containsValue(value));
        }

        //case3: rename nonexisting to nonexisting:
        try {
            indexer.moveMetadata("SomePhantasyName", "SomeOtherPhantasyName");
            fail("Successfuly renamed nonexisting resource");
        } catch (IllegalArgumentException ex) {
            assertTrue("Unable to rename nonexisting resource", true);
        }

        //case4: rename nonexisting to existing:
        try {
            indexer.moveMetadata("SomePhantasyName", thirdResource);
            fail("Successfuly renamed nonexisting resource to an existing one");
        } catch (IllegalArgumentException ex) {
            assertTrue("Unable to rename nonexisting resource", true);
        }

    }

    @Test
    public void testSearch() throws IOException, Exception {
        String keyWord = "myKeyword";
        String uniqueKeyword = "uniqueK";
        String resource = "jjfile.txt";
        String contents = "Some content " + keyWord;
        indexer.createMetadata(resource, standardMetadata, contents);
        System.out.println("Adding " + resource + " sucessful");

        String thirdResource = "3file.t";
        String thirdContent = "Some other other content " + keyWord + " " + uniqueKeyword;
        Map<String, String> meta = new HashMap<String, String>();
        meta.put("jjKey", "jjValue");
        meta.put("jjKey2", "jjValue2");
        meta.put("jjKey3", "jjValue3");
        indexer.createMetadata(thirdResource, meta, thirdContent);
        indexer.commit();
        
        List<SearchResult> results = indexer.search("bla", 5);
        assertTrue(results.isEmpty());

        results = indexer.search(keyWord, 1);
        assertEquals(1, results.size());
        results = indexer.search(keyWord, 2);
        assertEquals(2, results.size());
        System.out.println("Search results " + keyWord);
        for (SearchResult searchResult : results) {
            System.out.println("\t" + searchResult.getResourceName());
        }

        results = indexer.search(uniqueKeyword, 20);
        assertEquals(1, results.size());
        System.out.println("Search results " + uniqueKeyword);
        for (SearchResult searchResult : results) {
            System.out.println("\t" + searchResult.getResourceName());
            assertTrue(searchResult.getResourceName().equals(thirdResource));
        }

        results = indexer.search("jjValue", 20);
        assertTrue(results.isEmpty());

    }

    @Test
    public void testAdvancedSearch() throws IOException {
        //it is a pseudo-test: presents capabilites and strenghts of lucene query
        //syntax
        //populate repository
        List<Resource> list = new ArrayList<Resource>();
        list.add(new Resource("Josa", "Pera", 85, 88));
        list.add(new Resource("Miri", "Misa", 47, 13));
        list.add(new Resource("Joan", "Miri", 54, 4));
        list.add(new Resource("Lusa", "Joan", 34, 6));
        list.add(new Resource("Luke", "Vesa", 78, 48));
        list.add(new Resource("Dari", "Veke", 69, 73));
        list.add(new Resource("Mike", "Dari", 17, 63));
        list.add(new Resource("Jori", "Mike", 62, 34));
        list.add(new Resource("Vesa", "Daan", 92, 62));
        list.add(new Resource("Veri", "Luan", 96, 89));
        list.add(new Resource("Dake", "Dasa", 76, 32));
        list.add(new Resource("Joke", "Dake", 10, 99));
        list.add(new Resource("Luri", "Joke", 74, 59));
        Resource mike = new Resource("O'Harry", "Mike", 11, 20);
        Resource johnny = new Resource("Z'Harry", "Mike", 15, 12);
        Resource misspelledMike = new Resource("Barry", "Mikke", 15, 12);

        list.add(mike);
        list.add(johnny);
        list.add(misspelledMike);

        Map<String, Resource> resources = new HashMap<String, Resource>();

        for (Resource resource : list) {
            resources.put(resource.getName(), resource);
            System.out.println("Indexing: " + resource.getName() + " =-> " + resource);
            indexer.createMetadata(resource.getName(), resource.getAsMetadata(), "someContent");
        }
        indexer.commit();
        
        //do some advanced searches:
        //altName = mike
        String altNameQuery = "altName:\"Mike\"";
        List<SearchResult> result = indexer.search(altNameQuery, 20);
        assertEquals(3, result.size());
        presentResults(altNameQuery, result, resources);

        //wildcard query on name
        String wildcardQuery = "altName:Mi*";
        result = indexer.search(wildcardQuery, 20);
        assertEquals(6, result.size());
        presentResults(wildcardQuery, result, resources);

        //compound query:
        String compoundQuery = "altName:Mike AND param1:11";
        result = indexer.search(compoundQuery, 20);
        assertEquals(1, result.size());
        presentResults(compoundQuery, result, resources);

        //range query on id:
        String rangeQuery = "param1:[1 TO 50]";
        result = indexer.search(rangeQuery, 20);
        assertEquals(7, result.size());
        presentResults(rangeQuery, result, resources);

        //range query on two parameters:
        //TODO: but it takes ints as alphanumericals
        //the fix will be applied as soon as we agree on Lucene version (as the fix is
        //apparently version specific)
        String compoundRangeQuery = "param1:[1 TO 50] AND param2:[10 TO 25]";
        result = indexer.search(compoundRangeQuery, 20);
        assertEquals(4, result.size());
        presentResults(compoundRangeQuery, result, resources);

        //ranges for non numerical fields:
        String rangeAlphaQuery = "name:[Jarek TO Luzkas]";
        result = indexer.search(rangeAlphaQuery, 20);
        assertEquals(7, result.size());
        presentResults(rangeAlphaQuery, result, resources);

        //fuzzy search:
        String proximitSearch = "altName:Mike~";
        result = indexer.search(proximitSearch, 20);
        assertEquals(9, result.size());
        presentResults(proximitSearch, result, resources);

        //grouping:
        String groupedQuery = "( altName:\"Mike\" OR name:[\"Jarek\" TO \"Luzkas\"] ) AND param2:[1 TO 6]";
        result = indexer.search(groupedQuery, 20);
        assertEquals(7, result.size());
        presentResults(groupedQuery, result, resources);



    }

    @Test
    public void proximitySearchTest() throws IOException {
        
        Resource res1 = new Resource("Resource1", "Simple Resource 1", 1, 2);
        Resource res2 = new Resource("Resource2", "Simple Resource 2", 3, 4);
        Resource res3 = new Resource("Resource3", "Simple Resource 3", 5, 6);


        indexer.createMetadata(res1.getName(), res1.getAsMetadata(), "Because Apache Lucene is a full-text search engine and not a conventional database, "
                + "it cannot handle numerical ranges (e.g., field value is inside user defined bounds, even dates are numerical values).");

        indexer.createMetadata(res2.getName(), res2.getAsMetadata(), "We have developed an extension to Apache spaceholder Lucene that stores the numerical values"
                + " in a special string-encoded format with variable precision (all numerical values like doubles, longs, floats, and ints are converted"
                + " to lexicographic sortable string representations and stored with different precisions.");
        indexer.createMetadata(res3.getName(), res3.getAsMetadata(), "A range is then divided by Apache spaceholder spaceholder spaceholder Lucene recursively into multiple intervals for "
                + "searching: The center of the range is searched only with the lowest possible precision in the trie, while the boundaries are "
                + "matched more exactly. This reduces the number of terms dramatically.");

        indexer.commit();
        
        String nonProximitySearch = "Apache Lucene";
        List<SearchResult> result = indexer.search(nonProximitySearch, 20);
        System.out.println("Items matching query: " + nonProximitySearch);
        for (SearchResult item : result) {
            System.out.println("\t" + item.getResourceName());
        }
        assertEquals(3, result.size());

        //Apache must not be further than 2 words from Lucene:
        String proximitySearch = "\"Apache Lucene\"~2";
        result = indexer.search(proximitySearch, 20);
        System.out.println("Items matching query: " + proximitySearch);
        for (SearchResult item : result) {
            System.out.println("\t" + item.getResourceName());
        }
        assertEquals(2, result.size());

        //Must contain spaceholder and might contain conventional
        String mustQuery = "+spaceholder conventional";
        result = indexer.search(mustQuery, 20);
        System.out.println("Items matching query: " + mustQuery);
        for (SearchResult item : result) {
            System.out.println("\t" + item.getResourceName());
        }
        assertEquals(2, result.size());
    }

    private void presentResults(String query, List<SearchResult> results, Map<String, Resource> mapping) {
        System.out.println("Have <"+results.size()+"> results for: " + query);
        for (SearchResult searchResult : results) {
            assertTrue(mapping.containsKey(searchResult.getResourceName()));
            System.out.println("\t" + searchResult.getResourceName() + " --->" + mapping.get(searchResult.getResourceName()));
        }
    }
    private static int idGenerator = 0;

    class Resource {

        final int id;
        final String name;
        final String alternativeName;
        final int param1;
        final int param2;

        public Resource(String name, String alternativeName, int param1, int param2) {
            this.id = idGenerator++;
            this.name = name;
            this.alternativeName = alternativeName;
            this.param1 = param1;
            this.param2 = param2;
        }

        public String getAlternativeName() {
            return alternativeName;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public int getParam1() {
            return param1;
        }

        public int getParam2() {
            return param2;
        }

        public Map<String, String> getAsMetadata() {
            Map<String, String> ret = new HashMap<String, String>();
            ret.put("id", String.valueOf(id));
            ret.put("name", name);
            ret.put("altName", alternativeName);
            ret.put("param1", String.valueOf(param1));
            ret.put("param2", String.valueOf(param2));
            return ret;
        }

        @Override
        public String toString() {
            return String.format("Resource %d (\"%s\", \"%s\", %d, %d);", id, name, alternativeName, param1, param2);

        }
    }

    private static LuceneIndexer createNewIndexer() {

        File file = new File(directory);
        if (file.exists()) {
            System.out.println("Removing existing index: " + directory);
            FileUtils.deleteQuietly(file);

            boolean exists = file.exists();
            if (exists) {
                System.out.println("Unable to delete directory: " + directory);
            }
        }
        return new LuceneIndexer(directory);
    }
}
