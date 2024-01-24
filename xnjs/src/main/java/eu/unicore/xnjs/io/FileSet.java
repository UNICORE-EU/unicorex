package eu.unicore.xnjs.io;

import java.io.File;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import eu.unicore.xnjs.util.IOUtils;

/**
 * describes a file set
 *
 * @author schuller
 */
public class FileSet implements Serializable {

	private static final long serialVersionUID = 1L;

	final String[] includes;

	final String[] excludes;
	
	final String base;
	
	final boolean recurse;

	/**
	 * construct a fileset from the given "simple" wildcard expression
	 * @param expr - the expression
	 * @param isDir - if <code>true</code>, the expression denotes a single directory. All files in 
	 *        that directory will be included recursively 
	 */
	public FileSet(String expr, boolean isDir){
		if(!expr.startsWith("/")){
			expr = "/"+expr;
		}
		if(!isDir){
			File f = new File(expr);
			this.base = findBase(f);
			this.includes=new String[]{expr};
			this.excludes=new String[]{};
			this.recurse = mustRecurse();
		}
		else{
			this.includes=new String[]{IOUtils.getNormalizedPath(expr+"/**")};
			this.excludes=new String[]{};
			this.base = expr;
			this.recurse = true;
		}
	}	

	public FileSet(String base, String[] incl, String[] excl){
		this.includes=incl;
		this.excludes=excl;
		this.base=base;
		this.recurse = mustRecurse();
	}
	
	public boolean mustRecurse(){
		boolean recurse = false;
		for(String i : includes){
			String rel = IOUtils.getRelativePath(i, base);
			recurse = recurse || matchesSubdirs(rel);
		}
		return recurse;
	}

	private boolean matchesSubdirs(String exp){
		String f = new File(exp).getParent();
		return f!=null && (f.contains("*") || f.contains("?"));
	}
	
	/**
	 * finds the base of a path expression, which is the topmost directory that
	 * does not contain any wildcards
	 */
	private String findBase(File path){
		File parent = path.getParentFile();
		if(hasWildcards(parent.getName())){
			return findBase(parent);
		}
		else{
			return path.getParent();	
		}
	}
	
	/**
	 * construct a fileset from the given path specification
	 * @param expr - the expression (must not denote a single directory)
	 */
	public FileSet(String expr){
		this(expr,false);
	}

	/**
	 * check if the given (full) path is to be included in this fileset
	 * @param path - full path to some file
	 */
	public boolean matches(String path){
		if(!path.startsWith("/"))path="/"+path;
		boolean included=isIncluded(path);
		boolean excluded=isExcluded(path);
		return included && !excluded;
	}
	
	public boolean isRecurse(){
		return recurse;
	}

	public String getBase(){
		return base;
	}

	public String[] getIncludes(){
		return includes;
	}
	
	public String[] getExcludes(){
		return excludes;
	}
	
	public boolean isMultifile(){
		return includes.length>1 || hasWildcards(includes[0]);
	}
	
	public String toString(){
		return "[Fileset base="+base+" incl="+Arrays.deepToString(includes)+" excl="+Arrays.deepToString(excludes)+" recurse="+recurse+"]";
	}
	
	protected boolean isIncluded(String path){
		boolean res=false;
		//check if it is in the includes
		if(includes.length>0){
			for(String include: includes){
				res=res || match(path,include);
			}
		}
		//else everything is included
		else res=true;
		return res;
	}
	
	protected boolean isExcluded(String path){
		if(excludes.length>0){
			for(String exclude: excludes){
				if(match(path,exclude))return true;
			}
		}
		return false; 
	}
	
	boolean match(String path, String expr){
		Pattern p = getPattern(expr);
		return p.matcher(path).matches();
	}
	
	private Map<String, Pattern>patterns=new HashMap<String, Pattern>();
	
	Pattern getPattern(String expr){
		Pattern p=patterns.get(expr);
		if(p==null){
			p=compilePattern(expr);
			patterns.put(expr, p);
		}
		return p;
	}
	

	/*
	 * translate wildcards "*" and "?" into a regular expression pattern
	 * and create the regexp Pattern
	 * 
	 * TODO handle special characters?
	 */
	private Pattern compilePattern(String expr){
		String pattern = expr.replace(".","\\.")
				.replace("/**", "___PLACEH_O_LDER___")
				.replace("*", "[^/]*")
				.replace("___PLACEH_O_LDER___",".*")
				.replace("?", ".");
		if(expr.startsWith("/")){
			pattern = "/?"+pattern;
		}
		return Pattern.compile(pattern);
	}
	
	public static boolean hasWildcards(String expr){
		return expr.contains("*")|| expr.contains("?");
	}
}
