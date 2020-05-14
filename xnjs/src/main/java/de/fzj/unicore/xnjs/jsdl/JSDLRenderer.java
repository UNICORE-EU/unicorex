package de.fzj.unicore.xnjs.jsdl;

import java.util.Iterator;
import java.util.Map;

import org.apache.xmlbeans.XmlObject;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.OperatingSystemType;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.OperatingSystemTypeEnumeration;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.RangeValueType;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.ResourcesDocument;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.ResourcesType;
import org.ggf.schemas.jsdl.x2005.x11.jsdlPosix.EnvironmentType;
import org.ggf.schemas.jsdl.x2005.x11.jsdlPosix.POSIXApplicationDocument;
import org.ggf.schemas.jsdl.x2005.x11.jsdlPosix.POSIXApplicationType;

import de.fzj.unicore.xnjs.idb.ApplicationInfo;
import de.fzj.unicore.xnjs.idb.ApplicationInfoRenderer;
import de.fzj.unicore.xnjs.idb.ApplicationMetadata;
import de.fzj.unicore.xnjs.idb.OptionDescription;
import de.fzj.unicore.xnjs.idb.Partition;
import de.fzj.unicore.xnjs.resources.RangeResource;
import de.fzj.unicore.xnjs.resources.Resource;
import de.fzj.unicore.xnjs.resources.ResourceSet;
import de.fzj.unicore.xnjs.resources.ResourceSetRenderer;
import de.fzj.unicore.xnjs.util.XmlBeansUtils;
import eu.unicore.jsdl.extensions.AllowedType;
import eu.unicore.jsdl.extensions.ArgumentDocument.Argument;
import eu.unicore.jsdl.extensions.ArgumentMetadataDocument.ArgumentMetadata;
import eu.unicore.jsdl.extensions.MetadataDocument;
import eu.unicore.jsdl.extensions.ResourceRequestDocument;

/**
 * renders XNJS resources, apps etc into JSDL
 * 
 * @author schuller
 */
