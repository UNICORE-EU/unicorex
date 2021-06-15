/*********************************************************************************
 * Copyright (c) 2011 Forschungszentrum Juelich GmbH 
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

package de.fzj.unicore.uas.metadata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import javax.xml.namespace.QName;

import org.apache.logging.log4j.Logger;
import org.unigrids.services.atomic.types.MetadataType;
import org.unigrids.x2006.x04.services.metadata.CreateMetadataDocument;
import org.unigrids.x2006.x04.services.metadata.CreateMetadataResponseDocument;
import org.unigrids.x2006.x04.services.metadata.DeleteMetadataDocument;
import org.unigrids.x2006.x04.services.metadata.DeleteMetadataResponseDocument;
import org.unigrids.x2006.x04.services.metadata.DirectoryDocument.Directory;
import org.unigrids.x2006.x04.services.metadata.FederatedMetadataSearchDocument;
import org.unigrids.x2006.x04.services.metadata.FederatedMetadataSearchDocument.FederatedMetadataSearch;
import org.unigrids.x2006.x04.services.metadata.FederatedMetadataSearchResponseDocument;
import org.unigrids.x2006.x04.services.metadata.GetMetadataDocument;
import org.unigrids.x2006.x04.services.metadata.GetMetadataResponseDocument;
import org.unigrids.x2006.x04.services.metadata.MetadataManagementPropertiesDocument;
import org.unigrids.x2006.x04.services.metadata.SearchMetadataDocument;
import org.unigrids.x2006.x04.services.metadata.SearchMetadataResponseDocument;
import org.unigrids.x2006.x04.services.metadata.SearchMetadataResponseDocument.SearchMetadataResponse;
import org.unigrids.x2006.x04.services.metadata.StartMetadataExtractionDocument;
import org.unigrids.x2006.x04.services.metadata.StartMetadataExtractionResponseDocument;
import org.unigrids.x2006.x04.services.metadata.UpdateMetadataDocument;
import org.unigrids.x2006.x04.services.metadata.UpdateMetadataResponseDocument;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.uas.MetadataManagement;
import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.client.MetadataClient;
import de.fzj.unicore.uas.impl.UASWSResourceImpl;
import de.fzj.unicore.uas.impl.sms.SMSUtils;
import de.fzj.unicore.uas.util.LogUtil;
import de.fzj.unicore.uas.util.Pair;
import eu.unicore.security.Client;
import eu.unicore.services.Home;
import eu.unicore.services.InitParameters;
import eu.unicore.services.exceptions.ResourceNotCreatedException;
import eu.unicore.services.ws.BaseFault;
import eu.unicore.services.ws.utils.WSServerUtilities;

/**
 * abstract metadata management service implementation
 * 
 * @author schuller
 */
