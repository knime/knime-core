/*
 * ------------------------------------------------------------------
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
 *   08.06.2006 (Tobias Koetter): created
 */
package org.knime.base.node.viz.pie.node.fixed;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.node.viz.aggregation.AggregationMethod;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * The dialog of the {@link PieNodeModel} where the user can
 * define the x column and the number of rows.
 * @author Tobias Koetter, University of Konstanz
 */
public class PieNodeDialogPane extends DefaultNodeSettingsPane {

    private static final String ALL_ROWS_LABEL = "Display all rows";

    private static final String NO_OF_ROWS_LABEL = "No. of rows to display:";

    private static final String X_COL_SEL_LABEL = "Pie column:";

    private static final String AGGR_COL_SEL_LABEL = "Aggregation column:";

    private final SettingsModelIntegerBounded m_noOfRows;

    private final SettingsModelBoolean m_allRows;

    private final SettingsModelString m_pieColumn;

    private final SettingsModelString m_aggrMethod;

    private final SettingsModelString m_aggrColumn;

    /**
     * Constructor for class HistogramNodeDialogPane.
     *
     */
    @SuppressWarnings("unchecked")
    protected PieNodeDialogPane() {
        super();
        m_noOfRows = new SettingsModelIntegerBounded(
                PieNodeModel.CFGKEY_NO_OF_ROWS,
                PieNodeModel.DEFAULT_NO_OF_ROWS, 0,
                Integer.MAX_VALUE);
        m_allRows = new SettingsModelBoolean(
                PieNodeModel.CFGKEY_ALL_ROWS, false);
        m_allRows.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                m_noOfRows.setEnabled(!m_allRows.getBooleanValue());
            }
        });
        m_pieColumn = new SettingsModelString(
                PieNodeModel.CFGKEY_PIE_COLNAME, "");
        m_aggrColumn = new SettingsModelString(
                PieNodeModel.CFGKEY_AGGR_COLNAME, null);
        m_aggrColumn.setEnabled(!AggregationMethod.COUNT.equals(
                AggregationMethod.getDefaultMethod()));
        m_aggrMethod = new SettingsModelString(PieNodeModel.CFGKEY_AGGR_METHOD,
                AggregationMethod.getDefaultMethod().name());
        m_aggrMethod.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                final AggregationMethod method =
                    AggregationMethod.getMethod4Command(
                            m_aggrMethod.getStringValue());
                m_aggrColumn.setEnabled(
                        !AggregationMethod.COUNT.equals(method));
            }
        });

        final DialogComponentNumber noOfRowsComp =
            new DialogComponentNumber(m_noOfRows,
                NO_OF_ROWS_LABEL, new Integer(1));
        final DialogComponentBoolean allRowsComp =
            new DialogComponentBoolean(m_allRows, ALL_ROWS_LABEL);
        final DialogComponentColumnNameSelection pieCol =
                new DialogComponentColumnNameSelection(
                                m_pieColumn,
                                PieNodeDialogPane.X_COL_SEL_LABEL, 0, true,
                                PieNodeModel.PIE_COLUMN_FILTER);
        final DialogComponentColumnNameSelection aggrCols =
            new DialogComponentColumnNameSelection(m_aggrColumn,
                    AGGR_COL_SEL_LABEL, 0, false, DoubleValue.class);

        final DialogComponentButtonGroup aggrMethod =
            new DialogComponentButtonGroup(m_aggrMethod, "Aggregation method: ",
                    false, AggregationMethod.values());

        createNewGroup("Rows to display:");
        addDialogComponent(allRowsComp);
        addDialogComponent(noOfRowsComp);

        createNewGroup("Column selection:");
        addDialogComponent(pieCol);
        addDialogComponent(aggrMethod);
        addDialogComponent(aggrCols);
    }
}