public class JSDLRenderer implements ResourceSetRenderer<ResourcesDocument>, 
ApplicationInfoRenderer<POSIXApplicationDocument>
{

	public ResourcesDocument render(Partition partition) {
		ResourcesDocument doc = render(partition.getResources());
		OperatingSystemType t = doc.getResources().addNewOperatingSystem();
		if(partition.getOperatingSystemVersion()!=null){
			t.setOperatingSystemVersion(partition.getOperatingSystemVersion());
		}
		try{
			if(partition.getOperatingSystem()!=null){
				t.addNewOperatingSystemType().setOperatingSystemName(
						OperatingSystemTypeEnumeration.Enum.forString(partition.getOperatingSystem()));
			}
		}catch(Exception e){
			t.addNewOperatingSystemType().setOperatingSystemName(OperatingSystemTypeEnumeration.LINUX);
		}
		return doc;
	}
	
	
	public ResourcesDocument render(ResourceSet resources) {
		ResourcesDocument doc=ResourcesDocument.Factory.newInstance();
		ResourcesType rt=doc.addNewResources();
		Iterator<Resource>iterator=resources.getResources().iterator();
		while(iterator.hasNext()){
			Resource r=iterator.next();
			String name=r.getName();
			if(JSDLResourceSet.RUN_TIME.equals(name)){
				rt.setIndividualCPUTime(renderRVT(r));
			}
			else if(JSDLResourceSet.CPUS_PER_NODE.equals(name)){
				rt.setIndividualCPUCount(renderRVT(r));
			}
			else if(JSDLResourceSet.MEMORY_PER_NODE.equals(name)){
				rt.setIndividualPhysicalMemory(renderRVT(r));
			}
			else if(JSDLResourceSet.TOTAL_CPUS.equals(name)){
				rt.setTotalCPUCount(renderRVT(r));
			} 
			else if(JSDLResourceSet.NODES.equals(name)){
				rt.setTotalResourceCount(renderRVT(r));
			}
			else if(JSDLResourceSet.RESERVATION_ID.equals(name)){
				try{
					String resID="<u6rr:ReservationReference xmlns:u6rr=\"http://www.unicore.eu/unicore/xnjs\">"+r.getStringValue()+"</u6rr:ReservationReference>";
					XmlObject o=XmlObject.Factory.parse(resID);
					XmlBeansUtils.append(o, doc);
				}catch(Exception ex){
					ex.printStackTrace();
				}
			} 
			//non JSDL stuff rendered as JSDL-UNICORE extensions
			else{
				ResourceRequestDocument rsd=ResourceRequestDocument.Factory.newInstance();
				rsd.addNewResourceRequest().setName(name);
				String value=r.getStringValue();
				rsd.getResourceRequest().setValue(value);
				XmlBeansUtils.append(rsd, doc);
			}
		}
		return doc;
	}
	
	private RangeValueType renderRVT(Resource r){
		RangeValueType rvt = RangeValueType.Factory.newInstance();
		if(r instanceof RangeResource){
			RangeResource rr = (RangeResource)r;
			if(r.getDoubleValue()!=null){
				rvt.addNewExact().setDoubleValue(r.getDoubleValue());
			}
			if(rr.getLower()!=null){
				rvt.addNewRange().addNewLowerBound().setStringValue(String.valueOf(rr.getLower()));
				rvt.getRangeArray(0).addNewUpperBound().setStringValue(String.valueOf(rr.getUpper()));
			}
		}
		else {
			String expr = r.getStringValue();
			JSDLUtils.setExpression(rvt, expr);
		}
		return rvt;
	}

	public POSIXApplicationDocument render(ApplicationInfo applicationInfo) {
		POSIXApplicationDocument pad=POSIXApplicationDocument.Factory.newInstance();
		POSIXApplicationType pa=pad.addNewPOSIXApplication();
		pa.addNewExecutable().setStringValue(applicationInfo.getExecutable());
		for(String arg: applicationInfo.getArguments()){
			pa.addNewArgument().setStringValue(arg);
		}
		for(Map.Entry<String, String> env: applicationInfo.getEnvironment().entrySet()){
			EnvironmentType e=pa.addNewEnvironment();
			e.setName(env.getKey());
			e.setStringValue(env.getValue());
		}
		if(applicationInfo.getStdout()!=null){
			pa.addNewOutput().setStringValue(applicationInfo.getStdout());
		}
		if(applicationInfo.getStderr()!=null){
			pa.addNewError().setStringValue(applicationInfo.getStderr());
		}
		if(applicationInfo.getStdin()!=null){
			pa.addNewInput().setStringValue(applicationInfo.getStdin());
		}
		return pad;
	}

	
	public MetadataDocument render(ApplicationMetadata meta){
		MetadataDocument mdd = MetadataDocument.Factory.newInstance();
		mdd.addNewMetadata();
		for(OptionDescription option: meta.getOptions()){
			Argument arg = mdd.getMetadata().addNewArgument();
			arg.setName(option.getName());
			ArgumentMetadata amd = arg.addNewArgumentMetadata();
			amd.setDescription(option.getDescription());
			String type = option.getType().toString().toLowerCase();
			amd.setType(AllowedType.Enum.forString(type));
			if("filename".equals(type)) {
				String[] mtypes = option.getValidValues();
				if(mtypes!=null && mtypes.length>0){
					amd.setMimeType(renderMimetype(mtypes));
				}
			}
			else if("choice".equals(type)) {
				String[] choices = option.getValidValues();
				if(choices!=null) for(String c: choices) {
					amd.addNewValidValue().setStringValue(c);
				}
			}
		}
		return mdd;
	}
	
	private String renderMimetype(String[] mtypes){
		StringBuilder sb = new StringBuilder();
		for(String m: mtypes){
			if(sb.length()>0)sb.append(", ");
			sb.append(m);
		}
		return sb.toString();
	}
}
