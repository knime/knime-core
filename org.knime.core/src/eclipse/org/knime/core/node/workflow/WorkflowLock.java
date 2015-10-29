/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Oct 27, 2015 (wiswedel): created
 */
package org.knime.core.node.workflow;

import java.util.concurrent.locks.ReentrantLock;

import org.knime.core.node.util.CheckUtils;

/**
 * A lock instance associated with a workflow or subnode. It serves two purposes: preventing concurrent access to the
 * internals of a workflow (like removing connections while a reset is propagating) and deferring state updates of
 * the meta node/workflow/subnode to when the lock is finally released.
 *
 * <p/>
 * This class is wrapping a {@link ReentrantLock}. Workflows and contained meta/sub nodes share the same
 * {@link ReentrantLock} but are still distinct {@link WorkflowLock} instances to properly isolate the status
 * update request.
 *
 * @noreference This class is not intended to be referenced by clients.
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public final class WorkflowLock implements AutoCloseable {

    private final ReentrantLock m_reentrantLock;
    private final WorkflowManager m_wfm;

    private boolean m_checkForNodeStateChanges;
    private boolean m_propagateChanges;
    private int m_holdCountThisInstance;

    /** An instance for a workflow project.
     * @param wfm The non-null workflow.
     */
    WorkflowLock(final WorkflowManager wfm) {
        m_wfm = CheckUtils.checkArgumentNotNull(wfm);
        m_reentrantLock = new ReentrantLock();
    }

    /** An instance for a contained meta node or sub node.
     * @param wfm The meta node instance itself (or wfm within a {@link SubNodeContainer}).
     * @param parent The parent workflow.
     */
    WorkflowLock(final WorkflowManager wfm, final NodeContainerParent parent) {
        m_wfm = CheckUtils.checkArgumentNotNull(wfm);
        m_reentrantLock = parent.getReentrantLockInstance();
    }

    /** @return the {@link ReentrantLock} - rarely used except for wait methods that need to sleep on the lock. */
    ReentrantLock getReentrantLock() {
        return m_reentrantLock;
    }

    /** @return whether calling thread holds the lock.
     * @see java.util.concurrent.locks.ReentrantLock#isHeldByCurrentThread()
     */
    boolean isHeldByCurrentThread() {
        return m_reentrantLock.isHeldByCurrentThread();
    }

    /** Acquires the lock (and increments hold count) and returns this instance. See {@link NodeContainerParent#lock()}.
     * @return this.
     */
    public WorkflowLock lock() {
        m_reentrantLock.lock();
        m_holdCountThisInstance += 1;
        return this;
    }

    /** Unlocks as per {@link ReentrantLock#unlock()}, possibly causing a state update check and notification on the
     * workflow when this is the last unlock. */
    public void unlock() {
        if (m_reentrantLock.isHeldByCurrentThread()) {
            CheckUtils.checkState(m_holdCountThisInstance > 0,
                "ReentrantLock is held by current thread but not associated with this workflow lock");
        }
        m_holdCountThisInstance -= 1;
        try {
            if (m_holdCountThisInstance == 0 && m_checkForNodeStateChanges) {
                boolean propagateChanges = m_propagateChanges;
                m_propagateChanges = false;
                m_checkForNodeStateChanges = false;
                m_wfm.checkForNodeStateChanges(propagateChanges);
            }
        } finally {
            m_reentrantLock.unlock();
        }
    }

    /** Queues a state update check and notification when the lock is finally released by the calling thread.
     * This method is to be called when the lock is hold by the calling thread.
     * @param propagateChanges Whether to propagate state changes to the parent workflow (if any)
     * @see WorkflowManager#checkForNodeStateChanges(boolean)
     */
    void queueCheckForNodeStateChangeNotification(final boolean propagateChanges) {
        assert m_reentrantLock.isHeldByCurrentThread() : "Can't queue state check - lock not held by current thread";
        m_checkForNodeStateChanges = true;
        if (propagateChanges) {
            m_propagateChanges = true;
        }
    }

    /** {@linkplain #unlock() Unlocks} the lock.
     * <p/>
     * {@inheritDoc} */
    @Override
    public void close() {
        unlock();
    }

}
