/* Created on Jul 10, 2006 4:52:37 PM by thor
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
 */
package org.knime.base.node.meta.xvalidation;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * This factory creates all necessary classes for the cross validation
 * partioning node.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class XValidatePartitionerFactory extends
        NodeFactory<XValidatePartitionModel> {
    /**
     * {@inheritDoc}
     */
    @Override
    public XValidatePartitionModel createNodeModel() {
        return new XValidatePartitionModel();
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

    /**
     * {@inheritDoc}
     */
    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new XValidateDialog();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<XValidatePartitionModel> createNodeView(final int index,
            final XValidatePartitionModel model) {
        return null;
    }
}
