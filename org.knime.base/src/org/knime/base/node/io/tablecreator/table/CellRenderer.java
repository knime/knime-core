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
 *   20.08.2010 (hofer): created
 */
package org.knime.base.node.io.tablecreator.table;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;

import org.knime.base.node.io.filereader.ColProperty;
import org.knime.base.node.io.filereader.DataCellFactory;
import org.knime.core.data.DataCell;
import org.knime.core.data.DoubleValue;

/**
 * The cell renderer used for the spreadsheet table.
 *
 * @author Heiko Hofer
 */
class CellRenderer extends DefaultTableCellRenderer {
    private static final long serialVersionUID = 2151522400721057440L;

    private SpreadsheetTableModel m_tableModel;
    private Color m_notInOutputForeground;
    private Color m_notInOutputBackground;
    private Color m_notInOutputSelForeground;
    private Color m_notInOutputSelBackground;
    private boolean m_showOutputTable;



    /**
     * Creates a new instance.
     *
     * @param tableModel the {@link TableModel} of the spreadsheet
     */
    CellRenderer(final SpreadsheetTableModel tableModel) {
        m_tableModel = tableModel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Component getTableCellRendererComponent(final JTable table,
            final Object value, final boolean isSelected,
            final boolean hasFocus, final int row, final int column) {
        setForeground(table.getForeground());
        setBackground(table.getBackground());
        super.getTableCellRendererComponent(table, value, isSelected,
                hasFocus, row, column);
        if (m_showOutputTable && (!isInFilledArea(row, column)
                || isSkipped(column))) {
            if (isSelected) {
                setForeground(m_notInOutputSelForeground);
                setBackground(m_notInOutputSelBackground);
            } else {
                setForeground(m_notInOutputForeground);
                setBackground(m_notInOutputBackground);
            }
        }
        ColProperty colProperty =
            m_tableModel.getColumnProperties().get(column);
        String missingValuePattern = colProperty != null ?
            colProperty.getMissingValuePattern() : "";
        if (value instanceof Cell) {
            Cell cell = (Cell)value;
            if (cell.getValue() == null) {
                setBackground(reddishBackground());
                setForeground(table.getForeground());
            }
        } else { // value is an empty string
            if (null != colProperty && isInFilledArea(row, column)) {
                DataCellFactory cellFactory = new DataCellFactory();
                cellFactory.setMissingValuePattern(missingValuePattern);
                DataCell dataCell = cellFactory.createDataCellOfType(
                      colProperty.getColumnSpec().getType(), value.toString());
                if (null == dataCell) {
                    setBackground(reddishBackground());
                    setForeground(table.getForeground());
                }
            }
        }
        return this;
    }

    private Color reddishBackground() {
        Color b = getBackground();
        return new Color((b.getRed() + 255) / 2, b.getGreen() / 2,
                b.getBlue() / 2);
    }

    private boolean isInFilledArea(final int row, final int column) {
        int maxRow = m_tableModel.getMaxRow();
        int maxColumn = m_tableModel.getMaxColumn();
        return !(row >= maxRow || column >= maxColumn);
    }

    private boolean isSkipped(final int column) {
        boolean skipped = false;
        if (m_tableModel.getColumnProperties().containsKey(column)) {
            skipped = m_tableModel.getColumnProperties().get(column)
                                    .getSkipThisColumn();
        }
        return skipped;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setValue(final Object value) {
        if (value instanceof Cell) {
            Cell cell = (Cell)value;
            if (cell.getValue() != null && cell.getType().isCompatible(
                  DoubleValue.class)) {
                setHorizontalAlignment(JTextField.RIGHT);
            } else {
                setHorizontalAlignment(JTextField.LEFT);
            }
            setText(((Cell)value).getText());
        } else {
            setText((value == null) ? "" : value.toString());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateUI() {
        super.updateUI();
        applyDefaults();
    }

    /**
     * Overridden to change colors.
     */
    protected void applyDefaults() {
        Color selBackground = UIManager.getColor("Table.selectionBackground");
        m_notInOutputForeground = Color.gray;
        m_notInOutputBackground = Color.lightGray;
        m_notInOutputSelForeground = Color.darkGray;
        m_notInOutputSelBackground = selBackground.darker();
        m_showOutputTable = false;
    }

    /**
     * @param show
     */
    public void showOutputTable(final boolean show) {
        m_showOutputTable = show;
    }

}
