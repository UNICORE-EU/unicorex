package eu.unicore.xnjs.persistence;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import eu.unicore.persist.util.GSONConverter;
import eu.unicore.util.Log;

public class GSONUtils  {

	public static class XmlBeansConverter implements GSONConverter{

		@Override
		public Type getType() {
			return XmlObject.class;
		}

		@Override
		public Object[] getAdapters() {
			return new Object[]{xBeanAdapter};
		}

		@Override
		public boolean isHierarchy() {
			return true;
		}
	}
	
	private static final XmlBeansAdapter xBeanAdapter = new XmlBeansAdapter();
	
	public static class XmlBeansAdapter implements JsonSerializer<XmlObject>, JsonDeserializer<XmlObject>{

		@Override
		public JsonElement serialize(XmlObject src, Type typeOfSrc,
				JsonSerializationContext context) {
			ByteArrayOutputStream bos=new ByteArrayOutputStream();
			try{
				src.save(bos);
				return new JsonPrimitive(bos.toString());
			}
			catch(IOException io){
				// can't happen
				return new JsonPrimitive("failed: "+Log.getDetailMessage(io));
			}
		}

		@Override
		public XmlObject deserialize(JsonElement json, Type typeOfT,
				JsonDeserializationContext context)
						throws JsonParseException {
			try{
				return XmlObject.Factory.parse(json.getAsString());
			}catch(XmlException xe){
				throw new JsonParseException(xe);
			}
		}

	}

}
