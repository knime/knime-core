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
 *
 * History
 *   08.06.2006 (Tobias Koetter): created
 */
package org.knime.base.node.viz.histogram.node;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import org.knime.base.node.viz.histogram.impl.AbstractHistogramPlotter;
import org.knime.base.node.viz.histogram.util.AggregationColumnDialogComponent;
import org.knime.base.node.viz.histogram.util.SettingsModelColorNameColumns;

import java.awt.Dimension;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * The dialog of the {@link HistogramNodeModel} where the user can
 * define the x column and the number of rows.
 * @author Tobias Koetter, University of Konstanz
 */
public class HistogramNodeDialogPane extends DefaultNodeSettingsPane {

    private static final String ALL_ROWS_LABEL = "Display all rows";

    private static final String NO_OF_ROWS_LABEL = "No. of rows to display:";

    private static final String X_COL_SEL_LABEL = "Binning column:";

    private static final String AGGR_COL_SEL_LABEL = "Aggregation column:";

    private final SettingsModelIntegerBounded m_noOfRows;

    private final SettingsModelBoolean m_allRows;

    private final SettingsModelString m_xColumnModel;

    private final DialogComponentColumnNameSelection m_xColumnSelectBox;
    /**
     * Constructor for class HistogramNodeDialogPane.
     *
     */
    @SuppressWarnings("unchecked")
    protected HistogramNodeDialogPane() {
        super();
        createNewGroup("Rows to display:");
        m_noOfRows = new SettingsModelIntegerBounded(
                AbstractHistogramNodeModel.CFGKEY_NO_OF_ROWS,
                AbstractHistogramNodeModel.DEFAULT_NO_OF_ROWS, 0,
                Integer.MAX_VALUE);
        final DialogComponentNumber noOfRowsComp =
            new DialogComponentNumber(m_noOfRows,
                NO_OF_ROWS_LABEL, new Integer(1));
        m_allRows = new SettingsModelBoolean(
                AbstractHistogramNodeModel.CFGKEY_ALL_ROWS, false);
        m_allRows.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                m_noOfRows.setEnabled(!m_allRows.getBooleanValue());
            }
        });
        final DialogComponentBoolean allRowsComp =
            new DialogComponentBoolean(m_allRows, ALL_ROWS_LABEL);
        addDialogComponent(allRowsComp);
        addDialogComponent(noOfRowsComp);

        createNewGroup("Column selection:");
        m_xColumnModel = new SettingsModelString(
                AbstractHistogramNodeModel.CFGKEY_X_COLNAME, "");
        m_xColumnSelectBox = new DialogComponentColumnNameSelection(
                        m_xColumnModel,
                        HistogramNodeDialogPane.X_COL_SEL_LABEL, 0, true,
                        AbstractHistogramPlotter.X_COLUMN_FILTER);
        //the x column select box
        addDialogComponent(m_xColumnSelectBox);

        //the aggregation column select box
        final SettingsModelColorNameColumns colorNameCols =
                new SettingsModelColorNameColumns(
                        AbstractHistogramNodeModel.CFGKEY_AGGR_COLNAME, null);
        final AggregationColumnDialogComponent aggrCols =
            new AggregationColumnDialogComponent(AGGR_COL_SEL_LABEL,
                    colorNameCols, new Dimension(150, 155),
                AbstractHistogramPlotter.AGGREGATION_COLUMN_FILTER);
        addDialogComponent(aggrCols);
    }

    /**
     * @param listener the {@link ChangeListener} to add to the x column
     * select box
     */
    protected void addXColumnChangeListener(final ChangeListener listener) {
        m_xColumnModel.addChangeListener(listener);
    }

    /**
     * @return the {@link DataColumnSpec} of the selected x column or
     * <code>null</code> if none is selected
     */
    protected DataColumnSpec getSelectedXColumnSpec() {
        return m_xColumnSelectBox.getSelectedAsSpec();
    }
}
