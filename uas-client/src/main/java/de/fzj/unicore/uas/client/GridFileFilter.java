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
 ********************************************************************************/
package de.fzj.unicore.uas.client;

import java.util.regex.Pattern;

import org.unigrids.services.atomic.types.GridFileType;

/**
 * Provides filter criteria for GridFileTypes
 * 
 * @author Christian Hohmann
 * 
 */
public class GridFileFilter implements IGridFileFilter {

	public final static int FILES_AND_FOLDERS = 0;
	public final static int ONLY_FILES = 1;
	public final static int ONLY_FOLDERS = 2;

	private int fileFolder = 0;
	private String initialCriteria;
	private String searchCriteria;
	private Pattern compiledPattern;
	private boolean doubleStarExists = false;
	private String criteriaBeforeDoubleStar;
	private Pattern compiledPrePattern;

	/**
	 * criteria needs to be relative to search root
	 */
	public GridFileFilter(String criteria) {
		this.setCriteria(criteria);
	}

	/**
	 * checks the syntax of criteria and returns it in the corrected format
	 * /path/path/file (leading / and no / as last char except of root (/)
	 * @param criteria
	 * @return correct syntax of criteria
	 */
	
	private static String syntaxCheck (String criteria){
		if (!criteria.startsWith("/")) {
			criteria = "/" + criteria;
		}

		if (criteria.length() > 1 && criteria.endsWith("/")) {
			criteria = criteria.substring(0, criteria.length() - 1);
		}
		return criteria;
	}
	
	
	/**
	 * translates the given criteria into a regular expression representing criteria
	 * is static to allow flexible use of this method
	 * @param criteria
	 * @return the translated criteria
	 */
	public static String translateCriteria(String criteria){
		criteria = syntaxCheck(criteria);

		if (criteria.indexOf("?") == -1 && criteria.indexOf("*") == -1) {
			/*
			 * no symbols in criteria, nothing to translate
			 */
			return criteria;
		} else {
			
			/*
			 * masking of '.' in searchString in [.], for . is searched literally
			 */
			
			int loopstop = criteria.length();
			for (int i = 0; i < loopstop; i++) {
				if (criteria.charAt(i) == '.') {
					String tempCrit;
					if (i != loopstop-1){
					tempCrit = criteria.substring(0, i) + "[.]"
							+ criteria.substring(i + 1, criteria.length());
					}
					else{
						tempCrit = criteria.substring(0, i) + "[.]";
					}
					criteria = tempCrit;
					i += 2;
					loopstop +=2;
				}
			}
			
			/*
			 * replace ? with . 
			 * replace * with [^/]* 
			 * replace ** with ( / | ( / . * / )) (without blanks)
			 * 
			 */
			criteria = criteria.replace('?', '.');
			loopstop = criteria.length();
			for (int i = 0; i < loopstop; i++) {
				if (criteria.charAt(i) == '*') {
					if (i + 1 < criteria.length()
							&& criteria.charAt(i + 1) == '*') {
						// double star
						String tmpString = criteria.substring(0, i - 1)
								+ "(/|(/.*/))"
								+ criteria.substring(i + 3, criteria.length());
						criteria = tmpString;
						loopstop += 6;
						i += 8;
					} else {
						String tmpString = criteria.substring(0, i) + "[^/]*"
								+ criteria.substring(i + 1, criteria.length());
						criteria = tmpString;
						loopstop += 4;
						i += 4;
					}
				}
			}
			return criteria;
		}
	}
	
	/**
	 * translates the given criteria into a regular expression and sets it for this filter
	 */ 
	public void setCriteria(String criteria) {
		criteria = syntaxCheck(criteria);
		// set initialCriteria
		this.initialCriteria = criteria;
		criteria = translateCriteria(criteria);

		this.searchCriteria = criteria;
		this.compiledPattern = Pattern.compile(this.searchCriteria);
		int doubleStarPosition = criteria.indexOf("(/|(/.*/))");
		if (doubleStarPosition!=-1) {
			//criteria has doubleStar
			this.doubleStarExists = true;
			this.criteriaBeforeDoubleStar = criteria.substring(0, doubleStarPosition);
			if (this.criteriaBeforeDoubleStar == "") {
				this.criteriaBeforeDoubleStar = "/";
			}
			this.compiledPrePattern = Pattern
					.compile(criteriaBeforeDoubleStar);
		}
	}

