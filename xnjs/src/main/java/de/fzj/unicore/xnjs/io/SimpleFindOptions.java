/*********************************************************************************
 * Copyright (c) 2008 Forschungszentrum Juelich GmbH 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * (1) Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the disclaimer at the end. Redistributions in
 * binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 * 
 * (2) Neither the name of Forschungszentrum Juelich GmbH nor the names of its 
 * contributors may be used to endorse or promote products derived from this 
 * software without specific prior written permission.
 * 
 * DISCLAIMER
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *********************************************************************************/
 

package de.fzj.unicore.xnjs.io;

import java.util.Calendar;
import java.util.regex.Pattern;

import de.fzj.unicore.xnjs.tsi.TSI;

/**
 * provides some common find options 
 * 
 * @author schuller
 */
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
