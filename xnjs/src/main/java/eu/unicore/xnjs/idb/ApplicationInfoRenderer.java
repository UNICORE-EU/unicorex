package eu.unicore.xnjs.idb;


/**
 * this interface allows to render application information in a certain format (XML, JSON, ...)
 *
 * @param <T> - the result type
 * 
 * @author schuller
 */
public interface ApplicationInfoRenderer<T>{

	public T render(ApplicationInfo applicationInfo);
	
}
