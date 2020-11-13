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
 *********************************************************************************/


package de.fzj.unicore.xnjs.jsdl;

import java.io.IOException;
import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.logging.log4j.Logger;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.ApplicationDocument;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.ApplicationType;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.DataStagingType;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.JobDefinitionDocument;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.RangeValueType;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.ResourcesType;
import org.ggf.schemas.jsdl.x2005.x11.jsdlPosix.EnvironmentType;
import org.ggf.schemas.jsdl.x2005.x11.jsdlPosix.GroupNameDocument;
import org.ggf.schemas.jsdl.x2005.x11.jsdlPosix.POSIXApplicationDocument;
import org.ggf.schemas.jsdl.x2005.x11.jsdlPosix.UserNameDocument;
import org.ggf.schemas.jsdl.x2006.x07.jsdlHpcpa.HPCProfileApplicationDocument;
import org.w3c.dom.Attr;

import de.fzj.unicore.xnjs.beans.idb.AllowedType;
import de.fzj.unicore.xnjs.beans.idb.ResourceDocument;
import de.fzj.unicore.xnjs.beans.idb.ResourceSettingDocument;
import de.fzj.unicore.xnjs.beans.idb.ResourceSettingDocument.ResourceSetting;
import de.fzj.unicore.xnjs.io.impl.OAuthToken;
import de.fzj.unicore.xnjs.io.impl.UsernamePassword;
import de.fzj.unicore.xnjs.resources.BooleanResource;
import de.fzj.unicore.xnjs.resources.DoubleResource;
import de.fzj.unicore.xnjs.resources.IntResource;
import de.fzj.unicore.xnjs.resources.Resource.Category;
import de.fzj.unicore.xnjs.resources.StringResource;
import de.fzj.unicore.xnjs.resources.ValueListResource;
import de.fzj.unicore.xnjs.util.LogUtil;
import de.fzj.unicore.xnjs.util.XmlBeansUtils;
import edu.virginia.vcgr.jsdl.sweep.SweepUtility;
import eu.unicore.jsdl.extensions.ResourceRequestDocument;

public class JSDLUtils {

	private static final Logger logger=LogUtil.getLogger(LogUtil.XNJS,JSDLUtils.class);

	//prevent instantiation
	private JSDLUtils(){}

	public static final QName JSDL_JOBDEFINITION=new QName("http://schemas.ggf.org/jsdl/2005/11/jsdl","JobDefinition");

	public static final QName JSDL_POSIX_APP=POSIXApplicationDocument.type.getDocumentElementName();

	public final static String extensionNS = "http://www.unicore.eu/unicore/jsdl-extensions";

	public final static String expression = "expression";

	/**
	 * check if the job definition contains stage in elements
	 * @param jdd
	 * @return true if the JSDL doc contains "staging in" elements
	 */
	public static boolean hasStageIn(JobDefinitionDocument jdd){
		try{
			DataStagingType[] dst=jdd.getJobDefinition().getJobDescription().getDataStagingArray();
			for(DataStagingType d: dst){
				if(d.getSource()!=null){
					return true;
				}
			}
		}catch(Exception ex){return false;}
		return false;
	}

	/**
	 * check if the job definition contains stage out elements
	 * @param jdd
	 * @return true if there are stage out elements
	 */
	public static boolean hasStageOut(JobDefinitionDocument jdd){
		try{
			DataStagingType[] dst=jdd.getJobDefinition().getJobDescription().getDataStagingArray();
			for(DataStagingType d: dst){
				if(d.getTarget()!=null){
					return true;
				}
			}
		}catch(Exception ex){return false;}
		return false;
	}

	/**
	 * return the staging in elements of the job definition
	 * @param jdd JobDefinitionDocument
	 * @return DataStagingType[] stage in elements 
	 */
	public static List<DataStagingType> getStageInArrayAsList(JobDefinitionDocument jdd){
		ArrayList<DataStagingType>v=new ArrayList<DataStagingType>();
		try{
			DataStagingType[] dst=jdd.getJobDefinition().getJobDescription().getDataStagingArray();
			for(DataStagingType d: dst){
				fix(d);
				if(d.getSource()!=null){
					v.add(d);
				}
			}
		}catch(Exception ex){return new ArrayList<DataStagingType>();}
		return v;
	}

