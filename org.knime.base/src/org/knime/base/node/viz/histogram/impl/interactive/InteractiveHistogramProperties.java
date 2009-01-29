/*
 * ------------------------------------------------------------------
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
 */
package org.knime.base.node.viz.histogram.impl.interactive;

import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeListener;

import org.knime.base.node.viz.aggregation.AggregationMethod;
import org.knime.base.node.viz.histogram.datamodel.AbstractHistogramVizModel;
import org.knime.base.node.viz.histogram.impl.AbstractHistogramPlotter;
import org.knime.base.node.viz.histogram.impl.AbstractHistogramProperties;
import org.knime.base.node.viz.histogram.util.AggregationColumnFilterPanel;
import org.knime.base.node.viz.histogram.util.ColorColumn;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionComboxBox;

/**
 * The properties panel of the Histogram plotter which allows the user to change
 * the look and behaviour of the histogram plotter. The following options are
 * available:
 * <ol>
 * <li>Bar width</li>
 * <li>Number of bars for a numeric x column</li>
 * <li>different aggregation methods</li>
 * <li>hide empty bars</li>
 * <li>show missing value bar</li>
 * </ol>
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public class InteractiveHistogramProperties extends 
    AbstractHistogramProperties {
    
    private static final long serialVersionUID = -2199619830257018206L;

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(InteractiveHistogramProperties.class);

    private static final String X_COLUMN_LABEL = "Binning column:";

    private static final String AGGREGATION_COLUMN_LABEL = 
        "Aggregation column:";
    
    private static final String AGGREGATION_COLUMN_DISABLED_TOOLTIP = 
        "Not available for aggregation method count";
    
    private static final String AGGREGATION_COLUMN_ENABLED_TOOLTIP = 
        "Select the column used for aggregation";

    private static final Dimension HORIZONTAL_SPACER_DIM = new Dimension(10, 1);
    
    private final ColumnSelectionComboxBox m_xCol;
    
    private final AggregationColumnFilterPanel m_aggrCol;
    
    /**
     * Constructor for class FixedColumnHistogramProperties.
     * 
     * @param tableSpec the {@link DataTableSpec} to initialize the column
     * @param vizModel the aggregation method which should be set
     */
    @SuppressWarnings("unchecked")
    public InteractiveHistogramProperties(final DataTableSpec tableSpec,
            final AbstractHistogramVizModel vizModel) {
       super(tableSpec, vizModel);
       // the column select boxes for the X axis
       m_xCol = new ColumnSelectionComboxBox((Border)null, 
               AbstractHistogramPlotter.X_COLUMN_FILTER);
       m_xCol.setBackground(this.getBackground());

       m_aggrCol = 
           new AggregationColumnFilterPanel(null, 
                   new Dimension(150, 10),
                   AbstractHistogramPlotter.AGGREGATION_COLUMN_FILTER);
       m_aggrCol.setBackground(this.getBackground());
       m_aggrCol.setToolTipText(AGGREGATION_COLUMN_DISABLED_TOOLTIP);
       super.addColumnTab(createColumnSettingsBox());
    }
    

    /**
     * The columns settings panel which contains the x column 
     * and aggregation column selection box.
     * 
     * @return the columns selection panel
     */
    private Box createColumnSettingsBox() {
//the x column box
        final Box xColumnBox = Box.createHorizontalBox();
//        xColumnBox.setBorder(BorderFactory
//                .createEtchedBorder(EtchedBorder.RAISED));
        final JLabel xColLabelLabel = new JLabel(X_COLUMN_LABEL);
        xColumnBox.add(Box.createRigidArea(HORIZONTAL_SPACER_DIM));
        xColumnBox.add(xColLabelLabel);
        xColumnBox.add(Box.createHorizontalGlue());
        xColumnBox.add(m_xCol);
        xColumnBox.add(Box.createRigidArea(HORIZONTAL_SPACER_DIM));
//the aggregation column box
        final Box aggrColumnBox = Box.createHorizontalBox();
//      xColumnBox.setBorder(BorderFactory
//              .createEtchedBorder(EtchedBorder.RAISED));
      final JLabel aggrColLabelLabel = new JLabel(AGGREGATION_COLUMN_LABEL);
      aggrColumnBox.add(Box.createRigidArea(HORIZONTAL_SPACER_DIM));
      aggrColumnBox.add(aggrColLabelLabel);
      aggrColumnBox.add(Box.createHorizontalGlue());
      aggrColumnBox.add(m_aggrCol);
      aggrColumnBox.add(Box.createRigidArea(HORIZONTAL_SPACER_DIM));
//        final Box aggrColumnBox = Box.createHorizontalBox();
//        aggrColumnBox.add(m_aggrCol);
//        aggrColumnBox.add(Box.createRigidArea(HORIZONTAL_SPACER_DIM));

//the box which surround both column selection boxes
        final Box columnsBox = Box.createVerticalBox();
        columnsBox.setBorder(BorderFactory
                .createEtchedBorder(EtchedBorder.RAISED));
        columnsBox.add(Box.createVerticalGlue());
        columnsBox.add(xColumnBox);
        columnsBox.add(Box.createVerticalGlue());
        columnsBox.add(aggrColumnBox);
        columnsBox.add(Box.createVerticalGlue());
        return columnsBox;
    }

    /**
     * @param listener adds the listener to the x column select box.
     */
    protected void addXColumnChangeListener(final ActionListener listener) {
            m_xCol.addActionListener(listener);
    }
    /**
     * @param listener adds the listener to the aggregation column select box.
     */
    protected void addAggrColumnChangeListener(final ChangeListener listener) {
        m_aggrCol.addChangeListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateColumnSelection(final DataTableSpec spec,
            final String xColName, 
            final Collection<ColorColumn> aggrColumns,
            final AggregationMethod aggrMethod) {
        try {
            if (xColName == null) {
                final String er = "No binning column available";
                LOGGER.warn(er);
                throw new IllegalArgumentException(er);
            }
            //remove all action listener to avoid unnecessary calls
            final ActionListener[] listeners = m_xCol.getActionListeners();
            for (ActionListener listener : listeners) {
                m_xCol.removeActionListener(listener);
            }
            m_xCol.setEnabled(true);
            m_xCol.update(spec, xColName);
            for (ActionListener listener : listeners) {
                m_xCol.addActionListener(listener);
            }
        } catch (final NotConfigurableException e) {
            m_xCol.setEnabled(false);
        }
        m_aggrCol.update(spec, aggrColumns);
        // set the values for the y axis select box
        if (m_aggrCol.getNoOfColumns() < 1) {
            // if we have no aggregation columns disable it
            m_aggrCol.setEnabled(false);
        } else {
            // enable the select box only if it contains at least one value
            m_aggrCol.setEnabled(true);
        }
        // enable or disable the aggregation method buttons depending if
        // aggregation columns available or not
//        for (final Enumeration<AbstractButton> buttons = m_aggrMethButtonGrp
//                .getElements(); buttons.hasMoreElements();) {
//            final AbstractButton button = buttons.nextElement();
//            button.setEnabled(m_aggrCol.getModel().getSize() > 0);
//        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void onSelectAggrMethod(final String actionCommand) {
        if (m_aggrCol.getNoOfColumns() < 1) {
            m_aggrCol.setEnabled(false);
            m_aggrCol.setToolTipText(
                    AGGREGATION_COLUMN_DISABLED_TOOLTIP);
        } else {
            m_aggrCol.setEnabled(true);
            m_aggrCol.setToolTipText(
                    AGGREGATION_COLUMN_ENABLED_TOOLTIP);
        }
    }


    /**
     * @return all selected aggregation columns
     */
    public List<ColorColumn> getSelectedAggrColumns() {
        final ColorColumn[] inclCols = 
            m_aggrCol.getIncludedColorNameColumns();
        if (inclCols == null) {
            return null;
        }
        return Arrays.asList(inclCols);
    }
}
