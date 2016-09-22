/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * -------------------------------------------------------------------
 *
 * History
 *   Jun 9, 2006 (wiswedel): created
 */
package org.knime.core.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.swing.filechooser.FileSystemView;

import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.Platform;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.WorkflowContext;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.util.pathresolve.ResolverUtil;

/**
 * Utility class to do some basic file handling that is not available through
 * java API. This includes copying of files and deleting entire directories.
 * These methods are mainly used for the load/save of the workflow.
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class FileUtil {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(FileUtil.class);

    private static final Set<File> TEMP_FILES = Collections.synchronizedSet(new HashSet<File>());

    private static final AtomicLong TEMP_DIR_UNIFIER = new AtomicLong((int)(100000 * Math.random()));

    // timeout when connecting to or reading from URLs
    private static int urlTimeout = 1000;

    private static final boolean IS_WINDOWS = Platform.OS_WIN32.equals(Platform.getOS());

    static {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                synchronized (TEMP_FILES) {
                    for (File f : TEMP_FILES) {
                        if (!f.exists()) {
                            continue;
                        }

                        if (f.isFile()) {
                            f.delete();
                        } else if (f.isDirectory()) {
                            try {
                                deleteRecursively(f, false);
                            } catch (Exception ex) {
                                LOGGER.error(ex.getMessage(), ex);
                            }
                        }
                    }
                }
            }
        });

        String to = System.getProperty(KNIMEConstants.PROPERTY_URL_TIMEOUT);
        if (to != null) {
            try {
                urlTimeout = Integer.parseInt(to);
            } catch (NumberFormatException ex) {
                LOGGER.error("Illegal value for property "
                        + KNIMEConstants.PROPERTY_URL_TIMEOUT + ": " + to);
            }
        }
    }

    /** Don't let anybody instantiate this class. */
    private FileUtil() {
        // don't instantiate
    }

    /**
     * Copies a file. The implementation uses a temporary buffer of 8kB. This
     * method will report progress to the given execution monitor and will also
     * check a canceled status. The copy process will report progress in the
     * full range of the execution monitor. Consider to use a sub-execution
     * monitor if the copy process is only a small part of the entire work.
     *
     * @param file The file to copy.
     * @param destination The destination file, fully qualified (do not provide
     *            a directory).
     * @param exec The execution monitor for progress information.
     * @throws CanceledExecutionException If canceled. The destination file will
     *             be deleted.
     * @throws IOException If that fail for any reason.
     */
    public static void copy(final File file, final File destination,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        final long size = file.length();
        byte[] cache = new byte[BUFF_SIZE];
        CanceledExecutionException cee = null;
        try (OutputStream copyOutStream = new FileOutputStream(destination);
                InputStream copyInStream = new FileInputStream(file)) {
            int read;
            long processed = 0;
            exec.setMessage("Copying \"" + file.getName() + "\"");
            while ((read = copyInStream.read(cache, 0, cache.length)) > 0) {
                copyOutStream.write(cache, 0, read);
                processed += read;
                exec.setProgress(processed / (double)size);
                try {
                    exec.checkCanceled();
                } catch (CanceledExecutionException c) {
                    cee = c;
                    break;
                }
            }
        }
        // delete destination file if canceled.
        if (cee != null) {
            if (!destination.delete()) {
                LOGGER.warn("Unable to delete \"" + destination.getName()
                        + "\" after copying has been canceled.");
            }
            throw cee;
        }
    } // copy(File, File, ExecutionMonitor)

    /**
     * Copies the given source (either a file or a directory) into the given
     * target. If the source file or directory exist, it will be removed first.
     * File permissions are not handled explicitly.
     *
     * @param sourceDir contains all source file and directories to be copied
     * @param targetDir target file (created or replaced) with the given source
     *            file structure
     * @throws IOException if the source does not exist or the source could not
     *             be copied due to file permissions
     */
    public static void copyDir(final File sourceDir, final File targetDir)
            throws IOException {
        if (!sourceDir.exists()) {
            throw new IOException("Source directory \"" + sourceDir
                    + "\" does not exist.");
        }
        if (sourceDir.isDirectory()) {
            if (!targetDir.isDirectory() && !targetDir.mkdirs()) {
                throw new IOException("Cannot create target directory \"" + targetDir.getAbsolutePath() + "\"");
            }
            final String[] sourceDirList = sourceDir.list();
            if (sourceDirList == null) {
                throw new IOException("Can't copy directory \"" + sourceDir
                        + "\", no read permissions.");
            }
            for (String child : sourceDirList) {
                copyDir(new File(sourceDir, child), new File(targetDir, child));
            }
        } else {
            if (targetDir.isDirectory()) {
                FileUtil.deleteRecursively(targetDir);
            }
            copy(sourceDir, targetDir);
        }
    }

    /**
     * Copies the bytes as read from <code>input</code> to the output stream
     * <code>destination</code>. Neither <code>input</code> nor
     * <code>destination</code> get closed at the end!
     *
     * @param input any input stream from which data is read. The stream does not need to be buffered.
     * @param destination any output stream to which data is written. The stream does not need to be buffered.
     * @throws IOException If that fails for any reason.
     * @throws NullPointerException If any argument is <code>null</code>.
     */
    public static void copy(final InputStream input,
            final OutputStream destination) throws IOException {
        byte[] cache = new byte[BUFF_SIZE];
        int read;
        while ((read = input.read(cache, 0, cache.length)) > 0) {
            destination.write(cache, 0, read);
        }
    }

    /**
     * Copies the chars as read from <code>source</code> to the writer
     * <code>destination</code>. Neither <code>input</code> nor
     * <code>destination</code> get closed at the end!
     *
     * @param source To read from
     * @param destination To write to
     * @throws IOException If that fails for any reason.
     * @throws NullPointerException If any argument is <code>null</code>.
     */
    public static void copy(final Reader source, final Writer destination)
            throws IOException {
        char[] cache = new char[BUFF_SIZE];
        int read;
        while ((read = source.read(cache, 0, cache.length)) > 0) {
            destination.write(cache, 0, read);
        }
    }

    /**
     * Copies a file. The implementation uses a temporary buffer of 8kB.
     *
     * @param file The file to copy.
     * @param destination The destination file, fully qualified (do not provide
     *            a directory).
     * @throws IOException If that fail for any reason.
     */
    public static void copy(final File file, final File destination)
            throws IOException {
        ExecutionMonitor exec = new ExecutionMonitor();
        try {
            copy(file, destination, exec);
        } catch (CanceledExecutionException cee) {
            // can't happen, private execution monitor
        }
    }

    /**
     * Deletes a given directory recursively. If the argument represents a file,
     * the file will be deleted. If it represents a symbolic link, it won't
     * follow the link but simply delete the link. Links contained in any of the
     * subdirectories are deleted without touching the link source.
     *
     * @param dir The directory or file to delete.
     * @return If that was successful.
     */
    public static boolean deleteRecursively(final File dir) {
        return deleteRecursively(dir, true);
    }

    private static boolean deleteRecursively(final File dir, final boolean removeFromTempfiles) {
        String name = dir.getName();
        File dirWithCanonicalParent = dir;
        File parentFile = dir.getParentFile();
        if (parentFile != null) {
            try {
                // get canonical parent (resolve symlinks in parent path)
                dirWithCanonicalParent =
                        new File(parentFile.getCanonicalFile(), name);
            } catch (IOException e) {
                // ignore, leave dir as it is
            }
        }
        File canonicalDir;
        try {
            canonicalDir = dirWithCanonicalParent.getCanonicalFile();
        } catch (IOException e) {
            return false;
        }

        // a symbolic link has a different canonical path than its actual path,
        // unless it's a link to itself
        if (!IS_WINDOWS
                && !canonicalDir.equals(dirWithCanonicalParent
                        .getAbsoluteFile())) {
            // this file is a symbolic link, and there's no reason for us to
            // follow it, because then we might be deleting something outside of
            // the directory we were told to delete

            // we delete the link here and return
            return dirWithCanonicalParent.delete();
        }

        // now we go through all of the files and subdirectories in the
        // directory and delete them one by one
        File[] files = canonicalDir.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                File file = files[i];

                // in case this directory is actually a symbolic link, or it's
                // empty, we want to try to delete the link before we try
                // anything
                boolean deleted = file.delete();
                if (!deleted && file.isDirectory()) {
                    // deleting the file failed, so maybe it's a non-empty
                    // directory; otherwise, there's nothing else we can do
                    deleteRecursively(file, removeFromTempfiles);
                }
            }
        }
        // now that we tried to clear the directory out, we can try to delete it
        // again
        if (dirWithCanonicalParent.delete()) {
            if (removeFromTempfiles) {
                // remove from temporary files, if they are in the list
                TEMP_FILES.remove(dir);
                TEMP_FILES.remove(dirWithCanonicalParent);
            }
            return true;
        } else {
            return false;
        }
    } // deleteRecursively(File)

    // size of read buffer when reading/writing from/to a zip stream
    private static final int BUFF_SIZE = 16384;

    /**
     * Packs all files and directories passed in the includeList into a zip
     * file. Recursively adds all files contained in directories. Files in the
     * include list are placed in the root of the archive. Files and directories
     * in the include list must not have the same (simple) name - otherwise an
     * I/O Exception is thrown.
     *
     * @param zipFile the zip file that should be created. If it exists it will
     *            be overwritten.
     * @param includeList list of files or directories to add to the zip
     *            archive. Directories will be added with their content
     *            (recursively). Files are placed in the root of the archive
     *            (i.e. their path is not preserved). If entries have the same
     *            (simple) name, an I/O Exception is thrown.
     * @param compressionLevel the desired compression level, see
     *            {@link ZipOutputStream#setLevel(int)}
     * @param filter each file (and directory) contained is only included in the
     *            zip archive if it is accepted by the filter. If a directory is
     *            not accepted, it entire content is excluded from the zip. Must
     *            not be null.
     * @param exec receives progress messages and is checked for cancel
     *            requests. Optional, can be null.
     *
     * @return <code>true</code> if all files and dirs accepted by the filter
     *         are included, <code>false</code> if an error occurs reading a
     *         file, if a directory is unreadable.
     * @throws CanceledExecutionException if the operation was canceled through
     *             the <code>exec</code>
     * @throws IOException if an I/O error occurs when writing the zip file, or
     *             if two files or directories in the include list have the same
     *             (simple) name.
     */
    public static boolean zipDir(final File zipFile,
            final Collection<File> includeList, final int compressionLevel,
            final ZipFileFilter filter, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        ZipOutputStream zout =
                new ZipOutputStream(new BufferedOutputStream(
                        new FileOutputStream(zipFile)));
        zout.setLevel(compressionLevel);
        try {
            return zipDir(zout, includeList, filter, exec);
        } finally {
            zout.close();
        }
    }

    /** Similar to {@link #zipDir(ZipOutputStream, Collection, String, ZipFileFilter, ExecutionMonitor)}, whereby
     * all elements are directly put into the zip root.
     * @param zout See delegating method
     * @param includeList See delegating method
     * @param filter See delegating method
     * @param exec See delegating method
     * @return See delegating method
     * @throws IOException See delegating method
     * @throws CanceledExecutionException See delegating method
     *
     */
    public static boolean zipDir(final ZipOutputStream zout,
        final Collection<File> includeList, final ZipFileFilter filter,
        final ExecutionMonitor exec) throws IOException, CanceledExecutionException {
        return zipDir(zout, includeList, "", filter, exec);
    }

    /**
     * Packs all files and directories passed in the includeList into a zip
     * stream. Recursively adds all files contained in directories. Files in the
     * include list are placed in the root of the archive. Files and directories
     * in the include list must not have the same (simple) name - otherwise an
     * I/O Exception is thrown. The passed stream is not closed when the method
     * returns. The stream should have the appropriate compression level set.
     *
     * @param zout a zipped output stream. Zip entries for each file are added
     *            to the stream. The compression level is not changed by this
     *            method. The stream remains open after the method returns!
     * @param includeList list of files or directories to add to the zip
     *            archive. Directories will be added with their content
     *            (recursively). Files are placed in the root of the archive
     *            (i.e. their path is not preserved).
     * @param zipEntryPrefix an optional parameter to specify the parent entry of
     *            the added directory content. In most cases this parameter is
     *            "" or null but can also be, e.g. "subfolder1/subfolder2/" as
     *            parent hierarchy. Callers should then create the respective
     *            (empty) zip entries up-front and should include the '/'
     *            at the end of this string
     * @param filter each file (and directory) contained is only included in the
     *            zip archive if it is accepted by the filter. If a directory is
     *            not accepted, it entire content is excluded from the zip. Must
     *            not be null.
     * @param exec receives progress messages and is checked for cancel
     *            requests. Optional, can be null.
     *
     * @return <code>true</code> if all files and dirs accepted by the filter
     *         are included, <code>false</code> if an error occurs reading a
     *         file in a directory, if a directory is unreadable.
     * @throws CanceledExecutionException if the operation was canceled through
     *             the <code>exec</code>
     * @throws IOException if an I/O error occurs when writing the zip file, or
     *             if two files or directories in the include list have the same
     *             (simple) name, or an element in the include list doesn't
     *             exist.
     * @since 3.2
     */
    public static boolean zipDir(final ZipOutputStream zout,
            final Collection<File> includeList, final String zipEntryPrefix, final ZipFileFilter filter,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {

        ExecutionMonitor execMon = exec;
        if (execMon == null) {
            execMon = new ExecutionMonitor();
        }
        // traverse the source to get a good progress estimate
        long size = 0;
        if (exec != null) {
            for (File f : includeList) {
                size += getFileSizeRec(f);
            }
        } else {
            size = Long.MAX_VALUE;
        }
        ZipWrapper zipper = new ZipWrapper(zout, zipEntryPrefix);

        // the read buffer, re-used for each file
        final byte[] buff = new byte[BUFF_SIZE];

        // false if unable to look into a sub dir or an I/O error occurs
        boolean complete = true;

        for (File f : includeList) {

            if (!filter.include(f)) {
                continue;
            }
            if (f.isFile()) {
                complete &= addZipEntry(buff, zipper, f, f.getName(), execMon, size);
            } else if (f.isDirectory()) {
                complete &= addOneDir(zipper, f, filter, execMon, size, buff);
            } else {
                throw new IOException("File " + f.getAbsolutePath()
                        + " not added to zip archive");
            }
        }

        return complete;
    }

    /**
     * recursively adds the file sizes.
     * @param fileOrDir to get the (contained) size for. Must not be null.
     * @return the sum of all file sizes
     */
    private static long getFileSizeRec(final File fileOrDir) {
        if (fileOrDir.isFile()) {
            return fileOrDir.length();
        } else if (fileOrDir.isDirectory()) {
            File[] children = fileOrDir.listFiles();
            if (children == null) {
                return 0;
            } else {
                long s = 0;
                for (File f : children) {
                    s += getFileSizeRec(f);
                }
                return s;
            }
        } else {
            return 0;
        }
    }

    private static boolean addOneDir(final ZipWrapper zout,
            final File rootDir, final ZipFileFilter filter,
            final ExecutionMonitor exec, final long origSize, final byte[] buff) throws IOException,
            CanceledExecutionException {

        // false if unable to look into a sub dir or an I/O error occurs
        boolean complete = true;

        Stack<File> dirs = new Stack<File>();

        String rootName = rootDir.getName();
        zout.putNextEntry(new ZipEntry(rootName + "/"));
        zout.closeEntry();

        // now, traverse the root dir
        dirs.push(rootDir);

        int rootEndIdx = rootDir.getAbsolutePath().length() + 1;
        while (!dirs.isEmpty()) {

            File d = dirs.pop();
            File[] ls = d.listFiles();
            if (ls == null) {
                // the dir was not accessible
                complete = false;
                continue;
            }
            for (File f : ls) {

                if (!filter.include(f)) {
                    continue;
                }

                String fName =
                        f.getAbsolutePath().substring(rootEndIdx)
                                .replace('\\', '/');
                String entryName = rootName + "/" + fName;

                if (f.isFile()) {

                    complete &= addZipEntry(buff, zout, f, entryName, exec, origSize);

                } else if (f.isDirectory()) {

                    zout.putNextEntry(new ZipEntry(entryName + "/"));
                    zout.closeEntry();
                    dirs.push(f);
                }
            }
        }

        return complete;

    }

    private static boolean addZipEntry(final byte[] buf,
            final ZipWrapper zout, final File f, final String entryName,
            final ExecutionMonitor exec, final long origSize) throws IOException,
            CanceledExecutionException {
        InputStream in = new FileInputStream(f);
        try {
            zout.putNextEntry(new ZipEntry(entryName));
            int read;
            while ((read = in.read(buf)) >= 0) {
                exec.checkCanceled();
                zout.write(buf, 0, read);
                exec.setProgress(zout.getBytesWritten() / (double)origSize);
            }
            exec.setProgress(zout.getBytesWritten() / (double)origSize, "Added file " + entryName);
        } catch (CanceledExecutionException cee) {
            throw cee;
        } catch (IOException ioe) {
            throw ioe;
        } catch (Throwable t) {
            LOGGER.debug(
                    "Error while adding file to zip archive ("
                            + f.getAbsolutePath() + ")", t);
            return false;
        } finally {
            zout.closeEntry();
            in.close();
        }
        return true;
    }

    /**
     * Passed to the
     * {@link FileUtil#zipDir(File, Collection, int, ZipFileFilter, ExecutionMonitor)}
     * method to exclude certain files from being archived and added to the zip
     * file.<br>
     * A default implementation accepting all files is
     * {@link #ZIP_INCLUDEALL_FILTER}
     *
     */
    public interface ZipFileFilter {
        /**
         * Called with each file in the to-be-zipped directory.
         *
         * @param f the to be added to the zip archive.
         * @return true, if the file should be added to the zip file, false, if
         *         it should be skipped/excluded.
         */
        boolean include(final File f);
    }

    /**
     * A filter that causes all files to be included in the zip archive.
     */
    public static final ZipFileFilter ZIP_INCLUDEALL_FILTER =
            new ZipFileFilter() {

                @Override
                public boolean include(final File f) {
                    return true;
                }
            };

    /**
     * Recursively packs all the the files and directories beneath the
     * <code>rootDir</code> into a zip file. The zip file contains the root
     * directory as the only entry in its root.
     *
     * @param zipFile the zip file that should be created. If it exists it will
     *            be overwritten.
     * @param rootDir the directory to pack.
     * @param compressionLevel the desired compression level, see
     *            {@link ZipOutputStream#setLevel(int)}
     *
     * @return <code>true</code> if all files and dirs are included,
     *         <code>false</code> if an error occurs reading a file or if a
     *         directory is unreadable.
     * @throws IOException if an I/O error occurs
     */
    public static boolean zipDir(final File zipFile, final File rootDir,
            final int compressionLevel) throws IOException {
        try {
            return zipDir(zipFile, rootDir, compressionLevel,
                    ZIP_INCLUDEALL_FILTER, null);
        } catch (CanceledExecutionException e) {
            // doesn't happen as we provide no execution monitor
            return false;
        }
    }

    /**
     * Recursively packs all the the files and directories beneath the
     * <code>rootDir</code> into a zip file. The zip file contains the root
     * directory as the only entry in its root.
     *
     * @param zipFile the zip file that should be created. If it exists it will
     *            be overwritten.
     * @param rootDir the directory to pack.
     * @param compressionLevel the desired compression level, see
     *            {@link ZipOutputStream#setLevel(int)}
     * @param filter each file (and dir) contained in the rootDir is only
     *            included in the zip archive if it is accepted by the filter.
     *            The rootDir is always included. Files from the
     *            <code>addRootFiles</code> list are also not filtered. Must not
     *            be null.
     * @param exec receives progress messages and is checked for cancel
     *            requests. Optional, can be null.
     *
     * @return <code>true</code> if all files and dirs are included,
     *         <code>false</code> if an error occurs reading a file, if a
     *         directory is unreadable, or a file in the
     *         <code>addRootFiles</code> list is a directory, not readable or
     *         doesn't exist.
     * @throws CanceledExecutionException if the operation was canceled through
     *             the <code>exec</code>
     * @throws IOException if an I/O error occurs
     */
    public static boolean zipDir(final File zipFile, final File rootDir,
            final int compressionLevel, final ZipFileFilter filter,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        return zipDir(zipFile, Collections.singletonList(rootDir),
                compressionLevel, filter, exec);
    }

    /**
     * Extracts the contents of the given ZIP file into the destination
     * directory.
     *
     * @param zipFile a ZIP file
     * @param destDir the destination directory, must already exist
     * @throws IOException if an I/O error occurs
     */
    public static void unzip(final File zipFile, final File destDir)
            throws IOException {
        if (!destDir.exists()) {
            throw new IOException("Destination directory does not exist: "
                    + destDir);
        }
        if (!destDir.isDirectory()) {
            throw new IOException("Destination is not a directory: " + destDir);
        }
        ZipInputStream in = new ZipInputStream(new FileInputStream(zipFile));
        unzip(in, destDir, 0);
    }

    /**
     * Stores the content of the zip stream in the specified directory. If a
     * strip level larger than zero is specified, it strips off that many path
     * segments from the zip entries. If the zip stream contains elements with
     * less path segments, they all end up directly in the specified dir.
     *
     * @param zipStream must contain a zip archive. Is unpacked an stored in the
     *            specified directory.
     * @param dir the destination directory the content of the zip stream is
     *            stored in
     * @param stripLevel the number of path segments (directory levels) striped
     *            off the file (and dir) names in the zip archive.
     * @throws IOException if it was not able to store the content
     */
    public static void unzip(final ZipInputStream zipStream, final File dir,
            final int stripLevel) throws IOException {
        ZipEntry e;
        byte[] buf = new byte[BUFF_SIZE];
        while ((e = zipStream.getNextEntry()) != null) {

            String name = e.getName().replace('\\', '/');
            name = stripOff(name, stripLevel);

            if (e.isDirectory()) {
                if (!name.isEmpty() && !name.equals("/")) {
                    File d = new File(dir, name);
                    if (!d.mkdirs() && !d.exists()) {
                        throw new IOException("Could not create directory '"
                                + d.getAbsolutePath() + "'.");
                    }
                }
            } else {
                File f = new File(dir, name);
                File parentDir = f.getParentFile();
                if (!parentDir.exists() && !parentDir.mkdirs()) {
                    throw new IOException("Could not create directory '"
                            + parentDir.getAbsolutePath() + "'.");
                }


                try (OutputStream out = new FileOutputStream(f)) {
                    int read;
                    while ((read = zipStream.read(buf)) >= 0) {
                        out.write(buf, 0, read);
                    }
                }
            }
        }
        zipStream.close();

    }

    /**
     * Strip off the path the specified amount of segments. Segment separator
     * must be a '/'.
     *
     * @param path the string from which the first <code>level</code> segments
     *            are stripped off
     * @param level the number of segments that are stripped off.
     * @return the specified <code>path</code> with the first <code>level</code>
     *         segments (that is directories) stripped off.
     */
    private static String stripOff(final String path, final int level) {
        if (path == null) {
            return null;
        }
        int l = level;
        if (!path.isEmpty() && path.charAt(0) == '/') {
            l++;
        }
        String[] segm = path.split("/", l + 1);
        return segm[segm.length - 1];
    }

    /**
     * Creates a temporary directory that is automatically deleted when the JVM
     * shuts down.
     *
     * @param prefix the prefix string to be used in generating the file's name
     *
     * @return an abstract pathname denoting a newly-created empty directory
     * @throws IOException if the directory could not be created
     */
    public static File createTempDir(final String prefix) throws IOException {
        return createTempDir(prefix, null, true);
    }

    /**
     * Creates a temporary directory that is automatically deleted when the JVM shuts down. If no root directory is
     * specified, the files are created in the temp dir associated with the workflow (set in the {@link WorkflowContext}
     * ), or - if that is null - in the global temp dir.
     *
     * @param prefix the prefix string to be used in generating the file's name
     * @param dir the directory in which the file is to be created, or <code>null</code> if the default temporary-file
     *            directory is to be used
     * @return an abstract pathname denoting a newly-created empty directory
     * @throws IOException if the directory could not be created
     */
    public static File createTempDir(final String prefix, final File dir) throws IOException {
        return createTempDir(prefix, dir, true);
    }

    /**
     * Creates a temporary directory that is automatically deleted when the JVM shuts down. If no root directory is
     * specified, the files are created in the temp dir associated with the workflow (set in the {@link WorkflowContext}
     * ), or - if that is null - in the global temp dir.
     *
     * @param prefix the prefix string to be used in generating the file's name
     * @param dir the directory in which the file is to be created, or <code>null</code> if the default temporary-file
     *            directory is to be used
     * @param deleteOnExit if <code>true</code>, the file is deleted when the JVM shuts down
     * @return an abstract pathname denoting a newly-created empty directory
     * @throws IOException if the directory could not be created
     */
    public static File createTempDir(final String prefix, final File dir, final boolean deleteOnExit)
            throws IOException {
        File rootDir = dir;
        if (rootDir == null) {
            rootDir = getTmpDir();
        }
        File tempDir;
        do {
            tempDir = new File(rootDir, prefix + (TEMP_DIR_UNIFIER.getAndIncrement()));
        } while (tempDir.exists());
        if (!tempDir.mkdirs()) {
            throw new IOException("Cannot create temporary directory '" + tempDir.getCanonicalPath() + "'.");
        }
        if (deleteOnExit) {
            TEMP_FILES.add(tempDir);
        }
        return tempDir;
    }

    /** Reads the current temp dir from the workflow context or returns the standard tmp dir, if not set. */
    private static File getTmpDir() {
        final File fallbackDir = KNIMEConstants.getKNIMETempPath().toFile();

        NodeContext nodeContext = NodeContext.getContext();
        if (nodeContext == null) {
            return fallbackDir;
        }

        WorkflowManager wfm = nodeContext.getWorkflowManager();
        if (wfm == null) {
            return fallbackDir;
        }

        WorkflowContext workflowContext = wfm.getContext();
        if (workflowContext == null) {
            return fallbackDir;
        }

        if (!workflowContext.getTempLocation().isDirectory()) {
            LOGGER.error("Temp folder \"" + workflowContext.getTempLocation().getAbsolutePath()
                + "\" does not exist (associated with node context \"" + nodeContext
                + "\") - using fallback temp folder (\"" + fallbackDir.getAbsolutePath() + "\"");
            return fallbackDir;
        } else {
            return workflowContext.getTempLocation();
        }
    }


    /**
     * Creates a temp file in the temp directory associated with the flow/node.
     *
     * @param prefix see {@link File#createTempFile(String, String)}
     * @param suffix see {@link File#createTempFile(String, String)}
     * @param rootDir the directory in which the file should be created
     * @param deleteOnExit if <code>true</code>, the file is deleted when the JVM shuts down
     * @return see {@link File#createTempFile(String, String)}
     * @throws IOException see {@link File#createTempFile(String, String)}
     * @since 2.9
     */
    public static File
        createTempFile(final String prefix, final String suffix, final File rootDir, final boolean deleteOnExit)
            throws IOException {
        File tmpFile = File.createTempFile(prefix, suffix, rootDir);
        if (deleteOnExit) {
            TEMP_FILES.add(tmpFile);
        }
        return tmpFile;
    }

    /**
     * Creates a temp file in the temp directory associated with the flow/node.
     *
     * @param prefix see {@link File#createTempFile(String, String)}
     * @param suffix see {@link File#createTempFile(String, String)}
     * @param deleteOnExit if true, the file is deleted when the JVM shuts down.
     * @return see {@link File#createTempFile(String, String)}
     * @throws IOException see {@link File#createTempFile(String, String)}
     * @since 2.8
     */
    public static File createTempFile(final String prefix, final String suffix, final boolean deleteOnExit)
        throws IOException {
        return createTempFile(prefix, suffix, getTmpDir(), deleteOnExit);
    }

    /**
     * Creates a temp file that is deleted when the JVM is shut down. See
     * {@link #createTempFile(String, String, boolean)}.
     *
     * @param prefix see {@link #createTempFile(String, String)}
     * @param suffix see {@link #createTempFile(String, String)}
     * @return the created temp file
     * @throws IOException see {@link #createTempFile(String, String)}
     * @since 2.8
     */
    public static File createTempFile(final String prefix, final String suffix) throws IOException {
        return createTempFile(prefix, suffix, true);
    }

    private static Pattern FORBIDDEN_WINDOWS_NAMES = Pattern
        .compile("^(?:(?:CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(?:\\.[^.]*)?|[ \\.])$");

    private static Pattern ILLEGAL_PATH_CHARACTERS = Pattern.compile("[^a-zA-Z0-9 ]");

    /** For some suggested name the returned string can be used to create a file. All unsupported characters are
     * replaced by '_'. Used when a workflow is saved to derive the folder name for a node. The returned string
     * may change between version (as we allow more special characters).
     * @param strWithWeirdChars Some string (not null, length &gt; 0)
     * @param maxLength If name should be truncated, specify some value &gt; 0 (&lt;= 0 means no truncation)
     * @return the name
     * @since 2.8
     */
    public static String getValidFileName(final String strWithWeirdChars, final int maxLength) {
        Matcher m = ILLEGAL_PATH_CHARACTERS.matcher(strWithWeirdChars);
        String result = m.replaceAll("_");
        if ((maxLength > 0) && (result.length() > maxLength)) {
            result = result.substring(0, maxLength).trim();
        }

        if (IS_WINDOWS) {
            m = FORBIDDEN_WINDOWS_NAMES.matcher(result);
            if (m.matches()) {
                result = "_" + result.substring(1);
            }
        }
        return result;
    }

    /**
     * Sets the permissions on a given file or directory. If a directory is
     * specified it recursively sets the permissions on it and all contained
     * files or directories.
     *
     * @param f a file or directory to change the permissions on (recursively).
     * @param readable if the readable-bit should be set, or <code>null</code>
     *            if its value shouldn't be changed
     * @param writable if the writable-bit should be set, or <code>null</code>
     *            if its value shouldn't be changed
     * @param executable if the executable-bit should be set, or
     *            <code>null</code> if its value shouldn't be changed
     * @param ownerOnly If <code>true</code>, the read permission applies only
     *            to the owner's read permission; otherwise, it applies to
     *            everybody. If the underlying file system cannot distinguish
     *            the owner's read permission from that of others, then the
     *            permission will apply to everybody, regardless of this value.
     * @return <code>true</code> if and only if the operation succeeded. The
     *         operation will fail if the user does not have permission to
     *         change the access permissions of this abstract pathname.
     */
    public static boolean chmod(final File f, final Boolean readable,
            final Boolean writable, final Boolean executable,
            final boolean ownerOnly) {
        boolean b = true;

        // if the x on a directory is removed recursion must happen first
        if ((executable != null) && !executable.booleanValue() && f.isDirectory()) {
            File[] dirList = f.listFiles(); // null if no read permissions
            if (dirList != null) {
                for (File entry : dirList) {
                    b &= chmod(entry, readable, writable, executable, ownerOnly);
                }
            }
        }

        if (readable != null) {
            b &= f.setReadable(readable, ownerOnly);
        }
        if (writable != null) {
            b &= f.setWritable(writable, ownerOnly);
        }
        if (executable != null) {
            b &= f.setExecutable(executable, ownerOnly);
        }

        // in all other cases do the recursion after changing the permissions
        if (executable == null || executable.booleanValue()) {
            if (f.isDirectory()) {
                File[] dirList = f.listFiles(); // null if no read permissions
                if (dirList != null) {
                    for (File entry : dirList) {
                        b &=
                                chmod(entry, readable, writable, executable,
                                        ownerOnly);
                    }
                }
            }
        }

        return b;
    }

    /**
     * Returns the file path from a 'file' or 'knime' URL. For the latter the file is tried to be resolved, whereby only
     * local files (e.g. workflow relative URLs) are successfully resolved. If the 'knime' URL points to a non-local
     * file, <code>null</code> is returned.
     *
     * @param fileUrl an URL with the 'file' or 'knime' protocol
     * @return the path or <code>null</code>
     * @throws IllegalArgumentException if the URL protocol is neither 'file' nor 'knime' or resolving a 'knime'-URL
     *             fails
     */
    public static File getFileFromURL(final URL fileUrl) {
        if (fileUrl.getProtocol().equalsIgnoreCase("file")) {
            File dataFile = new File(fileUrl.getPath());
            if (!dataFile.exists()) {
                try {
                    dataFile = new File(URLDecoder.decode(fileUrl.getPath(), "UTF-8"));
                } catch (UnsupportedEncodingException ex) {
                    // ignore it
                }
            }
            return dataFile;
        } else if (fileUrl.getProtocol().equalsIgnoreCase("knime")) {
            try {
                return ResolverUtil.resolveURItoLocalFile(fileUrl.toURI());
            } catch (IOException | URISyntaxException e) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }
        } else {
            throw new IllegalArgumentException("Not a file or knime URL: '" + fileUrl + "'");
        }
    }

    /**
     * Tries to resolve the given URL into a local path. The method can handle all URLs that are backed by local files
     * especially the 'knime' protocol. If the URL does not denote a local path, <code>null</code> is returned.
     *
     * @param url any URL
     * @return the path or <code>null</code>
     * @throws IOException if an I/O error occurs while resolving the URL
     * @throws URISyntaxException if the passed URL does not conform with RFC2396 for URIs
     * @since 2.11
     */
    public static Path resolveToPath(final URL url) throws IOException, URISyntaxException {
        if (looksLikeUNC(url)) {
            return Paths.get(url.toURI());
        } else {
            URL resolvedUrl = url.openConnection().getURL();

            if (resolvedUrl.getProtocol().equalsIgnoreCase("file")) {
                String pathString = resolvedUrl.getPath();
                if (pathString.contains(" ")) {
                    // fix non-encoded URL path
                    URL fixedUrl = new URL(url.toString().replace(" ", "%20"));
                    return Paths.get(fixedUrl.toURI());
                } else {
                    return Paths.get(resolvedUrl.toURI());
                }
            } else {
                return null;
            }
        }
    }

    /**
     * Returns whether the given URL is very likely a UNC URL. UNCs are only supported by Windows, so on all other
     * OS this will always return <code>false</code>.
     *
     * @param url an URL
     * @return <code>true</code> if it's likely a UNC URL, <code>false</code> otherwise
     * @since 2.11
     */
    public static boolean looksLikeUNC(final URL url) {
        // this looks like an UNC path, a real file URL does not have a host
        // Java does not handle UNC URLs correctly, see bug #5864
        return Platform.OS_WIN32.equals(Platform.getOS()) && "file".equalsIgnoreCase(url.getProtocol())
            && !StringUtils.isEmpty(url.getHost());
    }


    /**
     * Returns whether the given path is within a mounted network drive on Windows.
     *
     * @param path any path
     * @return <code>true</code> if the path is on a network drive, <code>false</code> otherwise or if the current OS is
     *         not Windows
     * @since 2.12
     */
    public static boolean isWindowsNetworkMount(final Path path) {
        if (!Platform.OS_WIN32.equals(Platform.getOS())) {
            return false;
        }

        Path root = path.getRoot();
        FileSystemView fsv = FileSystemView.getFileSystemView();
        return fsv.getSystemDisplayName(root.toFile()).contains("\\\\");
    }

    /**
     * Returns whether the given file-URL is within a mounted network drive on Windows.
     *
     * @param url any file-URL
     * @return <code>true</code> if the path is on a network drive, <code>false</code> otherwise or if the current OS is
     *         not Windows
     * @since 2.12
     */
    public static boolean isWindowsNetworkMount(final URL url) {
        if (!Platform.OS_WIN32.equals(Platform.getOS()) || !"file".equals(url.getProtocol())) {
            return false;
        }

        Path path = getFileFromURL(url).toPath();
        Path root = path.getRoot();
        FileSystemView fsv = FileSystemView.getFileSystemView();
        return fsv.getSystemDisplayName(root.toFile()).contains("\\\\");
    }


    /**
     * Returns whether the given path is very likely a UNC path. UNCs are only supported by Windows, so on all other
     * OS this will always return <code>false</code>.
     *
     * @param path a path
     * @return <code>true</code> if it's likely a UNC path, <code>false</code> otherwise
     * @since 2.11
     */
    public static boolean looksLikeUNC(final Path path) {
        // Java does not handle UNC URLs correctly, see bug #5864
        return Platform.OS_WIN32.equals(Platform.getOS()) && path.toString().startsWith("\\\\");
    }

    /**
     * Tries to convert the given path into a URL. Either the path is already a valid URL or it denotes a local file
     * and is then converted into a file URL.
     *
     * @param path any path
     * @return a URL
     * @throws MalformedURLException if the path is neither a URL nor a proper filesystem path
     * @throws InvalidPathException if the parameter looks like a file system path but is invalid
     * @since 2.11
     */
    public static URL toURL(final String path) throws MalformedURLException, InvalidPathException {
        if (path.isEmpty()) {
            throw new InvalidPathException("", "Empty path");
        }
        try {
            return new URL(path.replace(" ", "%20")); // replacement of spaces is a fallback only
        } catch (MalformedURLException ex) {
            return Paths.get(path).toAbsolutePath().toUri().toURL();
        }
    }

    /**
     * Tries to open an output URL connection to the given URL. If the URL is an http URL the given HTTP method is set
     * and the response code following the initial request is evaluated. Otherwise the connection is simply configured
     * for output.
     *
     * @param url any URL
     * @param httpMethod the HTTP method in case the url is http
     * @return a URLConnection configured for output
     * @throws IOException if an I/O error occurs
     * @since 2.11
     */
    public static URLConnection openOutputConnection(final URL url, final String httpMethod) throws IOException {
        URLConnection urlConnection = url.openConnection();

        if (urlConnection instanceof HttpURLConnection) {
            ((HttpURLConnection)urlConnection).setRequestMethod(httpMethod);
            ((HttpURLConnection)urlConnection).setChunkedStreamingMode(1 << 20);
            urlConnection = new HttpURLConnectionDecorator((HttpURLConnection)urlConnection);
        }

        urlConnection.setDoOutput(true);
        urlConnection.connect();

        return urlConnection;
    }

    /**
     * Open an input stream on the given URL using the default timeout for
     * connecting and reading. The timeout is taken from the system
     * property {@link KNIMEConstants#PROPERTY_URL_TIMEOUT} and defaults to
     * 1000 ms.
     *
     * @param url any URL
     * @return an input stream
     * @throws IOException if an I/O error occurs
     * @since 2.6
     */
    public static InputStream openStreamWithTimeout(final URL url)
            throws IOException {
      return openStreamWithTimeout(url, urlTimeout);
    }

    /**
     * Open an input stream on the given URL using the given timeout for
     * connecting and reading.
     *
     * @param url any URL
     * @param timeout the read/connection timeout
     * @return an input stream
     * @throws IOException if an I/O error occurs
     * @since 2.10
     */
    public static InputStream openStreamWithTimeout(final URL url, final int timeout)
            throws IOException {
        URLConnection conn = url.openConnection();
        conn.setConnectTimeout(timeout);
        conn.setReadTimeout(timeout);
        return conn.getInputStream();
    }

    /** Opens a buffered input stream for the location (file path or URL).
     * @param loc the location; can be both a file path or URL.
     * @return a buffered input stream.
     * @throws IOException Forwarded from file input stream or url.openStream.
     * @throws InvalidSettingsException If the argument is invalid or null.
     * @since 2.6 */
    public static InputStream openInputStream(final String loc)
        throws IOException, InvalidSettingsException {
        if (loc == null || loc.length() == 0) {
            throw new InvalidSettingsException("No location provided");
        }
        InputStream stream;
        try {
            URL url = new URL(loc);
            stream = FileUtil.openStreamWithTimeout(url);
        } catch (MalformedURLException mue) {
            File file = new File(loc);
            if (!file.exists()) {
                throw new InvalidSettingsException(
                        "No such file or URL: " + loc, mue);
            }
            stream = new FileInputStream(file);
        }
        return new BufferedInputStream(stream);
    }

    private static final class ZipWrapper extends ZipOutputStream {
        private long m_written = 0;
        private ZipOutputStream m_zipper;
        private String m_zipEntryPrefix;

        /**
         * Wraps a zip stream and counts the bytes written.
         * @param zipStream stream to wrap
         * @param zipEntryPrefix a possibly null prefix to prepend to all zip entry names
         */
        ZipWrapper(final ZipOutputStream zipStream, final String zipEntryPrefix) {
            super(new NullOutputStream());
            m_zipper = zipStream;
            m_zipEntryPrefix = StringUtils.defaultIfBlank(zipEntryPrefix, null);
        }
        /**
         * @return the number of original (uncompressed) bytes written to the stream.
         */
        public long getBytesWritten() {
            return m_written;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public synchronized void write(final byte[] b, final int off, final int len) throws IOException {
            m_written += len;
            m_zipper.write(b, off, len);
        }
        /**
         * @return
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            return m_zipper.hashCode();
        }
        /**
         * @param b
         * @throws IOException
         * @see java.io.FilterOutputStream#write(byte[])
         */
        @Override
        public void write(final byte[] b) throws IOException {
            m_written += b.length;
            m_zipper.write(b);
        }
        /**
         * @param comment
         * @see java.util.zip.ZipOutputStream#setComment(java.lang.String)
         */
        @Override
        public void setComment(final String comment) {
            m_zipper.setComment(comment);
        }
        /**
         * @param obj
         * @return
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(final Object obj) {
            return m_zipper.equals(obj);
        }
        /**
         * @param method
         * @see java.util.zip.ZipOutputStream#setMethod(int)
         */
        @Override
        public void setMethod(final int method) {
            m_zipper.setMethod(method);
        }
        /**
         * @param level
         * @see java.util.zip.ZipOutputStream#setLevel(int)
         */
        @Override
        public void setLevel(final int level) {
            m_zipper.setLevel(level);
        }
        /**
         * @param e
         * @throws IOException
         * @see java.util.zip.ZipOutputStream#putNextEntry(java.util.zip.ZipEntry)
         */
        @Override
        public void putNextEntry(final ZipEntry e) throws IOException {
            ZipEntry entry = e;
            if (m_zipEntryPrefix != null) {
                entry = new ZipEntry(m_zipEntryPrefix + e.getName());
            }
            m_zipper.putNextEntry(entry);
        }
        /**
         * @param b
         * @throws IOException
         * @see java.util.zip.DeflaterOutputStream#write(int)
         */
        @Override
        public void write(final int b) throws IOException {
            m_written += 1;
            m_zipper.write(b);
        }
        /**
         * @throws IOException
         * @see java.util.zip.ZipOutputStream#closeEntry()
         */
        @Override
        public void closeEntry() throws IOException {
            m_zipper.closeEntry();
        }
        /**
         * @throws IOException
         * @see java.util.zip.DeflaterOutputStream#flush()
         */
        @Override
        public void flush() throws IOException {
            m_zipper.flush();
        }
        /**
         * @return
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return m_zipper.toString();
        }
        /**
         * @throws IOException
         * @see java.util.zip.ZipOutputStream#finish()
         */
        @Override
        public void finish() throws IOException {
            m_zipper.finish();
        }
        /**
         * @throws IOException
         * @see java.util.zip.ZipOutputStream#close()
         */
        @Override
        public void close() throws IOException {
            m_zipper.close();
        }
    }

}
