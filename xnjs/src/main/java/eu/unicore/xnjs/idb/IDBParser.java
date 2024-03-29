package eu.unicore.xnjs.idb;

import java.io.File;
import java.util.Collection;

public interface IDBParser {

	public void handleFile(File source, boolean singleFile) throws Exception;
	
	public void readApplications(Collection<ApplicationInfo>idb) throws Exception;
	
}
