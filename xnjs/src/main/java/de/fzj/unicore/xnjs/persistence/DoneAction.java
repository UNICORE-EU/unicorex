package de.fzj.unicore.xnjs.persistence;

import java.io.Serializable;

import de.fzj.unicore.xnjs.ems.Action;
import eu.unicore.persist.annotations.ID;
import eu.unicore.persist.annotations.Table;
import eu.unicore.persist.util.JSON;
import eu.unicore.persist.util.Wrapper;

/**
 * wrapper for storing "done" actions in a different database
 *
 * @author schuller
 */
@Table(name="FINISHED_JOBS")
@JSON(customHandlers={Wrapper.WrapperConverter.class,GSONUtils.XmlBeansConverter.class})
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
