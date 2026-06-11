package eu.unicore.xnjs.io;

import java.time.Duration;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import jakarta.inject.Singleton;

@Singleton
public class ACLSupportCache {

	private final Cache<String, Boolean> aclSupportCache;

	public ACLSupportCache(){
		aclSupportCache = initCache();
	}

	public void cacheACLSupport(String root, String path, Boolean value) {
		aclSupportCache.put(root+path, value);
	}

	public Boolean getACLCachedSupport(String root, String path) {
		return aclSupportCache.getIfPresent(root+path);
	}
	
	private Cache<String, Boolean> initCache() {
		Cache<String, Boolean> c = CacheBuilder.newBuilder().concurrencyLevel(4).initialCapacity(1000)
				.expireAfterAccess(Duration.ofSeconds(20))
				.expireAfterWrite(Duration.ofSeconds(20))
				.build();
		return c;
	}
	
}
