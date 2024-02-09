package eu.unicore.uas.trigger.impl;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import eu.unicore.security.Client;
import eu.unicore.uas.trigger.TriggeredAction;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.io.IStorageAdapter;

/**
 * builds a JSDL and submits a job to the XNJS, resulting in a batch job  
 * 
 * @author schuller
 */
public abstract class BaseAction implements TriggeredAction {

	protected Map<String,String> getContext(IStorageAdapter storage, String filePath, Client client, XNJS xnjs){
		Map<String,String>context=new HashMap<String, String>();
		File f=new File(filePath);
		context.put(TriggeredAction.FILE_NAME, f.getName());
		String parent=storage.getStorageRoot()+"/"+(f.getParent()!=null?f.getParent():".")+"/";
		context.put(TriggeredAction.CURRENT_DIR, parent);
		context.put(TriggeredAction.FILE_PATH, parent+f.getName());
		context.put(TriggeredAction.BASE_DIR, storage.getStorageRoot());
		return context;
	}
	
	protected String expandVariables(String var, Map<String,String>context){
		if(var.contains("${")){
			for(Object o: context.keySet()){
				String key=String.valueOf(o);
				String s="${"+key+"}";
				String p="\\$\\{"+key+"\\}";
				if(var.contains(s)){
					var=var.replaceAll(p,String.valueOf(context.get(o)));
				}
			}
		}
		return var;
	}

}
