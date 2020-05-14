package de.fzj.unicore.xnjs.resources;

/**
 * this interface allows to render a resource set in a certain format (XML, JSON, ...)
 *
 * @param <T> - the result type
 *  
 * @author schuller
 */
public interface ResourceSetRenderer<T>{
	
	public T render(ResourceSet resources);

}