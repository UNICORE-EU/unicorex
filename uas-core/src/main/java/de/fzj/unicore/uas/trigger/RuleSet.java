package de.fzj.unicore.uas.trigger;

import java.util.HashSet;

/**
 * RuleSet holds a set of rules for a given directory, plus some metadata.<br/>
 * Rules from parent directories can be inherited, also the list of child directories
 * to be included or excluded can be configured.<br/>
 * By default, all directories are included.
 *
 * @author schuller
 */
public class RuleSet extends HashSet<Rule>{

	private static final long serialVersionUID = 1L;

	private final String baseDirectory;
	
	private String[] includeDirPatterns;
	
	private String[] excludeDirPatterns;
	
	public RuleSet(String baseDirectory){
		this.baseDirectory=baseDirectory;
	}
	
	// if >= 0 the rule set will be executed periodically
	private int repeatInterval;

	public void setRepeatInterval(int interval){
		this.repeatInterval=interval;
	}
	
	public int getRepeatInterval(){
		return repeatInterval;
	}
	
	public String getBaseDirectory(){
		return baseDirectory;
	}

	public String[] getIncludeDirPatterns() {
		return includeDirPatterns;
	}

	public void setIncludeDirPatterns(String[] includeDirPatterns) {
		this.includeDirPatterns = includeDirPatterns;
	}

	public String[] getExcludeDirPatterns() {
		return excludeDirPatterns;
	}

	public void setExcludeDirPatterns(String[] excludeDirPatterns) {
		this.excludeDirPatterns = excludeDirPatterns;
	}

}
