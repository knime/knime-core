/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 * -------------------------------------------------------------------
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
     * @param factory the nodeSetFactory needed to restore a instance of
     *      underlying node
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
