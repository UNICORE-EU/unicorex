package de.fzj.unicore.xnjs.ems.event;

import org.json.JSONObject;

import de.fzj.unicore.xnjs.ems.Action;

public interface INotificationSender {

	public void send(JSONObject msg, final Action action) throws Exception;

}
