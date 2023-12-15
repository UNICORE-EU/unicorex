package de.fzj.unicore.xnjs.io;

import java.util.Calendar;
import java.util.regex.Pattern;

import de.fzj.unicore.xnjs.tsi.TSI;

public class SimpleFindOptions{

	private SimpleFindOptions(){}
	
	public static FileFilter suffixMatch(final String suffix, final boolean recurse){
		return new FileFilter(){
	
			public boolean accept(XnjsFile file, TSI tsi) {
				return file.getPath().endsWith(suffix);
			}
			
			public boolean recurse() {
				return recurse;
			}
		};
	}
	
	
	public static FileFilter prefixMatch(final String prefix, final boolean recurse){
		return new FileFilter(){
	
			public boolean accept(XnjsFile file, TSI tsi) {
				return file.getPath().startsWith(prefix);
			}
			
			public boolean recurse() {
				return recurse;
			}
		};
	}
	
	public static FileFilter nameContains(final String part, final boolean recurse){
		return new FileFilter(){
	
			public boolean accept(XnjsFile file, TSI tsi) {
				return file.getPath().contains(part);
			}
			
			public boolean recurse() {
				return recurse;
			}
		};
	}
	
	public static FileFilter lowerSizeBound(final long size, final boolean recurse){
		return new FileFilter(){
	
			public boolean accept(XnjsFile file, TSI tsi) {
				return file.getSize()>=size;
			}
			
			public boolean recurse() {
				return recurse;
			}
		};
	}
	
	public static FileFilter upperSizeBound(final long size, final boolean recurse){
		return new FileFilter(){
	
			public boolean accept(XnjsFile file, TSI tsi) {
				return file.getSize()<=size;
			}
			
			public boolean recurse() {
				return recurse;
			}
		};
	}

	public static FileFilter lastAccessBefore(final Calendar lastAccess, final boolean recurse){
		return new FileFilter(){
	
			public boolean accept(XnjsFile file, TSI tsi) {
				return file.getLastModified().before(lastAccess);
			}
			
			public boolean recurse() {
				return recurse;
			}
		};
	}

	public static FileFilter lastAccessAfter(final Calendar lastAccess, final boolean recurse){
		return new FileFilter(){
	
			public boolean accept(XnjsFile file, TSI tsi) {
				return file.getLastModified().after(lastAccess);
			}
			
			public boolean recurse() {
				return recurse;
			}
		};
	}

	public static FileFilter filesOnly(final boolean recurse){
		return new FileFilter(){
	
			public boolean accept(XnjsFile file, TSI tsi) {
				return !file.isDirectory();
			}
			
			public boolean recurse() {
				return recurse;
			}
		};
	}

	public static FileFilter directoriesOnly(final boolean recurse){
		return new FileFilter(){
	
			public boolean accept(XnjsFile file, TSI tsi) {
				return file.isDirectory();
			}
			
			public boolean recurse() {
				return recurse;
			}
		};
	}
	
	/**
	 * simple matching of "*" and "?" within a string expression
	 * @param match
	 */
	public static FileFilter stringMatch(final String match, final boolean recurse){
		return new FileFilter(){
	
			final Pattern p=Pattern.compile(makeRegExp());
			
			public boolean accept(XnjsFile file, TSI tsi) {
				return p.matcher(file.getPath()).find();
			}
			
			public boolean recurse() {
				return recurse;
			}
			
			private String makeRegExp(){
				StringBuilder pattern=new StringBuilder();
				pattern.append("^");
				pattern.append(match.replace(".","\\.").replace("*", ".*").replace("?", "."));
				pattern.append("$");
				return pattern.toString();
			}
		};
	}
	
	/**
	 * matches according to a given regular expression
	 * 
	 * @param regularExpression - a (Java) regular expression
	 */
	public static FileFilter regExpMatch(final String regularExpression, final boolean recurse){
		return new FileFilter(){
	
			final Pattern p=Pattern.compile(regularExpression);
			
			public boolean accept(XnjsFile file, TSI tsi) {
				return p.matcher(file.getPath()).find();
			}
			public boolean recurse() {
				return recurse;
			}
		};
	}
	
	public static boolean isWildCard(String pattern){
		return pattern!=null && (pattern.contains("*")||pattern.contains("?"));
	}
	
}
