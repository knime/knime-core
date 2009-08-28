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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CopyNodePersistor;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.Node;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.workflow.SingleNodeContainer.SingleNodeContainerSettings;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
final class CopySingleNodeContainerPersistor implements
        SingleNodeContainerPersistor {
    
    private final NodeFactory<NodeModel> m_nodeFactory;
    private final CopyNodePersistor m_nodePersistor;
    private final List<FlowObject> m_flowObjectList;
    private final SingleNodeContainerSettings m_sncSettings;
    private final CopyNodeContainerMetaPersistor m_metaPersistor;
    
    /**
     * 
     */
    public CopySingleNodeContainerPersistor(
            final SingleNodeContainer m_original, 
            final boolean preserveDeletableFlag) {
        Node originalNode = m_original.getNode();
        FlowObjectStack stack = m_original.getFlowObjectStack();
        List<FlowObject> objs;
        if (stack != null) {
            objs = stack.getFlowObjectsOwnedBy(m_original.getID());
        } else {
            objs = Collections.emptyList();
        }
        m_flowObjectList = new ArrayList<FlowObject>(objs.size());
        for (FlowObject o : objs) {
            m_flowObjectList.add(o.cloneAndUnsetOwner());
        }
        m_sncSettings = m_original.getSingleNodeContainerSettings().clone();
        m_nodeFactory = originalNode.getFactory();
        m_nodePersistor = originalNode.createCopyPersistor();
        m_metaPersistor = new CopyNodeContainerMetaPersistor(
                m_original, preserveDeletableFlag);

    }

    /** {@inheritDoc} */
    @Override
    public Node getNode() {
        Node node = new Node(m_nodeFactory);
        m_nodePersistor.loadInto(node);
        return node;
    }

    /** {@inheritDoc} */
    @Override
    public List<FlowObject> getFlowObjects() {
        if (m_flowObjectList.isEmpty()) {
            return Collections.emptyList();
        }
        List<FlowObject> clones = 
            new ArrayList<FlowObject>(m_flowObjectList.size());
        for (FlowObject o : m_flowObjectList) {
            clones.add(o.clone());
        }
        return clones;
    }

    /** {@inheritDoc} */
    @Override
    public NodeContainerMetaPersistor getMetaPersistor() {
        return m_metaPersistor;
    }

    /** {@inheritDoc} */
    @Override
    public NodeContainer getNodeContainer(final WorkflowManager parent,
            final NodeID id) {
        return new SingleNodeContainer(parent, id, this);
    }

    /** {@inheritDoc} */
    @Override
    public void loadNodeContainer(
            final Map<Integer, BufferedDataTable> tblRep,
            final ExecutionMonitor exec, final LoadResult loadResult) {
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
    public SingleNodeContainerSettings getSNCSettings() {
        return m_sncSettings;
    }

    /** {@inheritDoc} */
    @Override
    public void preLoadNodeContainer(final ReferencedFile nodeFileRef,
            final NodeSettingsRO parentSettings, final LoadResult loadResult) {
    }

}
