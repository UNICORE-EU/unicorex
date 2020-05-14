package de.fzj.unicore.uas;

import java.util.Map;
import java.util.ServiceLoader;

/**
 * Some filetransfers require extra parameters. This interface provides a 
 * link from existing parameter sources (such as a config file, or a preferences page) 
 * to these filetransfer parameters.<br/> 
 * Multiple of these filetransfer parameter provider can be used at the same time,
 * so implementations should check the provided parameters and only add to them as 
 * appropriate.<br/>
 * 
 * At runtime, implementations are found using the {@link ServiceLoader} mechanism.
 * Thus, a file named 'de.fzj.unicore.uas.FiletransferParameterProvider' needs to 
 * be placed into the META-INF/services directory, which contains the name of the implementation
 * that should be loaded.
 * 
 * @author schuller
 */
public interface FiletransferParameterProvider {

	/**
	 * put additional parameters into the given map<br/>
	 * 
	 * @param params   - the map of parameters to add to
	 * @param protocol - the selected protocol
	 */
	public void provideParameters(Map<String,String> params, String protocol);
	
}