	/**
	 * fix staging type: if URI does not have a scheme assume file://
	 */
	private static void fix(DataStagingType d){
		try{
			String s=d.getSource().getURI();
			URI source=new URI(s);
			if(source.getScheme()==null){
				String p=s.startsWith("/")? "" : "/";
				source=new URI("file://"+p+s);
				d.getSource().setURI(source.toString());
			}
		}
		catch(Exception e){}
		try{
			String t=d.getTarget().getURI();
			URI target=new URI(t);
			if(target.getScheme()==null){
				String p=t.startsWith("/")? "" : "/";
				target=new URI("file://"+p+t);
				d.getTarget().setURI(target.toString());
			}
		}
		catch(Exception e){}
	}

	/**
	 * return the staging in elements of the job definition
	 * @param jdd JobDefinitionDocument
	 * @return DataStagingType[] stage in elements 
	 */
	public static List<DataStagingType> getStageOutArrayAsList(JobDefinitionDocument jdd){
		ArrayList<DataStagingType>v=new ArrayList<DataStagingType>();
		try{
			DataStagingType[] dst=jdd.getJobDefinition().getJobDescription().getDataStagingArray();
			for(DataStagingType d: dst){
				if(d.getTarget()!=null){
					v.add(d);
				}
			}
		}catch(Exception ex){return new ArrayList<DataStagingType>();}
		return v;
	}

	/**
	 * extracts a Posix Application document from the given ApplicationDocument
	 * @param ad
	 * @return POSIXApplicationDocument
	 * returns null in case of errors
	 */
	public static POSIXApplicationDocument extractPosixApplication(XmlObject ad){
		XmlCursor cursor=ad.newCursor();
		if(XmlBeansUtils.skipToElement(cursor, POSIXApplicationDocument.type.getDocumentElementName()))
		{
			try{
				POSIXApplicationDocument pd=POSIXApplicationDocument.Factory.parse(cursor.newReader());
				return pd;
			}catch(Exception ex){//was no posix app inside
			}
			finally{
				if(cursor!=null)cursor.dispose();
			}
		}
		return null;
	}

	/**
	 * extracts a HPCProfile Application document from the given ApplicationDocument
	 * @param ad
	 * @return HPCProfileApplicationDocument
	 * @throws IllegalArgumentException in case of errors
	 */
	public static HPCProfileApplicationDocument extractHpcpApplication(ApplicationDocument ad){
		String source=ad.toString().replace(hpcp_ns2, hpcp_ns);
		try{
			ad=ApplicationDocument.Factory.parse(source);
		}catch(XmlException ex){}
		XmlCursor cursor=ad.newCursor();
		QName hQ=HPCProfileApplicationDocument.type.getDocumentElementName();
		if(XmlBeansUtils.skipToElement(cursor, hQ))
		{
			try{
				HPCProfileApplicationDocument pd=HPCProfileApplicationDocument.Factory.parse(cursor.newReader());
				return pd;
			}catch(Exception ex){//was no posix app inside
			}
			finally{
				if(cursor!=null)cursor.dispose();
			}
		}
		return null;
	}


	private static String hpcp_ns=HPCProfileApplicationDocument.type.getDocumentElementName().getNamespaceURI();
	private static String hpcp_ns2="http://schemas.ogf.org/jsdl/2006/07/jsdl-hpcpa";
	private static String posix_ns=POSIXApplicationDocument.type.getDocumentElementName().getNamespaceURI();

