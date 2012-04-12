/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2008 - 2012
 * KNIME.com, Zurich, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.com
 * email: contact@knime.com
 * ---------------------------------------------------------------------
 *
 * History
 *   Mar 20, 2012 (morent): created
 */

package org.knime.workbench.repository.model;

import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSetFactory;

/**
 * A node template for dynamic nodes. Additional to the node factory class, a
 * {@link NodeSetFactory} class and the node factory ID is needed, to restore
 * the underlying node.
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 *
 */
public class DynamicNodeTemplate extends NodeTemplate {
    private NodeSetFactory m_nodeSetFactory;

    private final String m_factoryId;

    /**
     * Constructs a new DynamicNodeTemplate.
     *
     * @param nodeSetId The id of the NodeSetFactory.
     * @param factoryId The id of the NodeFactory
     * @param nodeSetFactory the NodeSetFactory that created this
     *            DynamicNodeTemplate
     */
    public DynamicNodeTemplate(final String nodeSetId, final String factoryId,
            final NodeSetFactory nodeSetFactory) {
        super(nodeSetId + "_" + factoryId);
        m_factoryId = factoryId;
        m_nodeSetFactory = nodeSetFactory;
    }

    /**
     * @param factory the nodeSetFactory needed to restore a instance of underlying node
     */
    public void setNodeSetFactory(final NodeSetFactory factory) {
        m_nodeSetFactory = factory;
    }

    /**
     * @return the class of the NodeSetFactory that created this
     *         DynamicNodeTemplate
     */
    public Class<? extends NodeSetFactory> getNodeSetFactoryClass() {
        return m_nodeSetFactory.getClass();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeFactory<? extends NodeModel> createFactoryInstance()
            throws Exception {
        NodeFactory<? extends NodeModel> instance =
                super.createFactoryInstance();
        instance.loadAdditionalFactorySettings(m_nodeSetFactory
                .getAdditionalSettings(m_factoryId));
        return instance;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof DynamicNodeTemplate)) {
            return false;
        }
        return getID().equals(((DynamicNodeTemplate)obj).getID());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return getID().hashCode();
    }
}
