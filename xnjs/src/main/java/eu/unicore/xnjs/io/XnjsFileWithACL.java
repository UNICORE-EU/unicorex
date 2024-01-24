package eu.unicore.xnjs.io;

/**
 * 
 * Describes a file, with information on ACL <br/>
 * 
 * @author golbi
 */
public interface XnjsFileWithACL extends XnjsFile {

	public ACLEntry[] getACL();
}
