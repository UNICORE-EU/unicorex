package de.fzj.unicore.xnjs.io.git;

import java.io.File;
import java.io.OutputStream;
import java.time.Instant;

import org.eclipse.jgit.dircache.DirCacheCheckout.CheckoutMetadata;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.lib.CoreConfig.EolStreamType;
import org.eclipse.jgit.lib.CoreConfig.SymLinks;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.WorkingTreeOptions;
import org.eclipse.jgit.util.RawParseUtils;

import de.fzj.unicore.xnjs.io.ChangePermissions;
import de.fzj.unicore.xnjs.io.ChangePermissions.Mode;
import de.fzj.unicore.xnjs.io.ChangePermissions.PermissionsClass;
import de.fzj.unicore.xnjs.io.IStorageAdapter;
import de.fzj.unicore.xnjs.io.git.TSIFileModeCache.CacheItem;

/**
 * Checkout helper using the TSI to access the target filesystem
 *
 * @author schuller
 */
public class TSICheckout {

	private final TSIFileModeCache cache;

	private final WorkingTreeOptions options;

	private final IStorageAdapter tsi;

	static CheckoutMetadata EMPTY = new CheckoutMetadata(EolStreamType.DIRECT, null);

	/**
	 * Creates a new {@link TSICheckout} for checking out from the given
	 * repository.
	 */
	public TSICheckout(Repository repo, IStorageAdapter tsi) {
		this.cache = new TSIFileModeCache(repo, tsi);
		this.options = repo.getConfig().get(WorkingTreeOptions.KEY);
		this.tsi = tsi;
	}

	/**
	 * Ensure that the given parent directory exists, and cache the information
	 * that gitPath refers to a file.
	 */
	public void safeCreateParentDirectory(String gitPath, File parentDir,
			boolean makeSpace) throws Exception {
		cache.safeCreateParentDirectory(gitPath, parentDir);
	}

	/**
	 * Checks out the gitlink given by the {@link DirCacheEntry}.
	*/
	public void checkoutGitlink(DirCacheEntry entry, String gitPath)
			throws Exception {
		String path = gitPath != null ? gitPath : entry.getPathString();
		File gitlinkDir = new File("/", path);
		File parentDir = gitlinkDir.getParentFile();
		CacheItem cachedParent = cache.safeCreateDirectory(path, parentDir);
		tsi.mkdir(gitlinkDir.getPath());
		cachedParent.insert(path.substring(path.lastIndexOf('/') + 1),
				FileMode.GITLINK);
		entry.setLastModified(Instant.now());
	}

	/**
	 * Checks out the file given by the {@link DirCacheEntry}.
	*/
	public void checkout(DirCacheEntry entry, CheckoutMetadata metadata,
			ObjectReader reader, String gitPath) throws Exception {
		if (metadata == null) {
			metadata = EMPTY;
		}
		ObjectLoader ol = reader.open(entry.getObjectId());
		String path = gitPath != null ? gitPath : entry.getPathString();
		File f = new File("/", path);
		File parentDir = f.getParentFile();
		CacheItem cachedParent = cache.safeCreateDirectory(path, parentDir);
		if (entry.getFileMode() == FileMode.SYMLINK
				&& options.getSymLinks() == SymLinks.TRUE) {
			byte[] bytes = ol.getBytes();
			String target = RawParseUtils.decode(bytes);
			tsi.link(target, f.getPath());
			cachedParent.insert(f.getName(), FileMode.SYMLINK);
			entry.setLength(bytes.length);
			entry.setLastModified(Instant.now());
			return;
		}
		String name = f.getName();
		if (name.length() > 200) {
			name = name.substring(0, 200);
		}
		try (OutputStream os = tsi.getOutputStream(f.getPath())){
			TSIDirCacheCheckout.getContent(cache.getRepository(), path, metadata, ol,
					options,
					os);
			// The entry needs to correspond to the on-disk file size
			if (metadata.eolStreamType == EolStreamType.DIRECT
					&& metadata.smudgeFilterCommand == null) {
				entry.setLength(ol.getSize());
			} else {
				os.flush();
				long size = tsi.getProperties(f.getPath()).getSize();
				entry.setLength(size);
			}
			if (options.isFileMode()) {
				if (FileMode.EXECUTABLE_FILE.equals(entry.getRawMode())) {
					tsi.chmod2(f.getPath(), makeExecutable, false);
				}
			}
			cachedParent.remove(f.getName());
			entry.setLastModified(Instant.now());
		}

	}

	private static ChangePermissions[] makeExecutable = 
		new ChangePermissions[] {
				new ChangePermissions(Mode.ADD, PermissionsClass.OWNER, "--x"),
				new ChangePermissions(Mode.ADD, PermissionsClass.GROUP, "--x")
		};

}