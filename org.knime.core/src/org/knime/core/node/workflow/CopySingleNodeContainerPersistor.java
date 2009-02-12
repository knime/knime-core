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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.Node;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
final class CopySingleNodeContainerPersistor implements
        SingleNodeContainerPersistor {
    
    private final SingleNodeContainer m_original;
    private final NodeSettingsRO m_sncSettings;
    private final Node m_node;
    private final boolean m_preserveDeletableFlag;
    
    /**
     * 
     */
    public CopySingleNodeContainerPersistor(
            final SingleNodeContainer original, 
            final boolean preserveDeletableFlag) {
        m_original = original;
        m_node = new Node(m_original.getNode());
        NodeSettings sncSettings = new NodeSettings("snc_settings");
        m_original.saveSNCSettings(sncSettings);
        m_sncSettings = sncSettings;
        m_preserveDeletableFlag = preserveDeletableFlag;
    }

    /** {@inheritDoc} */
    @Override
    public Node getNode() {
        return m_node;
    }

    /** {@inheritDoc} */
    @Override
    public List<ScopeObject> getScopeObjects() {
        ScopeObjectStack stack = m_original.getScopeObjectStack();
        List<ScopeObject> objs;
        if (stack != null) {
            objs = stack.getScopeObjectsOwnedBy(m_original.getID());
        } else {
            objs = Collections.emptyList();
        }
        List<ScopeObject> clones = new ArrayList<ScopeObject>(objs.size());
        for (ScopeObject o : objs) {
            clones.add(o.cloneAndUnsetOwner());
        }
        return clones;
    }

    /** {@inheritDoc} */
    @Override
    public NodeContainerMetaPersistor getMetaPersistor() {
        return new CopyNodeContainerMetaPersistor(
                m_original, m_preserveDeletableFlag);
    }

    /** {@inheritDoc} */
    @Override
    public NodeContainer getNodeContainer(final WorkflowManager parent,
            final NodeID id) {
        return new SingleNodeContainer(parent, id, this);
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
    public NodeSettingsRO getSNCSettings() {
        return m_sncSettings;
    }

    /** {@inheritDoc} */
    @Override
    public LoadResult preLoadNodeContainer(final ReferencedFile nodeFileRef,
            final NodeSettingsRO parentSettings) {
        return new LoadResult();
    }

}
