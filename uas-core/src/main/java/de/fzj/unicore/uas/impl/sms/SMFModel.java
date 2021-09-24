package de.fzj.unicore.uas.impl.sms;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.impl.UASBaseModel;

public class SMFModel extends UASBaseModel {

	private static final long serialVersionUID = 1L;

	/**
	 * maps SMS to SMS owner DNs 
	 */
	Map<String, String>smsOwners=new HashMap<String, String>();

	public Map<String, String> getSmsOwners() {
		return smsOwners;
	}

	public void setSmsOwners(Map<String, String> smsOwners) {
		this.smsOwners = smsOwners;
	}

	public List<String> getSmsIDs() {
		return getChildren(UAS.SMS);
	}

	public void setSmsIDs(List<String> smsIDs) {
		getChildren().put(UAS.SMS, smsIDs);
	}

	@Override
	public boolean removeChild(String uid) {
		smsOwners.remove(uid);
		return super.removeChild(uid);
	}
	
	
}
