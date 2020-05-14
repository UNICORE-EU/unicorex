package de.fzj.unicore.xnjs.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.fzj.unicore.xnjs.io.ACLEntry.Type;
import de.fzj.unicore.xnjs.io.ChangePermissions.Mode;
import de.fzj.unicore.xnjs.io.ChangePermissions.PermissionsClass;

public class TestACLAndPermissions {

	@Test
	public void testUnixPermissions(){
		UNIXPermissionEntry p1=new UNIXPermissionEntry(true,"rw-");
		UNIXPermissionEntry p2=new UNIXPermissionEntry(true,"rw-");
		assertEquals(p1,p2);
		assertTrue(p1.hashCode()==p2.hashCode());
		assertFalse(p1.equals(new Object()));
		assertFalse(p1.equals(null));
		UNIXPermissionEntry p3=new UNIXPermissionEntry(true,"rwx");
		assertNotSame(p1,p3);
		assertTrue(p1.hashCode()!=p3.hashCode());
	}
	
	@Test
	public void testACLEntry(){
		ACLEntry p1=new ACLEntry(Type.USER,"foo","rwx",false);
		ACLEntry p2=new ACLEntry(Type.USER,"foo","rwx",false);
		assertEquals(p1,p2);
		assertTrue(p1.hashCode()==p2.hashCode());
		assertFalse(p1.equals(new Object()));
		assertFalse(p1.equals(null));
		ACLEntry p3=new ACLEntry(Type.GROUP,"bar","rwx",false);
		assertNotSame(p1,p3);
		assertTrue(p1.hashCode()!=p3.hashCode());
	}
	
	@Test
	public void testChangePermissions(){
		ChangePermissions p1=new ChangePermissions(Mode.ADD,PermissionsClass.GROUP,"rwx");
		ChangePermissions p2=new ChangePermissions(Mode.ADD,PermissionsClass.GROUP,"rwx");
		assertEquals(p1,p2);
		assertTrue(p1.hashCode()==p2.hashCode());
		assertFalse(p1.equals(new Object()));
		assertFalse(p1.equals(null));
		ChangePermissions p3=new ChangePermissions(Mode.ADD,PermissionsClass.GROUP,"rw-");
		assertNotSame(p1,p3);
		assertTrue(p1.hashCode()!=p3.hashCode());
		ChangePermissions p4=new ChangePermissions(Mode.SET,PermissionsClass.OWNER,"r--");
		assertNotSame(p1,p4);
		assertTrue(p1.hashCode()!=p4.hashCode());
	}
}
