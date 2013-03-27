/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 *   30.08.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter.columns;

import java.awt.Dimension;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeListener;

import org.knime.base.node.viz.plotter.AbstractPlotterProperties;
import org.knime.base.util.coordinate.Coordinate;
import org.knime.base.util.coordinate.NumericCoordinate;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.NominalValue;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionComboxBox;

/**
 * <p>
 * Provides functionality to select the x and the y column to display.
 * Selects first and second column per default and allows the filtering of 
 * a certain DataType and to fix some column to a certain value. The information
 * about the available columns and if they are compatible with the restricting
 * {@link org.knime.core.data.DataValue}s is taken from a 
 * {@link org.knime.core.data.DataTableSpec} which has to be provided in the 
 * <code>update</code> methods:
 * {@link #update(DataTableSpec)}, {@link #update(DataTableSpec, int, int)},
 * and {@link #updateRangeSpinner(DataColumnSpec, DataColumnSpec)}.
 * </p>
 * <p> 
 * In addition the ranges for the x and y axis can be adapted, i.e. the minimum 
 * and maximum for each column can be adapted. The registration of listeners is 
 * wrapped by this class, corresponding methods are provided.
 * </p>
 * @author Fabian Dill, University of Konstanz
 */
public class TwoColumnProperties extends AbstractPlotterProperties {
    
    /** Layout constant (space between elements). */
    public static final int SPACE = 15;
    
    /** Layout constant (space to the border). */
    public static final int SMALL_SPACE = 5;
    
