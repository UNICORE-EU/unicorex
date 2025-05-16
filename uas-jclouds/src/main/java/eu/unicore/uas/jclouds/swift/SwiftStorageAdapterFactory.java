package eu.unicore.uas.jclouds.swift;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.net.ssl.SSLContext;

import org.apache.logging.log4j.Logger;
import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.logging.log4j.config.Log4JLoggingModule;
import org.jclouds.openstack.keystone.config.KeystoneProperties;

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
import eu.emi.security.authn.x509.X509CertChainValidatorExt;
import eu.emi.security.authn.x509.helpers.BinaryCertChainValidator;
import eu.unicore.security.canl.SSLContextCreator;
import eu.unicore.services.Kernel;
import eu.unicore.services.Model;
import eu.unicore.util.Log;

/**
 * Creates and configures the jClouds S3 connector
 * 
 * @author schuller
 */
public class SwiftStorageAdapterFactory implements StorageAdapterFactory {

	private static final Logger logger = Log.getLogger(Log.SERVICES, SwiftStorageAdapterFactory.class);

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
		SwiftModel model = (SwiftModel)m;
		String username = model.getUsername();
		String password = model.getPassword();
		String endpoint = model.getEndpoint();
		String bucket = model.getContainer();
		String region = model.getRegion();
		return createStorageAdapter(parent.getKernel(), username, password, endpoint, bucket, region);
	}

	private IStorageAdapter createStorageAdapter(Kernel kernel, String username, String password, 
			String endpoint, String bucket, String region)	throws IOException {
		Properties overrides = new Properties();
		overrides.put(KeystoneProperties.KEYSTONE_VERSION, "3");
		ContextBuilder builder = ContextBuilder.newBuilder("openstack-swift")
				.overrides(overrides)
				.credentials(username, password).endpoint(endpoint);
		Set<Module>modules = new HashSet<>();
		modules.addAll(getHTTPSClientConfig(kernel));
	    modules.add(new Log4JLoggingModule());
		builder.modules(modules);
		BlobStoreContext context = builder.buildView(BlobStoreContext.class);
		BlobStore blobStore = context.getBlobStore();
		if(logger.isDebugEnabled()){
			logger.debug("Connected to Swift {} username {} password {}", endpoint,
					(username!=null ? username : "n/a"),
					(password!=null ? "***" : "n/a")
					);	
		}
		return new BlobStoreStorageAdapter(kernel, endpoint, bucket, blobStore, region);
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
								X509CertChainValidatorExt v = kernel.getClientConfiguration().getValidator();
								if(v==null){
									v = new BinaryCertChainValidator(true);
								}
								ctx = SSLContextCreator.createSSLContext(
										null, 
										v, 
										"TLS", 
										"SWiftConnector", logger,
										kernel.getClientConfiguration().getServerHostnameCheckingMode()
										);
							}catch(Exception ex){
								throw new RuntimeException("Cannot setup SSL context for Swift connector",ex);
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