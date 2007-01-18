/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any quesions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * --------------------------------------------------------------------- *
 * 
 * History
 *   18.01.2007 (ohl): created
 */
package org.knime.base.node.mine.regression.linear.view;

import java.awt.BasicStroke;
import java.awt.Color;

import org.knime.base.node.mine.regression.linear.LinearRegressionParams;
import org.knime.base.node.viz.plotter.basic.BasicDrawingPane;
import org.knime.base.node.viz.plotter.scatter.ScatterPlotter;
import org.knime.base.node.viz.plotter.scatter.ScatterPlotterDrawingPane;
import org.knime.base.util.coordinate.NumericCoordinate;
import org.knime.core.data.DataTableSpec;

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

    private double[] m_xCoords;

    private double[] m_yCoords;

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
        LinearRegressionParams params =
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
            if (!xName.equals(params.getTargetColumnName())) {
                double yMin = params.getApproximationFor(xName, xMin);
                double yMax = params.getApproximationFor(xName, xMax);
                m_xCoords = new double[]{xMin, xMax};
                m_yCoords = new double[]{yMin, yMax};
                int strokeSize =
                        Math.max(1,
                                ((ScatterPlotterDrawingPane)getDrawingPane())
                                        .getDotSize() / 3);
                ((BasicDrawingPane)getDrawingPane()).clearPlot();
                addLine(m_xCoords, m_yCoords, Color.BLACK, new BasicStroke(
                        strokeSize));
                getDrawingPane().repaint();
            }
        }
    }

    /**
     * Same as {@link #updatePaintModel()}.
     */
    @Override
    public void updateSize() {
        updatePaintModel();
    }

}
