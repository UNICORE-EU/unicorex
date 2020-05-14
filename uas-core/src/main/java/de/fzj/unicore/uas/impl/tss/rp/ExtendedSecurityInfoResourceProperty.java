/*
 * Copyright (c) 2011-2012 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package de.fzj.unicore.uas.impl.tss.rp;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.unigrids.services.atomic.types.SecurityDocument;
import org.unigrids.services.atomic.types.SelectedXGroupType;

import de.fzj.unicore.uas.impl.UASWSResourceImpl;
import de.fzj.unicore.uas.xnjs.XNJSFacade;
import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.security.util.AuthZAttributeStore;
import de.fzj.unicore.wsrflite.xmlbeans.renderers.SecurityInfoRenderer;
import eu.unicore.security.Client;
import eu.unicore.security.Xlogin;
import eu.unicore.services.ws.impl.WSResourceImpl;
import eu.unicore.util.Log;

/**
 * Extends the USE {@link SecurityInfoRenderer} by providing 
 * actual client's groups. This is relevant if usage of OS groups 
 * is turned on (what is the default) and user has not selected some 
 * preferred groups. Of course this implementation can be used only by 
 * WS-Resources backed by a TSI.
 * 
 * @author K. Benedyczak
 */
public class ExtendedSecurityInfoResourceProperty extends SecurityInfoRenderer {

	public ExtendedSecurityInfoResourceProperty(WSResourceImpl parent, boolean addServerCert) {
		super(parent);
	}
	
	@Override
	protected void addSelectedGroup(SecurityDocument secDoc) {
		Client client = AuthZAttributeStore.getClient();
		Xlogin xloginO = client.getXlogin();
		boolean useOs = xloginO.isAddDefaultGroups();
		SelectedXGroupType selectedXGroup = SelectedXGroupType.Factory.newInstance();
		selectedXGroup.setUseOSDefaults(useOs);
		
		Set<String> allGroups = new HashSet<String>(); 
		String[] groupsFromTsi = getGroupsFromTsi(client);
		if (xloginO.isGroupSelected())
			selectedXGroup.setPrimaryGroup(xloginO.getGroup());
		else if (groupsFromTsi.length > 0)
			selectedXGroup.setPrimaryGroup(groupsFromTsi[0]);
		
		String[] supGroups = xloginO.getSelectedSupplementaryGroups(); 
		if (supGroups != null && supGroups.length > 0) {
			Collections.addAll(allGroups, supGroups);
			if (useOs) {
				Collections.addAll(allGroups, groupsFromTsi);
			}
		} else {
			if (useOs) {
				Collections.addAll(allGroups, groupsFromTsi);
			}
		}
		allGroups.remove(selectedXGroup.getPrimaryGroup());
			
		if (allGroups.size() > 0)
			selectedXGroup.setSupplementaryGroupArray(
				allGroups.toArray(new String[allGroups.size()]));
		
		secDoc.getSecurity().setClientSelectedXgroup(selectedXGroup);
	}
	

	private String[] getGroupsFromTsi(Client client) {
		if (client.getXlogin().getUserName() == null)
			return new String[0];
		
		String xnjsReference=((UASWSResourceImpl)parent).getXNJSReference();
		try{
			Kernel kernel=parent.getKernel();
			return XNJSFacade.get(xnjsReference, kernel).getTSI(client).getGroups();
		}catch(Exception ex){
			Log.logException("Can't get groups from the operating system", ex);
			return new String[0];
		}
	}
	
}