	/**
	 * extract a Posix / HPCP application <br/> 
	 * If necessary, convert to POSIX form. 
	 * 
	 * @param ad
	 * @throws IllegalArgumentException
	 */
	public static POSIXApplicationDocument extractUserApplication(ApplicationDocument ad){
		POSIXApplicationDocument res=extractPosixApplication(ad);
		if(res!=null)return res;

		//since POSIX and HPCProfile differ by namespace, element names only, 
		//and HPCP is a subset of POSIX, we can simply do string replacement to convert

		try{
			HPCProfileApplicationDocument hAppDoc=extractHpcpApplication(ad);
			if(hAppDoc==null)return null;
			res=POSIXApplicationDocument.Factory.parse(hAppDoc.toString()
					.replace(hpcp_ns, posix_ns)
					.replace("HPCProfile", "POSIX"));
			return res;
		}catch(Exception e){
			return null;
		}
	}	

	public static ApplicationType handleHPCP(ApplicationType app){
		String hpcpS=app.toString().replace(hpcp_ns2, hpcp_ns);
		if( hpcpS.contains(hpcp_ns) && hpcpS.contains("HPCProfile")) {
			String conv=hpcpS.replace(hpcp_ns, posix_ns).replace("HPCProfile", "POSIX");
			try{
				ApplicationType res=ApplicationType.Factory.parse(conv);
				return res;
			}catch(Exception ex){
				return null;
			}
		}else return app;
	}

	/**
	 * get an environment entry
	 * 
	 * @param name - the name of the environment variable
	 * @param env - JSDL EnvironmentType[]
	 * @return the value or <code>null</code> if not set
	 */
	public static String getEnvironmentValue(String name, EnvironmentType[] env){
		for(EnvironmentType e: env){
			if(e.getName().equals(name))return e.getStringValue();
		}
		return null;
	}


	/**
	 * extract site-specific resources using the "old" ResourceSetting xml 
	 * 
	 * @see #extractResourceSpecification(ResourcesType)
	 * 
	 * @param rt
	 * @throws XmlException
	 */
	public static ResourceSetting[] extractSiteSpecificResources(ResourcesType rt)throws XmlException{
		XmlObject[]res=XmlBeansUtils.extractAnyElements(rt, ResourceSettingDocument.type.getDocumentElementName());
		ResourceSetting[] result=new ResourceSettingDocument.ResourceSetting[res.length];
		for (int i = 0; i < result.length; i++) {
			result[i]=((ResourceSettingDocument)res[i]).getResourceSetting();
		}
		return result;
	}

	/**
	 * extracts the non-JSDL resources from the given JSDL {@link ResourcesType}
	 * 
	 * @see #extractSiteSpecificResources(ResourcesType)
	 * 
	 * @param rt
	 * @throws XmlException
	 */
	public static List<ResourceDocument> extractResourceSpecification(ResourcesType rt)throws XmlException{
		List<ResourceDocument>result=new ArrayList<ResourceDocument>();

		XmlObject[]resources=XmlBeansUtils.extractAnyElements(rt, ResourceDocument.type.getDocumentElementName());
		for (int i = 0; i < resources.length; i++) {
			ResourceDocument r=((ResourceDocument)resources[i]);
			//sanity check
			if(r.getResource().getType()==null){
				r.getResource().setType(AllowedType.INT);
			}
			result.add(r);
		}
		
		return result;
	}

	/**
	 * extracts the non-JSDL resources from the given JSDL {@link ResourcesType}
	 * 
	 * @see #extractSiteSpecificResources(ResourcesType)
	 * 
	 * @param rt
	 * @throws XmlException
	 */
	public static List<ResourceRequestDocument> extractResourceRequest(ResourcesType rt)throws XmlException{
		List<ResourceRequestDocument>result=new ArrayList<ResourceRequestDocument>();

		XmlObject[]resources=XmlBeansUtils.extractAnyElements(rt, ResourceRequestDocument.type.getDocumentElementName());
		for (int i = 0; i < resources.length; i++) {
			ResourceRequestDocument r=((ResourceRequestDocument)resources[i]);
			result.add(r);
		}
		
		//and old-style stuff
		XmlObject[]resourceSettings=XmlBeansUtils.extractAnyElements(rt, ResourceSettingDocument.type.getDocumentElementName());
		for (int i = 0; i < resourceSettings.length; i++) {
			ResourceSetting r=((ResourceSettingDocument)resourceSettings[i]).getResourceSetting();
			try{
				ResourceRequestDocument rd=ResourceRequestDocument.Factory.newInstance();
				rd.addNewResourceRequest().setName(r.getName());
				String value=r.getValue().getExactArray(0).getStringValue();
				rd.getResourceRequest().setValue(value);
				result.add(rd);
			}
			catch(Exception ex){
				LogUtil.logException("Invalid resource request : "+r.getName(), ex, logger);
			}
		}

		return result;
	}


