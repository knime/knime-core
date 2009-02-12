/*
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
 * -------------------------------------------------------------------
 *
 * History
 *    21.09.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.pie.node.fixed;

import org.knime.core.node.property.hilite.HiLiteHandler;

import org.knime.base.node.viz.pie.datamodel.fixed.FixedPieVizModel;
import org.knime.base.node.viz.pie.impl.PiePlotter;
import org.knime.base.node.viz.pie.impl.fixed.FixedPiePlotter;
import org.knime.base.node.viz.pie.impl.fixed.FixedPieProperties;
import org.knime.base.node.viz.pie.node.PieNodeView;


/**
 * The fixed implementation of the {@link PieNodeView}.
 * @author Tobias Koetter, University of Konstanz
 */
public class FixedPieNodeView
extends PieNodeView<FixedPieProperties, FixedPieVizModel, FixedPieNodeModel> {

    /**Constructor for class FixedPieNodeView.
     * @param nodeModel the node model
     */
    FixedPieNodeView(final FixedPieNodeModel nodeModel) {
        super(nodeModel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PiePlotter<FixedPieProperties, FixedPieVizModel> getPlotter(
            final FixedPieVizModel vizModel, final HiLiteHandler handler) {
        final FixedPieProperties properties = new FixedPieProperties(vizModel);
        return new FixedPiePlotter(properties, handler);
    }
}
