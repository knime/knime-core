/*
 * ------------------------------------------------------------------ *
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
                ((LinRegDataProvider)getDataProvider()).getIncludedColumns();
        ((LinRegLinePlotterProperties)getProperties())
                .setIncludedColumns(includedCols);
        // update the combo boxes
        DataTableSpec spec =
                getDataProvider().getDataArray(0).getDataTableSpec();
        ((LinRegLinePlotterProperties)getProperties()).update(spec);
        super.updatePaintModel();
        if (params != null) {
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
        if (params != null) {
            double xMin =
                    ((NumericCoordinate)getXAxis().getCoordinate())
                            .getMinDomainValue();
            double xMax =
                    ((NumericCoordinate)getXAxis().getCoordinate())
                            .getMaxDomainValue();
            String xName = getSelectedXColumn().getName();
            List<String>includedCols = Arrays.asList(
                    ((LinRegDataProvider)getDataProvider())
                .getIncludedColumns());
            
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

}
