/*********************************************************************************
 * Copyright (c) 2006 Forschungszentrum Juelich GmbH 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * (1) Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the disclaimer at the end. Redistributions in
 * binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 * 
 * (2) Neither the name of Forschungszentrum Juelich GmbH nor the names of its 
 * contributors may be used to endorse or promote products derived from this 
 * software without specific prior written permission.
 * 
 * DISCLAIMER
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ********************************************************************************/

package de.fzj.unicore.uas.xnjs;

import java.io.IOException;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.cxf.common.util.StringUtils;
import org.apache.logging.log4j.Logger;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import com.google.inject.Singleton;

import de.fzj.unicore.uas.fts.FileTransferCapabilities;
import de.fzj.unicore.uas.fts.FileTransferCapability;
import de.fzj.unicore.uas.security.RegistryIdentityResolver;
import de.fzj.unicore.uas.util.LogUtil;
import de.fzj.unicore.uas.util.Pair;
import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.fts.IFTSController;
import de.fzj.unicore.xnjs.io.DataStageInInfo;
import de.fzj.unicore.xnjs.io.DataStageOutInfo;
import de.fzj.unicore.xnjs.io.DataStagingCredentials;
import de.fzj.unicore.xnjs.io.IFileTransfer;
import de.fzj.unicore.xnjs.io.IFileTransferCreator;
import eu.unicore.client.Endpoint;
import eu.unicore.security.Client;
import eu.unicore.security.wsutil.client.authn.ServiceIdentityResolver;
import eu.unicore.services.Kernel;
import eu.unicore.services.registry.RegistryHandler;
import eu.unicore.services.security.ETDAssertionForwarding;
import eu.unicore.services.security.util.AuthZAttributeStore;
import eu.unicore.services.ws.WSUtilities;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * creates {@link IFileTransfer} instances that use a UNICORE protocol such as BFT 
 *
 * @author schuller
 */
@Singleton
public class UFileTransferCreator implements IFileTransferCreator{
	
	protected static final Logger logger = LogUtil.getLogger(LogUtil.DATA,UFileTransferCreator.class);
	
	private final XNJS xnjs;
	
	private final Kernel kernel;
	
	private final ServiceIdentityResolver serverDNResolver;
	
	public UFileTransferCreator(XNJS config) {
		super();
		this.xnjs = config;
		ServiceIdentityResolver res = null;
		kernel = config.get(Kernel.class);
		try{
			RegistryHandler rh = kernel.getAttribute(RegistryHandler.class);
			if(rh.getExternalRegistryClient()!=null){
				res = new RegistryIdentityResolver(rh.getExternalRegistryClient());
			}
			else{
				res = new ServiceIdentityResolver(){

					@Override
					public String resolveIdentity(String serviceURL)
							throws IOException {
						return null;
					}

					@Override
					public void registerIdentity(String serviceURL,
							String identity) {
					}
					
				};
			}
		}catch(Exception e){}
		this.serverDNResolver = res;
	}

	@Override
	public int getPriority() {
		return 1;
	}
	

	public IFileTransfer createFileExport(Client client, String workdir, DataStageOutInfo info) {
		String source = info.getFileName();
		URI target = info.getTarget();
		DataStagingCredentials creds = info.getCredentials();

		if(isREST(target)) {
			Pair<String,String>urlInfo = extractUrlInfo(target);
			String protocol = urlInfo.getM1();
			
			FileTransferCapability fc=FileTransferCapabilities.getCapability(protocol, kernel);
			if(fc!=null){
				if(fc.isAvailable()){
					Endpoint ep = new Endpoint(urlInfo.getM2());
					return createExportREST(ep, fc.getExporter(),client,workdir,source,target,creds);
				}
				else{
					throw new RuntimeException("File transfer for protocol <"+protocol+"> is not available!");
				}
			}
		}
		return null;
	}

	@Override
	public IFileTransfer createFileImport(Client client, String workdir, DataStageInInfo in){
		URI source = in.getSources()[0];
		String target = in.getFileName();
		DataStagingCredentials creds = in.getCredentials();
		
		if(isREST(source)) {
			Pair<String,String>urlInfo = extractUrlInfo(source);
			String protocol = urlInfo.getM1();
			FileTransferCapability fc=FileTransferCapabilities.getCapability(protocol, kernel);
			if(fc!=null){
				if(fc.isAvailable()){
					Endpoint ep = new Endpoint(urlInfo.getM2());
					return createImportREST(ep, fc.getImporter(),client,workdir,source,target,creds);
				}
				else{
					throw new RuntimeException("File transfer for protocol <"+protocol+"> is not available!");
				}
			}
		}
		return null;
	}

	protected boolean isREST(URI url) {
		String uri = url.toString();
		return uri.contains("/rest/core/storages/") && uri.contains("/files");
	}
	
	public String getProtocol() {
		return String.valueOf(FileTransferCapabilities.getProtocols(kernel));
	}
	
	public String getStageOutProtocol() {
		return getProtocol();
	}
	
	public String getStageInProtocol() {
		return getProtocol();
	}
	
