package de.fzj.unicore.uas.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Helper for dealing with groups of properties
 * 
 * @author schuller
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class PropertyHelper {

	private final Map properties;

	private final String[] acceptedPatterns;

	private final boolean isRegexp;

	private final Pattern[] patterns;

	/**
	 * filter the given properties using the supplied patterns
	 */
	public PropertyHelper(Map properties, String... patterns) {
		this(properties,false,patterns);
	}

	/**
	 * filter the given properties using the supplied patterns
	 * 
	 * @param properties - the properties
	 * @param isRegexp - whether the patterns denote Java regular expressions
	 * @param patterns - the accepted patterns
	 */
	public PropertyHelper(Map properties, boolean isRegexp, String... patterns) {
		this.properties = properties;
		this.acceptedPatterns = patterns;
		this.isRegexp=isRegexp;
		this.patterns=isRegexp?createPatterns():null;
	}
	
	/**
	 * returns an iterator over the valid keys
	 */
	public Iterator<String> keys() {
		final Iterator<String> backing = properties.keySet().iterator();
		return new Iterator<String>() {
			private String next = null;

			boolean filter(String key) {
				if(isRegexp){
					return filterRegexp(key);
				}
				else return filterPlain(key);
			}

			public boolean hasNext() {
				return getNextMatching()!=null;
			}

			//this is idempotent
			private String getNextMatching() {
				if(next!=null)return next;
				if(!backing.hasNext()){
					next=null;
					return null;
				}
				String res = backing.next();
				if (res == null){
					next=null;
					return null;
				}

				if(!filter(res)){
					//skip
					return getNextMatching();
				}

				next=res;
				return res;
			}

			public String next() {
				String res=getNextMatching();
				next=null;
				return res;
			}

			public void remove() {
				backing.remove();
			}

		};
	}
	
	private Pattern[] createPatterns(){
		Pattern[] ps=new Pattern[acceptedPatterns.length];
		for(int i=0; i<acceptedPatterns.length; i++){
			ps[i]=Pattern.compile(acceptedPatterns[i]);
		}
		return ps;
	}

	private boolean filterRegexp(String key){
		for(Pattern p: patterns){
			if(p.matcher(key).matches())return true;
		}
		return false;
	}
	
	private boolean filterPlain(String key){
		for (String p : acceptedPatterns) {
			if (key.startsWith(p))
				return true;
		}
		return false;
	}
	
	/**
	 * gets the properties whose keys match the accepted patterns
	 * 
	 * @return a map
	 */
	public Map<String,String>getFilteredMap(){
		return getFilteredMap(null);
	}

	/**
	 * gets the properties whose keys match the accepted patterns AND
	 * whose keys contain the supplied string
	 * 
	 * @param containedString
	 * @return a map
	 */
	public Map<String,String>getFilteredMap(String containedString){
		Map<String, String> props=new HashMap<String, String>();
		Iterator<String>keys=keys();
		while(keys.hasNext()){
			String key=keys.next();
			if(containedString==null){
				props.put(key, String.valueOf(properties.get(key)));
				continue;
			}
			if(key.contains(containedString)){
				props.put(key, String.valueOf(properties.get(key)));	
			}
		}
		return props;
	}

}
