/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