    /** Layout constant (combobox width, etc). */
    public static final int COMPONENT_WIDTH = 150;
    
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            TwoColumnProperties.class);
    
    /** The x column selection box. */
    protected ColumnSelectionComboxBox m_xSelector;
    /** The y column selection box. */
    protected ColumnSelectionComboxBox m_ySelector;
   
    
    private JSpinner m_xMinSpinner;
    private JSpinner m_xMaxSpinner;
    private JSpinner m_yMinSpinner;
    private JSpinner m_yMaxSpinner;
    
    
    /**
     * Creates a properties tab with the default properties
     * ({@link org.knime.base.node.viz.plotter.AbstractPlotterProperties})
     * and a tab for the selection of two columns and the adjustment of their 
     * ranges. There is no restriction on the displayed 
     * {@link org.knime.core.data.DataValue}s.
     *
     */
    @SuppressWarnings("unchecked")
    public TwoColumnProperties() {
       this(new Class[]{NominalValue.class, DoubleValue.class},
               new Class[]{NominalValue.class, DoubleValue.class});
    }
    
    /**
     * Creates a properties tab with the default properties
     * ({@link org.knime.base.node.viz.plotter.AbstractPlotterProperties})
     * and a tab for the selection of two columns and the adjustment of their 
     * ranges, in addition, each column selection can be restricted to display 
     * only columns which are compatible with certain 
     * {@link org.knime.core.data.DataValue}s. 
     * 
     * @param allowedXTypes allowed data types to be selecteable for the x 
     * column.
     * @param allowedYTypes allowed data types to be selectable for the y 
     * column
     */
    public TwoColumnProperties(
            final Class<? extends DataValue>[]allowedXTypes,
            final Class<? extends DataValue>[]allowedYTypes) {
        super();
        m_xSelector = new ColumnSelectionComboxBox("X Column:", allowedXTypes);
        m_ySelector = new ColumnSelectionComboxBox("Y Column:", allowedYTypes);
        Box compositeBox = Box.createHorizontalBox();
        compositeBox.add(m_xSelector);
        compositeBox.add(m_ySelector);
        compositeBox.setBorder(BorderFactory.createEtchedBorder(
                EtchedBorder.RAISED));
        JPanel panel = new JPanel();
        panel.add(compositeBox);
        panel.add(createRangeBox());
        m_xSelector.setBackground(panel.getBackground());
        m_ySelector.setBackground(panel.getBackground());
        addTab("Column Selection", panel);
    }
    
    /**
     * Creates the necessary graphical components for adjusting the displayed  
     * column ranges.
     * @return the box containing the graphical components for the column ranges
     * adjustment
     */
    private Box createRangeBox() {
        m_xMinSpinner = new JSpinner(new SpinnerNumberModel(1.0, 
                null, null, 0.1));
        m_xMaxSpinner = new JSpinner(new SpinnerNumberModel(1.0, 
                null, null, 0.1));
        m_yMinSpinner = new JSpinner(new SpinnerNumberModel(1.0, 
                null, null, 0.1));
        m_yMaxSpinner = new JSpinner(new SpinnerNumberModel(1.0, 
                null, null, 0.1));
        Dimension spinnerSize = new Dimension(100, 
                m_xMinSpinner.getPreferredSize().height);
        m_xMinSpinner.setPreferredSize(spinnerSize);
        m_xMaxSpinner.setPreferredSize(spinnerSize);
        m_yMinSpinner.setPreferredSize(spinnerSize);
        m_yMaxSpinner.setPreferredSize(spinnerSize);
        
        Box xBox = Box.createHorizontalBox();
        xBox.add(Box.createHorizontalStrut(SMALL_SPACE));
        xBox.add(new JLabel("X attribute"));
        xBox.add(Box.createHorizontalStrut(SPACE));
        xBox.add(m_xMinSpinner);
        xBox.add(Box.createHorizontalStrut(SPACE));
        xBox.add(m_xMaxSpinner);
        xBox.add(Box.createHorizontalStrut(SMALL_SPACE));
        
        Box yBox = Box.createHorizontalBox();
        yBox.add(Box.createHorizontalStrut(SMALL_SPACE));
        yBox.add(new JLabel("Y attribute"));
        yBox.add(Box.createHorizontalStrut(SPACE));
        yBox.add(m_yMinSpinner);
        yBox.add(Box.createHorizontalStrut(SPACE));
        yBox.add(m_yMaxSpinner);
        yBox.add(Box.createHorizontalStrut(SMALL_SPACE));
        
        Box rangeBox = Box.createVerticalBox();
        rangeBox.add(Box.createVerticalStrut(SMALL_SPACE));
        rangeBox.add(xBox);
        rangeBox.add(Box.createVerticalStrut(SPACE));
        rangeBox.add(yBox);
        rangeBox.add(Box.createVerticalStrut(SMALL_SPACE));
        rangeBox.setBorder(BorderFactory.createEtchedBorder(
                EtchedBorder.RAISED));
        return rangeBox;
    }
    
    /**
     * Updates the values of the range spinner acording to the current 
     * columns.
     * 
     * @param xColumn selected x column 
     * @param yColumn selected y column
     */
    protected void updateRangeSpinner(final DataColumnSpec xColumn,
            final DataColumnSpec yColumn) {
        Coordinate xCoordinate = Coordinate.createCoordinate(xColumn);
        Coordinate yCoordinate = Coordinate.createCoordinate(yColumn);
        if (xCoordinate == null || xCoordinate.isNominal()) {
            // disable: no ranges
            m_xMinSpinner.setEnabled(false);
            m_xMaxSpinner.setEnabled(false);
        } else {
            // enable
            m_xMinSpinner.setEnabled(true);
            m_xMaxSpinner.setEnabled(true);
            // get min and max values
            double xMin = ((NumericCoordinate)xCoordinate).getMinDomainValue();
            double xMax = ((NumericCoordinate)xCoordinate).getMaxDomainValue();
            // set them
            m_xMinSpinner.setValue(xMin);
            m_xMaxSpinner.setValue(xMax);
        }
        if (yCoordinate == null || yCoordinate.isNominal()) {
            // disable: no ranges
            m_yMinSpinner.setEnabled(false);
            m_yMaxSpinner.setEnabled(false);
        } else {
            // enable
            m_yMinSpinner.setEnabled(true);
            m_yMaxSpinner.setEnabled(true);
            // get min and max values
            double yMin = ((NumericCoordinate)yCoordinate).getMinDomainValue();
            double yMax = ((NumericCoordinate)yCoordinate).getMaxDomainValue();
            // set them
            m_yMinSpinner.setValue(yMin);
            m_yMaxSpinner.setValue(yMax);            
        }
    }
    
    /**
     * 
     * @param listener the item listener for the x column box.
     */
    public void addXColumnListener(final ItemListener listener) {
        m_xSelector.addItemListener(listener);
    }
    
    /**
     * 
     * @param listener the item listener for the y column box.
     */
    public void addYColumnListener(final ItemListener listener) {
        m_ySelector.addItemListener(listener);
    }
    
    /**
     * 
     * @return the selected x column spec
     */
    public DataColumnSpec getSelectedXColumn() {
        return (DataColumnSpec)m_xSelector.getSelectedItem();
    }
    
    /**
     * 
     * @return the selected y column spec
     */
    public DataColumnSpec getSelectedYColumn() {
        return (DataColumnSpec)m_ySelector.getSelectedItem();
    }
    
    /**
     * 
     * @param listener for the x min value.
     */
    public void addXMinListener(final ChangeListener listener) {
        m_xMinSpinner.addChangeListener(listener);
    }
    /**
     * 
     * @param listener for the x max value.
     */
    public void addXMaxListener(final ChangeListener listener) {
        m_xMaxSpinner.addChangeListener(listener);
    }
    /**
     * 
     * @param listener for the y min value.
     */
    public void addYMinListener(final ChangeListener listener) {
        m_yMinSpinner.addChangeListener(listener);
    }
    /**
     * 
     * @param listener for the y min value.
     */
    public void addYMaxListener(final ChangeListener listener) {
        m_yMaxSpinner.addChangeListener(listener);
    }
    
    /**
     * 
     * @return adjusted x min value.
     */
    public double getXMinValue() {
        return (Double)m_xMinSpinner.getValue();
    }
    
    /**
     * 
     * @return adjusted x max value.
     */    
    public double getXMaxValue() {
        return (Double)m_xMaxSpinner.getValue();
    }
    
    /**
     * 
     * @return adjusted y min value.
     */
    public double getYMinValue() {
        return (Double)m_yMinSpinner.getValue();
    }
    
    /**
     * 
     * @return adjusted y max value.
     */
    public double getYMaxValue() {
        return (Double)m_yMaxSpinner.getValue();
    }
    
    /**
     * Updates the selection boxes with the passed 
     * {@link org.knime.core.data.DataTableSpec} and sets
     * 0 and 1 as x and y.
     * 
     * @param spec the new {@link org.knime.core.data.DataTableSpec}
     */
    public void update(final DataTableSpec spec) {
        int xIdx = -1;
        int yIdx = -1;
        if (m_xSelector != null && m_xSelector.getSelectedColumn() != null) {
            xIdx = spec.findColumnIndex(m_xSelector.getSelectedColumn());
        }
        if (m_ySelector != null && m_ySelector.getSelectedColumn() != null) {
            yIdx = spec.findColumnIndex(m_ySelector.getSelectedColumn());
        }
        if (xIdx == -1) {
            xIdx = 0;
        }
        if (yIdx == -1) {
            yIdx = 1;
            if (spec.getNumColumns() <= 1) {
                yIdx = 0;
            }
        }
        update(spec, xIdx, yIdx);
    }
    
    /**
     * Updates the selection boxes with the new 
     * {@link org.knime.core.data.DataTableSpec} and selects the passed 
     * indices. 
     * 
     * @param spec the new data table spec.
     * @param xPreSelect the x column index (-1 if unknown)
     * @param yPreSelect the y column (-1 if unknown)
     */
    public void update(final DataTableSpec spec, final int xPreSelect,
            final int yPreSelect) {
        try {
            m_xSelector.update(spec, spec.getColumnSpec(xPreSelect).getName(),
                    true);
            m_ySelector.update(spec, spec.getColumnSpec(yPreSelect).getName(),
                    true);
        } catch (NotConfigurableException e) {
            LOGGER.warn(e.getMessage(), e);
        }
        DataColumnSpec x = (DataColumnSpec)m_xSelector.getSelectedItem();
        DataColumnSpec y = (DataColumnSpec)m_ySelector.getSelectedItem();
        updateRangeSpinner(x, y);
    }
    

}
