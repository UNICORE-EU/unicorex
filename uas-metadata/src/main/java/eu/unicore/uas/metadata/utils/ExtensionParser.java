package eu.unicore.uas.metadata.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.ParseContext;

import org.apache.tika.parser.Parser;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import eu.unicore.uas.metadata.LuceneIndexer;

/**
 * Simple metadata extractor, extracts only file extension.
 *
 * <p> 
 * Old convert method from {@link eu.unicore.uas.metadata.MetadataCrawler}. If not file extension can
 * be identified empty string is returned. The parser expects Metadata.RESOURCE_NAME_KEY
 * to be set with proper file path.
 * </p>
 *
 * @author jrybicki
 */
public class ExtensionParser implements Parser {  
    
	private static final long serialVersionUID=1l;

    @Override
    public Set<MediaType> getSupportedTypes(ParseContext context) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void parse(InputStream stream, ContentHandler handler, Metadata metadata, ParseContext context) throws IOException, SAXException, TikaException {
        String path = metadata.get(LuceneIndexer.RESOURCE_NAME_KEY);
               
        String ext = FilenameUtils.getExtension(path);
        if (!ext.isEmpty()) {
            metadata.add("Tags", ext);
        }
    }
}
