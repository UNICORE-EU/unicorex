package eu.unicore.xnjs.tsi.remote.single;

import java.io.File;
import java.io.FileInputStream;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import eu.unicore.security.Client;
import eu.unicore.util.Log;

public class FileIdentityResolver implements IdentityResolver {

	private static final Logger logger = Log.getLogger(Log.SECURITY, FileIdentityResolver.class);

	private File idFile;

	private long lastUpdate;

	@Override
	public void updateIdentities(Client client, IdentityStore store) {
		if(idFile!=null && idFile.lastModified() > lastUpdate) {
			update(store);
			lastUpdate = idFile.lastModified();
		}
	}

	public void setFile(String file) {
		this.idFile = new File(file);
	}

	int update(IdentityStore store) {
		int num = 0;
		try(FileInputStream fis = new FileInputStream(idFile)){
			JSONObject ids = new JSONObject(IOUtils.toString(fis, "UTF-8"));
			for(String uid: ids.keySet()) {
				try {
					JSONObject idspec = ids.getJSONObject(uid);
					String keyfile = idspec.getString("key");
					String pubfile = idspec.optString("pub", keyfile+".pub");
					byte[] pass = idspec.optString("passphrase", "").getBytes();
					byte[] priv = null;
					byte[] pub = null;
					try(FileInputStream in = new FileInputStream(keyfile)){
						priv = IOUtils.toString(in, "UTF-8").getBytes();
					}
					try(FileInputStream in = new FileInputStream(pubfile)){
						pub = IOUtils.toString(in, "UTF-8").getBytes();
					}
					logger.debug("Registering keypair for <{}>", uid);
					store.register(uid, priv, pub, pass);
					num++;
				}catch(Exception ex) {
					logger.error("Could not read SSH identity for user <{}>: {}", uid, ex);
				}
			}
		}catch(Exception ex) {
			logger.error("Could not read SSH identities from {}: {}", idFile, ex);
		}
		logger.info("Read <{}> identities from {}", num, idFile);
		return num;
	}
}
