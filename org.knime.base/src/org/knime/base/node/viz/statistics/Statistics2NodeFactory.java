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
 *   18.04.2005 (cebron): created
 */
package org.knime.base.node.viz.statistics;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * Factory class for the Statistics Node.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class Statistics2NodeFactory extends NodeFactory<Statistics2NodeModel> {
    
    /**
     * Empty default constructor.
     */
    public Statistics2NodeFactory() {
        super();
    }
    
    /**
     * This node has no dialog.
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return new Statistics2NodeDialogPane();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Statistics2NodeModel createNodeModel() {
        return new Statistics2NodeModel();
    }
    
    /**
     * The view offers statistical information on the input table.
     */
    @Override
    public NodeView<Statistics2NodeModel> createNodeView(final int viewIndex, 
            final Statistics2NodeModel nodeModel) {
        return new Statistics2NodeView(nodeModel);
    }
    
    /**
     * This node has one view.
     */
    @Override
    public int getNrNodeViews() {
          return 1;
    }
    
    /**
     * No dialog for this node.
     */
    @Override
    public boolean hasDialog() {
        return true;
    }
}
