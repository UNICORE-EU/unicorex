package de.fzj.unicore.xnjs.tsi.remote;

import java.io.BufferedReader;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlObject;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.ResourcesDocument;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.ResourcesType;

import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.idb.Incarnation;
import de.fzj.unicore.xnjs.jsdl.JSDLParser;
import de.fzj.unicore.xnjs.resources.ResourceRequest;
import de.fzj.unicore.xnjs.tsi.IReservation;
import de.fzj.unicore.xnjs.tsi.ReservationStatus;
import de.fzj.unicore.xnjs.tsi.ReservationStatus.Status;
import de.fzj.unicore.xnjs.tsi.TSIUnavailableException;
import de.fzj.unicore.xnjs.util.ErrorCode;
import de.fzj.unicore.xnjs.util.LogUtil;
import eu.unicore.security.Client;

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
	
	/**
	 *Cancel a reservation on the classic TSI
	 *
	 *@see TSIUtils#makeCancelReservationCommand(String)
	 *
	 */
	public void cancelReservation(String reservationID, Client client)
	throws ExecutionException {
		try{	
			if(logger.isDebugEnabled()){
				logger.debug("Cancel reservation "+reservationID+" for client "+client);
			}
			String tsiCmd=TSIUtils.makeCancelReservationCommand(reservationID);
			TSIConnection conn=null;
			try
			{
				conn = getTSIConnection(client);
				String res=conn.send(tsiCmd);
				if(res.contains("TSI_FAILED")){
					String msg="Resource reservation on classic TSI failed. Reply was <"+res+">";
					ErrorCode ec=new ErrorCode(ErrorCode.ERR_TSI_EXECUTION,msg);
					throw new ExecutionException(ec);
				}
			}finally{
				if(conn!=null)conn.done();
			}
		}catch(Exception e){
			logger.error("Could not cancel reservation.",e);
			throw new ExecutionException(e);
		}

	}

	/**
	 * Make a reservation on the classic TSI.<br/>
	 * 
	 * <ul> 
	 * <li>the resource request is incarnated, i.e. the resource defaults from the IDB 
	 *     are merged in</li>
	 * <li>the reservation request is sent to the classic TSI as a #TSI_MAKE_RESERVATION command</li>
	 * <li>the TSI replies with TSI_OK and the reservation ID.</li>
	 * </ul>
	 */
	public String makeReservation(XmlObject resources, Calendar startTime,
			Client client) throws ExecutionException {
		try{
			if(logger.isDebugEnabled()){
				logger.debug("Processing resource reservation "+resources.toString()+"\nStart time "+startTime.getTime().toString());
			}
			ResourcesType rt=ResourcesDocument.Factory.parse(resources.toString()).getResources();
			List<ResourceRequest>resourceRequest = new JSDLParser().parseRequestedResources(rt);
			List<ResourceRequest>incarnated = grounder.incarnateResources(resourceRequest, client);
			String tsiCmd = TSIUtils.makeMakeReservationCommand(incarnated, startTime, client);
			TSIConnection conn=null;
			try{
				conn = getTSIConnection(client);
				String res=conn.send(tsiCmd);
				if(res.contains("TSI_FAILED")){
					String msg="Resource reservation on classic TSI failed. Reply was <"+res+">";
					ErrorCode ec=new ErrorCode(ErrorCode.ERR_TSI_EXECUTION,msg);
					throw new ExecutionException(ec);
				}
				String resID=res.replace("TSI_OK","").trim(); //strip TSI_OK and newlines
				return resID;
			}finally{
				if(conn!=null)conn.done();
			}
		}catch(Exception e){
			logger.error("Could not reserve resources.",e);
			throw new ExecutionException(e);
		}
	}

	/**
	 * Query a reservation.<br/>
	 * 
	 * This sends a line "#TSI_RESERVATION_REFERENCE NNN" to the classic TSI,
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
			if(logger.isDebugEnabled()){
				logger.debug("Querying resource reservation "+reservationID+" for client "+client.getDistinguishedName());
			}
			String tsiCmd=TSIUtils.makeQueryReservationCommand(reservationID);
			TSIConnection conn=null;
			String res;
			try{
				conn = getTSIConnection(client);
				res=conn.send(tsiCmd);
				if(res.contains("TSI_FAILED")){
					String msg="Query resource reservation on classic TSI failed. Reply was <"+res+">";
					ErrorCode ec=new ErrorCode(ErrorCode.ERR_TSI_EXECUTION,msg);
					throw new ExecutionException(ec);
				}
			}
			finally{
				if(conn!=null)conn.done();
			}
			
			rs=parseTSIReply(res);
		
		}catch(Exception e){
			LogUtil.logException("Could not query reservation.",e,logger);
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
		return tsiConnectionFactory.getTSIConnection(admin, null, null, -1);
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

}