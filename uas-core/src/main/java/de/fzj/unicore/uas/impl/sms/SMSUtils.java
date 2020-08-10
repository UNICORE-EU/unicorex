package de.fzj.unicore.uas.impl.sms;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.unigrids.services.atomic.types.ACLEntryTypeType;
import org.unigrids.services.atomic.types.PermissionsType;
import org.unigrids.x2006.x04.services.sms.ACLChangeModeType;
import org.unigrids.x2006.x04.services.sms.ChangeACLEntryType;
import org.unigrids.x2006.x04.services.sms.ChangeACLType;
import org.unigrids.x2006.x04.services.sms.ChangePermissionsEntryType;
import org.unigrids.x2006.x04.services.sms.ExtendedChangePermissionsType;
import org.unigrids.x2006.x04.services.sms.PermissionsChangeModeType;
import org.unigrids.x2006.x04.services.sms.PermissionsClassType;

import de.fzj.unicore.uas.util.LogUtil;
import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.io.ACLEntry.Type;
import de.fzj.unicore.xnjs.io.ChangeACL;
import de.fzj.unicore.xnjs.io.ChangeACL.ACLChangeMode;
import de.fzj.unicore.xnjs.io.ChangePermissions.Mode;
import de.fzj.unicore.xnjs.io.ChangePermissions.PermissionsClass;
import de.fzj.unicore.xnjs.io.IStorageAdapter;
import de.fzj.unicore.xnjs.io.Permissions;
import de.fzj.unicore.xnjs.util.URIUtils;

/**
 * utility methods for the SMS
 */
public class SMSUtils {

	// use SMS logger for this class
	private static final Logger logger = LogUtil.getLogger(LogUtil.DATA,SMSBaseImpl.class);

	private SMSUtils(){}


	public static void extendedChangePermissions(String file, IStorageAdapter tsi, 
			ExtendedChangePermissionsType extendedCh, boolean recursive) throws ExecutionException {
		ChangePermissionsEntryType request[] = extendedCh.getChangePermissionsEntryArray();
		if (request == null)
			throw new ExecutionException("Invalid request - null change perms array");
		de.fzj.unicore.xnjs.io.ChangePermissions xnjsRequest[] = 
				new de.fzj.unicore.xnjs.io.ChangePermissions[request.length];
		for (int i=0; i<request.length; i++) {
			xnjsRequest[i] = new de.fzj.unicore.xnjs.io.ChangePermissions();
			if (request[i].getKind() == null || request[i].getMode() == null ||
					request[i].getPermissions() == null)
				throw new ExecutionException("Invalid request - null change perms entries");

			if (request[i].getKind().equals(PermissionsClassType.GROUP))
				xnjsRequest[i].setClazz(PermissionsClass.GROUP);
			else if (request[i].getKind().equals(PermissionsClassType.USER))
				xnjsRequest[i].setClazz(PermissionsClass.OWNER);
			else
				xnjsRequest[i].setClazz(PermissionsClass.OTHER);

			if (request[i].getMode().equals(PermissionsChangeModeType.ADD))
				xnjsRequest[i].setMode(Mode.ADD);
			else if (request[i].getMode().equals(PermissionsChangeModeType.SUBTRACT))
				xnjsRequest[i].setMode(Mode.SUBTRACT);
			else
				xnjsRequest[i].setMode(Mode.SET);

			xnjsRequest[i].setPermissions(request[i].getPermissions());
		}		
		tsi.chmod2(file, xnjsRequest, recursive);
	}

	public static void setACL(String file, IStorageAdapter tsi, ChangeACLType aclChange, boolean recursive) 
			throws ExecutionException {
		ChangeACLEntryType request[] = aclChange.getChangeACLEntryArray();
		if (request == null)
			throw new ExecutionException("Invalid request - null change ACL array");
		ChangeACL[] xnjsRequest = new ChangeACL[request.length];
		for (int i=0; i<request.length; i++) {
			if (request[i].getKind() == null || request[i].getMode() == null ||
					request[i].getPermissions() == null)
				throw new ExecutionException("Invalid request - null change ACL entries");
			xnjsRequest[i] = new ChangeACL();
			if (request[i].getMode().equals(ACLChangeModeType.MODIFY)) {
				xnjsRequest[i].setChangeMode(ACLChangeMode.MODIFY);
				xnjsRequest[i].setPermissions(request[i].getPermissions());
			} else
				xnjsRequest[i].setChangeMode(ACLChangeMode.REMOVE);

			xnjsRequest[i].setSubject(request[i].getSubject());

			if (request[i].getKind().equals(ACLEntryTypeType.GROUP))
				xnjsRequest[i].setType(Type.GROUP);
			else
				xnjsRequest[i].setType(Type.USER);

			if (request[i].isSetDefaultACL() && request[i].getDefaultACL())
				xnjsRequest[i].setDefaultACL(true);
			else
				xnjsRequest[i].setDefaultACL(false);
		}

		boolean clearAll = aclChange.isSetClearACL() && aclChange.getClearACL();
		tsi.setfacl(file, clearAll, xnjsRequest, recursive);
	}

	public static void legacyChangePermissions(String file, IStorageAdapter tsi, org.unigrids.x2006.x04.services.sms.ChangePermissionsDocument.ChangePermissions in) 
			throws ExecutionException {
		PermissionsType p=in.getPermissions();
		Permissions permissions=new Permissions(p.getReadable(), p.getWritable(), p.getExecutable());
		tsi.chmod(file, permissions);
		if(logger.isDebugEnabled()){
			logger.debug("Changed user permissions for '"+file+"' to <"+permissions.toString()+">");
		}
	}


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
	public static de.fzj.unicore.xnjs.io.ChangePermissions[] getChangePermissions(String unixPermissions) {
		if(!unixPermissions.matches("([rwx-][rwx-][rwx-]){1,3}")) {
			throw new IllegalArgumentException("Illegal permissions string <"+unixPermissions+">");
		}
		Matcher m = unixPermPattern.matcher(unixPermissions);
		List<de.fzj.unicore.xnjs.io.ChangePermissions>res = new ArrayList<>();
		int i=0;
		while(m.find() && i<3) {
			String kind = jsonPermKinds[i];
			String perm = m.group();
			if(perm!=null){
				de.fzj.unicore.xnjs.io.ChangePermissions cp = new de.fzj.unicore.xnjs.io.ChangePermissions();
				cp.setClazz(PermissionsClass.valueOf(kind));
				cp.setMode(Mode.SET);
				cp.setPermissions(perm);
				res.add(cp);
			}
			i++;
		}
		return res.toArray(new de.fzj.unicore.xnjs.io.ChangePermissions[res.size()]);
	}
	
	
}
