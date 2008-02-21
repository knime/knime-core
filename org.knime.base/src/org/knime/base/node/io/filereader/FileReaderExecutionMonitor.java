/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 * ---------------------------------------------------------------------
 *
 * History
 *   16.01.2008 (ohl): created
 */
package org.knime.base.node.io.filereader;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeProgressMonitor;

/**
 * Execution monitor used by the {@link FileAnalyzer}. Supports immediate
 * interruption of execution (in contrast to user cancellation, which finishes
 * fast - but not immediate).
 * <p>
 * IMPORTANT NOTE: In contrast to the default implementation cancel and
 * interrupt requests must be set with the execution monitor, not the progress
 * monitor!
 *
 * @author ohl, University of Konstanz
 */
public class FileReaderExecutionMonitor extends ExecutionMonitor {

    private boolean m_interrupt = false;

    private boolean m_cancel = false;

    /**
     * Creates a new object with a default progress monitor
     */
    FileReaderExecutionMonitor() {
        super();
    }

    /**
     * Creates a new object with the specified {@link NodeProgressMonitor}.
     * @param progressMonitor the progress monitor to use.
     */
    FileReaderExecutionMonitor(final NodeProgressMonitor progressMonitor) {
        super(progressMonitor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void checkCanceled() throws CanceledExecutionException {
        if (wasCanceled()) {
            throw new CanceledExecutionException("Execution canceled.");
        }
        // in case they did set cancel through the progress monitor.
        super.checkCanceled();
    }

    /**
     * Causes execution to finish fast (with a - more or less - usable result).
     */
    public void setExecuteCanceled() {
        m_cancel = true;
        // do it also in the super, to get somehow a compatible behavior
        super.getProgressMonitor().setExecuteCanceled();
    }

    /**
     * Checks the cancel flag.
     * <p>
     * NOTE: if the cancellation was triggered in the progress monitor, this
     * method will return false. Always cancel execution through this execution
     * monitor.
     *
     * @return true, if execution was canceled.
     */
    public boolean wasCanceled() {
        return m_cancel;
    }

    /**
     * Throws an exception if the execution was interrupted. Doesn't throw an
     * exception, if it was canceled.
     *
     * @throws InterruptedExecutionException if execution was interrupted
     */
    public void checkInterrupted() throws InterruptedExecutionException {
        if (wasInterrupted()) {
            throw new InterruptedExecutionException("Execution interrupted.");
        }
    }

    /**
     * Called when the execution should be interrupted immediately without
     * result.
     */
    public void setExecuteInterrupted() {
        m_interrupt = true;
    }

    /**
     * Checks if the execution was interrupted.
     *
     * @return true, if execution was interrupted
     */
    public boolean wasInterrupted() {
        return m_interrupt;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExecutionMonitor createSubProgress(final double maxProg) {
        ExecutionMonitor e = super.createSubProgress(maxProg);
        // steal the progress monitor and implant it into a FREM
        return new FileReaderExecutionMonitor(e.getProgressMonitor());
    }
}
