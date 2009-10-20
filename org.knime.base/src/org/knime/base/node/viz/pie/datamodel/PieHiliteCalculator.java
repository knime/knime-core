/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
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
