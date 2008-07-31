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
 * --------------------------------------------------------------------- *
 * 
 * History
 *   06.05.2008 (gabriel): created
 */
package org.knime.base.node.preproc.filter.columnref;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * Factory to create a Reference Column Filter node.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class ColumnFilterRefNodeFactory 
        extends NodeFactory<ColumnFilterRefNodeModel> {

    /**
     * {@inheritDoc} 
     */
    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new ColumnFilterRefNodeDialogPane();
    }

    /**
     * {@inheritDoc} 
     */
    @Override
    public ColumnFilterRefNodeModel createNodeModel() {
        return new ColumnFilterRefNodeModel();
    }

    /**
     * {@inheritDoc} 
     */
    @Override
    public NodeView<ColumnFilterRefNodeModel> createNodeView(
            final int index, final ColumnFilterRefNodeModel model) {
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
