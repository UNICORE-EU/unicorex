package de.fzj.unicore.xnjs.io.git;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.Repository;

import de.fzj.unicore.xnjs.io.IStorageAdapter;

/**
 * Cache git tree entries using the TSI for file system operations
 *
 * @author schuller
 */
public class TSIFileModeCache {

	private final CacheItem root = new CacheItem(FileMode.TREE);

	private final Repository repo;

	private final IStorageAdapter tsi;

	public TSIFileModeCache(Repository repo, IStorageAdapter tsi) {
		this.repo = repo;
		this.tsi = tsi;
	}

	public Repository getRepository() {
		return repo;
	}

	/**
	 * Ensure that the given parent directory exists, and cache the information
	 * that gitPath refers to a file.
	 */
	public void safeCreateParentDirectory(String gitPath, File parentDir)
			throws Exception {
		CacheItem cachedParent = safeCreateDirectory(gitPath, parentDir);
		cachedParent.remove(gitPath.substring(gitPath.lastIndexOf('/') + 1));
	}

	/**
	 * Ensures the given directory {@code dir} with the given git path exists.
	 */
	public CacheItem safeCreateDirectory(String gitPath, File dir) throws Exception {
		int i = gitPath.lastIndexOf('/');
		String parentPath = null;
		if (i >= 0) {
			parentPath = gitPath.substring(0, i);
		}
		tsi.mkdir(dir.getPath());
		CacheItem cachedParent = root;
		if (parentPath != null) {
			cachedParent = add(parentPath, FileMode.TREE);
		}
		return cachedParent;
	}

	/**
	 * Records the given {@link FileMode} for the given git path in the cache.
	 * If an entry already exists for the given path, the previously cached file
	 * mode is overwritten.
	*/
	private CacheItem add(String gitPath, FileMode finalMode) {
		if (gitPath.isEmpty()) {
			throw new IllegalArgumentException();
		}
		String[] parts = gitPath.split("/");
		int n = parts.length;
		int i = 0;
		CacheItem curr = root;
		while (i < n) {
			CacheItem next = curr.child(parts[i]);
			if (next == null) {
				break;
			}
			curr = next;
			i++;
		}
		if (i == n) {
			curr.setMode(finalMode);
		} else {
			while (i < n) {
				curr = curr.insert(parts[i],
						i + 1 == n ? finalMode : FileMode.TREE);
				i++;
			}
		}
		return curr;
	}

	public static class CacheItem {

		private FileMode mode;

		private Map<String, CacheItem> children;

		public CacheItem(FileMode mode) {
			this.mode = mode;
		}

		public FileMode getMode() {
			return mode;
		}

		public CacheItem child(String childName) {
			if (children == null) {
				return null;
			}
			return children.get(childName);
		}

		public CacheItem insert(String childName, @NonNull FileMode childMode) {
			if (!FileMode.TREE.equals(mode)) {
				throw new IllegalArgumentException();
			}
			if (children == null) {
				children = new HashMap<>();
			}
			CacheItem newItem = new CacheItem(childMode);
			children.put(childName, newItem);
			return newItem;
		}

		public CacheItem remove(String childName) {
			if (children == null) {
				return null;
			}
			return children.remove(childName);
		}

		public void setMode(@NonNull FileMode mode) {
			this.mode = mode;
			if (!FileMode.TREE.equals(mode)) {
				children = null;
			}
		}
	}

}
