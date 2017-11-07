/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 * ------------------------------------------------------------------------
 */
package org.knime.base.node.mine.regression.linear2.view;

import java.util.List;

import org.knime.base.node.mine.regression.RegressionContent;
import org.knime.base.node.viz.plotter.DataProvider;
import org.knime.base.node.viz.plotter.scatter.ScatterPlotter;
import org.knime.base.node.viz.plotter.scatter.ScatterPlotterDrawingPane;
import org.knime.base.util.coordinate.NumericCoordinate;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.util.Pair;

/**
 * A scatter plot with a regression line. The
 * {@link LinReg2LinePlotterProperties} ensure that the y axis is fixed to the
 * target column and that only the columns used for model calculation can be
 * selected as x axis.
 *
 * @author Heiko Hofer
 *
 */
public class LinReg2LinePlotter extends ScatterPlotter {

    /**
     *
     * @param panel the drawing pane
     * @param props the properties (control panel)
     */
    public LinReg2LinePlotter(final ScatterPlotterDrawingPane panel,
            final LinReg2LinePlotterProperties props) {
        super(panel, props);
    }

    /**
     * Retrieves the linear regression params, updates the column selection
     * boxes appropriately and adds the regression line to the scatterplot.
     */
    @Override
    public void updatePaintModel() {
        DataProvider dataProvider = getDataProvider();
        if (dataProvider == null) {
            return;
        }
        RegressionContent content = ((LinReg2DataProvider)dataProvider).getLinRegContent();
        if (content == null) {
            return;
        }

        List<String>includedList = content.getCovariates();
        String target = content.getSpec().getTargetCols().get(0).getName();
        // set the target column to fix
        ((LinReg2LinePlotterProperties)getProperties()).setTargetColumn(target);

        // get the included columns
        ((LinReg2LinePlotterProperties)getProperties()).setIncludedColumns(
            includedList.toArray(new String[includedList.size()]));
        // update the combo boxes
        DataTableSpec spec = getDataProvider().getDataArray(0).getDataTableSpec();
        ((LinReg2LinePlotterProperties)getProperties()).update(spec);
        super.updatePaintModel();
        double xMin = ((NumericCoordinate)getXAxis().getCoordinate()).getMinDomainValue();
        double xMax = ((NumericCoordinate)getXAxis().getCoordinate()).getMaxDomainValue();
        String xName = getSelectedXColumn().getName();



        if (!xName.equals(target) && includedList.contains(xName)) {
            double yMin = getApproximationFor(xName, xMin, content);
            double yMax = getApproximationFor(xName, xMax, content);
            ((LinReg2LineDrawingPane)getDrawingPane()).setLineFirstPoint(
                    getMappedXValue(new DoubleCell(xMin)),
                    getMappedYValue(new DoubleCell(yMin)));
            ((LinReg2LineDrawingPane)getDrawingPane()).setLineLastPoint(
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
        DataProvider dataProvider = getDataProvider();
        if (dataProvider == null) {
            return;
        }
        RegressionContent content = ((LinReg2DataProvider)dataProvider).getLinRegContent();
        if (content == null) {
            return;
        }
        double xMin =
                ((NumericCoordinate)getXAxis().getCoordinate())
                        .getMinDomainValue();
        double xMax = ((NumericCoordinate)getXAxis().getCoordinate()).getMaxDomainValue();
        String xName = getSelectedXColumn().getName();

        List<String>includedList = content.getCovariates();
        String target = content.getSpec().getTargetCols().get(0).getName();

        if (!xName.equals(target) && includedList.contains(xName)) {
            double yMin = getApproximationFor(xName, xMin, content);
            double yMax = getApproximationFor(xName, xMax, content);
            ((LinReg2LineDrawingPane)getDrawingPane()).setLineFirstPoint(
                    getMappedXValue(new DoubleCell(xMin)),
                    getMappedYValue(new DoubleCell(yMin)));
            ((LinReg2LineDrawingPane)getDrawingPane()).setLineLastPoint(
                    getMappedXValue(new DoubleCell(xMax)),
                    getMappedYValue(new DoubleCell(yMax)));
        }
    }

    /**
     * Does a prediction when the given variable has the value v and all other
     * variables have their mean value. Used to determine the line in a 2D plot.
     *
     * @param variable the variable currently shown on x
     * @param v its value
     * @return the value of the linear regression line
     */
    public double getApproximationFor(final String variable, final double v) {
        RegressionContent content = ((LinReg2DataProvider)getDataProvider()).getLinRegContent();
        if (content != null) {
            return getApproximationFor(variable, v, content);
        } else {
            return Double.NaN;
        }
    }

    /**
     * Does a prediction when the given variable has the value v and all other
     * variables have their mean value. Used to determine the line in a 2D plot.
     *
     * @param variable the variable currently shown on x
     * @param v its value
     * @return the value of the linear regression line
     */
    private double getApproximationFor(final String variable, final double v, final RegressionContent content) {
        double approx = content.getIntercept();
        double[] means = content.getMeans();

        List<String> parameters = content.getParameters();
        int varIndex = parameters.indexOf(variable);
        for (int i = 0; i < parameters.size(); i++) {
            double val = i == varIndex ? v : means[i];
            approx += content.getCoefficients().get(Pair.create(parameters.get(i), 1)) * val;
        }
        return approx;
    }
}
