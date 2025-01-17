/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 * Created on Apr 24, 2013 by wiswedel
 */
package org.knime.core.node.workflow;

import java.util.concurrent.atomic.AtomicInteger;

import org.knime.core.internal.ApplicationHealthInternal;
import org.knime.core.node.workflow.NodeContainer.State;

/** (Package scope) implementation of {@link NodeContainerState}.
 *
 * @author Bernd Wiswedel, Michael Berthold, KNIME AG, Zurich, Switzerland
 */
enum InternalNodeContainerState implements NodeContainerState {

    IDLE(ApplicationHealthInternal.NODESTATE_OTHER),
    CONFIGURED(ApplicationHealthInternal.NODESTATE_OTHER),
    UNCONFIGURED_MARKEDFOREXEC(ApplicationHealthInternal.NODESTATE_OTHER),
    CONFIGURED_MARKEDFOREXEC(ApplicationHealthInternal.NODESTATE_OTHER),
    EXECUTED_MARKEDFOREXEC(ApplicationHealthInternal.NODESTATE_OTHER),
    CONFIGURED_QUEUED(ApplicationHealthInternal.NODESTATE_OTHER),
    EXECUTED_QUEUED(ApplicationHealthInternal.NODESTATE_OTHER),
    PREEXECUTE(ApplicationHealthInternal.NODESTATE_OTHER),
    EXECUTING(ApplicationHealthInternal.NODESTATE_EXECUTING_COUNTER),
    EXECUTINGREMOTELY(ApplicationHealthInternal.NODESTATE_EXECUTING_COUNTER),
    POSTEXECUTE(ApplicationHealthInternal.NODESTATE_OTHER),
    EXECUTED(ApplicationHealthInternal.NODESTATE_EXECUTED_COUNTER);

    private final AtomicInteger m_nrOfNativeNodesInStateCount;

    InternalNodeContainerState(final AtomicInteger nrOfNativeNodesInStateCount) {
        m_nrOfNativeNodesInStateCount = nrOfNativeNodesInStateCount;
    }

    void decrementInStateCount() {
        m_nrOfNativeNodesInStateCount.decrementAndGet();
    }

    void incrementInStateCount() {
        m_nrOfNativeNodesInStateCount.incrementAndGet();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isIdle() {
        return IDLE.equals(this);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isConfigured() {
        return CONFIGURED.equals(this);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isExecuted() {
        return EXECUTED.equals(this);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isExecutionInProgress() {
        switch (this) {
            case IDLE:
            case EXECUTED:
            case CONFIGURED: return false;
            default: return true;
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isWaitingToBeExecuted() {
        switch (this) {
            case UNCONFIGURED_MARKEDFOREXEC:
            case CONFIGURED_MARKEDFOREXEC:
            case EXECUTED_MARKEDFOREXEC:
            case CONFIGURED_QUEUED:
            case EXECUTED_QUEUED:
                return true;
            default:
                return false;
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isHalted() {
        switch (this) {
            case CONFIGURED_QUEUED:
            case EXECUTED_QUEUED:
            case EXECUTING:
            case EXECUTINGREMOTELY:
            case POSTEXECUTE:
            case PREEXECUTE:
                return false;
            default:
                return true;
        }
    }

    /**
     * @return <code>true</code> if node state is resetable, i.e. it's neither executing nor queued for execution (but
     *         it can be marked for execution)
     */
    boolean isResetable() {
        switch (this) {
            case EXECUTED:
            case EXECUTED_MARKEDFOREXEC:
            case CONFIGURED_MARKEDFOREXEC:
            case UNCONFIGURED_MARKEDFOREXEC:
            case CONFIGURED:
                return true;
            default:
                return false;
        }
    }

    /**
     * @return <code>true</code> if executing or queued for execution (but _not_ marked for execution)
     */
    boolean isExecutingOrQueued() {
        switch (this) {
            case EXECUTING:
            case EXECUTINGREMOTELY:
            case CONFIGURED_QUEUED:
            case EXECUTED_QUEUED:
                return true;
            default:
                return false;
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isExecutingRemotely() {
        return EXECUTINGREMOTELY.equals(this);
    }

    /** Translates this new state to the old style state that got deprecated with 2.8.
     * @return State
     * @see NodeContainer#getState()
     */
    State mapToOldStyleState() {
        switch (this) {
            case IDLE: return State.IDLE;
            case CONFIGURED: return State.CONFIGURED;
            case UNCONFIGURED_MARKEDFOREXEC: return State.UNCONFIGURED_MARKEDFOREXEC;
            case CONFIGURED_MARKEDFOREXEC: return State.MARKEDFOREXEC;
            case EXECUTED_MARKEDFOREXEC: return State.MARKEDFOREXEC;
            case CONFIGURED_QUEUED: return State.QUEUED;
            case EXECUTED_QUEUED: return State.QUEUED;
            case PREEXECUTE: return State.PREEXECUTE;
            case EXECUTING: return State.EXECUTING;
            case EXECUTINGREMOTELY: return State.EXECUTINGREMOTELY;
            case POSTEXECUTE: return State.POSTEXECUTE;
            case EXECUTED: return State.EXECUTED;
            default: throw new IllegalStateException("Unmapped internal state: " + this);
        }

    }

}
