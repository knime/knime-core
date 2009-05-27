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
 *   Feb 10, 2009 (wiswedel): created
 */
package org.knime.core.node.workflow;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.knime.core.data.container.ContainerTable;
import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;

/**
 * Persistor that is used when a workflow (a project) is loaded.
 * @author Bernd Wiswedel, University of Konstanz
 */
final class InsertWorkflowPersistor implements WorkflowPersistor {
    
    private final WorkflowPersistor m_wfmPersistor;
    
    /**
     * 
     */
    InsertWorkflowPersistor(final WorkflowPersistor wfmPersistor) {
        if (wfmPersistor == null) {
            throw new NullPointerException();
        }
        m_wfmPersistor = wfmPersistor;
    }

    /** {@inheritDoc} */
    @Override
    public Set<ConnectionContainerTemplate> getConnectionSet() {
        return Collections.emptySet();
    }

    /** {@inheritDoc} */
    @Override
    public HashMap<Integer, ContainerTable> getGlobalTableRepository() {
        throw new IllegalStateException("no table repository for root wfm");
    }

    /** {@inheritDoc} */
    @Override
    public WorkflowPortTemplate[] getInPortTemplates() {
        throw new IllegalStateException("no imports on root wfm");
    }

    /** {@inheritDoc} */
    @Override
    public UIInformation getInPortsBarUIInfo() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String getLoadVersion() {
        return m_wfmPersistor.getLoadVersion();
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        throw new IllegalStateException("can't set name on root");
    }

    /** {@inheritDoc} */
    @Override
    public Map<Integer, NodeContainerPersistor> getNodeLoaderMap() {
        return Collections.singletonMap(
                m_wfmPersistor.getMetaPersistor().getNodeIDSuffix(),
                (NodeContainerPersistor)m_wfmPersistor);
    }

    /** {@inheritDoc} */
    @Override
    public WorkflowPortTemplate[] getOutPortTemplates() {
        throw new IllegalStateException("no outports on root wfm");
    }

    /** {@inheritDoc} */
    @Override
    public UIInformation getOutPortsBarUIInfo() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean mustWarnOnDataLoadError() {
        return m_wfmPersistor.mustWarnOnDataLoadError();
    }

    /** {@inheritDoc} */
    @Override
    public NodeContainerMetaPersistor getMetaPersistor() {
        throw new IllegalStateException("no meta persistor for root wfm");
    }

    /** {@inheritDoc} */
    @Override
    public NodeContainer getNodeContainer(final WorkflowManager parent,
            final NodeID id) {
        throw new IllegalStateException("root has no parent, can't add node");
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDirtyAfterLoad() {
        return false;
    }
    
    /** {@inheritDoc} */
    @Override
    public boolean mustComplainIfStateDoesNotMatch() {
        throw new IllegalStateException("root has not meaningful state");
    }

    /** {@inheritDoc} */
    @Override
    public void loadNodeContainer(final Map<Integer, BufferedDataTable> tblRep,
            final ExecutionMonitor exec, final LoadResult loadResult) {
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
        throw new IllegalStateException("root can't be loaded");
    }

}
