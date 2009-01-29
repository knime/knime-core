/* Created on Oct 20, 2006 3:19:36 PM by thor
 * -------------------------------------------------------------------
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
 * ------------------------------------------------------------------- *
 */
package org.knime.base.node.viz.enrichment;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * This factory creates all necessary components for the enrichment plotter
 * node.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class EnrichmentPlotterFactory extends
    NodeFactory<EnrichmentPlotterModel> {
    /**
     * {@inheritDoc}
     */
    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new EnrichmentPlotterDialog();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EnrichmentPlotterModel createNodeModel() {
        return new EnrichmentPlotterModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<EnrichmentPlotterModel> createNodeView(final int viewIndex,
            final EnrichmentPlotterModel nodeModel) {
        return new EnrichmentPlotterView(nodeModel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getNrNodeViews() {
        return 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean hasDialog() {
        return true;
    }
}
