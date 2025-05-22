package eu.unicore.uas.jclouds.s3;

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

import eu.unicore.uas.UAS;
import eu.unicore.uas.fts.FileTransferModel;
import eu.unicore.uas.impl.BaseResourceImpl;
import eu.unicore.uas.jclouds.BlobStoreStorageAdapter;
import eu.unicore.uas.xnjs.StorageAdapterFactory;
import eu.unicore.xnjs.io.IStorageAdapter;
import eu.emi.security.authn.x509.X509CertChainValidator;
import eu.emi.security.authn.x509.helpers.BinaryCertChainValidator;
import eu.unicore.security.canl.SSLContextCreator;
import eu.unicore.services.Kernel;
import eu.unicore.services.Model;
import eu.unicore.util.Log;
import eu.unicore.util.httpclient.ServerHostnameCheckingMode;

/**
 * Creates and configures the jClouds S3 connector
 * 
 * @author schuller
 */
public class S3StorageAdapterFactory implements StorageAdapterFactory {

	private static final Logger logger = Log.getLogger(Log.SERVICES, S3StorageAdapterFactory.class);

	@Override
	public IStorageAdapter createStorageAdapter(BaseResourceImpl parent)
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
		String bucket = model.getBucket();
		String provider = model.getProvider();
		String region = model.getRegion();
		boolean validate = model.isSslValidate();
		return createStorageAdapter(parent.getKernel(), accessKey, secretKey, endpoint, provider, bucket, region, validate);
	}

	public IStorageAdapter createStorageAdapter(Kernel kernel, String accessKey, String secretKey, String endpoint, 
			String provider, String bucket, String region, boolean sslValidate) throws IOException {
		BlobStore blobStore = null;
		if("transient".equals(provider)){
			blobStore = getTransientBlobstore();
		}
		else{
			System.setProperty("jclouds.s3.virtual-host-buckets", "false");
			ContextBuilder builder = ContextBuilder.newBuilder(provider).credentials(accessKey, secretKey);
			if(endpoint!=null){
				builder.endpoint(endpoint);
			}
			Set<Module>modules = new HashSet<>();
			modules.addAll(getHTTPSClientConfig(kernel, sslValidate));
			builder.modules(modules);
			BlobStoreContext context = builder.buildView(BlobStoreContext.class);
			blobStore = context.getBlobStore();
			if(logger.isDebugEnabled()){
				logger.debug("Connected to S3 " + endpoint+" /" + bucket 
						+ " provider " + provider
						+ " accessKey " + (accessKey!=null ? "***" : "n/a")
						+ " secretKey " + (secretKey!=null ? "***" : "n/a"));	
			}

		}
		return new BlobStoreStorageAdapter(kernel, endpoint, bucket, blobStore, region);
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
	private Collection<Module> getHTTPSClientConfig(final Kernel kernel, final boolean sslValidation){
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
								X509CertChainValidator v = sslValidation ?
										kernel.getClientConfiguration().getValidator() :
										new BinaryCertChainValidator(true);
								ServerHostnameCheckingMode m = sslValidation ?
										ServerHostnameCheckingMode.NONE :
										kernel.getClientConfiguration().getServerHostnameCheckingMode();
								ctx = SSLContextCreator.createSSLContext(null, v, "TLS", "S3Connector", logger, m);
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