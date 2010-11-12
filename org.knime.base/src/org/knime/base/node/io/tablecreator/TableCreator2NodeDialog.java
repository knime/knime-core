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
 * ---------------------------------------------------------------------
 *
 * History
 *   02.08.2010 (hofer): created
 */
package org.knime.base.node.io.tablecreator;

import java.awt.BorderLayout;

import javax.swing.JPanel;

import org.knime.base.node.io.tablecreator.table.Spreadsheet;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObjectSpec;


/**
 * The dialog class of the Table Creator Node.
 *
 * @author  Heiko Hofer
 */
public final class TableCreator2NodeDialog extends NodeDialogPane {
    private Spreadsheet m_spreadsheet;
    private TableCreator2NodeSettings m_settings;

    /**
     * Create a new instance.
     */
    TableCreator2NodeDialog() {
        m_settings = new TableCreator2NodeSettings();
        addTab("Table Creator Settings", createTableCreatorSettingsTab());
    }

    private JPanel createTableCreatorSettingsTab() {
        JPanel p = new JPanel(new BorderLayout());
        m_spreadsheet = new Spreadsheet();
        p.add(m_spreadsheet, BorderLayout.CENTER);
        return p;
    }
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) {
        m_settings.loadSettingsForDialog(settings);
        m_spreadsheet.setData(
                m_settings.getColumnProperties(),
                m_settings.getRowIndices(),
                m_settings.getColumnIndices(),
                m_settings.getValues());
        m_spreadsheet.setRowIdPrefix(m_settings.getRowIdPrefix());
        m_spreadsheet.setRowIdSuffix(m_settings.getRowIdSuffix());
        m_spreadsheet.setRowIdStartValue(m_settings.getRowIdStartValue());
        m_spreadsheet.setHighlightOutputTable(
                m_settings.getHightlightOutputTable());
        // I (Heiko) would rather like to give the table the focus, but this
        // does not seem to work, instead clear focused cell
        m_spreadsheet.clearFocusedCell();
    }

    /**
     * {@inheritDoc}
     * @throws InvalidSettingsException
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
    throws InvalidSettingsException {
        // Commit editing - This is a workaround for a bug in the Dialog
        // since the table does not loose focus when OK or Apply is pressed.
        m_spreadsheet.stopCellEditing();
        if (m_spreadsheet.hasParseErrors()) {
            throw new InvalidSettingsException("Some cells cannot be parsed.");
        }
        m_settings.setRowIndices(m_spreadsheet.getRowIndices());
        m_settings.setColumnIndices(m_spreadsheet.getColumnIndices());
        m_settings.setValues(m_spreadsheet.getValues());
        m_settings.setColumnProperties(m_spreadsheet.getColumnProperties());
        m_settings.setRowIdPrefix(m_spreadsheet.getRowIdPrefix());
        m_settings.setRowIdSuffix(m_spreadsheet.getRowIdSuffix());
        m_settings.setRowIdStartValue(m_spreadsheet.getRowIdStartValue());
        m_settings.setHightlightOutputTable(
                m_spreadsheet.getHightLightOutputTable());
        m_settings.saveSettings(settings);

    }
}
