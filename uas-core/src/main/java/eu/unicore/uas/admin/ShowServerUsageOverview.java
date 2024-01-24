package eu.unicore.uas.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import eu.unicore.services.Kernel;
import eu.unicore.services.Service;
import eu.unicore.services.admin.AdminAction;
import eu.unicore.services.admin.AdminActionResult;
import eu.unicore.services.impl.DefaultHome;

/**
 * {@link AdminAction} which gives an overview of the server's usage per client, i.e.
 * how many jobs per client, etc. If no particular client DN is given,
 * information about total usage is given 
 */
public class ShowServerUsageOverview implements AdminAction {

	@Override
	public String getName() {
		return "ShowServerUsageOverview";
	}

	@SuppressWarnings("unchecked")
	@Override
	public AdminActionResult invoke(Map<String, String> params, Kernel kernel) {
		String requestedDN=params.get("clientDN"); //only DNs having this as substring will be shown
		boolean success=true;
		String message= requestedDN!=null ? "Retrieving usage for users matching '"+requestedDN+"'" : "Usage by all users";
		AdminActionResult res=new AdminActionResult(success,message);

		//map DNs to Servicename+Instances
		Map<String,Map<String,Integer>>merged=new HashMap<String, Map<String,Integer>>();

		List<String>serviceNames=new ArrayList<String>();
		List<Map<String,AtomicInteger>>instPerDN=new ArrayList<Map<String,AtomicInteger>>();
		
		for(Service s: kernel.getServices()){
			DefaultHome home=(DefaultHome)s.getHome();
			if(home==null)continue;
			//maps client DN to number of instances
			Map<String,AtomicInteger>perDN=home.getInstancesPerUser();
			String serviceName=home.getServiceName();
			serviceNames.add(serviceName);
			instPerDN.add(perDN);
		}
		merged=merge(requestedDN,serviceNames,
				instPerDN.toArray(new Map[instPerDN.size()]));

		for(String dn: merged.keySet()){
			Map<String,Integer>perService=merged.get(dn);
			StringBuilder sb=new StringBuilder();
			for(String serviceName: perService.keySet()){
				Integer num=perService.get(serviceName);
				if(num!=null&&num.intValue()>0){
					sb.append(serviceName).append(": ").append(num).append(" ");
				}
			}
			res.addResult(dn, sb.toString());
		}
		return res;
	}

	@SuppressWarnings("unchecked")
	Map<String,Map<String,Integer>>merge(String requestedDN, List<String>serviceNames,Map<String,AtomicInteger> ... perDNInstances){
		Map<String,Map<String,Integer>>merged=new HashMap<String, Map<String,Integer>>();

		for (int i=0; i<serviceNames.size(); i++){
			Map<String,? extends Number>instPerDN=perDNInstances[i];
			String serviceName=serviceNames.get(i);
			for(String dn: instPerDN.keySet()){
				if(requestedDN!=null && !dn.contains(requestedDN))continue;
				Map<String, Integer>instPerService=merged.get(dn);
				if(instPerService==null){
					instPerService=new HashMap<String, Integer>();
				}
				Number n=instPerDN.get(dn);
				int num= n!=null ? n.intValue(): 0;
				instPerService.put(serviceName, num);
				merged.put(dn, instPerService);
			}
		}
		return merged;
	}
	
	@Override
	public String getDescription() {
		return "parameters: [clientDN]";
	}
}