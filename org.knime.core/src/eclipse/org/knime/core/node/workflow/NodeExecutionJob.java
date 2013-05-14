/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 */
package org.knime.core.node.workflow;

import java.util.Arrays;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.Node;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortType;
import org.knime.core.node.util.StringFormat;
import org.knime.core.node.workflow.execresult.NodeContainerExecutionStatus;

/** Runnable that represents the execution of a node. This abstract class
 * defines the overall procedure of an execution including setup (e.g. to copy
 * the necessary data onto a cluster master), main execution and result
 * retrieval. It also reflects the current step of the execution in the node's
 * state.
 * @author Bernd Wiswedel, University of Konstanz
 * @author Peter Ohl, University of Konstanz
 */
public abstract class NodeExecutionJob implements Runnable {

    private final NodeLogger m_logger = NodeLogger.getLogger(getClass());

    /** Whether this job has been saved using the saveReconnectSettings
     * method in the corresponding {@link NodeExecutionJobManager}. This flag
     * can never be true if the job manager does not allow a disconnect. */
    private boolean m_isSavedForDisconnect = false;

    private final NodeContainer m_nc;
    private final PortObject[] m_data;


    /** Creates a new execution job for a given node. The array argument
     * represent the available input data..
     * @param nc The node that is to be executed.
     * @param data The input data of that node, must not be null,
     *           nor contain null elements.
     */
    protected NodeExecutionJob(final NodeContainer nc, final PortObject[] data) {
        if (nc == null || data == null) {
            throw new NullPointerException("Args must not be null.");
        }
        for (int i = 0; i < data.length; i++) {
            PortType type = nc.getInPort(i).getPortType();
            if (data[i] == null && !type.isOptional()) {
                throw new NullPointerException("Array arg must not contain null for non-optional ports");
            }
        }
        m_nc = nc;
        m_data = data;
    }

    /** {@inheritDoc} */
    @Override
    public final void run() {
        NodeContainerExecutionStatus status = null;
        // handle inactive branches -- do not delegate to custom job
        // manager (the node will just return inactive branch objects)
        boolean executeInactive = false;
        if (m_nc instanceof SingleNodeContainer) {
            SingleNodeContainer snc = (SingleNodeContainer)m_nc;
            if (!snc.isInactiveBranchConsumer() && Node.containsInactiveObjects(getPortObjects())) {
                executeInactive = true;
            }
        }

        if (!isReConnecting()) {
            try {
                // sets state PREEXECUTE
                if (!m_nc.notifyParentPreExecuteStart()) {
                    // node was canceled, omit any subsequent state transitions
                    return;
                }
                if (!executeInactive) {
                    beforeExecute();
                }
            } catch (Throwable throwable) {
                logError(throwable);
                status = NodeContainerExecutionStatus.FAILURE;
            }
            try {
                // sets state EXECUTING
                m_nc.notifyParentExecuteStart();
            } catch (IllegalFlowObjectStackException e) {
                status = NodeContainerExecutionStatus.FAILURE;
            } catch (Throwable throwable) {
                status = NodeContainerExecutionStatus.FAILURE;
                logError(throwable);
            }
        }
        // check thread cancelation
        if (status == null) {
            if (Thread.interrupted()) {
                status = NodeContainerExecutionStatus.FAILURE;
            } else {
                try {
                    m_nc.getProgressMonitor().checkCanceled();
                } catch (CanceledExecutionException cee) {
                    status = NodeContainerExecutionStatus.FAILURE;
                }
            }
        }
        try {
            if (status == null) {
                NodeLogger.getLogger(m_nc.getClass());
                // start message and keep start time
                final long time = System.currentTimeMillis();
                m_logger.debug(m_nc.getNameWithID() + " Start execute");
                if (executeInactive) {
                    SingleNodeContainer snc = (SingleNodeContainer)m_nc;
                    status = snc.performExecuteNode(getPortObjects());
                } else {
                    status = mainExecute();
                }
                if (status != null && status.isSuccess()) {
                    String elapsed = StringFormat.formatElapsedTime(System.currentTimeMillis() - time);
                    m_logger.info(m_nc.getNameWithID() + " End execute (" + elapsed + ")");
                }
            }
        } catch (Throwable throwable) {
            status = NodeContainerExecutionStatus.FAILURE;
            logError(throwable);
        }
        try {
            // sets state POSTEXECUTE
            m_nc.notifyParentPostExecuteStart();
            if (!executeInactive) {
                afterExecute();
            }
        } catch (Throwable throwable) {
            status = NodeContainerExecutionStatus.FAILURE;
            logError(throwable);
        }
        try {
            // sets state EXECUTED
            m_nc.notifyParentExecuteFinished(status);
        } catch (Exception e) {
            logError(e);
        }
    }

    private void logError(final Throwable e) {
        m_logger.error("Caught \"" + e.getClass().getSimpleName() + "\": "
                + e.getMessage(), e);
    }

    /** Whether this job has been disconnected (from a cluster execution, e.g.)
     * and is now resuming the execution. If so, the execution procedure will
     * skip the execution setup. This property will be false for locally
     * executed nodes.
     * @return The above described property.
     */
    protected abstract boolean isReConnecting();

    /** Called right after the node has switched to the {@link InternalNodeContainerState#PREEXECUTE}
     * state. Remote job executors will setup the execution environment in this
     * step and therefore overwrite this (empty) method. */
    protected void beforeExecute() {
        // possibly overwritten by sub-classes
    }

    /** Called when the main execution takes place. The node will be in the
     * appropriate state ({@link InternalNodeContainerState#EXECUTING} or
     * {@link InternalNodeContainerState#EXECUTINGREMOTELY} when this method is called.
     * @return Whether the execution was successful.
     */
    protected abstract NodeContainerExecutionStatus mainExecute();

    /** Called to finalize the execution. For instance, remote executors will
     * copy the result data onto the local machine. This method is called no
     * matter if the main execution was successful or not.*/
    protected void afterExecute() {
        // possibly overwritten by sub-classes
    }

    /** Called when the execution is to be canceled.
     * @return Whether the cancelation was successful.
     * @see java.util.concurrent.Future#cancel(boolean) */
    protected abstract boolean cancel();

    /** Access method for the input port objects.
     * @return Input port objects as passed into constructor.
     */
    protected final PortObject[] getPortObjects() {
        return m_data;
    }

    /** Get the input objects, excluding the mandatory flow variable input
     * port (added in v2.2.0). If this job represents the execution of a
     * workflow manager (possibly for remote execution), this method
     * returns the whole input object array. (Meta nodes do not have an
     * flow variable input)
     * @return The input objects, excluding the flow variable port.
     */
    protected final PortObject[] getPortObjectsExcludeFlowVariablePort() {
        PortObject[] data = getPortObjects();
        if (m_nc instanceof WorkflowManager) {
            return data;
        } else {
            return Arrays.copyOfRange(data, 1, data.length);
        }
    }

    /** Access method for the accompanying node container.
     * @return NodeContainer as passed into constructor.
     */
    protected final NodeContainer getNodeContainer() {
        return m_nc;
    }

    /**
     * @param isSavedForDisconnect the isSavedForDisconnect to set
     */
    void setSavedForDisconnect(final boolean isSavedForDisconnect) {
        m_isSavedForDisconnect = isSavedForDisconnect;
    }

    /**
     * @return the isSavedForDisconnect
     */
    boolean isSavedForDisconnect() {
        return m_isSavedForDisconnect;
    }

}