	/**
	 * create a transfer FROM a RESTful storage to a local file
	 * 
	 * the assumed URI format is
	 *   
	 *   unicore_protocol:http(s)://host:port/rest/core/storages/resourceID/files/filespec
	 * 
	 * @param clazz 
	 * @param client
	 * @param workdir
	 * @param source - remote file
	 * @param targetFile - local file
	 * @param creds - ignored
	 * @return IFileTransfer instance
	 */
	public IFileTransfer createImportREST(Endpoint ep, Class<? extends IFileTransfer> clazz, Client client, String workdir, URI source, String targetFile, DataStagingCredentials creds){
		String sourceFile = getFileSpec(source.toString());
		try{
			RESTFileTransferBase ft=(RESTFileTransferBase)clazz.getConstructor(XNJS.class).newInstance(xnjs);
			ft.setClient(client);
			ft.setWorkdir(workdir);
			ft.getInfo().setSource(sourceFile);
			ft.getInfo().setTarget(targetFile);
			ft.setStorageEndpoint(ep);
			ft.setExport(false);
			return ft;
		}catch(Exception e){
			logger.warn("Unable to instantiate file transfer", e);
			return null;
		}
	} 
	
	public IFileTransfer createExportREST(Endpoint ep, Class<? extends IFileTransfer> clazz, Client client, String workdir, String sourceFile, URI target, DataStagingCredentials credentials){
		String targetFile = getFileSpec(target.toString());
		try{
			RESTFileTransferBase ft = (RESTFileTransferBase)clazz.getConstructor(XNJS.class).newInstance(xnjs);
			ft.setClient(client);
			ft.getInfo().setSource(sourceFile);
			ft.getInfo().setTarget(targetFile);
			ft.setWorkdir(workdir);
			ft.setStorageEndpoint(ep);
			ft.setExport(true);
			return ft;
		}catch(Exception e){
			e.printStackTrace();
			logger.warn("Unable to instantiate file transfer", e);
			return null;
		}
	}
	
	
	public static String getFileSpec(String restURL) {
		String[] tokens = urlDecode(restURL.toString()).split("/files",2);
		return tokens.length>1? tokens[1] : "/";
	}
	
	private static final Pattern restURLPattern = Pattern.compile("(.*)://(.*/rest/core/storages/.*)/files/.*");
	
	/**
	 * extracts the storage part from a REST staging URL
	 */
	public static Pair<String,String>extractUrlInfo(URI url){
		String urlString = url.toString();
		Matcher m = restURLPattern.matcher(urlString);
		if(!m.matches())throw new IllegalArgumentException("Improperly formed storage URL <"+url+">");
		String schemeSpec=m.group(1);
		String protocol, scheme;
		
		if(schemeSpec.equalsIgnoreCase("http")||schemeSpec.equalsIgnoreCase("https")) {
			protocol = "BFT";
			scheme = schemeSpec;;
		}
		else {
			String[] tok = schemeSpec.split(":");
			protocol = tok[0];
			scheme = tok[1];
		}
		String base=m.group(2);
		String rest_url = scheme+"://"+base;
		return new Pair<String,String>(protocol, rest_url);
	}

	/**
	 * replace URI-encoded characters by their unencoded counterparts
	 * @param orig
	 * @return decoded URL
	 */
	public static String urlDecode(String orig){
		try{
			return orig.replaceAll("%20", " ");
		}catch(Exception e){
			return orig;
		}
	}
	
	// TODO better way to store and retrieve server DNs!?
	protected EndpointReferenceType createStorageEPR(URI uri)
	{
		String withoutScheme=uri.getSchemeSpecificPart();
		String upToFragment = withoutScheme.split("#")[0];
		
		EndpointReferenceType epr = WSUtilities.makeServiceEPR(upToFragment);
		Kernel kernel = xnjs.get(Kernel.class);
		try {
			IClientConfiguration sp = kernel.getClientConfiguration().clone();
			ETDAssertionForwarding.configureETD(AuthZAttributeStore.getClient(), sp);
			String serverDn = serverDNResolver.resolveIdentity(upToFragment);
			if (!StringUtils.isEmpty(serverDn)) {
				WSUtilities.addServerIdentity(epr, serverDn);
			}
		} catch (Exception e) {
			logger.error("Can't get server DN for <"+upToFragment+">", e);
		}
		return epr;
	}

	@Override
	public IFTSController createFTSImport(Client client, String workingDirectory, DataStageInInfo info)
			throws IOException {
		URI source = info.getSources()[0];
		Pair<String,String>urlInfo = extractUrlInfo(source);
		String protocol = urlInfo.getM1();
		Endpoint ep = new Endpoint(urlInfo.getM2());
		FileTransferCapability fc = FileTransferCapabilities.getCapability(protocol, kernel);
		if(fc==null || fc.getFTSImportsController()==null) {
			throw new IOException("Server-to-Server transfer not available for protocol "+protocol);
		}
		try{
			IFTSController fts = fc.getFTSImportsController().getConstructor(
					XNJS.class, Client.class, Endpoint.class, DataStageInInfo.class, String.class).
					newInstance(xnjs, client, ep, info, workingDirectory);
			return fts;
		}catch(Exception e) {
			throw new IOException(e);
		}
	}


	@Override
	public IFTSController createFTSExport(Client client, String workingDirectory, DataStageOutInfo info)
			throws IOException {
		URI target = info.getTarget();
		Pair<String,String>urlInfo = extractUrlInfo(target);
		String protocol = urlInfo.getM1();
		Endpoint ep = new Endpoint(urlInfo.getM2());
		FileTransferCapability fc = FileTransferCapabilities.getCapability(protocol, kernel);
		if(fc==null || fc.getFTSExportsController()==null) {
			throw new IOException("Server-to-Server transfer not available for protocol "+protocol);
		}
		try{
			IFTSController fts = fc.getFTSExportsController().getConstructor(
					XNJS.class, Client.class, Endpoint.class, DataStageOutInfo.class, String.class).
					newInstance(xnjs, client, ep, info, workingDirectory);
			return fts;
		}catch(Exception e) {
			throw new IOException(e);
		}
	}

}