public abstract class BaseMetadataManagementImpl extends UASWSResourceImpl
		implements MetadataManagement {

	private static final Logger logger = LogUtil.getLogger(LogUtil.DATA,
			BaseMetadataManagementImpl.class);

	public static final String INIT_SMS_ID = BaseMetadataManagementImpl.class
			.getName() + "_sms_id";

	public static final String INIT_CLASS_NAME = BaseMetadataManagementImpl.class
			.getName() + "_class_name";

	@Override
	public QName getResourcePropertyDocumentQName() {
		return MetadataManagementPropertiesDocument.type
				.getDocumentElementName();
	}

	@Override
	public void initialise(InitParameters initParams)
			throws Exception {
		super.initialise(initParams);
		getModel().setParentUID(initParams.parentUUID);
		getModel().setParentServiceName(UAS.SMS);
		logger.info("Created metadata management service <"
				+ getEPR().getAddress().getStringValue() + ">");
	}

	@Override
	public QName getPortType() {
		return MetadataManagement.META_PORT;
	}

	@Override
	public CreateMetadataResponseDocument CreateMetadata(
			CreateMetadataDocument req) throws BaseFault {
		try {
			String resourceName = normalizeResourceName(req.getCreateMetadata().getResourceName());
			Map<String, String> metadata = MetadataClient.asMap(req
					.getCreateMetadata().getMetadata());
			getMetadataManager().createMetadata(resourceName, metadata);
			CreateMetadataResponseDocument res = CreateMetadataResponseDocument.Factory
					.newInstance();
			res.addNewCreateMetadataResponse().setMetadataCreated(true);
			return res;
		} catch (Exception ex) {
			ex.printStackTrace();
			throw BaseFault.createFault("Error creating metadata", ex, true);
		}
	}

	@Override
	public DeleteMetadataResponseDocument DeleteMetadata(
			DeleteMetadataDocument req) throws BaseFault {
		try {
			String resourceName = normalizeResourceName(req.getDeleteMetadata().getResourceName());
			MetadataManager mm = getMetadataManager();
			mm.removeMetadata(resourceName);
			DeleteMetadataResponseDocument res = DeleteMetadataResponseDocument.Factory
					.newInstance();
			res.addNewDeleteMetadataResponse().setMetadataDeleted(true);
			return res;
		} catch (Exception ex) {
			throw BaseFault.createFault("Error deleting metadata", ex, true);
		}
	}

	@Override
	public GetMetadataResponseDocument GetMetadata(GetMetadataDocument request)
			throws BaseFault {
		String resourceName =  normalizeResourceName(request.getGetMetadata().getResourceName());
		try {
			MetadataManager mm = getMetadataManager();
			Map<String, String> meta = mm.getMetadataByName(resourceName);
			GetMetadataResponseDocument gmrd = GetMetadataResponseDocument.Factory
					.newInstance();
			MetadataType md = MetadataClient.convert(meta);
			gmrd.addNewGetMetadataResponse().setMetadata(md);
			return gmrd;
		} catch (Exception ex) {
			throw BaseFault.createFault("Error getting metadata", ex, true);
		}

	}

	@Override
	public SearchMetadataResponseDocument SearchMetadata(
			SearchMetadataDocument req) throws BaseFault {
		String searchString = req.getSearchMetadata().getSearchString();
		boolean isAdvancedSearch = req.getSearchMetadata().getIsAdvanced();
		SearchMetadataResponseDocument resD = SearchMetadataResponseDocument.Factory
				.newInstance();
		try {
			MetadataManager mm = getMetadataManager();
			SearchMetadataResponse res = resD.addNewSearchMetadataResponse();
			List<SearchResult> result = mm.searchMetadataByContent(
					searchString, isAdvancedSearch);
			for (SearchResult sr : result) {
				org.unigrids.x2006.x04.services.metadata.SearchResultDocument.SearchResult r1 = res
						.addNewSearchResult();
				r1.setResourceName(sr.getResourceName());
			}
			return resD;
		} catch (Exception ex) {
			throw BaseFault.createFault("Error during metadata search", ex,
					true);
		}
	}

	@Override
	public FederatedMetadataSearchResponseDocument FederatedMetadataSearch(
			FederatedMetadataSearchDocument in) throws BaseFault {
		try {
			FederatedMetadataSearch search = in.getFederatedMetadataSearch();

			String searchString = search.getSearchString();
			boolean isAdvanced = search.getIsAdvanced();
			
			String[] storagesListArray = search.getStoragesListArray();
			List<String> storagesList = Arrays.asList(storagesListArray);

			MetadataManager manager = getMetadataManager();

			Client client=getClient();
			Future<FederatedSearchResultCollection> future = manager
					.federatedMetadataSearch(client, searchString, storagesList, isAdvanced);
			FederatedMetadataSearchResponseDocument response = FederatedMetadataSearchResponseDocument.Factory
					.newInstance();
			response.addNewFederatedMetadataSearchResponse();

			EndpointReferenceType eprt = makeFederatedSearchTask(future);
			response.getFederatedMetadataSearchResponse()
					.setTaskReference(eprt);

			return response;
		} catch (Exception ex) {
			ex.printStackTrace();
			LogUtil.logException(
					"Error occurred during federated metadata search.", ex,
					logger);
			throw BaseFault.createFault(
					"Error occurred during federated metadata search.", ex,
					true);
		}
	}

	@Override
	public StartMetadataExtractionResponseDocument StartMetadataExtraction(
			StartMetadataExtractionDocument req) throws BaseFault {
		try {
			List<String>files=new ArrayList<String>();
			files.addAll(Arrays.asList(req.getStartMetadataExtraction().getFileArray()));

			List<Pair<String,Integer>>dirs=new ArrayList<Pair<String,Integer>>();
			for(Directory dir: req.getStartMetadataExtraction().getDirectoryArray()){
				dirs.add(new Pair<String,Integer>(dir.getBasePath(),Integer.valueOf((int)dir.getDepthLimit())));
			}
			
			if(files.size()==0 && dirs.size()==0){
				// old style - only use if nothing else was given
				String base = req.getStartMetadataExtraction().getBasePath();
				if (base == null)
					base = "/";
				int depthLimit = (int) req.getStartMetadataExtraction().getDepthLimit();
				dirs.add(new Pair<String,Integer>(base,depthLimit));
			}
			MetadataManager mm = getMetadataManager();
			
			
			Future<ExtractionStatistics> future = mm
					.startAutoMetadataExtraction(files, dirs);
			StartMetadataExtractionResponseDocument res = StartMetadataExtractionResponseDocument.Factory
					.newInstance();
			res.addNewStartMetadataExtractionResponse();
			try {
				EndpointReferenceType epr = makeTask(future);
				res.getStartMetadataExtractionResponse().setTaskReference(epr);
			} catch (ResourceNotCreatedException ex) {
				logger.error("Could not create Task instance for monitoring.");
			}
			return res;
		} catch (Exception ex) {
			LogUtil.logException("Error starting metadata extraction.", ex,
					logger);
			throw BaseFault.createFault("Error starting metadata extraction",
					ex, true);
		}
	}

	@Override
	public UpdateMetadataResponseDocument UpdateMetadata(
			UpdateMetadataDocument req) throws BaseFault {
		String resourceName =  normalizeResourceName(req.getUpdateMetadata().getResourceName());
		Map<String, String> metadata = MetadataClient.asMap(req
				.getUpdateMetadata().getMetadata());
		try {
			getMetadataManager().updateMetadata(resourceName, metadata);
			UpdateMetadataResponseDocument res = UpdateMetadataResponseDocument.Factory
					.newInstance();
			res.addNewUpdateMetadataResponse().setMetadataUpdated(true);
			return res;
		} catch (Exception ex) {
			throw BaseFault.createFault("Error updating metadata", ex, true);
		}
	}

	/**
	 * create a Task instance for monitoring
	 * 
	 * @param f
	 * @return the new unique ID
	 */
	protected EndpointReferenceType makeTask(Future<ExtractionStatistics> f)
			throws ResourceNotCreatedException {
		Home taskHome = kernel.getHome(UAS.TASK);
		if (taskHome == null) {
			logger.error("Task service is not deployed.");
			return null;
		}
		InitParameters init = new InitParameters();
		init.parentUUID = getUniqueID();
		init.parentServiceName = getServiceName();
		String uid = taskHome.createResource(init);
		new ExtractionWatcher(f, uid, kernel).run();
		EndpointReferenceType epr = WSServerUtilities.makeEPR(UAS.TASK, uid,
				kernel);
		return epr;
	}

	/**
	 * create a Task instance for federated search monitoring
	 * 
	 * @param resultCollector - future collecting the search results
	 * @return EPR of the Task for monitoring
	 */
	protected EndpointReferenceType makeFederatedSearchTask(
			Future<FederatedSearchResultCollection> resultCollector) throws Exception {

		Home taskHome = kernel.getHome(UAS.TASK);

		if (taskHome == null) {
			logger.error("Task service is not deployed.");
			return null;
		}
		InitParameters init = new InitParameters();
		init.parentUUID = getUniqueID();
		init.parentServiceName = getServiceName();
		String uid = taskHome.createResource(init);
		new FederatedMetadataSearchWatcher(resultCollector, uid, kernel).run();
		return WSServerUtilities.makeEPR(UAS.TASK, uid,	kernel);
	}

	protected String normalizeResourceName(String resourceName){
		String res=SMSUtils.urlDecode(resourceName).replace('\\','/');
		if(res.length()==0 || '/'!=res.charAt(0))res="/"+res;
		res=res.replace("//", "/");
		return res;
	}
	
	/**
	 * get the appropriate MetadataManager
	 */
	public abstract MetadataManager getMetadataManager() throws Exception;

}
