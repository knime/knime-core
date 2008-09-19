/*
 * ------------------------------------------------------------------
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
 *   30.10.2005 (mb): created
 */
package org.knime.base.node.io.portobject;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortType;

/** Node that connects to arbitrary model ports and reads the model as
 * ModelContent from a chosen file.
 * 
 * @author M. Berthold, University of Konstanz
 */
public class PortObjectReaderNodeFactory 
        extends NodeFactory<PortObjectReaderNodeModel> {

    private final PortType m_type;
    
    /** @param type Type of output. */
    public PortObjectReaderNodeFactory(final PortType type) {
        if (type == null) {
            throw new NullPointerException();
        }
        m_type = type;
    }
    
    /** Uses generic output port. */
    public PortObjectReaderNodeFactory() {
        this(new PortType(PortObject.class));
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public PortObjectReaderNodeModel createNodeModel() {
        return new PortObjectReaderNodeModel(m_type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<PortObjectReaderNodeModel> createNodeView(
            final int viewIndex, final PortObjectReaderNodeModel nodeModel) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDialog() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return new PortObjectReaderNodeDialog();
    }

}
