package eu.unicore.xnjs.util;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.net.URLCodec;


public class URIUtils {

	private URIUtils(){}

	public static String foo(String uri) throws URISyntaxException{
		return new URI(uri).toASCIIString();
	}

	/**
	 * encode the path, fragment and query parts of an URI string so 
	 * that a real valud URI can be built from it
	 * 
	 * @param uri -  the unencoded URI String
	 */
	public static String encodeAll(String uri) throws URISyntaxException {
		try{
			if(!uri.contains("://")){
				return encodeSimple(uri);
			}
			String path=getPath(uri);
			String query=getQuery(uri);
			String fragment=getFragment(uri);
			String scheme=getScheme(uri);
			String authority=getAuthority(uri);
			if(!authority.contains("://")){
				return new URI(scheme,authority,path,query,fragment).toASCIIString();
			}
			else{
				//handle weird UNICORE URIs e.g. "BFT:https://..." 
				String scheme2=getScheme(authority);
				String auth2=getAuthority(authority);
				return scheme+":"+new URI(scheme2,auth2,path,query,fragment).toASCIIString();
			}
		}catch(Exception e){
			throw new URISyntaxException(uri, LogUtil.createFaultMessage("Could not encode", e));
		}
	}

	private static String encodeSimple(String uri)throws URISyntaxException{
		String scheme=getScheme(uri);
		if(scheme==null){
			return encodeNoScheme(uri);
		}
		String specPart=getSchemeSpecificPart(uri);
		return new URI(scheme,specPart,null).toASCIIString();
	}
	
	private static String encodeNoScheme(String string)throws URISyntaxException{
		return new URI("xxx",string,null).toASCIIString().substring(4);
	}
	
	private static String getPath(String uri) {
		int doubleSlashIndex = uri.indexOf("//");
		boolean hasAuthority =  doubleSlashIndex >= 0;
		int start = -1;
		if(hasAuthority){
			start = uri.indexOf("/",doubleSlashIndex+2);
		}
		else{
			start = uri.indexOf(":");
		}
		if(start == -1) return null;

		int end = uri.indexOf("?",start+1);
		if(end == -1) end = uri.indexOf("#",start+1);
		if(end == -1) end = uri.length();
		String path= uri.substring(start,end);
		return path;
	}
	
	private static String getAuthority(String uri){
		int firstColon = uri.indexOf(":");
		int doubleSlashIndex = uri.indexOf("//");
		boolean hasAuthority =  doubleSlashIndex >= 0;
		int end = -1;
		if(hasAuthority){
			end = uri.indexOf("/",doubleSlashIndex+2);
		}
		if(end==-1)end = uri.length();
		
		int start=doubleSlashIndex+2;
		if(firstColon+1!=doubleSlashIndex)
		{
			start=firstColon+1;
		}
		return uri.substring(start,end);
	}

	private static String getQuery(String uri) {
		int queryStart = uri.indexOf("?");
		if(queryStart == -1) return null;
		int queryEnd = uri.indexOf("#");
		if(queryEnd == -1) queryEnd = uri.length();
		return uri.substring(queryStart+1,queryEnd);
	}

	private static String getFragment(String uri){
		int fragmentStart = uri.indexOf("#");
		if(fragmentStart == -1) return null;
		return uri.substring(fragmentStart+1);
	}
	
	private static String getScheme(String uri){
		int fragmentStart = uri.indexOf(":");
		if(fragmentStart == -1) return null;
		return uri.substring(0,fragmentStart);
	}
	
	private static String getSchemeSpecificPart(String uri){
		int fragmentStart = uri.indexOf(":");
		if(fragmentStart == -1) return null;
		String specPart=uri.substring(fragmentStart+1);
		if(specPart.startsWith("//")){
			specPart=specPart.substring(2);
		}
		return specPart;
	}

	public static String decode(String encoded){
		try{
			return new String(URLCodec.decodeUrl(encoded.getBytes()));
		}catch(DecoderException ex){
			throw new RuntimeException(ex);
		}
	}
	
}
