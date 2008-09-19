/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 * -------------------------------------------------------------------
 * 
 * History
 *   30.05.2005 (Florian Georg): created
 */
package org.knime.workbench.editor2;

import org.eclipse.gef.requests.CreationFactory;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.workbench.repository.model.NodeTemplate;

/**
 * This factory is able to create <code>NodeFactory</code> objects from the
 * <code>NodeTemplate</code> objects provided by the node repository. All new
 * model objects ("the nodes") that are added into a workflow are created
 * through this factory. Note that every <code>NodeTemplate</code> has a
 * factory for its own.
 * 
 * Note: As we can't add extra info here (at most we could have a
 * <code>Node</code>), this must be done later by reverse lookup of the
 * template from the <code>RepositoryManager</code>
 * 
 * @author Florian Georg, University of Konstanz
 */
public class NodeFromNodeTemplateCreationFactory implements CreationFactory {
    private Class<NodeFactory<? extends NodeModel>> m_factory;

    /**
     * New factory for the given template.
     * 
     * @param template The template from the repository.
     */
    public NodeFromNodeTemplateCreationFactory(final NodeTemplate template) {
        m_factory = template.getFactory();

    }

    /**
     * Creates a new <code>NodeFactory</code> instance.
     * 
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public Object getNewObject() {
        try {
            return (NodeFactory<? extends NodeModel>) 
                m_factory.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Can't instantiate NodeFactory "
                    + "from NodeTemplate", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Object getObjectType() {
        return NodeFactory.class;
    }
}
