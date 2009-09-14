/*
 * ------------------------------------------------------------------ *
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
 * ---------------------------------------------------------------------
 *
 */
package org.knime.core.node.workflow;

import java.util.Arrays;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.workflow.NodeContainer.State;
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
    protected NodeExecutionJob(
            final NodeContainer nc, final PortObject[] data) {
        if (nc == null || data == null) {
            throw new NullPointerException("Args must not be null.");
        }
        if (Arrays.asList(data).contains(null)) {
            throw new NullPointerException("Array arg must not contain null.");
        }
        m_nc = nc;
        m_data = data;
    }

    /** {@inheritDoc} */
    @Override
    public final void run() {
        NodeContainerExecutionStatus status = null;
        if (!isReConnecting()) {
            try {
                // sets state PREEXECUTE
                m_nc.notifyParentPreExecuteStart();
                beforeExecute();
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
        try {
            if (status == null) {
                status = mainExecute();
            }
        } catch (Throwable throwable) {
            status = NodeContainerExecutionStatus.FAILURE;
            logError(throwable);
        }
        try {
            // sets state POSTEXECUTE
            m_nc.notifyParentPostExecuteStart();
            afterExecute();
        } catch (Throwable throwable) {
            status = NodeContainerExecutionStatus.FAILURE;
            logError(throwable);
        }
        try {
            // sets state EXECUTED
            m_nc.notifyParentExecuteFinished(status);
        } catch (Throwable throwable) {
            logError(throwable);
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

    /** Called right after the node has switched to the {@link State#PREEXECUTE}
     * state. Remote job executors will setup the execution environment in this
     * step and therefore overwrite this (empty) method. */
    protected void beforeExecute() {
        // possibly overwritten by sub-classes
    }
    
    /** Called when the main execution takes place. The node will be in the 
     * appropriate state ({@link State#EXECUTING} or 
     * {@link State#EXECUTINGREMOTELY} when this method is called.
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
