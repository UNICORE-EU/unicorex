package eu.unicore.uas.metadata.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.xml.sax.ContentHandler;

import eu.unicore.uas.metadata.LuceneIndexer;

/**
 *
 * @author jj
 */

public class ExtensionParserTest {

    ExtensionParser parser = new ExtensionParser();
    ContentHandler handler = new BodyContentHandler(-1);
    private final ParseContext parseContext = new ParseContext();

    public ExtensionParserTest() {
    }

    @BeforeAll
    public static void setUpClass() throws Exception {
    }

    @AfterAll
    public static void tearDownClass() throws Exception {
    }

    @Test
    public void testParseMethod() throws Exception {
        String path = "/home/user/file/name.extt";

        Metadata meta = new Metadata();
        meta.add(LuceneIndexer.RESOURCE_NAME_KEY, path);
        parser.parse(null, handler, meta, parseContext);

        System.out.printf("Metadata for file (%s) are:\n", path);
        for (String key : meta.names()) {
            System.out.printf("Meta info: %s-->%s\n", key, meta.get(key));
        }

        assertEquals(2, meta.size());
        assertEquals("extt", meta.get("Tags"));
        assertEquals(path, meta.get(LuceneIndexer.RESOURCE_NAME_KEY));

        //and no extension:
        path = "/home/usr/file/nameWithoutExtension";
        meta = new Metadata();
        meta.add(LuceneIndexer.RESOURCE_NAME_KEY, path);
        parser.parse(null, handler, meta, parseContext);
        System.out.printf("Metadata for file (%s) are:\n", path);
        for (String key : meta.names()) {
            System.out.printf("Meta info: %s-->%s\n", key, meta.get(key));
        }

        assertEquals(1, meta.size());
        assertEquals(path, meta.get(LuceneIndexer.RESOURCE_NAME_KEY));

    }

    @Test
    public void testHandlerReuse() throws Exception {
        String path1 = "/home/user/file/name.extt";
        String path2 = "/home/user/file2/File2.name.ext";

        Metadata meta = new Metadata();
        meta.add(LuceneIndexer.RESOURCE_NAME_KEY, path1);
        parser.parse(null, handler, meta, parseContext);

        System.out.printf("Metadata for file1 (%s) are:\n", path1);
        for (String key : meta.names()) {
            System.out.printf("Meta info: %s-->%s\n", key, meta.get(key));
        }

        assertEquals(2, meta.size());
        assertEquals("extt", meta.get("Tags"));
        assertEquals(path1, meta.get(LuceneIndexer.RESOURCE_NAME_KEY));

        //and reparse:
        meta = new Metadata();
        meta.add(LuceneIndexer.RESOURCE_NAME_KEY, path2);
        parser.parse(null, handler, meta, parseContext);
        System.out.printf("Metadata for file2 (%s) are:\n", path2);
        for (String key : meta.names()) {
            System.out.printf("Meta info: %s-->%s\n", key, meta.get(key));
        }

        assertEquals(2, meta.size());
        assertEquals("ext", meta.get("Tags"));
        assertEquals(path2, meta.get(LuceneIndexer.RESOURCE_NAME_KEY));

    }

    @Test
    public void testNotsupported() {
    	assertThrows(UnsupportedOperationException.class, ()->{
    		parser.getSupportedTypes(parseContext);
    	});
    }

    @Test
    public void testNonProper() throws Exception {
        Metadata meta = new Metadata();
        meta.add(LuceneIndexer.RESOURCE_NAME_KEY, "");
        parser.parse(null, handler, meta, null);
    }
}
