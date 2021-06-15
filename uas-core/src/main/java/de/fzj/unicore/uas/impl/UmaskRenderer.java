package de.fzj.unicore.uas.impl;

import java.security.MessageDigest;
import java.util.List;

import org.unigrids.services.atomic.types.UmaskDocument;

import de.fzj.unicore.uas.SMSProperties;
import de.fzj.unicore.uas.StorageManagement;
import eu.unicore.services.exceptions.InvalidModificationException;
import eu.unicore.services.ws.AbstractXmlRenderer;
import eu.unicore.services.ws.Modifiable;

/**
 * Provides access to information about actual umask. This property is modifiable.
 * The property is used by SMS and TSS. 
 *
 * @author golbi
 */
public class UmaskRenderer extends AbstractXmlRenderer implements Modifiable<UmaskDocument>{

	private final UmaskSupport parent;

	/**
	 * Create a new umask property
	 * @param parent - the parent resource
	 */
	public UmaskRenderer(UmaskSupport parent) {
		super(StorageManagement.RPUmask);
		this.parent=parent;
	}

	@Override
	public UmaskDocument[] render() throws Exception {
		UmaskDocument ud=UmaskDocument.Factory.newInstance();
		ud.setUmask(parent.getUmask());
		return new UmaskDocument[]{ud};
	}

	@Override
	public boolean checkPermissions(int permissions) {
		return UPDATE == permissions;
	}

	@Override
	public void update(List<UmaskDocument> o)throws InvalidModificationException{
		if (o.size() != 1)
			throw new InvalidModificationException("Size must be equal to 1");
		String newUmask = o.get(0).getUmask();
		if (!SMSProperties.umaskPattern.matcher(newUmask).matches())
			throw new InvalidModificationException("Specified umask must be an octal number from 0 to 777.");
		parent.setUmask(newUmask);
	}
	
	public void delete()throws InvalidModificationException{
		throw new InvalidModificationException("Cannot delete umask property");
	}
	
	@Override
	public void insert(UmaskDocument o) throws InvalidModificationException {
		throw new InvalidModificationException("Cannot insert umask property");
	}

	@Override
	public void updateDigest(MessageDigest md) throws Exception {
		md.update(parent.getUmask().getBytes());
	}

}
