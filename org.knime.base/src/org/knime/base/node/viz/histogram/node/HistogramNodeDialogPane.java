/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * The dialog of the {@link HistogramNodeModel} where the user can
 * define the x column and the number of rows. 
 * @author Tobias Koetter, University of Konstanz
 */
public class HistogramNodeDialogPane extends DefaultNodeSettingsPane {

    private static final String ALL_ROWS_LABEL = "Display all rows";

    private static final String NO_OF_ROWS_LABEL = "No. of rows to display:";

    private static final String X_COL_SEL_LABEL = "X column:";

    private final DialogComponentNumber m_noOfRowsSpinner;

    private final DialogComponentBoolean m_allRowsBox;

    /**
     * Constructor for class HistogramNodeDialogPane.
     * 
     */
    @SuppressWarnings("unchecked")
    protected HistogramNodeDialogPane() {
        // TK_TODO: Abstract class which handles the row options and maybe 
        //some more

        super();
        createNewGroup("Rows to display:");
        m_noOfRowsSpinner = new DialogComponentNumber(new SettingsModelInteger(
                HistogramNodeModel.CFGKEY_NO_OF_ROWS,
                HistogramNodeModel.DEFAULT_NO_OF_ROWS),
                NO_OF_ROWS_LABEL, 1);
        final SettingsModelBoolean allRowsModel = new SettingsModelBoolean(
                HistogramNodeModel.CFGKEY_ALL_ROWS, false);
        allRowsModel.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                m_noOfRowsSpinner.setEnabled(!m_allRowsBox.isSelected());
            }
        });
        m_allRowsBox = new DialogComponentBoolean(allRowsModel, ALL_ROWS_LABEL);
        m_noOfRowsSpinner.setEnabled(!m_allRowsBox.isSelected());
        addDialogComponent(m_allRowsBox);
        addDialogComponent(m_noOfRowsSpinner);

        createNewGroup("Column selection:");
        addDialogComponent(new DialogComponentColumnNameSelection(
                new SettingsModelString(
                        HistogramNodeModel.CFGKEY_X_COLNAME, ""),
                HistogramNodeDialogPane.X_COL_SEL_LABEL, 0, DataValue.class));
    }
}
