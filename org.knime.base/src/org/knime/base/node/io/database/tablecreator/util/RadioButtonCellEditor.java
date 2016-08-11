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
 *   Jan 12, 2016 (budiyanto): created
 */
package org.knime.base.node.io.database.tablecreator.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JRadioButton;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;

/**
 * Cell Editor with jRadioButton as the underlying editor
 *
 * @author Budi Yanto, KNIME.com
 */
class RadioButtonCellEditor extends DefaultCellEditor implements ItemListener {

    private static final long serialVersionUID = 1L;

    private final Border BLACK_BORDER = new LineBorder(Color.BLACK);

    private final JRadioButton m_button;

    private JTable m_table;

    private int m_row;

    private int m_column;

    /**
     * Creates a new instance of RadioButtonCellEditor
     */
    RadioButtonCellEditor() {
        super(new JCheckBox());
        m_button = new JRadioButton();
        m_button.setHorizontalAlignment(SwingConstants.CENTER);
        m_button.setBorderPainted(true);
        m_button.addItemListener(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Component getTableCellEditorComponent(final JTable table, final Object value, final boolean isSelected,
        final int row, final int column) {

        if (value == null) {
            return null;
        }

        if (isSelected) {
            m_button.setForeground(table.getSelectionForeground());
            m_button.setBackground(table.getSelectionBackground());
        } else {
            m_button.setForeground(table.getForeground());
            m_button.setBackground(table.getBackground());
        }

        m_button.setBorder(BLACK_BORDER);

        m_table = table;
        m_row = row;
        m_column = column;

        m_button.setSelected(Boolean.valueOf(value.toString()));
        return m_button;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getCellEditorValue() {
        // Set all other value to false, to reflect the behavior of a radio button
        updateRadioButtonGroup(m_table, m_row, m_column);
        return m_button.isSelected();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void itemStateChanged(final ItemEvent evt) {
        super.fireEditingStopped();
    }

    /**
     * Update the radio buttons in the table to reflect the behavior of radio button group Only one radio button can be
     * true, all others must be false
     *
     * @param table the table that is asking the editor to edit
     * @param row the row of the cell being edited
     * @param column the column of the cell being edited
     */
    private void updateRadioButtonGroup(final JTable table, final int row, final int column) {
        TableCreatorTableModel model = (TableCreatorTableModel)table.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            if (i != row) {
                model.setValueAt(false, i, column);
            }
        }
    }

}
