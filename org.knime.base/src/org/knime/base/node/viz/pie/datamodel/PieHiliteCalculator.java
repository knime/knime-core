/*
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
 * History
 *    18.09.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.pie.datamodel;

import java.awt.geom.Arc2D;

import org.knime.base.node.viz.aggregation.AggregationMethod;
import org.knime.base.node.viz.aggregation.AggregationValModel;
import org.knime.base.node.viz.aggregation.AggregationValSubModel;
import org.knime.base.node.viz.aggregation.HiliteShapeCalculator;
import org.knime.base.node.viz.pie.util.GeometryUtil;

/**
 * The hilite calculator for the pie chart.
 * @author Tobias Koetter, University of Konstanz
 */
public class PieHiliteCalculator
    implements HiliteShapeCalculator<Arc2D, Arc2D> {

    private final PieVizModel m_pieVizModel;

    /**Constructor for class PieHiliteCalculator.
     * @param pieVizModel the {@link PieVizModel} to calculate the hilite
     * info for
     */
    protected PieHiliteCalculator(final PieVizModel pieVizModel) {
        m_pieVizModel = pieVizModel;
    }

    /**
     * @return the current aggregation method
     */
    public AggregationMethod getAggrMethod() {
        return m_pieVizModel.getAggregationMethod();
    }

    /**
     * {@inheritDoc}
     */
    public Arc2D calculateHiliteShape(
            final AggregationValModel<AggregationValSubModel<Arc2D, Arc2D>,
            Arc2D, Arc2D> model) {
        final double fraction;
        if (model.getRowCount() == 0) {
            fraction = 0;
        } else {
            fraction = model.getHiliteRowCount()
            / (double)model.getRowCount();
        }
        return GeometryUtil.calculateSubArc(model.getShape(),
                fraction);
    }

    /**
     * {@inheritDoc}
     */
    public Arc2D calculateHiliteShape(
            final AggregationValSubModel<Arc2D, Arc2D> model) {
        final double fraction;
        if (model.getRowCount() == 0) {
            fraction = 0;
        } else {
            fraction = model.getHiliteRowCount()
            / (double)model.getRowCount();
        }
        return GeometryUtil.calculateSubArc(model.getShape(),
                fraction);
    }
}
