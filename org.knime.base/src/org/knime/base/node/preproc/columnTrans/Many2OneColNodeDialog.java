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
 * ---------------------------------------------------------------------
 * 
 */
package org.knime.base.node.preproc.columnTrans;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.node.preproc.columnTrans.Many2OneColNodeModel.IncludeMethod;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;


/**
 * <code>NodeDialog</code> for the "Many2One" Node.
 * 
 * @author Tobias Koetter
 */
public class Many2OneColNodeDialog extends DefaultNodeSettingsPane {

    // new column name
    private DialogComponentString m_appendedColumnName;
    // column filter to condense
    private DialogComponentColumnFilter m_columns2Condense;
    // include method
    private DialogComponentStringSelection m_includeMethod;
    // include/exclude pattern
    private DialogComponentString m_pattern;
    // keep columns 
    private DialogComponentBoolean m_keepColumns;

    
    /**
     * Adds textfield for new column name,
     * column filter for columns to condense,
     * include method (max or min value in row, or 0,1, or regexp pattern),
     * textfield for regexp pattern (if selected),
     * boolean keep columns,
     * boolean allow multiple columns match in one row.
     *
     */
    public Many2OneColNodeDialog() {
        m_appendedColumnName = new DialogComponentString(
                new SettingsModelString(
                        Many2OneColNodeModel.CONDENSED_COL_NAME, 
                        "Condensed Column"), "Appended column name");
        m_columns2Condense = new DialogComponentColumnFilter(
                new SettingsModelFilterString(
                        Many2OneColNodeModel.SELECTED_COLS), 0);
        final SettingsModelString includeModel = new SettingsModelString(
                Many2OneColNodeModel.INCLUDE_METHOD, 
                Many2OneColNodeModel.IncludeMethod.Binary.name());
        String[] values = new String[IncludeMethod.values().length];
        for (int i = 0; i < IncludeMethod.values().length; i++) {
            values[i] = IncludeMethod.values()[i].name();
        }
        m_includeMethod = new DialogComponentStringSelection(
                includeModel, "Include method", values);
        final SettingsModelString patternModel = new SettingsModelString(
                Many2OneColNodeModel.RECOGNICTION_REGEX, "[^0]*");
        // initially disable/enable
        patternModel.setEnabled(includeModel.getStringValue()
                .equals(Many2OneColNodeModel.IncludeMethod.
                        RegExpPattern.name()));
        m_pattern = new DialogComponentString(patternModel, "Include Pattern");
        includeModel.addChangeListener(new ChangeListener() {
            // enable/disable depended on include method
            public void stateChanged(final ChangeEvent e) {
                    patternModel.setEnabled(includeModel.getStringValue()
                            .equals(Many2OneColNodeModel.IncludeMethod.
                                    RegExpPattern.name()));
            }
        });
        m_keepColumns = new DialogComponentBoolean(new SettingsModelBoolean(
                Many2OneColNodeModel.KEEP_COLS, true),
                "Keep original columns");
        addDialogComponent(m_columns2Condense);
        addDialogComponent(m_appendedColumnName);
        addDialogComponent(m_includeMethod);
        addDialogComponent(m_pattern);
        addDialogComponent(m_keepColumns);
        setDefaultTabTitle("Column settings");
    }
}
