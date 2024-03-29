package eu.unicore.xnjs.io.git;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.jgit.dircache.DirCacheCheckout.CheckoutMetadata;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.CoreConfig.AutoCRLF;
import org.eclipse.jgit.lib.CoreConfig.EolStreamType;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.treewalk.NameConflictTreeWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.WorkingTreeOptions;
import org.eclipse.jgit.util.io.EolStreamTypeUtil;

import eu.unicore.xnjs.io.IStorageAdapter;

/**
 * Checkout helper using the TSI to handle file system operations
 *
 * (based on {@link org.eclipse.jgit.dircache.DirCacheCheckout})
 */
public class TSIDirCacheCheckout {

	private final Repository repo;

	private final Map<String, CheckoutMetadata> updated = new LinkedHashMap<>();

	private final ObjectId mergeCommitTree;

	private NameConflictTreeWalk walk;

	private TSICheckout checkout;

	private final Map<String, DirCacheEntry> builder = new HashMap<>();
	private final Map<String, String> submoduleRevisions = new HashMap<>();

	private final IStorageAdapter tsi;

	public TSIDirCacheCheckout(Repository repo, ObjectId mergeCommitTree, IStorageAdapter tsi)
			throws IOException {
		this.repo = repo;
		this.mergeCommitTree = mergeCommitTree;
		this.tsi = tsi;
	}

	/**
	 * Scan index and merge tree (no HEAD). Used e.g. for initial checkout when
	 * there is no head yet.
	 */
	private void prescanOneTree()
			throws MissingObjectException, IncorrectObjectTypeException,
			CorruptObjectException, IOException {
		updated.clear();

		walk = new NameConflictTreeWalk(repo);
		walk.setHead(addTree(walk, mergeCommitTree));
	
		while (walk.next()) {
			update(walk.getTree(0, CanonicalTreeParser.class));
			if (walk.isSubtree())
				walk.enterSubtree();
		}
	}

	private int addTree(TreeWalk tw, ObjectId id) throws MissingObjectException,
			IncorrectObjectTypeException, IOException {
		if (id == null) {
			return tw.addTree(new EmptyTreeIterator());
		}
		return tw.addTree(id);
	}

	public boolean checkout() throws Exception {
		try (ObjectReader objectReader = repo.getObjectDatabase().newReader()) {
			checkout = new TSICheckout(repo, tsi);
			prescanOneTree();
			Iterator<Map.Entry<String, CheckoutMetadata>> toUpdate = updated
					.entrySet().iterator();
			Map.Entry<String, CheckoutMetadata> e = null;
			try {
				while (toUpdate.hasNext()) {
					e = toUpdate.next();
					String path = e.getKey();
					CheckoutMetadata meta = e.getValue();
					DirCacheEntry entry = builder.get(path);
					if (FileMode.GITLINK.equals(entry.getRawMode())) {
						checkout.checkoutGitlink(entry, path);
					} else {
						checkout.checkout(entry, meta, objectReader, path);
					}
					e = null;
				}
			} catch (Exception ex) {
				if (e != null) {
					toUpdate.remove();
				}
				while (toUpdate.hasNext()) {
					e = toUpdate.next();
					toUpdate.remove();
				}
				throw ex;
			}
		}
		return true;
	}

	private void update(CanonicalTreeParser tree) throws IOException {
		update(0, tree.getEntryPathString(), tree.getEntryObjectId(),
				tree.getEntryFileMode());
	}

	private void update(int index, String path, ObjectId mId,
			FileMode mode) throws IOException {
		if (!FileMode.TREE.equals(mode)) {
			updated.put(path, new CheckoutMetadata(
					walk.getCheckoutEolStreamType(index),
					walk.getSmudgeCommand(index)));

			DirCacheEntry entry = new DirCacheEntry(path, DirCacheEntry.STAGE_0);
			entry.setObjectId(mId);
			entry.setFileMode(mode);
			builder.put(path, entry);
			if(FileMode.GITLINK.equals(mode)) {
				submoduleRevisions.put(path, mId.getName());
			}
		}
	}
	
	public String getSubmoduleRevision(String path) {
		return submoduleRevisions.get(path);
	}

	public static void getContent(Repository repo, String path,
			CheckoutMetadata checkoutMetadata, ObjectLoader ol,
			WorkingTreeOptions opt, OutputStream os)
			throws IOException {
		getContent(repo, path, checkoutMetadata, ol::openStream, opt, os);
	}

	public interface StreamSupplier {
		InputStream load() throws IOException;
	}

	public static void getContent(Repository repo, String path,
			CheckoutMetadata checkoutMetadata, StreamSupplier inputStream,
			WorkingTreeOptions opt, OutputStream os)
			throws IOException {
		EolStreamType nonNullEolStreamType;
		if (checkoutMetadata.eolStreamType != null) {
			nonNullEolStreamType = checkoutMetadata.eolStreamType;
		} else if (opt.getAutoCRLF() == AutoCRLF.TRUE) {
			nonNullEolStreamType = EolStreamType.AUTO_CRLF;
		} else {
			nonNullEolStreamType = EolStreamType.DIRECT;
		}
		try (OutputStream channel = EolStreamTypeUtil.wrapOutputStream(
				os, nonNullEolStreamType)) {
			try (InputStream in = inputStream.load()) {
				in.transferTo(os);
			}
		}
	}

}