	public boolean setFileFolderCriteria(int i) {
		if (i < 0 || i > 2) {
			return false;
		}
		this.fileFolder = i;
		return true;
	}

	/*
	 * return true if file object matches the set filter criteria
	 */
	/* (non-Javadoc)
	 * @see de.fzj.unicore.uas.client.IGridFileFilter#match(org.unigrids.services.atomic.types.GridFileType)
	 */
	public boolean match(GridFileType file) {
		switch (this.fileFolder) {
		case ONLY_FILES:
			if (file.getIsDirectory()) {
				return false;
			}
			break;
		case ONLY_FOLDERS:
			if (!file.getIsDirectory()) {
				return false;
			}
			break;
		}
		// elements in root folder have no leading '/'
		String path = file.getPath();
		if (!path.startsWith("/")) {
			path = "/" + path;
		}
		return this.compiledPattern.matcher(path).matches();
	}

	/*
	 * return true if matches are possible for childs of the given file element
	 */
	/* (non-Javadoc)
	 * @see de.fzj.unicore.uas.client.IGridFileFilter#browseSubfolder(org.unigrids.services.atomic.types.GridFileType)
	 */
	public boolean browseSubfolder(GridFileType file) {
		String path = file.getPath();
		if (!path.startsWith("/")) {
			path = "/" + path;
		}

		int depthPath = this.depthOfPath(path);
		String criteria;
		String cuttedCriteria;
		if (this.doubleStarExists) {
			criteria = this.criteriaBeforeDoubleStar;
			cuttedCriteria = this.criteriaBeforeDoubleStar;
		} else {
			criteria = this.searchCriteria;
			cuttedCriteria = this.searchCriteria;
		}
		int depthCriteria = this.depthOfPath(criteria);
		if (depthPath == depthCriteria) {
			if (this.doubleStarExists) {
				// children match is possible if this matches to criteria part
				// before **
				return compiledPrePattern.matcher(path).matches();
			} else { // criteria is on same depth level than path, children
						// can not match criteria
				return false;
			}
		} else {
			if (depthPath < depthCriteria) {
				// match with part of preCriteria because path is shorter
				// than criteria before **

				int counter = depthOfPath(path);
				int critCounter = 0;
				for (int i = 0; i < cuttedCriteria.length(); i++) {
					if (cuttedCriteria.charAt(i) == '/') {
						if (i > 0) {
							if (cuttedCriteria.charAt(i - 1) != '^') {
								critCounter += 1;
								if (critCounter == counter + 1) {
									cuttedCriteria = cuttedCriteria.substring(
											0, i);
									break;
								}
							}
						} else {
							critCounter += 1;
							if (critCounter == counter + 1) {
								cuttedCriteria = cuttedCriteria.substring(0, i);
								break;
							}
						}
					}
				}
				Pattern p = Pattern.compile(cuttedCriteria);
				return p.matcher(path).matches();

			} else {
				/*
				 * if doubleStarExists, browsing until bottom is needed else no
				 * match can be possible
				 */
				return this.doubleStarExists;
			}
		}
	}

	/**
	 * 
	 * @param expression
	 * @param searchChar
	 * @return how many times '/' is in expression, '^/' is not counted!
	 */
	private int depthOfPath(String expression) {
		char searchChar = '/';
		int retval = 0;
		if (expression == null) {
			return -1;
		}
		for (int i = 0; i < expression.length(); i++) {
			if (expression.charAt(i) == searchChar) {
				if (i > 0) {
					if (expression.charAt(i - 1) != '^') {
						retval += 1;
					}
				} else {
					retval += 1;
				}
			}
		}
		return retval;
	}

	/* (non-Javadoc)
	 * @see de.fzj.unicore.uas.client.IGridFileFilter#getInitCriteria()
	 */
	public String getInitCriteria() {
		return this.initialCriteria;
	}
}