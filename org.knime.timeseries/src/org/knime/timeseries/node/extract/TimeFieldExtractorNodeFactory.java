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
 *   24.09.2009 (Fabian Dill): created
 */
package org.knime.timeseries.node.extract;

import org.knime.core.node.AbstractNodeView;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;

/**
 * 
 * @author Fabian Dill, KNIME.com, Zurich, Switzerland
 */
public class TimeFieldExtractorNodeFactory 
    extends NodeFactory<TimeFieldExtractorNodeModel> {

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new TimeFieldExtractorNodeDialog();
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public TimeFieldExtractorNodeModel createNodeModel() {
        return new TimeFieldExtractorNodeModel();
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public AbstractNodeView<TimeFieldExtractorNodeModel> createNodeView(
            final int viewIndex, final TimeFieldExtractorNodeModel nodeModel) {
        return null;
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    protected boolean hasDialog() {
        return true;
    }

}
