/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *   Nov 19, 2015 (budiyanto): created
 */
package org.knime.base.node.io.database.tablecreator.util;

import java.awt.Color;
import java.awt.Component;

import javax.swing.DefaultCellEditor;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;

/**
 * Cell editor used to edit the name in the table
 *
 * @author Budi Yanto, KNIME.com
 */
class NameCellEditor extends DefaultCellEditor {

    private static final long serialVersionUID = 1L;

    private static final Border BLACK_BORDER = new LineBorder(Color.BLACK);

    /** The column of the cell being edited **/
    private int m_columnIndex;

    /** The row of the cell being edited **/
    private int m_rowIndex;

    /** The table being edited **/
    private JTable m_table;

    /**
     * Creates a new instance of NameCellEditor
     */
    NameCellEditor() {
        super(new JTextField());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean stopCellEditing() {
        String name = ((JTextField)getComponent()).getText().trim();

        // Do some validation on the user input. //

        // Shows error message if the name is empty
        if (name == null || name.isEmpty()) {
            showErrorInfo("Empty Name Error", "Name cannot be empty.");

            return false; // stay at the editor
        }

        // Shows error message if there is a duplicate name
        for (int idx = 0; idx < m_table.getModel().getRowCount(); idx++) {
            String val = m_table.getModel().getValueAt(idx, m_columnIndex).toString();
            if (m_rowIndex != idx && val.equalsIgnoreCase(name)) {
                showErrorInfo("Duplicate Name Error", String.format("Duplicate name '%s' at row '%d'", name, idx));

                return false; // stay at the editor
            }
        }

        return super.stopCellEditing();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Component getTableCellEditorComponent(final JTable table, final Object value, final boolean isSelected,
        final int row, final int column) {
        m_table = table;
        m_rowIndex = row;
        m_columnIndex = column;
        ((JTextField)getComponent()).setBorder(BLACK_BORDER);
        return super.getTableCellEditorComponent(table, value, isSelected, row, column);
    }

    /**
     * Show error message in a pop up dialog
     *
     * @param title title of the pop up dialog
     * @param message error message
     */
    private void showErrorInfo(final String title, final String message) {
        JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(getComponent()), message, title,
            JOptionPane.ERROR_MESSAGE);
    }

}
