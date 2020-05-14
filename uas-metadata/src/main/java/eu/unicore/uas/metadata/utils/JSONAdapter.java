package eu.unicore.uas.metadata.utils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Format the metadata as JSON
 *
 * @author w.noor
 * @author jrybicki
 */
public class JSONAdapter {

    /**
     * Converts given metadata in form of a JSON string bytes into a map representation
     * 
     * @param what JSON string bytes
     * @return map of the metadata
     * @throws IOE
     */
    public Map<String, String> convert(byte[] what) throws JSONException {
        Map<String, String> ret = new HashMap<String, String>();

        String raw = new String(what);
        JSONObject json;

        json = new JSONObject(raw);

        @SuppressWarnings("unchecked")
		Iterator<String> it = (Iterator<String>) json.keys();
        while (it.hasNext()) {
            String key = it.next();
            ret.put(key, json.getString(key));
        }
        return ret;
    }

    /**
     * Converts given metadata from map form into a JSON string bytes
     * 
     * @param metadata map with original metadata
     * @return JSON string
     */
    public byte[] convert(Map<String, String> metadata) {
        JSONObject jsonObj = new JSONObject();

        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            try {
                jsonObj.put(entry.getKey(), entry.getValue());
            } catch (JSONException ex) {
                throw new IllegalArgumentException("Unable to convert the metadata in JSON format", ex);
            }
        }
        return jsonObj.toString().getBytes();
    }
}
