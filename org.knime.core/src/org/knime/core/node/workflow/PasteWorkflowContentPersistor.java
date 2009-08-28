/* ------------------------------------------------------------------
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
 * History
 *   Jul 2, 2009 (wiswedel): created
 */
package org.knime.core.node.workflow;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.knime.core.data.container.ContainerTable;
import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;

/**
 * Persistor that is used to represent, for instance the clipboard content.
 * It contains a list of nodes the connections connecting them. It does not
 * support any of the "load" routines as it is an in-memory persistor. Instead
 * it throws exceptions when any of the load routines are called.
 * @author Bernd Wiswedel, University of Konstanz
 */
public class PasteWorkflowContentPersistor implements WorkflowPersistor {

    private final Set<ConnectionContainerTemplate> m_connectionSet;
    private final Map<Integer, NodeContainerPersistor> m_loaderMap;
    
    /** Create new persistor.
     * @param connectionSet A copy of connection clones.
     * @param loaderMap The loader map.
     */
    PasteWorkflowContentPersistor(
            final Map<Integer, NodeContainerPersistor> loaderMap,
            final Set<ConnectionContainerTemplate> connectionSet) {
        m_connectionSet = connectionSet;
        m_loaderMap = loaderMap;
    }

    /** {@inheritDoc} */
    @Override
    public Set<ConnectionContainerTemplate> getConnectionSet() {
        return m_connectionSet;
    }
    
    /** {@inheritDoc} */
    @Override
    public HashMap<Integer, ContainerTable> getGlobalTableRepository() {
        return new HashMap<Integer, ContainerTable>();
    }

    /** {@inheritDoc} */
    @Override
    public WorkflowPortTemplate[] getInPortTemplates() {
        return new WorkflowPortTemplate[0];
    }

    /** {@inheritDoc} */
    @Override
    public UIInformation getInPortsBarUIInfo() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String getLoadVersion() {
        throwUnsupportedOperationException();
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        throwUnsupportedOperationException();
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public Map<Integer, NodeContainerPersistor> getNodeLoaderMap() {
        return m_loaderMap;
    }

    /** {@inheritDoc} */
    @Override
    public WorkflowPortTemplate[] getOutPortTemplates() {
        return new WorkflowPortTemplate[0];
    }

    /** {@inheritDoc} */
    @Override
    public UIInformation getOutPortsBarUIInfo() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public List<FlowVariable> getWorkflowVariables() {
        throwUnsupportedOperationException();
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean mustWarnOnDataLoadError() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public NodeContainerMetaPersistor getMetaPersistor() {
        throwUnsupportedOperationException();
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public NodeContainer getNodeContainer(final WorkflowManager parent, 
            final NodeID id) {
        throwUnsupportedOperationException();
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDirtyAfterLoad() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void loadNodeContainer(final Map<Integer, BufferedDataTable> tblRep,
            final ExecutionMonitor exec, final LoadResult loadResult)
            throws InvalidSettingsException, CanceledExecutionException,
            IOException {
        throwUnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public boolean mustComplainIfStateDoesNotMatch() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean needsResetAfterLoad() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void preLoadNodeContainer(final ReferencedFile nodeFileRef,
            final NodeSettingsRO parentSettings, final LoadResult loadResult)
            throws InvalidSettingsException, IOException {
        throwUnsupportedOperationException();
    }
    
    /** Throws a new exception with a meaningful error message.
     * It is called when a non supported method is invoked.
     */
    protected void throwUnsupportedOperationException() {
        String methodName = "<unknown>";
        StackTraceElement[] callStack = Thread.currentThread().getStackTrace();
        // top most element is this method, at index [1] we find the calling
        // method name.
        if (callStack.length > 3) {
            methodName = callStack[2].getMethodName() + "\"";
        }
        throw new UnsupportedOperationException("Calling \"" + methodName 
                + "\" not allowed on \"" + getClass().getSimpleName() + "\"");
    }
}
