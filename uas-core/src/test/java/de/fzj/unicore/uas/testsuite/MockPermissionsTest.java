/*
 * Copyright (c) 2010 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on 2011-06-11
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */
package de.fzj.unicore.uas.testsuite;

import static org.easymock.EasyMock.aryEq;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import org.junit.Test;
import org.unigrids.services.atomic.types.ACLEntryTypeType;
import org.unigrids.x2006.x04.services.sms.ACLChangeModeType;
import org.unigrids.x2006.x04.services.sms.ChangeACLEntryType;
import org.unigrids.x2006.x04.services.sms.ChangeACLType;
import org.unigrids.x2006.x04.services.sms.ChangePermissionsEntryType;
import org.unigrids.x2006.x04.services.sms.ExtendedChangePermissionsType;
import org.unigrids.x2006.x04.services.sms.PermissionsChangeModeType;
import org.unigrids.x2006.x04.services.sms.PermissionsClassType;

import de.fzj.unicore.uas.impl.sms.SMSUtils;
import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.io.ACLEntry.Type;
import de.fzj.unicore.xnjs.io.ChangeACL;
import de.fzj.unicore.xnjs.io.ChangeACL.ACLChangeMode;
import de.fzj.unicore.xnjs.io.ChangePermissions;
import de.fzj.unicore.xnjs.io.ChangePermissions.Mode;
import de.fzj.unicore.xnjs.io.ChangePermissions.PermissionsClass;
import de.fzj.unicore.xnjs.io.IStorageAdapter;

public class MockPermissionsTest {
	@Test
	public void testSetACL1() throws ExecutionException {
		ChangeACLType aclChange = ChangeACLType.Factory.newInstance();
		ChangeACLEntryType ace = aclChange.addNewChangeACLEntry();
		ace.setPermissions("rw-");
		ace.setSubject("user1");
		ace.setKind(ACLEntryTypeType.USER);
		ace.setMode(ACLChangeModeType.MODIFY);

		IStorageAdapter mockTSI = createMock(IStorageAdapter.class);
		
		//expectations:
		ChangeACL[] changeACLs = new ChangeACL[1];
		changeACLs[0] = new ChangeACL(Type.USER, "user1", "rw-", false, ACLChangeMode.MODIFY);
		mockTSI.setfacl(eq("test-f"), eq(false), aryEq(changeACLs), eq(false));
		replay(mockTSI);
		
		//test
		SMSUtils.setACL("test-f", mockTSI, aclChange, false);
		verify(mockTSI);
	}


	@Test
	public void testSetACL2() throws ExecutionException {
		ChangeACLType aclChange = ChangeACLType.Factory.newInstance();
		aclChange.setClearACL(true);
		ChangeACLEntryType ace = aclChange.addNewChangeACLEntry();
		ace.setPermissions("rwx");
		ace.setSubject("group1");
		ace.setKind(ACLEntryTypeType.GROUP);
		ace.setMode(ACLChangeModeType.REMOVE);
		ace.setDefaultACL(true);

		IStorageAdapter mockTSI = createMock(IStorageAdapter.class);
		
		//expectations:
		ChangeACL[] changeACLs = new ChangeACL[1];
		changeACLs[0] = new ChangeACL(Type.GROUP, "group1", null, true, ACLChangeMode.REMOVE);
		mockTSI.setfacl(eq("test-f"), eq(true), aryEq(changeACLs), eq(true));
		replay(mockTSI);
		
		//test
		SMSUtils.setACL("test-f", mockTSI, aclChange, true);
		verify(mockTSI);
	}

	@Test
	public void testChmod() throws ExecutionException {
		ExtendedChangePermissionsType extendedCh = ExtendedChangePermissionsType.Factory.newInstance();
		ChangePermissionsEntryType entry = extendedCh.addNewChangePermissionsEntry();
		entry.setKind(PermissionsClassType.GROUP);
		entry.setMode(PermissionsChangeModeType.ADD);
		entry.setPermissions("rwx");
		entry = extendedCh.addNewChangePermissionsEntry();
		entry.setKind(PermissionsClassType.USER);
		entry.setMode(PermissionsChangeModeType.SET);
		entry.setPermissions("r-x");
		entry = extendedCh.addNewChangePermissionsEntry();
		entry.setKind(PermissionsClassType.OTHER);
		entry.setMode(PermissionsChangeModeType.SUBTRACT);
		entry.setPermissions("--x");

		IStorageAdapter mockTSI = createMock(IStorageAdapter.class);
		
		//expectations:
		ChangePermissions chPerms[] = new ChangePermissions[3];
		chPerms[0] = new ChangePermissions(Mode.ADD, PermissionsClass.GROUP, "rwx");
		chPerms[1] = new ChangePermissions(Mode.SET, PermissionsClass.OWNER, "r-x");
		chPerms[2] = new ChangePermissions(Mode.SUBTRACT, PermissionsClass.OTHER, "--x");
		mockTSI.chmod2(eq("test-f"), aryEq(chPerms), eq(false));
		replay(mockTSI);
		
		//test
		SMSUtils.extendedChangePermissions("test-f", mockTSI, extendedCh, false);
		verify(mockTSI);
	}
}
