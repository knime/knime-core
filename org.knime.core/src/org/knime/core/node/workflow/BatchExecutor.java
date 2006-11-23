/* Created on Nov 23, 2006 12:07:08 PM by thor
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
 * ------------------------------------------------------------------- * 
 */
package org.knime.core.node.workflow;

import java.io.File;
import java.io.IOException;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.DefaultNodeProgressMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.util.FileUtil;

/**
 * Simple utility class that takes a workflow, either in a directory or zipped
 * into a single file, executes it and saves the results in the end. If the
 * input was a ZIP file the workflow is zipped back into a file.
 * 
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public final class BatchExecutor {
    private BatchExecutor() { /**/ }
    
    /**
     * Main method.
     * 
     * @param args a workflow directory or a zip input and output file
     * @throws IOException bum
     * @throws WorkflowException crash
     * @throws WorkflowInExecutionException bomb
     * @throws CanceledExecutionException ploing
     * @throws InvalidSettingsException shit
     */
    public static void main(final String[] args) throws IOException,
            InvalidSettingsException, CanceledExecutionException,
            WorkflowInExecutionException, WorkflowException {
        long t = System.currentTimeMillis();
        if (args.length < 1) {
            System.err.println("Usage: " + BatchExecutor.class
                    + " (workflowDir|(workflowZip resultZip))");
            System.exit(1);
        }
        File f = new File(args[0]);
        if (!f.exists()) {
            System.err.println("File '" + f.getPath() + "' does not exist.");
            System.exit(1);
        }

        if (f.isFile()) {
            File dir = FileUtil.createTempDir("BatchExecutor");
            FileUtil.unzip(f, dir);
            f = dir;
        }

        final WorkflowManager wfm = new WorkflowManager();
        File workflowFile = new File(f, WorkflowManager.WORKFLOW_FILE);
        wfm.load(workflowFile, new DefaultNodeProgressMonitor());
        wfm.executeAll(true);
        wfm.save(workflowFile, new DefaultNodeProgressMonitor());

        if (args.length == 2) {
            File dest = new File(args[1]);
            FileUtil.zipDir(dest, f, 9);
        }
        System.out.println("Finished in " + (System.currentTimeMillis() - t)
                + "ms");
    }
}
