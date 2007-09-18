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

    private final FixedPieVizModel m_pieVizModel;

    /**Constructor for class PieHiliteCalculator.
     * @param pieVizModel the {@link FixedPieVizModel} to calculate the hilite
     * info for
     */
    protected PieHiliteCalculator(final FixedPieVizModel pieVizModel) {
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
