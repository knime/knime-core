/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 *   08.06.2006 (Tobias Koetter): created
 */
package org.knime.base.node.viz.pie.node;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.node.viz.aggregation.AggregationMethod;
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

    private static final String AGGR_COL_SEL_LABEL =
        "Aggregation column:";

    private final SettingsModelIntegerBounded m_noOfRows;

    private final SettingsModelBoolean m_allRows;

    private final SettingsModelString m_pieColumn;

    private final SettingsModelString m_aggrMethod;

    private final SettingsModelString m_aggrColumn;

    private final DialogComponentButtonGroup m_aggrMethButtonGroup;

    /**
     * Constructor for class HistogramNodeDialogPane.
     *
     */
    protected PieNodeDialogPane() {
        super();
        m_noOfRows = new SettingsModelIntegerBounded(
                PieNodeModel.CFGKEY_NO_OF_ROWS,
                PieNodeModel.DEFAULT_NO_OF_ROWS, 0,
                Integer.MAX_VALUE);
        m_allRows = new SettingsModelBoolean(
                PieNodeModel.CFGKEY_ALL_ROWS, false);
        m_allRows.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                m_noOfRows.setEnabled(!m_allRows.getBooleanValue());
            }
        });
        m_pieColumn = new SettingsModelString(
                PieNodeModel.CFGKEY_PIE_COLNAME, "");
        m_aggrColumn = new SettingsModelString(
                PieNodeModel.CFGKEY_AGGR_COLNAME, null);
        m_aggrMethod = new SettingsModelString(PieNodeModel.CFGKEY_AGGR_METHOD,
                AggregationMethod.getDefaultMethod().name());
        m_aggrMethButtonGroup =
            new DialogComponentButtonGroup(m_aggrMethod, null, false,
                    AggregationMethod.values());
        m_aggrMethod.setEnabled(m_aggrColumn.getStringValue() != null);
        m_aggrColumn.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
               boolean enable = m_aggrColumn.getStringValue() != null;
               m_aggrMethod.setEnabled(enable);
               if (!enable) {
                   m_aggrMethod.setStringValue(
                           AggregationMethod.COUNT.getActionCommand());
               }
            }
        });
//      m_aggrColumn.setEnabled(!AggregationMethod.COUNT.equals(
//      AggregationMethod.getDefaultMethod()));
//        m_aggrMethod.addChangeListener(new ChangeListener() {
//            public void stateChanged(final ChangeEvent e) {
//                final AggregationMethod method =
//                    AggregationMethod.getMethod4Command(
//                            m_aggrMethod.getStringValue());
//                m_aggrColumn.setEnabled(
//                        !AggregationMethod.COUNT.equals(method));
//            }
//        });

        final DialogComponentNumber noOfRowsComp =
            new DialogComponentNumber(m_noOfRows,
                NO_OF_ROWS_LABEL, 1);
        final DialogComponentBoolean allRowsComp =
            new DialogComponentBoolean(m_allRows, ALL_ROWS_LABEL);
        final DialogComponentColumnNameSelection pieCol =
                new DialogComponentColumnNameSelection(
                                m_pieColumn,
                                PieNodeDialogPane.X_COL_SEL_LABEL, 0, true,
                                PieNodeModel.PIE_COLUMN_FILTER);
        final DialogComponentColumnNameSelection aggrCols =
            new DialogComponentColumnNameSelection(m_aggrColumn,
                    AGGR_COL_SEL_LABEL, 0, false, true,
                    PieNodeModel.AGGREGATION_COLUMN_FILTER);

        createNewGroup("Rows to display:");
        addDialogComponent(allRowsComp);
        addDialogComponent(noOfRowsComp);

        createNewGroup("Column selection:");
        addDialogComponent(pieCol);
        addDialogComponent(aggrCols);

        createNewGroup("Aggregation method");
        addDialogComponent(m_aggrMethButtonGroup);
    }
}
