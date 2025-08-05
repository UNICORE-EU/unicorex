package eu.unicore.uas.impl.job;

import java.util.Calendar;

import eu.unicore.uas.impl.BaseInitParameters;
import eu.unicore.xnjs.ems.Action;

public class JobInitParameters extends BaseInitParameters {

	public final Action action;

	public JobInitParameters(Action action, Calendar terminationTime) {
		super(action.getUUID(), 
				terminationTime!=null? null: TerminationMode.DEFAULT, 
				terminationTime);
		this.action = action;
	}

	public boolean autostart = false;

	public boolean no_xnjs_submit = false;

	public String SMS;

	public String[] initialTags;

}
