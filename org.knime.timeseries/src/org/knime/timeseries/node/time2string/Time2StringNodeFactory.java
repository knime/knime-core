/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2008 - 2009
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
 *   28.09.2009 (Fabian Dill): created
 */
package org.knime.timeseries.node.time2string;

import org.knime.core.data.date.DateAndTimeValue;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;
import org.knime.timeseries.node.stringtotimestamp.String2DateDialog;

/**
 * 
 * @author Fabian Dill, KNIME.com, Zurich, Switzerland
 */
public class Time2StringNodeFactory extends NodeFactory<Time2StringNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new String2DateDialog(DateAndTimeValue.class, 
                Time2StringNodeModel.COL_NAME_SUFFIX, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Time2StringNodeModel createNodeModel() {
        return new Time2StringNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<Time2StringNodeModel> createNodeView(
            final int viewIndex,
            final Time2StringNodeModel nodeModel) {
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
