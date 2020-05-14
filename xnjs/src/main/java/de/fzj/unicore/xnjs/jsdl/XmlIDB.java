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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import javax.xml.namespace.QName;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlObject;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.ResourcesType;
import org.ggf.schemas.jsdl.x2005.x11.jsdlPosix.ArgumentType;
import org.ggf.schemas.jsdl.x2005.x11.jsdlPosix.EnvironmentType;
import org.ggf.schemas.jsdl.x2005.x11.jsdlPosix.FileNameType;
import org.ggf.schemas.jsdl.x2005.x11.jsdlPosix.POSIXApplicationType;

import de.fzj.unicore.xnjs.beans.idb.FileSystemDocument.FileSystem;
import de.fzj.unicore.xnjs.beans.idb.IDBApplicationDocument;
import de.fzj.unicore.xnjs.beans.idb.IDBDocument;
import de.fzj.unicore.xnjs.beans.idb.IDBDocument.IDB;
import de.fzj.unicore.xnjs.beans.idb.InfoDocument.Info;
import de.fzj.unicore.xnjs.beans.idb.PropertyDocument.Property;
import de.fzj.unicore.xnjs.beans.idb.ResourceDocument;
import de.fzj.unicore.xnjs.idb.ApplicationInfo;
import de.fzj.unicore.xnjs.idb.ApplicationMetadata;
import de.fzj.unicore.xnjs.idb.IDBImpl;
import de.fzj.unicore.xnjs.idb.IDBParser;
import de.fzj.unicore.xnjs.idb.OptionDescription;
import de.fzj.unicore.xnjs.idb.OptionDescription.Type;
import de.fzj.unicore.xnjs.idb.Partition;
import de.fzj.unicore.xnjs.resources.Resource;
import de.fzj.unicore.xnjs.resources.ResourceRequest;
import de.fzj.unicore.xnjs.resources.ResourceSet;
import de.fzj.unicore.xnjs.util.LogUtil;
import de.fzj.unicore.xnjs.util.XmlBeansUtils;

/**
 * support for legacy 1.x XML IDBs
 * 
 * @author schuller
 */
public class XmlIDB implements IDBParser {

	protected static final Logger logger=LogUtil.getLogger(LogUtil.JOBS,XmlIDB.class);

	private void createDefaultPartition() {
		Partition p = new Partition();
		p.setDescription("(legacy XML IDB)");
		p.setName(IDBImpl.DEFAULT_PARTITION);
		idb.getPartitionsInternal().add(p);
	}


	final IDBImpl idb;
	String data;

	public XmlIDB(IDBImpl idb) {
		this.idb = idb;
	}

	public XmlIDB(IDBImpl idb, String data) {
		this.idb = idb;
		this.data = data;
	}

	public void handleFile(File file)throws Exception{
		XmlObject o=XmlObject.Factory.parse(file);
		QName q=XmlBeansUtils.getQNameFor(o);
		boolean isIDB = IDBDocument.type.getDocumentElementName().equals(q);
		boolean readAllInfo = idb.getMainFile()==null || file.getAbsolutePath().equals(idb.getMainFile().getAbsolutePath());
		if(readAllInfo && isIDB){
			logger.info("Reading main IDB <"+file+">");
			IDBDocument idbd=IDBDocument.Factory.parse(file);
			IDBDocument.IDB xmlidb=idbd.getIDB();
			readIDB(xmlidb);
		}
		else {
			logger.info("Reading apps from <"+file+">");
			// only read apps definitions
			try(FileInputStream fis = new FileInputStream(file)){
				readApplications(fis, idb.getIdb());
			}
		}
	}

	protected void readIDB(IDB xmlidb){

		readProperties(xmlidb);

		if(xmlidb.getSubmitScriptTemplate()!=null){
			idb.setSubmitTemplate(xmlidb.getSubmitScriptTemplate());
		}
		if(xmlidb.getExecuteScriptTemplate()!=null){
			idb.setExecuteTemplate(xmlidb.getExecuteScriptTemplate());
		}

		if(xmlidb.getTargetSystemProperties()!=null){
			readFileSystems(xmlidb);
			readTextInfoProperties(xmlidb);
			readJSDLResources(xmlidb);
		}
		try{
			readApplications(xmlidb.newInputStream(),idb.getIdb());
		}catch(Exception ex){
			logger.error("Error reading applications from IDB", ex);
		}
	}

	private void readProperties(IDB xmlidb){
		Property[] props=xmlidb.getPropertyArray();
		if(props!=null){
			if(props.length>0) {
				logger.error("Properties in XML IDB are IGNORED - put them into xnjs.properties!");
			}
		}
	}

