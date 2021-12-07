package de.fzj.unicore.xnjs.util;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FilenameUtils;

import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.io.impl.UsernamePassword;
import de.fzj.unicore.xnjs.tsi.TSI;
import eu.unicore.util.Log;

public class IOUtils {

	private static final boolean nonUnix=File.separatorChar != '/';

	public static boolean isNonUnix(){
		return nonUnix;
	}

	/**
	 * calls digest.digest() and returns the string MD5 value
	 * 
	 * @param digest
	 */
	public static String md5String(MessageDigest digest){
		byte[] messageDigest = digest.digest();
		StringBuilder hexString = new StringBuilder();
		for (int i=0;i<messageDigest.length;i++) {
			String hex = Integer.toHexString(0xFF & messageDigest[i]); 
			if(hex.length()==1)hexString.append('0');
			hexString.append(hex);
		}
		return hexString.toString();
	}

	/**
	 * make an absolute path relative to the given root
	 * @param root - the root
	 * @param absolutePath - the full path
	 */
	public static String getRelativePath(String absolutePath, String root){
		return getRelativePath(new File(absolutePath), root);
	}


	/**
	 * Make an absolute, normalized path. 
	 * 
	 * @param root - the root
	 * @param relativePath - the relative path
	 * @param sanitizeRelativePath - if <code>true</code> the relative path will be checked for tricks involving ".."
	 * @return sanitized, normalized path
	 * @throws IllegalArgumentException if sanitization fails
	 */
	public static String getFullPath(String root, String relativePath, boolean sanitizeRelativePath){
		String saneRelPath = relativePath;
		if(sanitizeRelativePath){
			saneRelPath = getNormalizedPath(relativePath);
			if(saneRelPath == null)throw new IllegalArgumentException("Invalid relative path: <"+relativePath+">");
		}
		return getNormalizedPath(root+"/"+saneRelPath);
	}

	public static String getNormalizedPath(String path){
		while(path.startsWith("//"))path=path.substring(1);
		return FilenameUtils.normalize(path, true);
	}

	/**
	 * get the path elements, handling ".." and "." properly
	 * @param f - a File
	 * @param root - the root directory
	 * @return a list of path elements, starting with the file name
	 * @throws IllegalArgumentException if path is invalid (for example contains too many "/../")
	 */
	static List<String> getRelativePathList(File f, String root){
		if(isNonUnix()){
			return getRelativePathListNonUnix(f,root);
		}

		//cut off a possible trailing "/"
		if(root.length()!=1 && root.endsWith("/")){
			root=root.substring(0, root.lastIndexOf('/'));
		}
		//normalize
		while(root.contains("//")){
			root=root.replace("//", "/");
		} 

		List<String> l = new ArrayList<String>();
		File r=f;
		int skip=0;
		try{
			while(f != null && !root.equals(f.getPath())) {
				String name=f.getName();
				if("..".equals(name))skip++;
				else if(!".".equals(name)){
					if(skip>0)skip--;
					else{
						l.add(name);
					}
				}
				f=f.getParentFile();
			}
		}catch(Exception ex){
			throw new IllegalArgumentException("Invalid path: <"+r.getPath()+">");
		}
		if(skip>0)throw new IllegalArgumentException("Invalid path: <"+r.getPath()+">: too many '..'");
		return l;
	}


	/**
	 * windows version
	 * 
	 * @param f - a file 
	 * @param root
	 */
	static List<String> getRelativePathListNonUnix(File f, String root){
		int skip=0;
		List<String> l = new ArrayList<String>();
		File r=f;
		try{
			f=f.getCanonicalFile();
			File rootFile=new File(root).getCanonicalFile();
			while(f != null && !rootFile.getPath().equals(f.getPath())) {
				String name=f.getName();
				if("..".equals(name))skip++;
				else if(!".".equals(name)){
					if(skip>0)skip--;
					else{
						l.add(name);
					}
				}
				f=f.getParentFile();
			}
		}catch(Exception e){
			throw new IllegalArgumentException(e);
		}

		if(skip>0)throw new IllegalArgumentException("Invalid path: <"+r.getPath()+">: too many '..'");
		return l;
	}


	/**
	 * get the relative path for the given file. UNIX style separator is assumed.
	 * @param f - the (absolute) file
	 * @param root - the root path
	 * @return relative path
	 */
	public static String getRelativePath(File f, String root){
		List<String>list=getRelativePathList(f, root);
		StringBuilder sb=new StringBuilder();
		for(String s: list){
			sb.insert(0, s);
			sb.insert(0, "/");
		}
		if(sb.length()==0)sb.append("/");
		return sb.toString();
	}	

	public static String md5(InputStream in)throws IOException{
		MessageDigest md5=null;
		try{
			md5=MessageDigest.getInstance("MD5");
		}catch(NoSuchAlgorithmException ne){
			throw new IOException("Algorithm MD5 not availble.");
		}
		byte[]buf=new byte[8192];
		int len;
		for(;;){
			len=in.read(buf);
			if(len<0)break;
			md5.update(buf, 0, len);
		}
		return md5String(md5);
	}

