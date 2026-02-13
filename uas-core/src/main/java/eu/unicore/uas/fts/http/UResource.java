package eu.unicore.uas.fts.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.util.resource.Resource;

import eu.unicore.services.Kernel;
import eu.unicore.services.messaging.impl.StringMessage;
import eu.unicore.uas.util.LogUtil;
import eu.unicore.util.Log;
import eu.unicore.xnjs.io.IStorageAdapter;

/**
 * Implementation of a Jetty {@link Resource} that accesses a file via
 * a TSI / IStorageAdapter
 * 
 * @author schuller
 */
public class UResource {

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
	 * the transferred bytes will be reported via {@link FileAccessStatus#setTransferredBytes(String, Long)}
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
			kernel.getAttribute(FileAccessStatus.class).setTransferredBytes(id, transferred);
		}
	}

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
			kernel.getMessaging().getChannel(id).publish(
					new StringMessage(lastErrorMessage));
		}catch(Exception ex){}
		return lastErrorMessage;
	}

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

	public long lastModified() {
		try{
			return storage.getProperties(path).getLastModified().getTimeInMillis();
		}
		catch(Exception ex){
			return 0;
		}
	}

	public long length() {
		try{
			return storage.getProperties(path).getSize();
		}
		catch(Exception ex){
			return 0;
		}
	}
}
