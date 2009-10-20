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
 * --------------------------------------------------------------------- *
 *
 * History
 *   18.01.2007 (ohl): created
 */
package org.knime.base.node.mine.regression.linear.view;

import java.util.Arrays;
import java.util.List;

import org.knime.base.node.mine.regression.linear.LinearRegressionContent;
import org.knime.base.node.viz.plotter.scatter.ScatterPlotter;
import org.knime.base.node.viz.plotter.scatter.ScatterPlotterDrawingPane;
import org.knime.base.util.coordinate.NumericCoordinate;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DoubleCell;

/**
 * A scatter plot with a regression line. The
 * {@link LinRegLinePlotterProperties} ensure that the y axis is fixed to the
 * target column and that only the columns used for model calculation can be
 * selected as x axis.
 *
 * @author Fabian Dill, University of Konstanz
 *
 */
public class LinRegLinePlotter extends ScatterPlotter {

    /**
     *
     * @param panel the drawing pane
     * @param props the properties (control panel)
     */
    public LinRegLinePlotter(final ScatterPlotterDrawingPane panel,
            final LinRegLinePlotterProperties props) {
        super(panel, props);
    }

    /**
     * Retrieves the linear regression params, updates the column selection
     * boxes appropriately and adds the regression line to the scatterplot.
     */
    @Override
    public void updatePaintModel() {
        if (getDataProvider() == null
                || getDataProvider().getDataArray(0) == null) {
            return;
        }
        LinearRegressionContent params =
                ((LinRegDataProvider)getDataProvider()).getParams();
        if (params == null) {
            return;
        }
        // set the target column to fix
        ((LinRegLinePlotterProperties)getProperties()).setTargetColumn(params
                .getTargetColumnName());

        // get the included columns
        String[] includedCols =
                ((LinRegDataProvider)getDataProvider()).getLearningColumns();
        ((LinRegLinePlotterProperties)getProperties())
                .setIncludedColumns(includedCols);
        // update the combo boxes
        DataTableSpec spec =
                getDataProvider().getDataArray(0).getDataTableSpec();
        ((LinRegLinePlotterProperties)getProperties()).update(spec);
        super.updatePaintModel();
        double xMin =
                ((NumericCoordinate)getXAxis().getCoordinate())
                        .getMinDomainValue();
        double xMax =
                ((NumericCoordinate)getXAxis().getCoordinate())
                        .getMaxDomainValue();
        String xName = getSelectedXColumn().getName();

        List<String>includedList = Arrays.asList(includedCols);

        if (!xName.equals(params.getTargetColumnName())
                && includedList.contains(xName)) {
            double yMin = params.getApproximationFor(xName, xMin);
            double yMax = params.getApproximationFor(xName, xMax);
            ((LinRegLineDrawingPane)getDrawingPane()).setLineFirstPoint(
                    getMappedXValue(new DoubleCell(xMin)),
                    getMappedYValue(new DoubleCell(yMin)));
            ((LinRegLineDrawingPane)getDrawingPane()).setLineLastPoint(
                    getMappedXValue(new DoubleCell(xMax)),
                    getMappedYValue(new DoubleCell(yMax)));
            getDrawingPane().repaint();
        }
    }

    /**
     * First calls super then adapts the regression line.
     */
    @Override
    public void updateSize() {
        if (getXAxis() == null || getXAxis().getCoordinate() == null
                || getYAxis() == null || getYAxis().getCoordinate() == null) {
            return;
        }
        super.updateSize();
        if (getDataProvider() == null
                || getDataProvider().getDataArray(0) == null) {
            return;
        }
        LinearRegressionContent params =
                ((LinRegDataProvider)getDataProvider()).getParams();
        if (params == null) {
            return;
        }
        double xMin =
                ((NumericCoordinate)getXAxis().getCoordinate())
                        .getMinDomainValue();
        double xMax =
                ((NumericCoordinate)getXAxis().getCoordinate())
                        .getMaxDomainValue();
        String xName = getSelectedXColumn().getName();
        List<String>includedCols = Arrays.asList(
                ((LinRegDataProvider)getDataProvider())
            .getLearningColumns());

        if (!xName.equals(params.getTargetColumnName())
                && includedCols.contains(xName)) {
            double yMin = params.getApproximationFor(xName, xMin);
            double yMax = params.getApproximationFor(xName, xMax);
            ((LinRegLineDrawingPane)getDrawingPane()).setLineFirstPoint(
                    getMappedXValue(new DoubleCell(xMin)),
                    getMappedYValue(new DoubleCell(yMin)));
            ((LinRegLineDrawingPane)getDrawingPane()).setLineLastPoint(
                    getMappedXValue(new DoubleCell(xMax)),
                    getMappedYValue(new DoubleCell(yMax)));
        }
    }
}
