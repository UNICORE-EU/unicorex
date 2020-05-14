package de.fzj.unicore.xnjs.io;

import java.util.ArrayList;
import java.util.List;

import de.fzj.unicore.xnjs.tsi.TSI;

/**
 * Composite find options allow to combine {@link SimpleFindOptions}
 * with AND and OR<br/>
 * The AND and OR branches are in turn combined using AND<br/>
 * @author schuller
 */
public class CompositeFindOptions implements FileFilter {

	private boolean recurse=true;
	
	private FileFilter base=new FileFilter(){
		public boolean accept(XnjsFile file, TSI tsi) {
			return true;
		}
		public boolean recurse() {
			return false;
		}
	};
	
	private final List<FileFilter> andOptions=new ArrayList<FileFilter>();
	
	private final List<FileFilter> orOptions=new ArrayList<FileFilter>();
	
	
	public boolean accept(XnjsFile file, TSI tsi) {
		boolean result=base.accept(file, tsi);
		for(FileFilter opt: andOptions){
			result=result && opt.accept(file, tsi);  
		}
		boolean orResult=false;
		for(FileFilter opt: orOptions){
			orResult=orResult || opt.accept(file, tsi);  
		}
		return result || orResult;
	}

	/**
	 * recurse flag
	 */
	public boolean recurse() {
		return recurse;
	}

	public CompositeFindOptions match(FileFilter opt){
		base=opt;
		return this;
	}
	
	
	public CompositeFindOptions and(FileFilter opt){
		andOptions.add(opt);
		return this;
	}
	
	public CompositeFindOptions or(FileFilter opt){
		orOptions.add(opt);
		return this;
	}

	public CompositeFindOptions setRecurse(boolean recurse){
		this.recurse=recurse;
		return this;
	}

}
