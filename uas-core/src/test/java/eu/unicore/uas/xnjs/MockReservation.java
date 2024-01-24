package eu.unicore.uas.xnjs;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.inject.Inject;

import eu.unicore.security.Client;
import eu.unicore.services.utils.Utilities;
import eu.unicore.uas.json.JSONUtil;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.ems.ExecutionException;
import eu.unicore.xnjs.idb.Incarnation;
import eu.unicore.xnjs.json.JSONParser;
import eu.unicore.xnjs.resources.ResourceRequest;
import eu.unicore.xnjs.tsi.IReservation;
import eu.unicore.xnjs.tsi.ReservationStatus;
import eu.unicore.xnjs.tsi.ReservationStatus.Status;
import eu.unicore.xnjs.tsi.remote.TSIMessages;

public class MockReservation implements IReservation {

	private static Map<String, ReservationStatus> reservations=new HashMap<>();

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
			TSIMessages tsiMessages = configuration.get(TSIMessages.class);
			String tsiCmd = tsiMessages.makeMakeReservationCommand(incarnated,startTime,client);
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
		return JSONParser.parseResourceRequest(JSONUtil.asJSON(source));
	}
}
