package de.fzj.unicore.uas.impl.sms;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.uas.util.LogUtil;
import eu.unicore.xnjs.io.ChangePermissions.Mode;
import eu.unicore.xnjs.io.ChangePermissions.PermissionsClass;
import eu.unicore.xnjs.util.URIUtils;

/**
 * utility methods for the SMS
 */
public class SMSUtils {

	// use SMS logger for this class
	private static final Logger logger = LogUtil.getLogger(LogUtil.DATA,SMSBaseImpl.class);

	private SMSUtils(){}

	/**
	 * URL-decode a string (e.g. replacing "%20" by spaces)
	 * 
	 * @param p - the string to decode
	 * @return decoded string
	 */
	public static String urlDecode(String p){
		try{
			return URLDecoder.decode(p.replace("+","%2B"), "UTF-8");
		}catch(Exception ex){
			logger.warn(ex);
			return p;
		}
	}

	/**
	 * fix illegal characters (like spaces) in the parameter,
	 * so a URL can be built from it
	 * @param orig
	 */
	public static String urlEncode(String orig){
		try{
			return URIUtils.encodeAll(orig);
		}catch(Exception e){
			logger.error(e);
			return orig;
		}
	}


	private static final String[]jsonPermKinds = {"OWNER","GROUP","OTHER"};


	private static final Pattern unixPermPattern = Pattern.compile("([rwx-][rwx-][rwx-])");

	/**
	 * convert the UNIX style permissions to XNJS chmod2 request
	 * @param unixPermissions - UNIX style permissions string like "rwx------"
	 */
	public static eu.unicore.xnjs.io.ChangePermissions[] getChangePermissions(String unixPermissions) {
		if(!unixPermissions.matches("([rwx-][rwx-][rwx-]){1,3}")) {
			throw new IllegalArgumentException("Illegal permissions string <"+unixPermissions+">");
		}
		Matcher m = unixPermPattern.matcher(unixPermissions);
		List<eu.unicore.xnjs.io.ChangePermissions>res = new ArrayList<>();
		int i=0;
		while(m.find() && i<3) {
			String kind = jsonPermKinds[i];
			String perm = m.group();
			if(perm!=null){
				eu.unicore.xnjs.io.ChangePermissions cp = new eu.unicore.xnjs.io.ChangePermissions();
				cp.setClazz(PermissionsClass.valueOf(kind));
				cp.setMode(Mode.SET);
				cp.setPermissions(perm);
				res.add(cp);
			}
			i++;
		}
		return res.toArray(new eu.unicore.xnjs.io.ChangePermissions[res.size()]);
	}
	
	
}
