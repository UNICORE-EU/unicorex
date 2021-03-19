package de.fzj.unicore.uas.fts.ws;

import java.util.Arrays;
import java.util.List;

import org.unigrids.services.atomic.types.ProtocolDocument;
import org.unigrids.services.atomic.types.ProtocolType;

import de.fzj.unicore.wsrflite.Resource;
import de.fzj.unicore.wsrflite.xmlbeans.renderers.ValueRenderer;

public class ProtocolRenderer extends ValueRenderer{

	private List<ProtocolType.Enum>protocols;
	
	/**
	 * if you want to provide the protocols dynamically, use this constructor
	 * and override the getProtocols() method
	 * @param parent
	 */
	public ProtocolRenderer(Resource parent){
		this(parent,(List<ProtocolType.Enum>)null);
	}
	
	public ProtocolRenderer(Resource parent, ProtocolType.Enum ... protocols){
		this(parent,Arrays.asList(protocols));
	}

	public ProtocolRenderer(Resource parent, List<ProtocolType.Enum>protocols){
		super(parent, ProtocolDocument.type.getDocumentElementName());
		this.protocols=protocols;
	}
	
	@Override
	protected ProtocolDocument[] getValue() throws Exception {
		List<ProtocolType.Enum>protocols=getProtocols();
		if(protocols==null)return null;
		
		ProtocolDocument[] ds=new ProtocolDocument[protocols.size()];
		int i=0;
		for(ProtocolType.Enum p: protocols){
			ProtocolDocument d=ProtocolDocument.Factory.newInstance();
			d.setProtocol(p);
			ds[i]=d;
			i++;
		}
		return ds;
	}

	public List<ProtocolType.Enum>getProtocols(){
		return protocols;
	}
	
}
