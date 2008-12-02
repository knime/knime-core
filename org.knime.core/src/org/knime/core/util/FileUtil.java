/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 *
 * History
 *   Jun 9, 2006 (wiswedel): created
 */
package org.knime.core.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;

/**
 * Utitlity class to do some basic file handling that is not available through
 * java API. This includes copying of files and deleting entire directories.
 * These methods are mainly used for the load/save of the workflow.
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class FileUtil {
    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(FileUtil.class);

    private static final List<File> TEMP_FILES;

    private static final boolean IS_WINDOWS =
            System.getProperty("os.name").startsWith("Windows");

    static {
        TEMP_FILES = new ArrayList<File>();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                for (File f : TEMP_FILES) {
                    if (!f.exists()) {
                        continue;
                    }

                    if (f.isFile()) {
                        f.delete();
                    } else if (f.isDirectory()) {
                        try {
                            deleteRecursively(f);
                        } catch (Exception ex) {
                            LOGGER.error(ex.getMessage(), ex);
                        }
                    }
                }
            }
        });
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
        final int bufSize = 8192;
        final long size = file.length();
        byte[] cache = new byte[bufSize];
        OutputStream copyOutStream = new FileOutputStream(destination);
        InputStream copyInStream = new FileInputStream(file);
        int read;
        long processed = 0;
        CanceledExecutionException cee = null;
        exec.setMessage("Copying \"" + file.getName() + "\"");
        while ((read = copyInStream.read(cache, 0, bufSize)) > 0) {
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
        copyOutStream.close();
        copyInStream.close();
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
     * Copies the bytes as read from <code>input</code> to the output stream
     * <code>destination</code>. Neither <code>input</code> nor
     * <code>destination</code> get closed at the end!
     *
     * @param input To read from
     * @param destination To write to
     * @throws IOException If that fails for any reason.
     * @throws NullPointerException If any argument is <code>null</code>.
     */
    public static void copy(final InputStream input,
            final OutputStream destination) throws IOException {
        final int bufSize = 8192;
        byte[] cache = new byte[bufSize];
        int read;
        while ((read = input.read(cache, 0, bufSize)) > 0) {
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
        final int bufSize = 8192;
        char[] cache = new char[bufSize];
        int read;
        while ((read = source.read(cache, 0, bufSize)) > 0) {
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
        // to see if this directory is actually a symbolic link to a directory,
        // we want to get its canonical path - that is, we follow the link to
        // the file it's actually linked to
        File candir;
        try {
            candir = dir.getCanonicalFile();
        } catch (IOException e) {
            return false;
        }

        // a symbolic link has a different canonical path than its actual path,
        // unless it's a link to itself
        if (!IS_WINDOWS && !candir.equals(dir.getAbsoluteFile())) {
            // this file is a symbolic link, and there's no reason for us to
            // follow it, because then we might be deleting something outside of
            // the directory we were told to delete

            // we delete the link here and return
            return dir.delete();
        }

        // now we go through all of the files and subdirectories in the
        // directory and delete them one by one
        File[] files = candir.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                File file = files[i];

                // in case this directory is actually a symbolic link, or it's
                // empty, we want to try to delete the link before we try
                // anything
                boolean deleted = file.delete();
                if (!deleted) {
                    // deleting the file failed, so maybe it's a non-empty
                    // directory
                    if (file.isDirectory()) {
                        deleteRecursively(file);
                    }
                    // otherwise, there's nothing else we can do
                }
            }
        }
        // now that we tried to clear the directory out, we can try to delete it
        // again
        return dir.delete();
    } // deleteRecursively(File)

    /**
     * Recursively packs all the the files and directories beneath the
     * <code>rootDir</code> into a zip file. The zip file will contain the
     * root directory as the only entry in its root.
     *
     * @param zipFile the zip file that should be created
     * @param rootDir the root directory
     * @param compressionLevel the desired compression level, see
     *            {@link ZipOutputStream#setLevel(int)}
     * @throws IOException if an I/O error occurs
     */
    public static void zipDir(final File zipFile, final File rootDir,
            final int compressionLevel) throws IOException {
        if (!rootDir.isDirectory()) {
            throw new IllegalArgumentException("The given root directory '"
                    + rootDir.getAbsolutePath() + "' is not a directory.");
        }

        ZipOutputStream zout =
                new ZipOutputStream(new BufferedOutputStream(
                        new FileOutputStream(zipFile)));
        zout.setLevel(compressionLevel);

        Stack<File> dirs = new Stack<File>();
        dirs.push(rootDir);
        zout.putNextEntry(new ZipEntry(rootDir.getName() + "/"));
        zout.closeEntry();

        byte[] buf = new byte[4096];
        while (!dirs.isEmpty()) {
            File d = dirs.pop();
            for (File f : d.listFiles()) {
                final String name =
                        rootDir.getName()
                                + "/"
                                + f.getCanonicalPath()
                                        .substring(
                                                rootDir.getCanonicalPath()
                                                        .length() + 1);

                if (f.isFile()) {
                    InputStream in = new FileInputStream(f);
                    zout.putNextEntry(new ZipEntry(name));
                    int read;
                    while ((read = in.read(buf)) >= 0) {
                        zout.write(buf, 0, read);
                    }
                    zout.closeEntry();
                    in.close();
                } else if (f.isDirectory()) {
                    zout.putNextEntry(new ZipEntry(name + "/"));
                    zout.closeEntry();
                    dirs.push(f);
                }
            }
        }

        zout.close();
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
            throw new IOException("Destination directory does no exist: "
                    + destDir);
        }
        if (!destDir.isDirectory()) {
            throw new IOException("Destination is not a directory: " + destDir);
        }
        ZipInputStream in = new ZipInputStream(new FileInputStream(zipFile));

        ZipEntry e;
        byte[] buf = new byte[4096];
        while ((e = in.getNextEntry()) != null) {
            if (e.isDirectory()) {
                File d = new File(destDir, e.getName());
                if (!d.mkdirs()) {
                    throw new IOException("Could not create directory '"
                            + d.getAbsolutePath() + "'.");
                }
            } else {
                File f = new File(destDir, e.getName());
                File parentDir = f.getParentFile();
                if (!parentDir.exists()) {
                    if (!parentDir.mkdirs()) {
                        throw new IOException("Could not create directory '"
                                + parentDir.getAbsolutePath() + "'.");
                    }
                }

                FileOutputStream out = new FileOutputStream(f);
                int read;
                while ((read = in.read(buf)) >= 0) {
                    out.write(buf, 0, read);
                }
                out.close();
            }
        }
        in.close();
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
        return createTempDir(prefix, null);
    }

    /**
     * Creates a temporary directory that is automatically deleted when the JVM
     * shuts down.
     *
     * @param prefix the prefix string to be used in generating the file's name
     * @param dir the directory in which the file is to be created, or
     *            <code>null</code> if the default temporary-file directory is
     *            to be used
     * @return an abstract pathname denoting a newly-created empty directory
     * @throws IOException if the directory could not be created
     */
    public static synchronized File createTempDir(final String prefix,
            final File dir) throws IOException {
        File rootDir =
                (dir == null) ? new File(System.getProperty("java.io.tmpdir"))
                        : dir;
        File tempDir;
        do {
            tempDir =
                    new File(rootDir, prefix + System.currentTimeMillis()
                            + TEMP_FILES.size());
        } while (tempDir.exists());
        if (!tempDir.mkdirs()) {
            throw new IOException("Cannot create temporary directory '"
                    + tempDir.getCanonicalPath() + "'.");
        }
        TEMP_FILES.add(tempDir);
        return tempDir;
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
     * @param ownerOnly If <code>true</code>, the read permission applies
     *            only to the owner's read permission; otherwise, it applies to
     *            everybody. If the underlying file system can not distinguish
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

        if (readable != null) {
            b &= f.setReadable(readable, ownerOnly);
        }
        if (writable != null) {
            b &= f.setWritable(writable, ownerOnly);
        }
        if (executable != null) {
            b &= f.setExecutable(executable, ownerOnly);
        }
        if (f.isDirectory()) {
            for (File entry : f.listFiles()) {
                b &= chmod(entry, readable, writable, executable, ownerOnly);
            }
        }

        return b;
    }
}
