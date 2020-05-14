package de.fzj.unicore.xnjs.idb;


/**
 * this interface allows to parse application information from a certain source format (XML, JSON, ...)
 *
 * @param <T> - the source type
 * 
 * @author schuller
 */
public interface ApplicationInfoParser<T>{

	public ApplicationInfo parseApplicationInfo(T sourceInfo) throws Exception;
	
}
