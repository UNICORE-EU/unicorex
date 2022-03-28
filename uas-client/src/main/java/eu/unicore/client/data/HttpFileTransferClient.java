package eu.unicore.client.data;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.input.BoundedInputStream;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.InputStreamEntity;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import de.fzj.unicore.uas.fts.FiletransferOptions;
import de.fzj.unicore.uas.fts.ProgressListener;
import eu.unicore.client.Endpoint;
import eu.unicore.services.rest.client.IAuthCallback;
import eu.unicore.util.Log;
import eu.unicore.util.httpclient.HttpUtils;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * Client for getting/putting a file through BFT 
 * (i.e. underlying protocol is HTTPS)
 * 
 * @author schuller
 */
public class HttpFileTransferClient extends FiletransferClient 
implements FiletransferOptions.IMonitorable, FiletransferOptions.SupportsPartialRead,
		   FiletransferOptions.Read,FiletransferOptions.Write,
           FiletransferOptions.ReadStream, FiletransferOptions.IAppendable
{

	private static final Logger logger = Log.getLogger(Log.CLIENT, HttpFileTransferClient.class);

	private final String accessURL;

	private Long totalBytesTransferred = 0L;

	private boolean append;
	
	private ProgressListener<Long> observer;

	public HttpFileTransferClient(Endpoint endpoint, JSONObject initialProperties, IClientConfiguration security, IAuthCallback auth) throws Exception {
		super(endpoint, initialProperties, security, auth);
		accessURL = initialProperties.getString("accessURL");
	}

	/**
	 * read remote data and copy to the given output stream
	 * @param os - the OutputStream to write the data to
	 * @throws Exception
	 */
	@Override
	public void readAllData(OutputStream os)throws Exception{
		HttpClient client = getClient();
		HttpGet get = new HttpGet(accessURL);
		totalBytesTransferred = read(os, get, client);
	}

	protected long read(OutputStream os, HttpGet get, HttpClient client)throws IOException{
		InputStream is = getInputStream(client, get);
		return copy(is, os);
	}

	public InputStream getInputStream() throws IOException {
		HttpClient client = getClient();
		HttpGet get = new HttpGet(accessURL);
		return getInputStream(client, get);
	}

	private InputStream getInputStream(HttpClient client, final HttpGet get) throws IOException {
		HttpResponse response = client.execute(get);
		int result = response.getStatusLine().getStatusCode();
		//check for 200 response
		if(result<200 || result >299 ){
			String status = response.getStatusLine().toString();
			get.reset();
			throw new IOException("Can't read remote data, server returned "+status);
		}
		final InputStream is = response.getEntity().getContent();
		return new FilterInputStream(is){
			public void close(){
				try{is.close();}catch(Exception ex) {}
				get.releaseConnection();
			}
		};
	}
	
	/**
	 * uploads the given data (setting the append flag)
	 * 
	 * @param data
	 * @throws Exception
	 */
	public void append(byte[] data) throws Exception {
		writeAllData(new ByteArrayInputStream(data), true);
	}

	/**
	 * uploads the given data
	 * 
	 * @param data
	 * @throws Exception
	 */
	public void write(byte[] data)throws Exception {
		writeAllData(new ByteArrayInputStream(data), false);
	}

	@Override
	public void writeAllData(InputStream source, long numBytes)throws Exception{
		if(numBytes<0){
			writeAllData(source);
		}
		else{
			writeAllData(new BoundedInputStream(source, numBytes));
		}
	}
	
	/**
	 * read local data and write to remote location
	 * 
	 * @param is -  the InputStream to read from
	 * @param append - whether to append to an existing file
	 * @throws Exception
	 */
	public void writeAllData(final InputStream is,boolean append)throws Exception{
		this.append = append;
		writeAllData(is);
	}


	/**
	 * read local data and write to remote location
	 * 
	 * @param is -  the InputStream to read from
	 * @throws Exception
	 */
	@Override
	public void writeAllData(final InputStream is)throws Exception{
		HttpClient client = getClient();
		HttpEntityEnclosingRequestBase upload = createMethodForUpload();
		//monitor transfer progress, costs a bit performance though
		InputStream decoratedStream = new InputStream(){
			@Override
			public int read() throws IOException {
				int b = is.read();
				if(b != -1){
					totalBytesTransferred++;
					if(observer!=null){
						observer.notifyProgress(Long.valueOf(1));
						if(observer.isCancelled())throw new ProgressListener.CancelledException("Cancelled.");
					}
				}
				return b;
			}
			@Override
			public int read(byte[] b, int off, int len) throws IOException {
				int r = is.read(b, off, len);
				if(r>0){
					totalBytesTransferred+=r;
					if(observer != null){
						observer.notifyProgress(Long.valueOf(r));
						if(observer.isCancelled())throw new ProgressListener.CancelledException("Cancelled.");
					}
				}
				return r;
			}
		};
		upload.setEntity(new InputStreamEntity(decoratedStream,-1));

		totalBytesTransferred = Long.valueOf(0);
		try{
			HttpResponse response = client.execute(upload);
			int result = response.getStatusLine().getStatusCode();
			String status = response.getStatusLine().toString();
			//check for 200 response
			if(result<200 || result >299 ){
				throw new IOException("Can't write data, server returned "+status);
			}
			if(logger.isDebugEnabled()){
				logger.debug("Total transferred bytes: "+totalBytesTransferred
						+", HTTP return status "+status);
			}
		}finally{
			upload.releaseConnection();
		}
	}


	public String getAccessURL(){
		return accessURL;
	}


	@Override
	public long readPartial(long offset, long length, OutputStream os)
			throws IOException {
		HttpClient client = getClient();
		HttpGet get = new HttpGet(accessURL);
		//Note: byte range is inclusive!
		get.addHeader("Range", "bytes="+offset+"-"+(offset+length-1));
		return read(os, get, client);
	}
	
	/**
	 * read the given number of bytes from the end of the file
	 * and write them to the given output stream
	 * 
	 * @param numberOfBytes - how many bytes to read
	 * @param os - stream to write to
	 */
	public long readTail(long numberOfBytes, OutputStream os)
			throws IOException {
		HttpClient client = getClient();
		HttpGet get = new HttpGet(accessURL);
		get.addHeader("Range", "bytes=-"+numberOfBytes);
		return read(os, get, client);
	}

	//copy all data from an input stream to an output stream
	private long copy(InputStream in, OutputStream out)throws IOException{
		int bufferSize = 16384;
		byte[] buffer = new byte[bufferSize];
		int len = 0;
		int c = 0;
		long progress = 0;
		//the total bytes transferred in this invocation of copy()
		long total = 0;
		while (true)
		{
			len = in.read(buffer,0,bufferSize);
			if (len<0 )
				break;
			if(len>0){
				c++;
				out.write(buffer,0,len);
				total+=len;
				progress+=len;
				if(c % 10 == 0){
					if(observer != null){
						observer.notifyProgress(progress);
						if(observer.isCancelled())throw new ProgressListener.CancelledException("Cancelled.");
						progress = 0;
					}
				}
			}
		}
		if(observer != null){
			observer.notifyProgress(progress);
		}
		out.flush();
		return total;
	}

	protected HttpClient getClient(){
		HttpClient client = HttpUtils.createClient(accessURL, security);
		return client;
	}

	/**
	 * the total bytes transferred. Note: this will only be updated once per call
	 * to readAllData() or readPartial(). If you need a 'live' value, use a {@link ProgressListener}
	 * and register it using {@link #setProgressListener(ProgressListener)}
	 */
	public long getTotalBytesTransferred(){
		return totalBytesTransferred;
	}

	/**
	 * register a progress callback
	 */
	@Override
	public void setProgressListener(ProgressListener<Long> o){
		observer = o;
	}

	@Override
	public void setAppend() {
		append = true;
	}
	
	protected HttpEntityEnclosingRequestBase createMethodForUpload(){
		return accessURL.contains("method=POST")? createPost(): createPut();
	}

	protected HttpEntityEnclosingRequestBase createPut(){
		HttpEntityEnclosingRequestBase upload = new HttpPut(accessURL);
		if(append)upload.addHeader("X-UNICORE-AppendData", "true");
		return upload;
	}

	protected HttpEntityEnclosingRequestBase createPost(){
		HttpEntityEnclosingRequestBase upload = new HttpPost(accessURL);
		upload.addHeader("Content-Type", "multipart/form-data; boundary=--part-boundary--");
		if(append)upload.addHeader("X-UNICORE-AppendData", "true");
		return upload;
	}
}
