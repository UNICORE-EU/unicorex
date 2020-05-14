/*********************************************************************************
 * Copyright (c) 2006 Forschungszentrum Juelich GmbH 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * (1) Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the disclaimer at the end. Redistributions in
 * binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 * 
 * (2) Neither the name of Forschungszentrum Juelich GmbH nor the names of its 
 * contributors may be used to endorse or promote products derived from this 
 * software without specific prior written permission.
 * 
 * DISCLAIMER
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *********************************************************************************/
 

package de.fzj.unicore.xnjs.io;

/**
 * interface to manage a running file transfer 
 * 
 * @author schuller
 */
public interface IFileTransfer extends Runnable {
	
	
	/**
	 * how to deal with existing files?
	 * <ul>
	 *  <li>OVERWRITE: overwrite existing file (default)</li> 
	 *  <li>APPEND: append to existing file</li>
	 *  <li>DONT_OVERWRITE: throw error if file exists</li>
	 *  <li>RESUME_FAILED_TRANSFER: try to pick up a previously aborted transfer</li>
	 * </ul>
	 */
	public static enum OverwritePolicy {
		 OVERWRITE,
		 APPEND,
		 DONT_OVERWRITE,
		 RESUME_FAILED_TRANSFER
	}
	
	/**
	 * For imports, UNICORE will check if the file is already
	 * available locally (e.g. the import source is on the same
	 * file system).
	 *  <ul>
	 *    <li>PREFER_COPY: (default) data is copied if possible</li> 
	 *    <li>FORCE_COPY: data is always copied</li>
	 *    <li>PREFER_LINK: data is sym-linked, if possible</li>
	 *  </ul>
	 */
	public static enum ImportPolicy {
		PREFER_COPY, 
		FORCE_COPY,
		PREFER_LINK,
	}
	
	/**
	 * set the storage adaptor
	 * @param adapter
	 */
	public void setStorageAdapter(IStorageAdapter adapter);
	
	/**
	 * set the overwrite behaviour
	 * @see OverwritePolicy
	 * @param overwrite
	 */
	public void setOverwritePolicy(OverwritePolicy overwrite)throws OptionNotSupportedException;
	
	/**
	 * set the import behaviour (import, try-copy, try-link).
	 * This is a best-effort feature, and an implementation may completely 
	 * ignore it!
	 */
	public void setImportPolicy(ImportPolicy policy);
	
	/**
	 * start the transfer
	 */
	public void run();

	/**
	 * attempt to abort the transfer
	 */
	public void abort();
	
	/**
	 * get the transfer info instance that keeps track of the status of this transfer
	 */
	public TransferInfo getInfo();
	
}