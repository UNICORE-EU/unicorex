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

package de.fzj.unicore.uas.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.xmlbeans.XmlObject;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.uas.Enumeration;
import eu.unicore.services.ws.WSUtilities;
import eu.unicore.unicore6.enumeration.EnumerationPropertiesDocument;
import eu.unicore.unicore6.enumeration.GetResultsRequestDocument;
import eu.unicore.unicore6.enumeration.GetResultsResponseDocument;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * a client to access an enumeration service
 * 
 * @author schuller
 */
public class EnumerationClient<T> extends BaseUASClient implements Iterable<T> {

	private final Enumeration enumeration;

	private final QName resultQName;

	private int batchSize=50;

	/**
	 * create a new enumeration client
	 * 
	 * @param url - the URL to connect to
	 * @param epr - the EPR of the target service
	 * @param sec - the security settings to use
	 * @param resultQName - the type of XML documents that are enumerated
	 */
	public EnumerationClient(String url, EndpointReferenceType epr,
			IClientConfiguration sec, QName resultQName) throws Exception {
		super(url, epr, sec);
		this.resultQName=resultQName;
		enumeration = makeProxy(Enumeration.class);
	}

	/**
	 * create a new enumeration client
	 * 
	 * @param address - the EPR of the target service
	 * @param sec - the security settings to use
	 * @param resultQName - the type of XML documents that are enumerated
	 */
	public EnumerationClient(EndpointReferenceType address,
			IClientConfiguration sec, QName resultQName) throws Exception {
		this(address.getAddress().getStringValue(), address, sec, resultQName);
	}

	/**
	 * get a batch of results of the given length, starting at the given offset
	 * 
	 * @param offset - the offset into the result set
	 * @param length - the number of results
	 * 
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public List<T>getResults(long offset, long length)throws Exception{
		List<T>results=new ArrayList<T>();
		GetResultsRequestDocument req=GetResultsRequestDocument.Factory.newInstance();
		req.addNewGetResultsRequest().setOffset(offset);
		req.getGetResultsRequest().setNumberOfResults(length);
		GetResultsResponseDocument respD=enumeration.GetResults(req);
		XmlObject[]res=WSUtilities.extractAnyElements(respD, resultQName);
		if(res!=null){
			for(XmlObject o: res){
				results.add((T)o);
			}
		}
		return results;
	}

	/**
	 * returns an iterator over the available results.
	 * This will lazily fetch more results as they are needed.
	 * NOTE: the methods will throw a {@link RuntimeException} in case 
	 * any of the remote methods throw an exception
	 */
	public Iterator<T> iterator(){
		return new Iterator<T>(){
			private int current=0;
			private List<T>buffer=null;
			public boolean hasNext(){
				try{
					return current<getNumberOfResults();
				}catch(Exception ex){
					throw new RuntimeException(ex);
				}
			}

			public T next() {
				if(buffer==null||buffer.size()==0)getNextBatch();
				current++;
				return buffer.remove(0);
			}

			public void remove() {
				// No op
			}

			private void getNextBatch(){
				try{
					buffer=getResults(current, batchSize);
				}catch(Exception ex){
					throw new RuntimeException(ex);
				}
			}
		};
	}

	public EnumerationPropertiesDocument getResourcePropertiesDocument()
	throws Exception {
		return EnumerationPropertiesDocument.Factory
		.parse(GetResourcePropertyDocument()
				.getGetResourcePropertyDocumentResponse()
				.newInputStream());
	}

	/**
	 * get the total number of results that are available
	 * @throws Exception
	 */
	public long getNumberOfResults()throws Exception{
		return getResourcePropertiesDocument().getEnumerationProperties().getNumberOfResults();
	}

	/**
	 * get the address of the parent service instance
	 * @throws Exception
	 */
	public EndpointReferenceType getParentServiceEPR()throws Exception{
		return getResourcePropertiesDocument().getEnumerationProperties().getParentServiceReference();
	}

	/**
	 * set the batch size that is used by the {@link #iterator()} method
	 * @param batchSize - the batch size, which is expected to be larger than 1
	 */
	public void setBatchSize(int batchSize){
		if(batchSize<1)throw new IllegalArgumentException("Need batch size larger than 1");
		this.batchSize=batchSize;
	}
}
