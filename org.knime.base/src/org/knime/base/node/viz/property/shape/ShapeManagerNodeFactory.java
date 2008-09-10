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
 */
package org.knime.base.node.viz.property.shape;

import org.knime.core.node.GenericNodeDialogPane;
import org.knime.core.node.GenericNodeFactory;
import org.knime.core.node.GenericNodeView;

/**
 * The shape manager factory which creates a
 * {@link org.knime.base.node.viz.property.shape.ShapeManagerNodeDialogPane}.
 * 
 * @see ShapeManagerNodeModel
 * @see ShapeManagerNodeDialogPane
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class ShapeManagerNodeFactory 
        extends GenericNodeFactory<ShapeManagerNodeModel> {
    
    /**
     * Empty default constructor.
     */
    public ShapeManagerNodeFactory() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ShapeManagerNodeModel createNodeModel() {
        return new ShapeManagerNodeModel();
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
    public GenericNodeDialogPane createNodeDialogPane() {
        return new ShapeManagerNodeDialogPane();
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
    public GenericNodeView<ShapeManagerNodeModel> createNodeView(
            final int index, final ShapeManagerNodeModel nodeModel) {
        return null;
    }
}
