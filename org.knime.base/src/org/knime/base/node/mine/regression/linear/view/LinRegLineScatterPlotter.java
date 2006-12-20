/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 *   Apr 6, 2006 (wiswedel): created
 */
package org.knime.base.node.mine.regression.linear.view;

import java.awt.Rectangle;

import org.knime.base.node.mine.regression.linear.LinearRegressionParams;
import org.knime.base.node.util.DataArray;
import org.knime.base.node.viz.scatterplot.ScatterPlotter;
import org.knime.base.node.viz.scatterplot.ScatterProps;
import org.knime.base.util.coordinate.Coordinate;
import org.knime.base.util.coordinate.NumericCoordinate;
import org.knime.core.data.def.DoubleCell;

/**
 * Customized ScatterPlotter that paints the input data in conjunction with the
 * regression line. The y column of the scatter plotter is fixed to the response
 * column of the regression.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class LinRegLineScatterPlotter extends ScatterPlotter {

    /** Params to get line from. */
    private LinearRegressionParams m_params;

    /**
     * Delegate to super class that sets a customized drawing pane.
     * 
     * @see ScatterPlotter#ScatterPlotter(DataArray, int, ScatterProps)
     */
    LinRegLineScatterPlotter(final DataArray rowContainer,
            final int initialWidth, final ScatterProps props) {
        super(rowContainer, initialWidth, props, new LinRegLineDrawingPane());
    }

    /**
     * Called when model changes, new data available for instance.
     * 
     * @param newRowContainer the new dots to paint, delegated to super class
     * @param params the new line parameters
     * @see ScatterPlotter#modelDataChanged(DataArray)
     */
    public void modelDataChanged(final DataArray newRowContainer,
            final LinearRegressionParams params) {
        m_params = params;
        modelDataChanged(newRowContainer);
    }

    /**
     * Calculates also the line to draw.
     * 
     * @see ScatterPlotter#updatePaintModel()
     */
    @Override
    protected void updatePaintModel() {
        Coordinate xCoord = getColHeader().getCoordinate();
        Coordinate yCoord = getRowHeader().getCoordinate();
        int x1 = -1;
        int x2 = -1;
        int y1 = -1;
        int y2 = -1;
        LinRegLineDrawingPane dP = getDrawingPane();
        // refuse to paint line when nominal coordinates are selected.
        if (xCoord instanceof NumericCoordinate
                && yCoord instanceof NumericCoordinate) {
            NumericCoordinate x = (NumericCoordinate)xCoord;
            NumericCoordinate y = (NumericCoordinate)yCoord;
            if (m_params != null) {
                double xMin = x.getMinDomainValue();
                double xMax = x.getMaxDomainValue();

                String xName = getXColName();
                if (!xName.equals(m_params.getTargetColumnName())) {
                    double yMin = m_params.getApproximationFor(xName, xMin);
                    double yMax = m_params.getApproximationFor(xName, xMax);

                    Rectangle drawingPane = calculateDrawingRectangle();
                    x1 = (int)x.calculateMappedValue(new DoubleCell(xMin),
                            drawingPane.width, true);
                    x2 = (int)x.calculateMappedValue(new DoubleCell(xMax),
                            drawingPane.width, true);
                    y1 = (int)y.calculateMappedValue(new DoubleCell(yMin),
                            drawingPane.height, true);
                    y2 = (int)y.calculateMappedValue(new DoubleCell(yMax),
                            drawingPane.height, true);
                    x1 += drawingPane.x;
                    x2 += drawingPane.x;
                    y1 = drawingPane.y + drawingPane.height - y1;
                    y2 = drawingPane.y + drawingPane.height - y2;
                }
            }
        }
        dP.setLineFirstPoint(x1, y1);
        dP.setLineLastPoint(x2, y2);
        super.updatePaintModel();
    }

    /**
     * Overridden to do cast.
     * 
     * @see ScatterPlotter#getDrawingPane()
     */
    @Override
    public LinRegLineDrawingPane getDrawingPane() {
        return (LinRegLineDrawingPane)super.getDrawingPane();
    }
}
