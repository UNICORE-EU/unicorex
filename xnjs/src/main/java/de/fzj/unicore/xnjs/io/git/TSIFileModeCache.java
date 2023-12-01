package de.fzj.unicore.xnjs.io.git;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.FileModeCache;
import org.eclipse.jgit.lib.FileModeCache.CacheItem;
import org.eclipse.jgit.lib.Repository;

import de.fzj.unicore.xnjs.io.IStorageAdapter;

/**
 * An adaptation of {@link FileModeCache} using the TSI for file system operations
 *
 * @author schuller
 */
public class TSIFileModeCache {

	private final XCacheItem root = new XCacheItem(FileMode.TREE);

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
		XCacheItem cachedParent = safeCreateDirectory(gitPath, parentDir);
		cachedParent.remove(gitPath.substring(gitPath.lastIndexOf('/') + 1));
	}

	/**
	 * Ensures the given directory {@code dir} with the given git path exists.
	 */
	public XCacheItem safeCreateDirectory(String gitPath, File dir) throws Exception {
		int i = gitPath.lastIndexOf('/');
		String parentPath = null;
		if (i >= 0) {
			parentPath = gitPath.substring(0, i);
		}
		tsi.mkdir(dir.getPath());
		XCacheItem cachedParent = root;
		if (parentPath != null) {
			cachedParent = add(parentPath, FileMode.TREE);
		}
		return cachedParent;
	}

	/**
	 * Records the given {@link FileMode} for the given git path in the cache.
	 * If an entry already exists for the given path, the previously cached file
	 * mode is overwritten.
	 *
	 * @param gitPath
	 *            to cache the {@link FileMode} for
	 * @param finalMode
	 *            {@link FileMode} to cache
	 * @return the {@link CacheItem} for the path
	 */
	@NonNull
	private XCacheItem add(String gitPath, FileMode finalMode) {
		if (gitPath.isEmpty()) {
			throw new IllegalArgumentException();
		}
		String[] parts = gitPath.split("/"); //$NON-NLS-1$
		int n = parts.length;
		int i = 0;
		XCacheItem curr = root;
		while (i < n) {
			XCacheItem next = curr.child(parts[i]);
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

	/**
	 * needed only because we cannot directly use {@link CacheItem}
	 */
	public static class XCacheItem {

		@NonNull
		private FileMode mode;

		private Map<String, XCacheItem> children;

		/**
		 * Creates a new {@link CacheItem}.
		 *
		 * @param mode
		 *            {@link FileMode} to cache
		 */
		public XCacheItem(@NonNull FileMode mode) {
			this.mode = mode;
		}

		/**
		 * Retrieves the cached {@link FileMode}.
		 *
		 * @return the {@link FileMode}
		 */
		@NonNull
		public FileMode getMode() {
			return mode;
		}

		/**
		 * Retrieves an immediate child of this {@link CacheItem} by name.
		 *
		 * @param childName
		 *            name of the child to get
		 * @return the {@link CacheItem}, or {@code null} if no such child is
		 *         known
		 */
		public XCacheItem child(String childName) {
			if (children == null) {
				return null;
			}
			return children.get(childName);
		}

		/**
		 * Inserts a new cached {@link FileMode} as an immediate child of this
		 * {@link CacheItem}. If there is already a child with the same name, it
		 * is overwritten.
		 *
		 * @param childName
		 *            name of the child to create
		 * @param childMode
		 *            {@link FileMode} to cache
		 * @return the new {@link CacheItem} created for the child
		 */
		public XCacheItem insert(String childName, @NonNull FileMode childMode) {
			if (!FileMode.TREE.equals(mode)) {
				throw new IllegalArgumentException();
			}
			if (children == null) {
				children = new HashMap<>();
			}
			XCacheItem newItem = new XCacheItem(childMode);
			children.put(childName, newItem);
			return newItem;
		}

		/**
		 * Removes the immediate child with the given name.
		 *
		 * @param childName
		 *            name of the child to remove
		 * @return the previously cached {@link CacheItem}, if any
		 */
		public XCacheItem remove(String childName) {
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
