/*********************************************************************************
 * Copyright (c) 2006 Forschungszentrum Juelich GmbH 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * (1) Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the disclaimer at the end. Redistributions in
 * binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 * 
 * (2) Neither the name of Forschungszentrum Juelich GmbH nor the names of its 
 * contributors may be used to endorse or promote products derived from this 
 * software without specific prior written permission.
 * 
 * DISCLAIMER
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ********************************************************************************/

package de.fzj.unicore.xnjs.tsi.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.junit.Test;

import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.io.ACLEntry;
import de.fzj.unicore.xnjs.io.ACLEntry.Type;
import de.fzj.unicore.xnjs.io.ChangeACL;
import de.fzj.unicore.xnjs.io.ChangeACL.ACLChangeMode;
import de.fzj.unicore.xnjs.tsi.remote.RemoteTSI;
import de.fzj.unicore.xnjs.io.XnjsFileImpl;

/**
 * These tests will work only when invoked on a system with a FS having ACL enabled.
 * @author K. Benedyczak
 */
public class TestACL extends LegacyTSITestCase {

	@Test
	public void testACLSupport() throws ExecutionException {
		RemoteTSI tsi=(RemoteTSI)xnjs.getTargetSystemInterface(null);
		assertNotNull(tsi);
		
		assertTrue(tsi.isACLSupported("/"));
	}
	
	@Test
	public void testGetSetACL() throws ExecutionException, IOException {
		RemoteTSI tsi=(RemoteTSI)xnjs.getTargetSystemInterface(null);
		assertNotNull(tsi);
		File tst = new File("target" + File.separator + "aclTestTmpDirectory");
		File subfile = new File(tst, "somefile");
		subfile.delete();
		tst.delete();
		tst.mkdir();
		subfile.createNewFile();
		
		XnjsFileImpl xnjsFile = new XnjsFileImpl();
		
		//1 - get - should get only 2 POSIX.1 entries (owner and owning group)
		tsi.getfacl(tst.getAbsolutePath(), xnjsFile);
		
		assertNotNull(xnjsFile.getACL());
		if (xnjsFile.getACL().length > 0)
			System.out.println(xnjsFile.getACL()[0]);
		assertEquals(2, xnjsFile.getACL().length);
		
		//2 - set u and g acl
		ChangeACL[] req = new ChangeACL[3];
		req[0] = new ChangeACL(Type.USER, "root", "r-x", false, ACLChangeMode.MODIFY);
		req[1] = new ChangeACL(Type.USER, "nobody", "r-x", true, ACLChangeMode.MODIFY);
		req[2] = new ChangeACL(Type.GROUP, "", "-w-", true, ACLChangeMode.MODIFY);
		tsi.setfacl(tst.getAbsolutePath(), false, req, false);
		
		tsi.getfacl(tst.getAbsolutePath(), xnjsFile);
		
		ACLEntry acl[] = xnjsFile.getACL();
		assertNotNull(acl);
		// should get 6 = 2 standard, 2 standard defaults (out of which one was explicitly set),
		// and 2 more which we have set. 
		assertEquals(Arrays.toString(acl), 6, acl.length);
		int all = 0;
		for (int i=0; i<acl.length; i++)
		{
			if (acl[i].getSubject().equals("root")) {
				assertTrue(acl[i].getPermissions().equals("r-x"));
				assertTrue(acl[i].getType().equals(Type.USER));
				assertFalse(acl[i].isDefaultACL());
				all++;
			}
			if (acl[i].getSubject().equals("nobody")) {
				assertTrue(acl[i].getPermissions().equals("r-x"));
				assertTrue(acl[i].getType().equals(Type.USER));
				assertTrue(acl[i].isDefaultACL());
				all++;
			} 
			if (acl[i].getSubject().equals("") && acl[i].getType().equals(Type.GROUP) 
					&& acl[i].isDefaultACL()) {
				assertTrue(acl[i].getPermissions().equals("-w-"));
				all++;
			}
			if (acl[i].getSubject().equals("") && acl[i].getType().equals(Type.USER)
					&& acl[i].isDefaultACL()) {
				assertTrue(acl[i].getPermissions().equals("rwx"));
				all++;
			}
			if (acl[i].getSubject().equals("") && acl[i].getType().equals(Type.GROUP) 
					&& !acl[i].isDefaultACL()) {
				all++;
			}
			if (acl[i].getSubject().equals("") && acl[i].getType().equals(Type.USER)
					&& !acl[i].isDefaultACL()) {
				all++;
			}
		}
		assertEquals(6, all);
		
		//3 - remove u ACL and default ACL
		ChangeACL[] req2 = new ChangeACL[2];
		req2[0] = new ChangeACL(Type.USER, "root", "r-x", false, ACLChangeMode.REMOVE);
		req2[1] = new ChangeACL(Type.USER, "nobody", "r-x", true, ACLChangeMode.REMOVE);
		tsi.setfacl(tst.getAbsolutePath(), false, req2, false);
		
		tsi.getfacl(tst.getAbsolutePath(), xnjsFile);
		
		acl = xnjsFile.getACL();
		assertNotNull(acl);
		assertEquals(Arrays.toString(acl), 4, acl.length);

		//4 - remove all
		tsi.setfacl(tst.getAbsolutePath(), true, new ChangeACL[0], false);
		
		tsi.getfacl(tst.getAbsolutePath(), xnjsFile);
		assertNotNull(xnjsFile.getACL());
		assertEquals(2, xnjsFile.getACL().length);
		
		//5 - set ACL recursively
		tsi.setfacl(tst.getAbsolutePath(), false, new ChangeACL[] {
			new ChangeACL(Type.USER, "nobody", "r-x", false, ACLChangeMode.MODIFY)
			}, true);
		tsi.getfacl(subfile.getAbsolutePath(), xnjsFile);
		acl = xnjsFile.getACL();
		assertNotNull(acl);
		assertEquals(Arrays.toString(acl), 3, acl.length);
		
		all = 0;
		for (int i=0; i<acl.length; i++)
		{
			if (acl[i].getSubject().equals("nobody")) {
				assertTrue(acl[i].getPermissions().equals("r-x"));
				assertTrue(acl[i].getType().equals(Type.USER));
				assertFalse(acl[i].isDefaultACL());
				all++;
			} 
			if (acl[i].getSubject().equals("") && acl[i].getType().equals(Type.GROUP) 
					&& !acl[i].isDefaultACL()) {
				all++;
			}
			if (acl[i].getSubject().equals("") && acl[i].getType().equals(Type.USER)
					&& !acl[i].isDefaultACL()) {
				all++;
			}
		}
		assertEquals(3, all);
		
	}
	
	
}
