package de.fzj.unicore.xnjs.util;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.Collection;
import java.util.Enumeration;
import java.util.PropertyPermission;
import java.util.Vector;

import groovy.lang.GroovyShell;
import groovy.security.GroovyCodeSourcePermission;

/**
 * provides a secured environment for the evaluation of dynamic scripts
 *
 * @author schuller
 */
public class ScriptSandbox {

	static{
		if(System.getSecurityManager()==null){
			Policy.setPolicy(new MyPolicy());
			System.setSecurityManager(new SecurityManager());
		}
	}

	private AccessControlContext acc;

	public ScriptSandbox(){
		initAccessControl(null);
	}

	public ScriptSandbox(Collection<Permission>permissions){
		initAccessControl(permissions);
	}

	public void initAccessControl(Collection<Permission>permissions){
		Permissions perms = new Permissions();
		perms.add(new RuntimePermission("accessDeclaredMembers"));
		perms.add(new PropertyPermission("*","read"));
		//permission to read Groovy code from script
		perms.add(new GroovyCodeSourcePermission("/groovy/shell"));
		if (permissions!=null){
			for (Permission p : permissions){
				perms.add(p);
			}
		}
		CodeSource cs = new CodeSource( null, (Certificate[])null );
		ProtectionDomain domain = new ProtectionDomain(cs, perms );
		acc = new AccessControlContext(new ProtectionDomain[] { domain } );
	}

	/**
	 * evaluates the given code with the predefined set of permissions
	 * 
	 * @param code - the code to execute
	 * @return the result of the evaluation
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	public Object eval(final GroovyShell shell, final String code){
		return AccessController.doPrivileged(new PrivilegedAction(){
			public Object run() {
				return shell.evaluate(code);
			}}, acc);
	}   



	private static All all=new All();

	/**
	 * security policy that allows everything.
	 *
	 * @author schuller
	 */
	public static class MyPolicy extends Policy{

		@Override
		public PermissionCollection getPermissions(CodeSource cs) {
			return all;
		}
	}


	public static class All extends PermissionCollection{

		private static final long serialVersionUID=1l;

		static final Vector<Permission> perms=new Vector<Permission>();

		static{
			perms.add(new AllPermission());
		}

		@Override
		public void add(Permission permission) {
			//NOP
		}

		@Override
		public Enumeration<Permission> elements() {
			return perms.elements();
		}

		@Override
		public boolean implies(Permission permission) {
			return true;
		}

	}


}
