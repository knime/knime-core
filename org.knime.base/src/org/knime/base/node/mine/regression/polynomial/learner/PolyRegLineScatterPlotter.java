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
 *   Apr 6, 2006 (wiswedel): created
 */
package org.knime.base.node.mine.regression.polynomial.learner;

import java.awt.Color;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

import org.knime.base.node.viz.plotter.scatter.ScatterPlotter;
import org.knime.base.node.viz.plotter.scatter.ScatterPlotterDrawingPane;
import org.knime.base.node.viz.plotter.scatter.ScatterPlotterProperties;
import org.knime.base.util.coordinate.Coordinate;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionComboxBox;

/**
 * This class is the plotter used inside the {@link PolyRegLineNodeView}.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public class PolyRegLineScatterPlotter extends ScatterPlotter {
    private static class MyProperties extends ScatterPlotterProperties {
        private final ColumnSelectionComboxBox m_xColumn =
                new ColumnSelectionComboxBox((Border)null, DoubleValue.class);
        /**
         * 
         */
        MyProperties() {
            JPanel p = new JPanel();
            p.add(new JLabel("Choose column for x-axis"));
            p.add(m_xColumn);
            addTab("Column Selection", p);

            removeTabAt(1); // remove the column selection tab, we have our own
        }
    }

    private DataColumnSpec m_xColumnSpec;

    private DataColumnSpec m_yColumnSpec;

    private final PolyRegLearnerNodeModel m_model;

    private DataTableSpec m_filteredSpec;

    /**
     * Creates a new plotter for showing the regression lines.
     * 
     * @param model the node model
     */
    PolyRegLineScatterPlotter(final PolyRegLearnerNodeModel model) {
        super(new ScatterPlotterDrawingPane(), new MyProperties());
        setDataProvider(model);
        
        m_model = model;
        
        final MyProperties props = (MyProperties)getProperties();
        props.m_xColumn.addItemListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                reset();
                
                DataTable table = m_model.getDataArray(0);
                if (table != null) {
                    m_xColumnSpec = table.getDataTableSpec()
                            .getColumnSpec(props.m_xColumn.getSelectedColumn());
                    getXAxis().setCoordinate(
                            Coordinate.createCoordinate(m_xColumnSpec));
                    getYAxis().setCoordinate(
                            Coordinate.createCoordinate(m_yColumnSpec));
                }

                updatePaintModel();
            }
        });

        modelChanged();
    }

    
    /**
     * This method must be called if the model has changed. It updates the
     * plotter to show the new model's values.
     */
    public void modelChanged() {
        DataTable data = m_model.getDataArray(0);
        if (data != null) {
            final DataTableSpec origSpec = data.getDataTableSpec();
            final MyProperties props = (MyProperties)getProperties();
    
            DataColumnSpec[] colSpecs =
                    new DataColumnSpec[origSpec.getNumColumns() - 1];
            int i = 0;
            for (DataColumnSpec cs : origSpec) {
                if (!m_model.getTargetColumn().equals(cs.getName())) {
                    colSpecs[i++] = cs;
                } else {
                    m_yColumnSpec = cs;
                    getYAxis().setCoordinate(Coordinate.createCoordinate(cs));
                }
            }
            m_xColumnSpec = colSpecs[0];
            getXAxis().setCoordinate(Coordinate.createCoordinate(colSpecs[0]));
    
            m_filteredSpec = new DataTableSpec(colSpecs);
            try {
                props.m_xColumn.update(m_filteredSpec, colSpecs[0].getName());
            } catch (NotConfigurableException ex) {
                // cannot happen
                assert false : ex.getMessage();
            }
    
            reset();
            updatePaintModel();
        }
   }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public DataColumnSpec getSelectedXColumn() {
        return m_xColumnSpec;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataColumnSpec getSelectedYColumn() {
        return m_yColumnSpec;
    }

    /**
     * Calculates also the line to draw.
     * 
     * @see ScatterPlotter#updatePaintModel()
     */
    @Override
    public void updatePaintModel() {
        if (m_xColumnSpec == null) {
            super.updatePaintModel();
            return;
        }

        double minX =
                ((DoubleValue)m_xColumnSpec.getDomain().getLowerBound())
                        .getDoubleValue();
        double maxX =
                ((DoubleValue)m_xColumnSpec.getDomain().getUpperBound())
                        .getDoubleValue();

        int points = Math.max(1000, 2 * getWidth());
        double[] xValues = new double[points];
        xValues[0] = minX;
        double step = (maxX - minX) / (xValues.length - 1.0);
        for (int i = 1; i < xValues.length; i++) {
            xValues[i] = xValues[i - 1] + step;
        }

        int colIndex = m_filteredSpec.findColumnIndex(m_xColumnSpec.getName());
        double[] yValues = new double[xValues.length];
        double[] betas = m_model.getBetas();
        double[] means = m_model.getMeanValues().clone();
        for (int i = 0; i < xValues.length; i++) {
            means[colIndex] = xValues[i];
            yValues[i] = computeYValue(means, betas);
        }        
        addLine(xValues, yValues, Color.CYAN, null);
        super.updatePaintModel();
    }

    private double computeYValue(final double[] xValues, final double[] betas) {
        int betaIndex = 0;
        double value = betas[betaIndex++];

        for (int i = 0; i < xValues.length; i++) {
            double x = xValues[i];
            for (int deg = 1; deg <= m_model.getDegree(); deg++) {
                value += x * betas[betaIndex++];
                x *= xValues[i];
            }

        }
        return value;
    }
}
