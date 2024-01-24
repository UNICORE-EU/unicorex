package eu.unicore.xnjs.tsi.remote;

import java.io.BufferedReader;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.logging.log4j.Logger;

import eu.unicore.security.Client;
import eu.unicore.xnjs.ems.ExecutionException;
import eu.unicore.xnjs.idb.Incarnation;
import eu.unicore.xnjs.resources.ResourceRequest;
import eu.unicore.xnjs.tsi.IReservation;
import eu.unicore.xnjs.tsi.ReservationStatus;
import eu.unicore.xnjs.tsi.ReservationStatus.Status;
import eu.unicore.xnjs.tsi.TSIUnavailableException;
import eu.unicore.xnjs.util.ErrorCode;
import eu.unicore.xnjs.util.LogUtil;

/**
 * Implementation of the {@link IReservation} interface using TSI functions
 *  
 * @author schuller
 */
public class Reservation implements IReservation {

	private static final Logger logger=LogUtil.getLogger(LogUtil.JOBS,Reservation.class);

	@Inject
	private Incarnation grounder;
	
	@Inject
	private TSIConnectionFactory tsiConnectionFactory;
	
	@Inject
	private TSIProperties tsiProperties;
	
	@Inject
	private TSIMessages tsiMessages;
	
	private String lastTSIHost = "n/a";

	/**
	 * Cancel a reservation
	 */
	public void cancelReservation(String reservationID, Client client)
	throws ExecutionException {
		try{
			logger.debug("Cancel reservation {} for client {}", reservationID, client);
			String tsiCmd = tsiMessages.makeCancelReservationCommand(reservationID);
			try(TSIConnection conn = getTSIConnection(client)){
				String res=conn.send(tsiCmd);
				if(res.contains("TSI_FAILED")){
					String msg="Resource reservation on TSI <"+lastTSIHost+"> failed. Reply was <"+res+">";
					ErrorCode ec=new ErrorCode(ErrorCode.ERR_TSI_EXECUTION,msg);
					throw new ExecutionException(ec);
				}
			}
		}catch(Exception e){
			throw new ExecutionException(e);
		}

	}

	/**
	 * Make a reservation
	 *
	 * <ul> 
	 * <li>the resource request is incarnated, i.e. the resource defaults from the IDB 
	 *     are merged in</li>
	 * <li>the reservation request is sent to the TSI as a #TSI_MAKE_RESERVATION command</li>
	 * <li>the TSI replies with TSI_OK and the reservation ID.</li>
	 * </ul>
	 */
	public String makeReservation(Map<String,String> resources, Calendar startTime,
			Client client) throws ExecutionException {
		try{
			logger.debug("Processing resource reservation {} Start time {}", resources, startTime.getTime());
			List<ResourceRequest>resourceRequest = parseResourceRequest(resources);
			List<ResourceRequest>incarnated = grounder.incarnateResources(resourceRequest, client);
			String tsiCmd = tsiMessages.makeMakeReservationCommand(incarnated, startTime, client);
			try(TSIConnection conn = getTSIConnection(client)){
				String res=conn.send(tsiCmd);
				if(res.contains("TSI_FAILED")){
					String msg="Resource reservation on TSI <"+lastTSIHost+"> failed. Reply was <"+res+">";
					ErrorCode ec=new ErrorCode(ErrorCode.ERR_TSI_EXECUTION,msg);
					throw new ExecutionException(ec);
				}
				return res.replace("TSI_OK","").trim();
			}
		}catch(Exception e){
			throw new ExecutionException(e);
		}
	}

	/**
	 * Query a reservation.<br/>
	 * 
	 * This sends a line "#TSI_RESERVATION_REFERENCE NNN" to the TSI,
	 * which replies with TSI_OK followed by one mandatory line
	 * <code>
	 * STATUS START_TIME
	 * </code>
	 * where STATUS is one of the constants defined in {@link ReservationStatus}
	 * and START__TIME is an ISO data/time (yyyy-MM-dd'T'HH:mm:ssZ), 
	 * and an optional second line containing a human-readable status.
	 * 
	 * eg.
	   <pre>
	   TSI_OK
	   WAITING 2007-11-13T22:00:+0100
	   Reserved 10 nodes.
	   </pre>
	 * 
	 */
	public ReservationStatus queryReservation(String reservationID, Client client) throws ExecutionException {
		ReservationStatus rs=new ReservationStatus();
		rs.setStatus(ReservationStatus.Status.UNKNOWN);

		try{
			logger.debug("Querying resource reservation {} for client {}", reservationID, client.getDistinguishedName());
			String tsiCmd = tsiMessages.makeQueryReservationCommand(reservationID);
			String res;
			try(TSIConnection conn = getTSIConnection(client)){
				res = conn.send(tsiCmd);
				if(res.contains("TSI_FAILED")){
					String msg="Query resource reservation on TSI <"+lastTSIHost+"> failed. Reply was <"+res+">";
					ErrorCode ec=new ErrorCode(ErrorCode.ERR_TSI_EXECUTION,msg);
					throw new ExecutionException(ec);
				}
			}
			rs=parseTSIReply(res);
		}catch(Exception e){
			throw new ExecutionException(e);
		}
		return rs;
	}
	
	/**
	 * Get the configured user ID for managing reservations. 
	 * If not set, use the user ID from the client object.
	 * 
	 * @param client
	 */
	protected String getReservationAdmin(Client client){
		String admin=tsiProperties.getValue(TSIProperties.RES_ADMIN_USER);
		if(admin==null && client!=null && client.getXlogin()!=null){
			admin=client.getXlogin().getUserName();
		}
		return admin;
	}

	protected TSIConnection getTSIConnection(Client client) throws TSIUnavailableException {
		String admin=getReservationAdmin(client);
		lastTSIHost = "n/a";
		TSIConnection c = tsiConnectionFactory.getTSIConnection(admin, null, null, -1);
		lastTSIHost = c.getTSIHostName();
		return c;
	}
	
	/**
	 * parse the result and build a reservation status<br/>
	 * Expected format: 
	 * first line is status word and (optional) start time (yyyy-MM-dd'T'HH:mm:ssZ) in reply, eg.
	 *WAITING 2007-11-13T22:00:+0100
	 *
	 * optional second line is description
	 */
	protected ReservationStatus parseTSIReply(String tsiReply)throws Exception{
		ReservationStatus rs=new ReservationStatus();
		
		BufferedReader br=new BufferedReader(new StringReader(tsiReply));
		
		String line=br.readLine();
		if(line==null || !"TSI_OK".equals(line)){
			throw new Exception("TSI reply <"+tsiReply+"> has wrong format: expect TSI_OK as first line");
		}
		
		line=br.readLine();
		String[] reply=line.trim().split(" ");
		rs.setStatus(Status.valueOf(reply[0].trim()));
		if(reply.length>1){
			SimpleDateFormat sf=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
			Calendar c=Calendar.getInstance();
			c.setTime(sf.parse(reply[1]));
			rs.setStartTime(c);
		}
		//last line is optional description
		line=br.readLine();
		if(line!=null){
			rs.setDescription(line);
		}
		return rs;	
	}

	public List<ResourceRequest> parseResourceRequest(Map<String,String> source) throws Exception {
		List<ResourceRequest> req = new ArrayList<>();
		if(source!=null) {
			for(Map.Entry<String, String> e: source.entrySet()) {
				req.add(new ResourceRequest(e.getKey(), e.getValue()));
			}
		}
		return req;
	}
}