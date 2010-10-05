/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
