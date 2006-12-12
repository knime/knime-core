/* -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   02.05.2006 (koetter): created
 */
package org.knime.base.node.viz.histogram.node;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * The dialog of the {@link FixedColumnHistogramNodeModel} where the user can
 * define the x and aggregation column and the number of rows.
 * 
 * @author Tobias Koetter
 */
public class FixedColumnHistogramNodeDialog extends DefaultNodeSettingsPane {

    private static final String X_COL_SEL_LABEL = "X column:";

    private static final String AGGR_COL_SEL_LABEL = "Aggregation column:";

    private final DialogComponentNumber m_noOfRowsSpinner;

    private final DialogComponentBoolean m_allRowsBox;

    /**
     * New pane for configuring BayesianClassifier node dialog.
     */
    @SuppressWarnings("unchecked")
    public FixedColumnHistogramNodeDialog() {
        super();
        createNewGroup("Rows to display:");
        m_noOfRowsSpinner = new DialogComponentNumber(new SettingsModelInteger(
                FixedColumnHistogramNodeModel.CFGKEY_NO_OF_ROWS,
                FixedColumnHistogramNodeModel.DEFAULT_NO_OF_ROWS),
                "No. of rows to display:", 1);
        final SettingsModelBoolean allRowsModel = new SettingsModelBoolean(
                FixedColumnHistogramNodeModel.CFGKEY_ALL_ROWS, false);
        allRowsModel.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                m_noOfRowsSpinner.setEnabled(!m_allRowsBox.isSelected());
            }
        });
        m_allRowsBox = new DialogComponentBoolean(allRowsModel, "All rows");
        m_noOfRowsSpinner.setEnabled(!m_allRowsBox.isSelected());
        addDialogComponent(m_allRowsBox);
        addDialogComponent(m_noOfRowsSpinner);

        createNewGroup("Column selection:");
        addDialogComponent(new DialogComponentColumnNameSelection(
                new SettingsModelString(
                        FixedColumnHistogramNodeModel.CFGKEY_X_COLNAME, ""),
                FixedColumnHistogramNodeDialog.X_COL_SEL_LABEL, 0,
                DataValue.class));

        addDialogComponent(new DialogComponentColumnNameSelection(
                new SettingsModelString(
                        FixedColumnHistogramNodeModel.CFGKEY_AGGR_COLNAME, ""),
                FixedColumnHistogramNodeDialog.AGGR_COL_SEL_LABEL, 0,
                DoubleValue.class));
    }
}