	public static String md5(File file)throws IOException{
		FileInputStream fis=new FileInputStream(file);
		try{
			return md5(fis);
		}
		finally{
			fis.close();
		}
	}

	public static String readFile(File file)throws IOException{
		FileInputStream fis=new FileInputStream(file);
		ByteArrayOutputStream os=new ByteArrayOutputStream();
		try{
			copy(fis,os,128*1024);
			return os.toString();
		}
		finally{
			fis.close();
		}
	}


	public static String readTSIFile(TSI tsi, String name, int limit)throws IOException, ExecutionException{
		try(InputStream is = tsi.getInputStream(name)){
			ByteArrayOutputStream bos=new ByteArrayOutputStream();
			copy(is,bos,limit);
			return bos.toString();
		}
	}

	public static void copy(InputStream is, OutputStream os, int limit)throws IOException{
		int c=0;
		int total=0;
		byte[] buf=new byte[8192];
		while( (c = is.read(buf))!=-1){
			os.write(buf,0,c);
			total+=c;
			if(total>limit){
				os.write("***** length limit exceeded, result is truncated! ****".getBytes());
			}
		}
	}

	public static String toString(InputStream is, int limit)throws IOException{
		ByteArrayOutputStream bos=new ByteArrayOutputStream();
		copy(is,bos,limit);
		return bos.toString();
	}

	/**
	 * adds credentials to an URL compliant with RFC 1738 
	 * @param original - URL without credentials
	 * @param creds - username and (optional) password
	 */
	public static URL addFTPCredentials(URL original, UsernamePassword creds){
		if(creds!=null && creds.getUser()!=null){
			return addFTPCredentials(original, creds.getUser(), creds.getPassword());
		}
		return original; 
	}

	/**
	 * adds credentials to an URL compliant with RFC 1738 
	 * @param original - URL without credentials
	 * @param user - required user name
	 * @param password - optional password
	 */
	public static URL addFTPCredentials(URL original, String user, String password){
		try{
			//constuct FTP URL 
			int port=original.getPort();
			StringBuilder sb=new StringBuilder();
			sb.append(original.getProtocol()).append("://");
			if(user!=null){
				sb.append(user);
				if(password!=null){
					sb.append(":").append(password);
				}
				sb.append("@");
			}
			sb.append(original.getHost());
			if(port!=-1){
				sb.append(":").append(port);
			}
			sb.append(original.getFile());
			return new URL(sb.toString());
		}catch(Exception e){
			Log.logException("Could not construct URL with credentials from <"+original.toString()+">", e);
		}
		return original; 
	}

	/**
	 * create a scp address line from the original URI
	 * 
	 * @param original
	 * @param user
	 */
	public static String makeSCPAddress(String original, String user)throws Exception{
		try{
			URI uri=new URI(original);
			StringBuilder sb=new StringBuilder();
			if(user!=null){
				sb.append(user);
				sb.append("@");
			}
			String spec=uri.getSchemeSpecificPart();
			while(spec.startsWith("/")){
				spec=spec.substring(1);
			}
			//split host and path
			sb.append(uri.getHost()); 
			if(uri.getPort()>-1){
				//TODO: port is an scp option "-P <port>"
				//scp-wrapper should be aware of alternative port
			}
			sb.append(":");
			sb.append(uri.getPath());
			return sb.toString();
		}catch(Exception e){
			Log.logException("Could not construct URL with user name from <"+original+">", e);
		}
		return original.toString(); 
	}

	private static final NumberFormat format=NumberFormat.getNumberInstance();

	/**
	 * format a number with the given maximum number of fractional digits
	 * @param number
	 * @param numDigits - maximum number of fractional digits
	 */
	public static synchronized String format(Object number, int numDigits){
		format.setMaximumFractionDigits(numDigits);
		return format.format(number);
	}

	public static void closeQuietly(Closeable stream){
		try{
			stream.close();
		}catch(Exception ex) {}
	}


	/**
	 * Include the path in single quotes "'" so the shell will not process the 
	 * content -> avoid user command injection.
	 * Also, escapes quotes that are already present in the path!
	 */
	public static String quote(String input){
		return "'"+input.replace("'","'\\''")+"'";
	}

	public static String getBasicAuth(String username, String pass){
		StringBuilder tmp = new StringBuilder();
		tmp.append(username);
		tmp.append(":");
		tmp.append(pass == null ? "null" : pass);
		byte[] b64 = Base64.encodeBase64(tmp.toString().getBytes());
		try{
			return "Basic "+new String(b64, "UTF-8");
		}catch(UnsupportedEncodingException ex){
			return "Basic "+new String(b64);
		}
	}
}
