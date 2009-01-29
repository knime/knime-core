/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 *   11.01.2007 (sieb): created
 */
package org.knime.workbench.editor2;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.swing.UIManager;

import org.eclipse.core.runtime.IProgressMonitor;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.DefaultNodeProgressMonitor;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;

/**
 * A runnable which is used by the {@link WorkflowEditor} to save a workflow
 * with a progress bar. NOTE: As the {@link UIManager} holds a reference to this
 * runnable an own class file is necessary sucht that all references to the
 * created workflow manager can be deleted, otherwise the manager can not be
 * deleted later and the memeory can not be freed.
 *
 * @author Christoph Sieb, University of Konstanz
 */
class SaveWorkflowRunnable extends PersistWorflowRunnable {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(SaveWorkflowRunnable.class);

    private WorkflowEditor m_editor;

    private File m_workflowFile;

    private StringBuffer m_exceptionMessage;

    private IProgressMonitor m_monitor;

    /**
     * Creates a runnable that saves the worfklow.
     *
     * @param editor
     *            the editor holding the workflow to save
     * @param workflowFile
     *            the file to save the workflow to
     * @param exceptionMessage
     *            holding an exception message
     * @param monitor
     *            the progress monitor to report the progress to
     */
    public SaveWorkflowRunnable(final WorkflowEditor editor,
            final File workflowFile, final StringBuffer exceptionMessage,
            final IProgressMonitor monitor) {
        m_editor = editor;
        m_workflowFile = workflowFile;
        m_exceptionMessage = exceptionMessage;
        m_monitor = monitor;
    }

    /**
     *
     */
    public void run(final IProgressMonitor pm) {
        CheckThread checkThread = null;
        try {
            // create progress monitor
            ProgressHandler progressHandler = new ProgressHandler(pm, m_editor
                    .getWorkflowManager().getNodeContainers().size(),
                    "Saving workflow... (can not be canceled)");
            final DefaultNodeProgressMonitor progressMonitor = new DefaultNodeProgressMonitor();
            progressMonitor.addProgressListener(progressHandler);

            // this task can not be canceled as the underlying
            // system does not support this yet (false)
            checkThread = new CheckThread(pm, progressMonitor, false);

            checkThread.start();

            // TODO: execution monitor? progress monitor??
            m_editor.getWorkflowManager().save(m_workflowFile.getParentFile(), 
                    new ExecutionMonitor(progressMonitor), true);
        } catch (FileNotFoundException fnfe) {
            LOGGER.fatal("File not found", fnfe);
            m_exceptionMessage.append("File access problems: "
                    + fnfe.getMessage());

            m_monitor.setCanceled(true);
        } catch (IOException ioe) {
            if (m_workflowFile.length() == 0) {
                LOGGER.info("New workflow created.");
            } else {
                LOGGER.error("Could not save workflow: "
                        + m_workflowFile.getName(), ioe);
                m_exceptionMessage.append("File access problems: "
                        + ioe.getMessage());
                m_monitor.setCanceled(true);
            }
        } catch (CanceledExecutionException cee) {
            LOGGER.info("Canceled saving worflow: " + m_workflowFile.getName());
            m_exceptionMessage.append("Saving workflow" + " was canceled.");
            m_monitor.setCanceled(true);
            /*
        } catch (Exception e) {
            // inform the user
            m_exceptionMessage.append("Execution in progress! "
                    + "The workflow could not be saved.");

            LOGGER.warn("Could not save workflow,"
                    + " node execution in progress");
            m_monitor.setCanceled(true);
            */
        } catch (Error e) {
            LOGGER.error("Could not save workflow", e);

            m_exceptionMessage.append("Could not save workflow: "
                    + e.getMessage());
            m_monitor.setCanceled(true);

        } catch (Exception e) {
            LOGGER.error("Could not save workflow", e);

            m_exceptionMessage.append("Could not save workflow: "
                    + e.getMessage());
            m_monitor.setCanceled(true);
        } finally {
            checkThread.finished();
            pm.subTask("Finished.");
            pm.done();
            m_editor = null;
            m_workflowFile = null;
            m_exceptionMessage = null;
            m_monitor = null;
        }

    }
}
