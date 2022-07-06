package eu.unicore.uas.metadata.utils;

import java.io.IOException;
import org.apache.tika.exception.TikaException;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.metadata.Metadata;
import org.xml.sax.ContentHandler;
import org.apache.tika.sax.BodyContentHandler;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.xml.sax.SAXException;

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

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testParseMethod() throws IOException, SAXException, TikaException {
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
    public void testHandlerReuse() throws IOException, SAXException, TikaException {
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

    @Test(expected = UnsupportedOperationException.class)
    public void testNotsupported() {
        parser.getSupportedTypes(parseContext);
    }

    @Test
    public void testNonProper() throws IOException, SAXException, TikaException {
        Metadata meta = new Metadata();
        meta.add(LuceneIndexer.RESOURCE_NAME_KEY, "");
        parser.parse(null, handler, meta, null);
    }
}
