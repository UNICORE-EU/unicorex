package de.fzj.unicore.xnjs.io.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.xnjs.util.IOUtils;
import de.fzj.unicore.xnjs.util.LogUtil;

/**
 * a fake server replying to HTTP requests
 */
public class FakeServer implements Runnable {
	
	private static final Logger log = LogUtil.getLogger("XNJS",FakeServer.class);

	private final int port;
	
	private ServerSocket serverSocket;

	private volatile boolean stopping=false;
	
	private volatile boolean stopped=false;
	
	private volatile boolean slow=false;
	
	private volatile boolean authRequired=false;
	
	private volatile int statusCode=200;
	
	private String answer="Everything is OK, thank you for contacting me.";
	
	private List<String> lastRequest = null;
	
	/**
	 * creates a FakeServer listening on the given port
	 * @param port
	 * @throws IOException
	 */
	public FakeServer(int port)throws IOException{
		serverSocket=new ServerSocket(port);
		serverSocket.setSoTimeout(5000);
		this.port=serverSocket.getLocalPort();
	}

	/**
	 * creates a FakeServer listening on a free port
	 * @see getPort()
	 */
	public FakeServer()throws IOException{
		this(0);
	}
	
	public String getURI(){
		return "http://localhost:"+port;
	}
	
	private static int n=0;
	public synchronized void start(){
		Thread t=new Thread(this);
		t.setName("FakeVSiteListenerThread"+(n++));
		t.start();
	}
	
	public synchronized void restart()throws Exception{
		if(serverSocket!=null)throw new IllegalStateException();
		
		serverSocket=new ServerSocket(port);
		stopping=false;
		stopped=false;
		start();
	}
	
	public void stop(){
		stopping=true;
	}
	
	public boolean isStopped(){
		return stopped;
	}

	public void run(){
		while(!stopping){
			try(Socket socket = serverSocket.accept()){
				lastRequest = readLines(socket.getInputStream());
				if(authRequired){
					if(getLastAuthNHeader()==null){
						writeAuthRequired(socket);
						continue;
					}
				}
				//normal reply
				if(slow)writeSlowReply(socket);
				else writeFastReply(socket);
			}catch(Exception ex){
				System.out.println("EX: "+ex.getClass().getName());
			}
		}
		log.info("Stopped.");
		stopped=true;
		IOUtils.closeQuietly(serverSocket);
		serverSocket=null;
	}
	
	private void writeFastReply(Socket socket)throws Exception{
		String status="HTTP/1.1 "+statusCode+" some reason";
		String reply="\nContent-Length: "+answer.length()+"\n\n"+answer;
		socket.getOutputStream().write(status.getBytes());
		socket.getOutputStream().write(reply.getBytes());
	}
	
	int delay=500;

	private void writeSlowReply(Socket socket)throws Exception{
		String status="HTTP/1.1 "+statusCode+" some reason";
		String reply1="\nContent-Length: "+answer.length()+"\n\n";
		
		socket.getOutputStream().write(status.getBytes());
		socket.getOutputStream().write(reply1.getBytes());
		for(byte b: answer.getBytes()){
			socket.getOutputStream().write(b);
			Thread.sleep(delay);
		}
	}

	private void writeAuthRequired(Socket socket)throws Exception{
		String status="HTTP/1.1 401 Unauthorized";
		String reply1="\nWWW-Authenticate: Basic realm=localhost\n\n\n";
		socket.getOutputStream().write(status.getBytes());
		socket.getOutputStream().write(reply1.getBytes());
	}
	
	public int getPort(){
		return port;
	}
	
	public void setStatusCode(int statusCode){
		this.statusCode=statusCode;
	}
	
	public void setVerySlowMode(boolean slow){
		this.slow=slow;
	}
	

	public void setRequireAuth(boolean what){
		this.authRequired=what;
	}
	
	public void setAnswer(String answer){
		this.answer=answer;
	}
	
	public String getLastAuthNHeader(){
		if(lastRequest != null){
			for(String s: lastRequest){
				if(s.startsWith("Authorization"))return s;
			}
		}
		return null;
	}
	
	 private List<String> readLines(InputStream in) throws IOException {
	        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
	        List<String> list = new ArrayList<String>();
	        String line = reader.readLine();
	        while (line != null) {
	            if(line.isEmpty())break;
	            list.add(line);
	            line = reader.readLine();
	        }
	        return list;
	    }
}