	protected void readTextInfoProperties(IDB xmlidb){
		Info[] infos=xmlidb.getTargetSystemProperties().getInfoArray();
		for(Info i: infos){
			String name=i.getName();
			String value=i.getStringValue();
			if(logger.isDebugEnabled())logger.debug("TextInfo Property: "+name+"='"+value+"'");
			if(name!=null && value!=null){
				idb.getTextInfoPropertiesNoUpdate().put(name.trim(), value.trim());
			}
		}
	}

	protected void readFileSystems(IDB xmlidb){
		FileSystem[] fs=xmlidb.getTargetSystemProperties().getFileSystemArray();
		String path;
		for(FileSystem f: fs){
			String name=f.getName();
			path=f.getIncarnatedPath();
			if(name==null || name.equals(""))continue;
			if(path==null || path.equals(""))continue;
			path=path.endsWith(File.separator)? path : path+File.separator;
			idb.getFilespaces().put(name,path);
			logger.debug("Filesystem <"+name+"> mapped to <"+path+">");
		}
	}

	protected void readJSDLResources(IDB xmlidb){
		try{
			ResourcesType resources=xmlidb.getTargetSystemProperties().getResources();
			if(idb.getPartitions().size()==0) {
				createDefaultPartition();
			}
			Partition p = idb.getDefaultPartition();

			ResourceSet jsdlResources = new JSDLResourceSet(resources);
			p.getResources().putAllResources(jsdlResources.getResources());

			XmlObject[]res=XmlBeansUtils.extractAnyElements(resources, ResourceDocument.type.getDocumentElementName());
			for(XmlObject o:res){
				ResourceDocument rs=(ResourceDocument)o;
				String name=rs.getResource().getName();
				logger.debug("Have resource: "+name);
				Resource resource = JSDLUtils.createResource(rs);
				p.getResources().putResource(resource);
			}

			try{
				String os = resources.getOperatingSystem().getOperatingSystemType().
						getOperatingSystemName().toString();
				String osVersion = resources.getOperatingSystem().getOperatingSystemVersion();
				if(os!=null)idb.getDefaultPartition().setOperatingSystem(os);
				if(osVersion!=null)idb.getDefaultPartition().setOperatingSystemVersion(osVersion);

			}catch(Exception ex){}

		}catch(Exception e){
			logger.error("Error reading resources specification from IDB.",e);
		}
	}

	protected void readApplications(InputStream source, Collection<ApplicationInfo> idb) throws Exception{
		List<IDBApplicationDocument> result = parseApplications(source);
		for(IDBApplicationDocument app : result){
			idb.add(parse(app.getIDBApplication()));
		}
	}

	public void readApplications(Collection<ApplicationInfo> idb) throws Exception {
		if(data==null)throw new IllegalStateException("no data to read applications from");
		List<IDBApplicationDocument> result = parseApplications(XmlObject.Factory.parse(data));
		for(IDBApplicationDocument app : result){
			idb.add(parse(app.getIDBApplication()));
		}
	}

	// TODO
	protected ApplicationInfo parse(IDBApplicationDocument.IDBApplication a){
		POSIXApplicationType pa;

		String name=a.getApplicationName();
		if(name == null)
		{
			logger.warn("Invalid application entry inside the IDB: The application name has not been set.");
			return null;
		}

		String version=a.getApplicationVersion();
		if(version == null)
		{
			logger.warn("Invalid application entry inside the IDB: The version of application "+name+" has not been set.");
			return null;
		}

		pa=a.getPOSIXApplication();
		if(pa == null)
		{
			logger.warn("Invalid application entry inside the IDB: The POSIXApplication element for application "+name+"<"+version+"> is missing.");
			return null;
		}

		FileNameType executable = pa.getExecutable();
		if(executable == null)
		{
			logger.warn("Invalid application entry inside the IDB: The executable for application "+name+"<"+version+"> is missing.");
			return null;
		}

		String description=a.getDescription();

		ApplicationInfo appInfo=new ApplicationInfo();
		appInfo.setName(name);
		appInfo.setVersion(version);
		appInfo.setExecutable(executable.getStringValue());
		appInfo.setDescription(description);

		appInfo.setMetadata(getMetadataFromPosixApplication(pa));

		for(ArgumentType posixArg: pa.getArgumentArray()){
			appInfo.addArgument(posixArg.getStringValue().trim());
		}
		for(EnvironmentType posixEnv: pa.getEnvironmentArray()){
			appInfo.getEnvironment().put(posixEnv.getName(),posixEnv.getStringValue());
		}
		appInfo.setPreCommand(merge(a.getPreCommandArray()));
		appInfo.setPostCommand(merge(a.getPostCommandArray()));
		appInfo.setResourceRequest(getNodeFilter(a.getBSSNodesFilterArray()));
		if(a.isSetPreferInteractive() && a.getPreferInteractive()){
			appInfo.getEnvironment().put("UC_PREFER_INTERACTIVE_EXECUTION","true");
			appInfo.setRunOnLoginNode(true);
		}
		logger.debug("Have application "+name+"<"+version+">"+", executable is "+pa.getExecutable());

		return appInfo;
	}
	
