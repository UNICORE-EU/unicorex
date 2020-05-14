package de.fzj.unicore.xnjs.incarnation;

import de.fzj.unicore.xnjs.ems.Action;
import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.idb.ApplicationInfo;
import de.fzj.unicore.xnjs.idb.IDB;

public interface ITweaker {

	public abstract void preScript(ApplicationInfo appDescription, Action job,
			IDB idb) throws ExecutionException;

	public abstract String postScript(ApplicationInfo appDescription,
			Action job, IDB idb, String script)
			throws ExecutionException;

}