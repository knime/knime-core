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
 * History
 *   Jun 9, 2008 (wiswedel): created
 */
package org.knime.core.node.workflow;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
 * @author Bernd Wiswedel, University of Konstanz
 */
class CopyWorkflowPersistor implements WorkflowPersistor {
    
    private final Map<Integer, NodeContainerPersistor> m_ncs;
    private final Set<ConnectionContainerTemplate> m_cons;
    private final UIInformation m_inportUIInfo;
    private final WorkflowPortTemplate[] m_inportTemplates;
    private final UIInformation m_outportUIInfo;
    private final WorkflowPortTemplate[] m_outportTemplates;
    private final String m_name;
    private final CopyNodeContainerMetaPersistor m_metaPersistor;
    private final HashMap<Integer, ContainerTable> m_tableRep;
    
    CopyWorkflowPersistor(final WorkflowManager original, 
            final HashMap<Integer, ContainerTable> tableRep,
            final boolean preserveDeletableFlags) {
        m_inportUIInfo = original.getInPortsBarUIInfo() != null 
            ? original.getInPortsBarUIInfo().clone() : null;
        m_outportUIInfo = original.getOutPortsBarUIInfo() != null 
            ? original.getOutPortsBarUIInfo().clone() : null;
        m_inportTemplates = new WorkflowPortTemplate[original.getNrInPorts()];
        m_outportTemplates = new WorkflowPortTemplate[original.getNrOutPorts()];
        for (int i = 0; i < m_inportTemplates.length; i++) {
            WorkflowInPort in = original.getInPort(i);
            m_inportTemplates[i] = 
                new WorkflowPortTemplate(i, in.getPortType());
        }
        for (int i = 0; i < m_outportTemplates.length; i++) {
            WorkflowOutPort in = original.getOutPort(i);
            m_outportTemplates[i] = 
                new WorkflowPortTemplate(i, in.getPortType());
        }
        m_name = original.getName();
        m_metaPersistor = new CopyNodeContainerMetaPersistor(
                original, preserveDeletableFlags);
        if (m_outportTemplates.length == 0 && m_inportTemplates.length == 0) {
            m_tableRep = new HashMap<Integer, ContainerTable>();
        } else {
            m_tableRep = tableRep;
        }
        m_ncs = new LinkedHashMap<Integer, NodeContainerPersistor>();
        m_cons = new LinkedHashSet<ConnectionContainerTemplate>();
        for (NodeContainer nc : original.getNodeContainers()) {
            m_ncs.put(nc.getID().getIndex(), nc.getCopyPersistor(
                    m_tableRep, true));
        }
        
        for (ConnectionContainer cc : original.getConnectionContainers()) {
            ConnectionContainerTemplate t = new ConnectionContainerTemplate(cc);
            m_cons.add(t);
        }
    }
    
    /** {@inheritDoc} */
    @Override
    public Set<ConnectionContainerTemplate> getConnectionSet() {
        return m_cons;
    }

    /** {@inheritDoc} */
    @Override
    public HashMap<Integer, ContainerTable> getGlobalTableRepository() {
        return m_tableRep;
    }

    /** {@inheritDoc} */
    @Override
    public UIInformation getInPortsBarUIInfo() {
        return m_inportUIInfo;
    }

    /** {@inheritDoc} */
    @Override
    public WorkflowPortTemplate[] getInPortTemplates() {
        return m_inportTemplates;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return m_name;
    }

    /** {@inheritDoc} */
    @Override
    public UIInformation getOutPortsBarUIInfo() {
        return m_outportUIInfo;
    }

    /** {@inheritDoc} */
    @Override
    public WorkflowPortTemplate[] getOutPortTemplates() {
        return m_outportTemplates;
    }
    
    /** {@inheritDoc} */
    @Override
    public NodeContainerMetaPersistor getMetaPersistor() {
        return m_metaPersistor;
    }

    /** {@inheritDoc} */
    @Override
    public String getLoadVersion() {
        return "Unknown";
    }

    /** {@inheritDoc} */
    @Override
    public Map<Integer, NodeContainerPersistor> getNodeLoaderMap() {
        return m_ncs;
    }

    /** {@inheritDoc} */
    @Override
    public NodeContainer getNodeContainer(final WorkflowManager parent,
            final NodeID id) {
        return parent.createSubWorkflow(this, id);
    }

    /** {@inheritDoc} */
    @Override
    public LoadResult loadNodeContainer(
            final Map<Integer, BufferedDataTable> tblRep,
            final ExecutionMonitor exec) throws InvalidSettingsException,
            CanceledExecutionException, IOException {
        return new LoadResult();
    }

    /** {@inheritDoc} */
    @Override
    public boolean needsResetAfterLoad() {
        return false;
    }
    
    /** {@inheritDoc} */
    @Override
    public boolean isDirtyAfterLoad() {
        return false;
    }
    
    /** {@inheritDoc} */
    @Override
    public boolean mustComplainIfStateDoesNotMatch() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public LoadResult preLoadNodeContainer(final ReferencedFile nodeFileRef,
            final NodeSettingsRO parentSettings) {
        return new LoadResult();
    }
    
    /** {@inheritDoc} */
    @Override
    public boolean mustWarnOnDataLoadError() {
        return true;
    }
    
}
