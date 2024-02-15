package eu.unicore.uas.trigger.impl;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;

import eu.unicore.security.Client;
import eu.unicore.uas.trigger.TriggeredAction;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.io.IStorageAdapter;

public abstract class BaseAction {

	protected Map<String,String> getContext(IStorageAdapter storage, String filePath, Client client, XNJS xnjs){
		Map<String,String>context = new HashMap<>();
		File f=new File(filePath);
		context.put(TriggeredAction.FILE_NAME, f.getName());
		String parentS = storage.getStorageRoot()+"/"+(f.getParent()!=null?f.getParent():".")+"/";
		String parent = FilenameUtils.normalize(parentS, true);
		context.put(TriggeredAction.CURRENT_DIR, parent);
		context.put(TriggeredAction.FILE_PATH, new File(parent, f.getName()).getPath());
		context.put(TriggeredAction.BASE_DIR, storage.getStorageRoot());
		return context;
	}
	
	protected Map<String,String> getContext(IStorageAdapter storage, List<String> files, Client client, XNJS xnjs){
		Map<String,String>context = new HashMap<>();
		StringBuilder sb = new StringBuilder();
		for(String f: files) {
			if(sb.length()>0)sb.append(" ");
			while(f.startsWith("/")) f = f.substring(1);
			sb.append("'").append(f).append("'");
		}
		context.put(TriggeredAction.FILES, sb.toString());
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
