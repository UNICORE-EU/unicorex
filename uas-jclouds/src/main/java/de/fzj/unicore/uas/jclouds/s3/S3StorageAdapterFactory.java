package de.fzj.unicore.uas.jclouds.s3;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.net.ssl.SSLContext;

import org.apache.logging.log4j.Logger;
import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;

import com.google.common.base.Supplier;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;

import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.fts.FileTransferModel;
import de.fzj.unicore.uas.impl.UASWSResourceImpl;
import de.fzj.unicore.uas.jclouds.BlobStoreStorageAdapter;
import de.fzj.unicore.uas.xnjs.StorageAdapterFactory;
import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.Model;
import de.fzj.unicore.xnjs.io.IStorageAdapter;
import eu.unicore.security.canl.SSLContextCreator;
import eu.unicore.util.Log;

/**
 * Creates and configures the jClouds S3 connector
 * 
 * @author schuller
 */
public class S3StorageAdapterFactory implements StorageAdapterFactory {

	private static final Logger logger = Log.getLogger(Log.SERVICES, S3StorageAdapterFactory.class);

	@Override
	public IStorageAdapter createStorageAdapter(UASWSResourceImpl parent)
			throws IOException {
		// load the model of the correct SMS resource: parent can be a file transfer
		Model m = parent.getModel();
		if(m instanceof FileTransferModel){
			try{
				String uid = ((FileTransferModel)m).getParentUID();
				m = parent.getKernel().getHome(UAS.SMS).get(uid).getModel();
			}catch(Exception ex){
				throw new IOException(ex);
			}
		}
		S3Model model = (S3Model)m;
		String accessKey = model.getAccessKey();
		String secretKey = model.getSecretKey();
		String endpoint = model.getEndpoint();
		String provider = model.getProvider();
		String region = model.getRegion();
		return createStorageAdapter(parent.getKernel(), accessKey, secretKey, endpoint, provider, region);
	}

	public IStorageAdapter createStorageAdapter(Kernel kernel, String accessKey, String secretKey, String endpoint, 
			String provider, String region) throws IOException {
		BlobStore blobStore = null;
		if("transient".equals(provider)){
			blobStore = getTransientBlobstore();
		}
		else{
			ContextBuilder builder = ContextBuilder.newBuilder(provider)
					.credentials(accessKey, secretKey);
			if(endpoint!=null){
				builder.endpoint(endpoint);
			}
			Set<Module>modules = new HashSet<Module>();
			modules.addAll(getHTTPSClientConfig(kernel));
			
			builder.modules(modules);
			BlobStoreContext context = builder.buildView(BlobStoreContext.class);
			blobStore = context.getBlobStore();
			if(logger.isDebugEnabled()){
				logger.debug("Connected to S3 " + endpoint
						+ " provider " + provider
						+ " accessKey " + (accessKey!=null ? "***" : "n/a")
						+ " secretKey " + (secretKey!=null ? "***" : "n/a"));	
			}

		}
		return new BlobStoreStorageAdapter(endpoint, blobStore, region);
	}

	// when using the 'transient' provider, we want the same in-memory blobstore 
	// for all requests

	private static BlobStore lbs = null;
	private synchronized BlobStore getTransientBlobstore(){
		if(lbs==null){
			logger.info("*** Creating in-memory S3 blob store.");
			ContextBuilder builder = ContextBuilder.newBuilder("transient")
					.credentials("n/a", "n/a");
			builder.endpoint("n/a");
			BlobStoreContext context = builder.buildView(BlobStoreContext.class);
			lbs = context.getBlobStore();
		}
		return lbs;
	}

	/**
	 * Sets up the SSL support using the container's truststore.
	 * Currently the container credential is not used.
	 * 
	 * TODO add more http connect/read timeouts and other settings (proxy...)
	 */
	private Collection<Module> getHTTPSClientConfig(final Kernel kernel){
		Module sslConfig = new AbstractModule() {
			@Override
			protected void configure() {
				Supplier<SSLContext> provider = new	Supplier<SSLContext>() {
					private SSLContext ctx;
					
					@Override
					// note this is called per-request, so want to cache the context
					public synchronized SSLContext get() {
						if(ctx==null){
							try{
								ctx = SSLContextCreator.createSSLContext(
										null,
										kernel.getClientConfiguration().getValidator(),
										"TLS",
										"S3Connector", logger,
										kernel.getClientConfiguration().getServerHostnameCheckingMode()
										);
							}catch(Exception ex){
								throw new RuntimeException("Cannot setup SSL context for S3 connector",ex);
							}
						}
						return ctx;
					}
				};
				bind(new TypeLiteral<Supplier<SSLContext>>(){}).toInstance(provider);
			}
		};
		
		return Collections.singleton(sslConfig);
	}
}