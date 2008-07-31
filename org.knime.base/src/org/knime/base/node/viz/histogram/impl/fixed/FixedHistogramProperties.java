/*
 * ------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 */
package org.knime.base.node.viz.histogram.impl.fixed;

import java.awt.Dimension;
import java.util.Collection;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.border.EtchedBorder;

import org.knime.base.node.viz.aggregation.AggregationMethod;
import org.knime.base.node.viz.histogram.datamodel.AbstractHistogramVizModel;
import org.knime.base.node.viz.histogram.impl.AbstractHistogramProperties;
import org.knime.base.node.viz.histogram.util.ColorColumn;
import org.knime.core.data.DataTableSpec;


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
public class FixedHistogramProperties extends 
    AbstractHistogramProperties {

    /**
     * The number of aggregation rows which should be displayed in
     * one row of the properties panel.*/
    private static final int NO_OF_AGGR_COLS_PER_ROW = 5;

    private static final long serialVersionUID = -6177238900813927896L;

    private static final String X_COLUMN_LABEL = "Binning column:";

    private static final String AGGREGATION_COLUMN_LABEL = 
        "Aggregation column:";
    
    private static final Dimension HORIZONTAL_SPACER_DIM = new Dimension(10, 1);
    
    private final JLabel m_xCol;
    
    private final JEditorPane m_aggrCol;
    
    /**
     * Constructor for class FixedColumnHistogramProperties.
     * 
     * @param tableSpec the {@link DataTableSpec} to initialize the column
     * @param vizModel the {@link AbstractHistogramVizModel}
     */
    public FixedHistogramProperties(final DataTableSpec tableSpec,
            final AbstractHistogramVizModel vizModel) {
        super(tableSpec, vizModel);
        m_xCol = new JLabel();
        m_aggrCol = new JEditorPane("text/html", "");
        m_aggrCol.setBackground(getBackground());
        super.addColumnTab(createColumnSettingsBox());
    }

    /**
     * The column information panel which contains the x column 
     * and aggregation column information.
     * 
     * @return the column information panel
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
        final JLabel aggrColumnLabel = new JLabel(AGGREGATION_COLUMN_LABEL);
        aggrColumnBox.add(Box.createRigidArea(HORIZONTAL_SPACER_DIM));
        aggrColumnBox.add(aggrColumnLabel);
        aggrColumnBox.add(Box.createHorizontalGlue());
        aggrColumnBox.add(m_aggrCol);
        aggrColumnBox.add(Box.createRigidArea(HORIZONTAL_SPACER_DIM));

//      the box which surround both column boxes
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
     * {@inheritDoc}
     */
    @Override
    protected void onSelectAggrMethod(final String actionCommand) {
        //nothing to do
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void updateColumnSelection(final DataTableSpec spec, 
            final String xColName, final Collection<ColorColumn> aggrColumns, 
            final AggregationMethod aggrMethod) {
        m_xCol.setText(xColName);
        m_aggrCol.setText(createAggrColTable(aggrColumns));
    }

    private static String createAggrColTable(
            final Collection<ColorColumn> cols) {
        if (cols == null || cols.size() < 1) {
            return "no columns selected";
        }
        StringBuilder buf = new StringBuilder();
        buf.append("<br>");
        buf.append("<table cellspacing='5'>");
        int i = 0;
        for (ColorColumn col : cols) {
            if (i % NO_OF_AGGR_COLS_PER_ROW == 0) {
                buf.append("<tr>");
            }
            buf.append("<td bgcolor='#");
            buf.append(Integer.toHexString(
                    col.getColor().getRGB() & 0x00ffffff));
            buf.append("'>");
            buf.append(col.getColumnName());
            buf.append("</td>");
            if (i % NO_OF_AGGR_COLS_PER_ROW == NO_OF_AGGR_COLS_PER_ROW - 1) {
                buf.append("</tr>");
            }
            i++;
        }
        if (i % NO_OF_AGGR_COLS_PER_ROW != 0) {
            while (i % NO_OF_AGGR_COLS_PER_ROW != 0) {
                //close the table line if the number of columns wasn't even
                buf.append("<td>&nbsp;</td>");
                i++;
            }
            buf.append("</tr>");
        }
        buf.append("</table>");
        return buf.toString();
    }
}