	List<ResourceRequest>getNodeFilter(String[]request){
		List<ResourceRequest>req = new ArrayList<>();
		
		return req;
	}

	private String merge(String[] cmds) {
		StringBuilder sb = new StringBuilder();
		for(String cmd: cmds) {
			if(sb.length()>0)sb.append("; ");
			sb.append(cmd);
		}
		return sb.length()>0? sb.toString() : null;
	}

	protected List<IDBApplicationDocument> parseApplications(InputStream source)throws Exception{
		XmlObject raw = XmlObject.Factory.parse(source);
		return parseApplications(raw);
	}

	protected List<IDBApplicationDocument> parseApplications(XmlObject raw)throws Exception{
		List<IDBApplicationDocument> result = new ArrayList<IDBApplicationDocument>();
		Collection<IDBApplicationDocument> apps = XmlBeansUtils.extractAnyElements(
				raw, IDBApplicationDocument.type.getDocumentElementName(), IDBApplicationDocument.class);
		for(XmlObject a: apps){
			result.add(IDBApplicationDocument.Factory.parse(a.newReader()));
		}
		return result;
	}


	/**
	 * extract metadata that is embedded into the posix app
	 * and add it
	 * 
	 * @param pa - posix app definition
	 * @param meta - metadata to add to
	 */
	protected ApplicationMetadata getMetadataFromPosixApplication(POSIXApplicationType pa){
		ApplicationMetadata meta = new ApplicationMetadata();
		for(ArgumentType a: pa.getArgumentArray()){
			OptionDescription arg = new OptionDescription();

			String text=a.getStringValue();
			Matcher m = IDBImpl.ARG_PATTERN.matcher(text);		
			if(m.matches()){
				String name = m.group(3) == null ? m.group(4) : m.group(3);
				arg.setName(name);
			}

			Map<String,String>map=XmlBeansUtils.extractAttributes(a);
			String type=map.get("Type");
			if(type==null)type="string";
			arg.setType(Type.valueOf(type.toUpperCase()));
			String description=map.get("Description");
			if(description==null) description=map.get("description");
			if(description==null)description="";
			arg.setDescription(description);


			String mime=map.get("MimeType");
			if(mime == null) mime = map.get("mimeType");
			if(mime!=null){
				arg.setValidValues(new String[]{mime});
			}

			//			String def=map.get("Default");
			//			if(def==null) def=map.get("default");
			//			if(def!=null){
			//				arg.setDefault(def);
			//			}
			//
			//			String depV=map.get("DependsOn");
			//			if(depV==null) depV=map.get("dependsOn");
			//			if(depV!=null){
			//				String[] dep=depV.split(" +");
			//				arg.setDependsOnArray(dep);	
			//			}
			//
			//			String exclV=map.get("Excludes");
			//			if(exclV==null) exclV=map.get("excludes");
			//			if(exclV!=null){
			//				String[] excl=exclV.split(" +");
			//				arg.setExcludesArray(excl);	
			//			}
			//			
			//			
			String validV=map.get("ValidValues");
			if(validV==null)validV = map.get("validValues");
			if(validV!=null){
				arg.setValidValues(validV.split(" +"));
			}
			//
			//			String mandatory=map.get("IsMandatory");
			//			if(mandatory == null) mandatory = map.get("isMandatory");
			//			if(mandatory!=null){
			//				boolean isMandatory=Boolean.parseBoolean(mandatory);
			//				arg.setIsMandatory(isMandatory);
			//			}
			//
			//			String enabled=map.get("IsEnabled");
			//			if(enabled==null) enabled = map.get("isEnabled");
			//			if(enabled!=null){
			//				boolean isEnabled=Boolean.parseBoolean(enabled);
			//				arg.setIsEnabled(isEnabled);
			//			}

			meta.getOptions().add(arg);

		}
		return meta;
	}


}
