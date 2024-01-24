package eu.unicore.uas.json;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An location that does not need to be resolved 
 * (e.g. a local file, UNICORE WSRF URL or 
 * other URL such as "mailto:...")
 *
 * @author schuller
 */
public class RawLocation implements Location {

	private final String location;
	
	public RawLocation(String location){
		this.location = location;
	}
	
	@Override
	public boolean isLocal() {
		return !isUnicoreURL() && !isRaw();
	}

	@Override
	public boolean isUnicoreURL() {
		return isU6URL(location);
	}

	@Override
	public boolean isRaw() {
		return isRawURL(location);
	}

	@Override
	public String getEndpointURL() {
		return location;
	}

	// pattern describing a UNICORE WSRF URL: "PROTOCOL:https://<url>#/path"
	protected final static String WSRF_URL_RE= "(([[\\w-]]+):)?([\\w-])+://(.)*#(.)*";
	public final static Pattern pattern = Pattern.compile(WSRF_URL_RE); 

	public static boolean isU6URL(String url){		
		Matcher m=pattern.matcher(url);
		return m.find();
	}

	// pattern describing a "raw" non-local URL
	protected final static String RAW_URL_RE= "([\\w-])+:(.)*";
	public final static Pattern rawPattern = Pattern.compile(RAW_URL_RE); 

	public static boolean isRawURL(String url){	
		boolean isU6=isU6URL(url);
		Matcher m=rawPattern.matcher(url);
		return !isU6 && m.find();
	}
}
