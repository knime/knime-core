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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   06.08.2010 (hofer): created
 */
package org.knime.base.node.io.tablecreator.table;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * The swing action for copying the selected cells to the system clipboard.
 *
 * @author Heiko Hofer
 */
class CopyAction extends AbstractAction {
    private static final long serialVersionUID = -3808997973654007267L;

    private JTable m_table;

    /**
     * Creates a new instance.
     *
     * @param table the 'model' for this action
     */
    CopyAction(final JTable table) {
        super("Copy");
        m_table = table;
        m_table.getSelectionModel().addListSelectionListener(
                new ListSelectionListener() {
            public void valueChanged(final ListSelectionEvent e) {
                setEnabled(!m_table.getSelectionModel().isSelectionEmpty());
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(final ActionEvent e) {
        int[] rows = m_table.getSelectedRows();
        int[] cols = m_table.getSelectedColumns();
        boolean isContiguousBlockSelected =
            rows.length == rows[rows.length - 1] - rows[0] + 1
            && cols.length == cols[cols.length - 1] - cols[0] + 1;
        if (!isContiguousBlockSelected) {
            JOptionPane.showMessageDialog(null,
                    "This function cannot be used for multiple selection",
                    "KNIME",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < rows.length; i++) {
            for (int k = 0; k < cols.length; k++) {
                Object value = m_table.getValueAt(rows[i], cols[k]);
                if (value instanceof Cell) {
                    builder.append(((Cell)value).getText());
                } else {
                    builder.append(value.toString());
                }

                if (k < cols.length - 1) {
                    builder.append("\t");
                }
            }
            builder.append("\n");
        }
        StringSelection str  = new StringSelection(builder.toString());
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(str,str);
    }

}
