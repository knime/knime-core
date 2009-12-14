/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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

    // if not null this is a sub exec
    private FileReaderExecutionMonitor m_parent;

    private boolean m_interrupt = false;

    private boolean m_cancel = false;

    /**
     * Creates a new object with a default progress monitor.
     */
    FileReaderExecutionMonitor() {
        super();
        m_parent = null;
    }

    /**
     * Creates a new object with the specified {@link NodeProgressMonitor}.
     * @param progressMonitor the progress monitor to use.
     */
    FileReaderExecutionMonitor(final NodeProgressMonitor progressMonitor) {
        super(progressMonitor);
        m_parent = null;
    }

    /**
     * private constructor used for creating sub exec monitors.
     * @param progressMonitor the progress monitor
     * @param parent the exec monitor the new one is sub exec of
     */
    private FileReaderExecutionMonitor(
            final NodeProgressMonitor progressMonitor,
            final FileReaderExecutionMonitor parent) {
        super(progressMonitor);
        assert parent != null;
        m_parent = parent;
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
     * Checks the cancel flag. If this is a sub exec, it also checks with the
     * parent.
     * <p>
     * NOTE: if the cancellation was triggered in the progress monitor, this
     * method will return false. Always cancel execution through this execution
     * monitor.
     *
     * @return true, if execution was canceled.
     */
    public boolean wasCanceled() {
        if (m_parent != null && m_parent.wasCanceled()) {
            return true;
        }
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
     * Checks if the execution was interrupted. In sub execs it also checks
     * with the parent.
     *
     * @return true, if execution was interrupted
     */
    public boolean wasInterrupted() {
        if (m_parent != null && m_parent.wasInterrupted()) {
            return true;
        }
        return m_interrupt;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExecutionMonitor createSubProgress(final double maxProg) {
        ExecutionMonitor e = super.createSubProgress(maxProg);
        // steal the progress monitor and implant it into a FREM
        return new FileReaderExecutionMonitor(e.getProgressMonitor(), this);
    }
}