	public static XmlObject getElement(XmlObject parent, QName qname)throws IOException,XmlException{
		XmlCursor cursor=parent.newCursor();
		if(XmlBeansUtils.skipToElement(cursor, qname)){
			return XmlObject.Factory.parse(cursor.newReader());
		}
		cursor.dispose();
		return null;
	}

	public static String extractUserName(XmlObject xml){
		XmlCursor cursor=xml.newCursor();
		try{
			if(XmlBeansUtils.skipToElement(cursor, UserNameDocument.type.getDocumentElementName())){
				return UserNameDocument.Factory.parse(cursor.newReader()).getUserName().getStringValue();
			}
		}catch(XmlException e){
			//ignore
		}catch(IOException e){
			//ignore
		}finally{
			cursor.dispose();	
		}
		return null;
	}

	public static String extractUserGroup(XmlObject xml){
		XmlCursor cursor=xml.newCursor();
		try{
			if(XmlBeansUtils.skipToElement(cursor, GroupNameDocument.type.getDocumentElementName())){
				return GroupNameDocument.Factory.parse(cursor.newReader()).getGroupName().getStringValue();
			}
		}catch(XmlException e){
			//ignore
		}catch(IOException e){
			//ignore
		}finally{
			cursor.dispose();	
		}
		return null;
	}

	public static de.fzj.unicore.xnjs.resources.Resource createResource(ResourceDocument doc)
			throws Exception {
		ResourceDocument.Resource res=doc.getResource();
		de.fzj.unicore.xnjs.resources.Resource resource;
		String name=res.getName();
		String min=res.getMin();
		String max=res.getMax();
		String val=res.getDefault();
		String description=res.getDescription();
		if (name == null || name.trim().equals(""))
			throw new Exception("name can not be empty"); 
		if (val == null)
			throw new Exception("default value must be set");
		
		AllowedType.Enum type=res.getType();
		
		if(AllowedType.DOUBLE.equals(type)){
			Double value=Double.valueOf(val);
			Double minF=min!=null?Double.valueOf(min):null;
			Double maxF=max!=null?Double.valueOf(max):null;
			resource=new DoubleResource(name,value,maxF,minF,Category.OTHER);
		}
		else if(AllowedType.INT.equals(type)){
			Long value = Long.valueOf(res.getDefault());
			Long minI = min!=null ? Long.valueOf(min):null;
			Long maxI = max!=null ? Long.valueOf(max):null;
			resource=new IntResource(name,value,maxI,minI,Category.OTHER);
		}
		else if(AllowedType.STRING.equals(type)){
			resource=new StringResource(name,val);
		}
		else if(AllowedType.CHOICE.equals(type)){
			String[] values=res.getAllowedValueArray();
			resource=new ValueListResource(name,val,Arrays.asList(values),Category.OTHER);
		} 
		else if(AllowedType.BOOLEAN.equals(type)){
			Boolean value=Boolean.valueOf(val);
			resource=new BooleanResource(name, value, Category.OTHER);
		}
		else {
			throw new Exception("Unknown type of resource " + name + ": " + type);
		}
		
		if (description != null)
			resource.setDescription(description);
		return resource;
	}

	/**
	 * create a date format instance according to 
	 * ISO 8601 (http://www.w3.org/TR/NOTE-datetime)
	 */
	public static DateFormat getDateFormat(){
		return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
	}

