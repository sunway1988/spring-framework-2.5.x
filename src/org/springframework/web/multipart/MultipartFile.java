package org.springframework.web.multipart;

import java.io.File;
import java.io.InputStream;

/**
 * Interface which represents a file received in a multipart request.
 *
 * <p>The file contents are either stored in memory or temporarily on disk.
 * In either case, the user is responsible for copying file contents to
 * a persistent store if desired. The temporary storages will be cleared
 * at the end of request processing.
 *
 * @author Juergen Hoeller
 * @author Trevor D. Cook
 * @since 29-Sep-2003
 * @see org.springframework.web.multipart.MultipartHttpServletRequest
 * @see org.springframework.web.multipart.MultipartResolver
 */
public interface MultipartFile {

	/**
	 * Return the name of the parameter in the multipart form.
	 * @return the name of the parameter
	 */
	String getName();

	/**
	 * Return the original filename in the client's filesystem.
	 * This may contain path information depending on the browser used,
	 * but it typically will not with any other than Opera.
	 * @return the original filename
	 */
	String getOriginalFileName();

	/**
	 * Return the content type of the file.
	 * @return the content type (or null if not defined)
	 */
	String getContentType();

	/**
	 * Return the size of the file in bytes.
	 * @return the size of the file
	 */
	long getSize();

	/**
	 * Return the contents of the file as an array of bytes.
	 * @return the contents of the file as bytes
	 */
	byte[] getBytes() throws MultipartException;

	/**
	 * Return an InputStream to read the contents of the file from.
	 * The user is responsible for closing the stream.
	 * @return the contents of the file as stream
	 * @throws org.springframework.web.multipart.MultipartException in case of access errors
	 * (if the temporary store fails)
	 */
	InputStream getInputStream() throws MultipartException;

	/**
	 * Transfer the received file to the given destination file.
	 * <p>This may either move the file in the filesystem, copy the file in the
	 * filesystem, or save memory-held contents to the destination file.
	 * <p>If the file has been moved in the filesystem, this operation cannot
	 * be invoked again. Therefore, call this method just once to be able to
	 * work with any storage mechanism.
	 * @param dest the destination file
	 * @throws org.springframework.web.multipart.MultipartException in case of reading or writing errors
	 * @throws java.lang.IllegalStateException if the file has already been moved in the
	 * filesystem as is not available anymore for another transfer
	 */
	void transferTo(File dest) throws MultipartException, IllegalStateException;

}
