package eu.unicore.uas.fts.http;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.channels.ReadableByteChannel;

import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.util.resource.Resource;

import eu.unicore.services.Kernel;
import eu.unicore.services.messaging.Message;
import eu.unicore.uas.util.LogUtil;
import eu.unicore.util.Log;
import eu.unicore.xnjs.io.IStorageAdapter;

/**
 * Implementation of a Jetty {@link Resource} that accesses a file via
 * a TSI / IStorageAdapter
 * 
 * @author schuller
 */
public class UResource extends 	Resource {

	private static final Logger logger = LogUtil.getLogger(LogUtil.DATA, UResource.class);

	private final String path;
	private final String id;
	private final IStorageAdapter storage;
	private final Kernel kernel;
	private long transferred = 0;
	private boolean append = false;
	// expected number of incoming bytes
	private long numberOfBytes = -1;

	private String lastErrorMessage = null;

	/**
	 * creates a Resource object for serving a file
	 * @param id - the unique ID of the resource, can be <code>null</code> if the resource is only temporary. If non-null,
	 * the transferred bytes will be reported via {@link FileServlet#setTransferredBytes(String, Long)}
	 * @param path - the path of the file relative to storage root
	 * @param storage
	 * @param kernel
	 */
	public UResource(String id, String path, IStorageAdapter storage, Kernel kernel){
		this.id = id;
		this.path = path;
		this.storage = storage;
		this.kernel = kernel;
	}

	public void setAppend(boolean append){
		this.append = append;
	}

	public void setNumberOfBytes(long numberOfBytes){
		this.numberOfBytes = numberOfBytes;
	}

	protected void updateTransferredBytes(){
		if(id!=null){
			kernel.getAttribute(FileServlet.class).setTransferredBytes(id, transferred);
		}
	}

	@Override
	public Resource addPath(String path) throws IOException,
			MalformedURLException {
		return null;
	}

	@Override
	public boolean delete() throws SecurityException {
		return false;
	}

	@Override
	public boolean exists() {
		return true;
	}

	@Override
	public File getFile() throws IOException {
		return null;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		try {
			final InputStream is = storage.getInputStream(path);
			InputStream decoratedStream = new InputStream(){

				@Override
				public int read() throws IOException {
					try{
						return is.read();
					}
					catch(Exception e){
						throw handleException("Error reading data", e);
					}
				}

				@Override
				public int read(byte[] b, int off, int len) throws IOException {
					try{
						int r=is.read(b, off, len);
						if(r>0){
							transferred+=r;
							updateTransferredBytes();
						}
						return r;
					}
					catch(Exception e){
						throw handleException("Error reading data", e);
					}
				}

				@Override
				public void close() throws IOException {
					is.close();
				}

			};
			return decoratedStream;
		}
		catch(Exception e){
			throw handleException("Error reading data", e);
		}
	}

	private IOException handleException(String message, Exception e) {
		createErrorMessage(message, e);
		if(e instanceof IOException){
			return (IOException)e;
		}
		else{
			return new IOException(e);
		}
	}

	private String createErrorMessage(String msg, Exception e){
		Log.logException(msg, e, logger);
		StringBuilder sb = new StringBuilder();
		sb.append(Log.createFaultMessage(msg, e)).append(" transferid=").append(id);
		sb.append(" path=").append(path);
		lastErrorMessage = sb.toString();
		if(id!=null) try{
			Message message = new Message(lastErrorMessage);
			kernel.getMessaging().getChannel(id).publish(message);
		}catch(Exception ex){}
		return lastErrorMessage;
	}

	@Override
	public String getName() {
		return path;
	}

	public OutputStream getOutputStream() throws IOException, SecurityException {
		try {
			final OutputStream os = storage.getOutputStream(path, append, numberOfBytes);
			OutputStream decoratedStream = new OutputStream(){

				@Override
				public void write(int b) throws IOException {
					try{
						os.write(b);
					}
					catch(Exception e){
						throw handleException("Error writing data", e);
					}
				}

				@Override
				public void write(byte[] b, int off, int len) throws IOException {
					try{
						os.write(b, off, len);
					}
					catch(Exception e){
						throw handleException("Error writing data", e);
					}
					transferred+=len;
					updateTransferredBytes();
				}

				@Override
				public void close() throws IOException {
					try{
						os.close();
					}catch(Exception e){
						throw handleException("Error writing data", e);
					}
				}

				@Override
				public void flush() throws IOException {
					try{
						os.flush();
					}catch(Exception e){
						throw handleException("Error writing data", e);
					}
				}
			};
			return decoratedStream;
		}
		catch(Exception e){
			throw handleException("Error writing data", e);
		}
	}

	@Override
	public URI getURI() {
		return null;
	}

	@Override
	public boolean isDirectory() {
		return false;
	}

	@Override
	public long lastModified() {
		return 0;
	}

	@Override
	public long length() {
		try{
			return storage.getProperties(path).getSize();
		}
		catch(Exception ex){
			return 0;
		}
	}

	@Override
	public String[] list() {
		return null;
	}

	@Override
	public boolean renameTo(Resource dest) throws SecurityException {
		return false;
	}

	@Override
	public boolean isContainedIn(Resource r) throws MalformedURLException
	{
		return false;
	}

	@Override
	public void close() {
	}

	@Override
	public ReadableByteChannel getReadableByteChannel() throws IOException {
		return null;
	}

}
