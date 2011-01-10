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
