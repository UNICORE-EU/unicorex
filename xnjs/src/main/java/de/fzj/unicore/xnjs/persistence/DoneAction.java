package de.fzj.unicore.xnjs.persistence;

import java.io.Serializable;

import de.fzj.unicore.persist.annotations.ID;
import de.fzj.unicore.persist.annotations.Table;
import de.fzj.unicore.persist.util.JSON;
import de.fzj.unicore.persist.util.Wrapper;
import de.fzj.unicore.xnjs.ems.Action;

/**
 * wrapper for storing "done" actions in a different database
 *
 * @author schuller
 */
@Table(name="FINISHED_JOBS")
@JSON(customHandlers={GSONUtils.XmlBeansConverter.class,Wrapper.WrapperConverter.class})
public class DoneAction implements Serializable{

	private static final long serialVersionUID = 1L;

	private final Action action;
	
	DoneAction(Action action){
		this.action=action;
	}
	
	@ID
	public String getUUID() {
		return action.getUUID();
	}
	
	public Action getAction(){
		return action;
	}
}
