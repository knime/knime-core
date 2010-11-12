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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   04.08.2010 (hofer): created
 */
package org.knime.base.node.io.tablecreator.table;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.StringTokenizer;

import javax.swing.AbstractAction;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.knime.base.node.io.filereader.ColProperty;


/**
 * The swing action to insert cells from the system clipboard.
 *
 * @author Heiko Hofer
 */
class PasteAction extends AbstractAction {
    private static final long serialVersionUID = -6887521547411990568L;
    private SpreadsheetTable m_table;
    private int m_endRow;
    private int m_endCol;
    private SpreadsheetTableModel m_tempModel;

    /**
     * Creates a new instance.
     *
     * @param table the 'model' for this action
     */
    PasteAction(final SpreadsheetTable table) {
        super("Paste");
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
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        int startRow = m_table.getFocusedRow();
        int startCol = m_table.getFocusedColumn();
        m_endRow = startRow;
        m_endCol = startCol;
        m_tempModel = new SpreadsheetTableModel();
        try {
            String trstring =
                    (String)(clipboard.getContents(this)
                            .getTransferData(DataFlavor.stringFlavor));
            StringTokenizer rows = new StringTokenizer(trstring, "\n", true);
            for (int i = 0; rows.hasMoreTokens(); i++) {
                String row = rows.nextToken();
                if (!row.equals("\n")) {
                    StringTokenizer cells = new StringTokenizer(row, "\t",
                            true);
                    for (int j = 0; cells.hasMoreTokens(); j++) {
                        String value = cells.nextToken();
                        if (!value.equals("\t")) {
                            setValueAt(value, startRow + i, startCol + j);
                            if (cells.hasMoreTokens()) {
                                cells.nextToken();
                                // When row ends with a delimiter
                                if (!cells.hasMoreTokens()) {
                                    setValueAt("", startRow + i,
                                            startCol + j + 1);
                                }
                            }
                        } else { // a empty cell
                            setValueAt("", startRow + i, startCol + j);
                            // When row ends with a delimiter
                            if (!cells.hasMoreTokens()) {
                                setValueAt("", startRow + i,
                                        startCol + j + 1);
                            }
                        }
                    }
                    if (rows.hasMoreTokens()) {
                        rows.nextToken();
                    }
                } else { // a empty row
                    m_table.setValueAt("", startRow + i, startCol);
                }
            }
            m_table.getSpreadsheetModel().addValues(
                    m_tempModel.getTableValues());
            m_tempModel.setData(new HashMap<Integer, ColProperty>(),
                    new int[0], new int[0], new String[0]);
            m_tempModel = null;
            m_table.getSelectionModel().setSelectionInterval(startRow,
                    m_endRow);
            m_table.getColumnModel().getSelectionModel()
                    .setSelectionInterval(startCol, m_endCol);
        } catch (Exception ex) {
            throw new IllegalArgumentException(ex.getMessage());
        }
    }

    private void setValueAt(final Object value, final int row, final int col) {
        if (row < m_table.getRowCount()
                && col < m_table.getColumnCount()) {
            m_endRow = Math.max(m_endRow, row);
            m_endCol = Math.max(m_endCol, col);
            m_tempModel.setValueAt(value, row, col);
        }
    }

}
