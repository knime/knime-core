/* 
 * ------------------------------------------------------------------
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
 * --------------------------------------------------------------------- *
 * 
 * History
 *   06.05.2008 (gabriel): created
 */
package org.knime.base.node.preproc.filter.rowref;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * Factory for the creation of a Reference Row Filter node.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class RowFilterRefNodeFactory 
        extends NodeFactory<RowFilterRefNodeModel> {

    /**
     * {@inheritDoc} 
     */
    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new RowFilterRefNodeDialogPane();
    }

    /**
     * {@inheritDoc} 
     */
    @Override
    public RowFilterRefNodeModel createNodeModel() {
        return new RowFilterRefNodeModel();
    }

    /**
     * {@inheritDoc} 
     */
    @Override
    public NodeView<RowFilterRefNodeModel> createNodeView(
            final int index, final RowFilterRefNodeModel model) {
        return null;
    }

    /**
     * {@inheritDoc} 
     */
    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    /**
     * {@inheritDoc} 
     */
    @Override
    protected boolean hasDialog() {
        return true;
    }

}