	public static boolean hasSweep(JobDefinitionDocument job) throws Exception{
		return SweepUtility.hasSweep(job.getDomNode());
	}
	

		
	public static boolean hasExpression(RangeValueType rvt){
		try{
			org.w3c.dom.Attr a = (org.w3c.dom.Attr)rvt.getDomNode().getAttributes().getNamedItemNS(extensionNS, expression);
			return a!=null;
		}catch(Exception ex){}
		return false;
	}
	
	public static String getExpression(RangeValueType rvt){
		try{
			Attr a = (org.w3c.dom.Attr)rvt.getDomNode().getAttributes().getNamedItemNS(extensionNS, expression);
			return a.getValue();
		}catch(Exception ex){}
		return null;
	}
	
	public static void setExpression(RangeValueType rvt, String expr){
		XmlCursor c = rvt.newCursor();
		c.toFirstContentToken();
		c.insertAttributeWithValue(new QName(JSDLUtils.extensionNS, JSDLUtils.expression),expr);
		c.dispose();
	}


	public final static QName AC_QNAME=new QName("http://schemas.ogf.org/hpcp/2007/11/ac","Credential");
	public final static QName AC_USERNAME=new QName("http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd","Username");
	public final static QName AC_PASSWD=new QName("http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd","Password");
	public final static QName AC_QNAME_2=new QName("http://schemas.ogf.org/hpcp/2007/01/fs","Credential");
	
	public final static QName TOKEN_QNAME=new QName("http://www.unicore.eu/unicore/jsdl-extensions","BearerToken");
	
	/** 
	 * Extracts username and password from the security credentials as defined in the HPC file staging profile (GFD.135)
	 * <br/><br/>
	   &lt;Credential xmlns="http://schemas.ogf.org/hpcp/2007/01/ac"&gt;<br/>
         &lt;UsernameToken xmlns="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd">&gt;<br/>
          &lt;Username>sc07demo &lt;/Username&gt;<br/>
          &lt;Password>hpcpsc07 &lt;/Password&gt;<br/>
           &lt;/UsernameToken&gt;<br/>
       &lt;/Credential&gt;<br/>
     * <br/>
	 * The first HPCP Activity Credential found in the given source document is used
	 * 
	 * @param source -  an XML document 
	 * @return {@link UsernamePassword} containing the username and password found in the XML, 
	 * or <code>null</code> if no Credential containing a UsernameToken element was found  
	 */
	public static UsernamePassword extractUsernamePassword(XmlObject source){
		String user=null;
		String passwd=null;
		UsernamePassword userNamePassword=null;
		try{
			XmlObject copy=source.copy();
			XmlObject credential=XmlBeansUtils.extractFirstAnyElement(copy, AC_QNAME);
			if(credential==null){
				//try namespace defined by GFD.135 (but not used in the example)
				credential=XmlBeansUtils.extractFirstAnyElement(copy, AC_QNAME_2);
			}
			if(credential!=null){
				user = XmlBeansUtils.getElementText(credential, AC_USERNAME);
				if(user==null)return null;
				passwd = XmlBeansUtils.getElementText(credential, AC_PASSWD);
				userNamePassword=new UsernamePassword(user, passwd);
			}
		}catch(Exception ex){
			// ignored
		}
		return userNamePassword;
	}
	
	public static OAuthToken extractOAuthToken(XmlObject source){
		OAuthToken token=null;
		try{
			XmlObject copy=source.copy();
			XmlObject credential=XmlBeansUtils.extractFirstAnyElement(copy, AC_QNAME);
			if(credential==null){
				//try namespace defined by GFD.135 (but not used in the example)
				credential=XmlBeansUtils.extractFirstAnyElement(copy, AC_QNAME_2);
			}
			if(credential!=null){
				String value = XmlBeansUtils.getElementText(credential, TOKEN_QNAME);
				if(value!=null)value=value.trim();
				token=new OAuthToken(value);
			}
		}catch(Exception ex){
			// ignored
		}
		return token;
	}

}

