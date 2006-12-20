/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 *   14.12.2005 (cebron): created
 */
package org.knime.base.node.mine.neural.rprop;

import javax.swing.JPanel;

import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeView;


/**
 * NodeView of the RProp Node. Provides an error plot.
 * 
 * @author Nicolas Cebron, University of Konstanz
 */
public class RPropNodeView extends NodeView {
    /**
     * @param model Underlying NodeModel
     */
    public RPropNodeView(final NodeModel model) {
        super(model);
    }

    /**
     * @see org.knime.core.node.NodeView#modelChanged()
     */
    @Override
    protected void modelChanged() {
        RPropNodeModel model = (RPropNodeModel)getNodeModel();
        if (model.getErrorPlot() != null) {
            JPanel show = model.getErrorPlot();
            super.setComponent(show);
        }
    }

    /**
     * @see org.knime.core.node.NodeView#onClose()
     */
    @Override
    protected void onClose() {
    }

    /**
     * @see org.knime.core.node.NodeView#onOpen()
     */
    @Override
    protected void onOpen() {
    }
}
