package de.fzj.unicore.uas.xnjs;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.inject.Inject;

import eu.unicore.services.utils.Utilities;
import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.idb.Incarnation;
import de.fzj.unicore.xnjs.resources.ResourceRequest;
import de.fzj.unicore.xnjs.tsi.IReservation;
import de.fzj.unicore.xnjs.tsi.ReservationStatus;
import de.fzj.unicore.xnjs.tsi.ReservationStatus.Status;
import de.fzj.unicore.xnjs.tsi.remote.TSIUtils;
import eu.unicore.security.Client;

public class MockReservation implements IReservation {

	private static Map<String, ReservationStatus> reservations=new HashMap<String, ReservationStatus>();

	public static String lastTSICommand;
	
	private final XNJS configuration;
	
	@Inject
	public MockReservation(XNJS configuration){
		this.configuration=configuration;
	}
	
	public void cancelReservation(String resID, Client arg1)
	throws ExecutionException {
		reservations.remove(resID);
	}

	public String makeReservation(Map<String,String> resources, Calendar startTime, Client client)
	throws ExecutionException {
		try{
			Incarnation gr=configuration.get(Incarnation.class);
			List<ResourceRequest>resourceRequest = parseResourceRequest(resources);
			List<ResourceRequest>incarnated=gr.incarnateResources(resourceRequest, client);
			String tsiCmd=TSIUtils.makeMakeReservationCommand(incarnated,startTime,client);
			lastTSICommand=tsiCmd;
			String resID=Utilities.newUniqueID();
			ReservationStatus rs=new ReservationStatus();
			rs.setStartTime(startTime);
			rs.setStatus(Status.WAITING);
			rs.setDescription("OK");
			reservations.put(resID, rs);
			return resID;
		}catch(Exception e){
			throw new ExecutionException(e);
		}
	}

	public static int count;
	
	public ReservationStatus queryReservation(String resID,Client arg2)
	throws ExecutionException {
		count++;
		//fake slow execution of the query command
		try{
			Thread.sleep(3000);
		}catch(InterruptedException ie){}
		return reservations.get(resID);
	}

	public static void clearCallCounter(){
		count=0;
	}
	
	public static boolean hasReservation(String id){
		return reservations.containsKey(id);
	}

	public List<ResourceRequest> parseResourceRequest(Map<String,String> source) throws Exception {
		List<ResourceRequest> req = new ArrayList<ResourceRequest>();
		if(source!=null) {
			for(Map.Entry<String, String> e: source.entrySet()) {
				req.add(new ResourceRequest(e.getKey(), e.getValue()));
			}
		}
		return req;
	}
}
