package eu.unicore.uas.impl.sms;

import java.util.List;

import eu.unicore.uas.UAS;
import eu.unicore.uas.impl.PersistingPrefsModel;

public class SMSModel extends PersistingPrefsModel {

	private static final long serialVersionUID = 1L;

	StorageDescription storageDescription;

	/**
	 * the directory in which the storage service operates 
	 */
	String workdir;

	Boolean enableDirectFiletransfer = Boolean.FALSE;

	Boolean inheritSharing = Boolean.FALSE;

	/**
	 * the umask of this storage
	 */
	String umask = "077";

	String fsname;

	String directoryScanUID;

	public StorageDescription getStorageDescription() {
		return storageDescription;
	}

	public void setStorageDescription(StorageDescription storageDescription) {
		this.storageDescription = storageDescription;
	}

	public String getWorkdir() {
		return workdir;
	}

	public void setWorkdir(String workdir) {
		this.workdir = workdir;
	}

	public Boolean getEnableDirectFiletransfer() {
		return enableDirectFiletransfer;
	}

	public void setEnableDirectFiletransfer(Boolean enableDirectFiletransfer) {
		this.enableDirectFiletransfer = enableDirectFiletransfer;
	}

	public Boolean getInheritSharing() {
		return inheritSharing;
	}

	public void setInheritSharing(Boolean inheritSharing) {
		this.inheritSharing = inheritSharing;
	}

	public String getUmask() {
		return umask;
	}

	public void setUmask(String umask) {
		this.umask = umask;
	}

	public List<String> getFileTransferUIDs() {
		return getChildren(UAS.SERVER_FTS);
	}

	public String getFsname() {
		return fsname;
	}

	public void setFsname(String fsname) {
		this.fsname = fsname;
	}

	public String getDirectoryScanUID() {
		return directoryScanUID;
	}

	public void setDirectoryScanUID(String directoryScanUID) {
		this.directoryScanUID = directoryScanUID;
	}

}
