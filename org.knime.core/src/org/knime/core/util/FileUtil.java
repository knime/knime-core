/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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
    
    /** Don't let anybody instantiate this class. */
    private FileUtil() { }

    /** Copies a file. The implementation uses a temporary buffer of 8kB.
     * This method will report progress to the given execution monitor and
     * will also check a canceled status. The copy process will report
     * progress in the full range of the execution monitor. Consider to use
     * a sub-execution monitor if the copy process is only a small part of the
     * entire work.
     * @param file The file to copy.
     * @param destination The destination file, fully qualified 
     *         (do not provide a directory).
     * @param exec The execution monitor for progress information.
     * @throws CanceledExecutionException If canceled. The destination file
     *          will be deleted.
     * @throws IOException If that fail for any reason.
     */
    public static void copy(final File file, final File destination, 
            final ExecutionMonitor exec) 
            throws IOException, CanceledExecutionException {
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
            exec.setProgress(processed / (double) size);
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
    
    /** Copies a file. The implementation uses a temporary buffer of 8kB.
     * @param file The file to copy.
     * @param destination The destination file, fully qualified 
     *         (do not provide a directory).
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
    
    /** Deletes a given directory recursively. If the argument represents 
     * a file, the file will be deleted. If it represents a symbolic link, 
     * it won't follow the link but simply delete the link. Links contained
     * in any of the subdirectories are deleted without touching the link 
     * source. 
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
        if (!candir.equals(dir.getAbsoluteFile())) {
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
    
}
