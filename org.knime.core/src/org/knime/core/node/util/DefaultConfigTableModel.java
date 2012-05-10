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
 *   11.01.2012 (hofer): created
 */
package org.knime.core.node.util;

import java.util.Arrays;
import java.util.Vector;

import javax.swing.table.DefaultTableModel;


/**
 * Default implementation of {@link ConfigTableModel}.
 *
 * @author Heiko Hofer
 * @since 2.6
 */
@SuppressWarnings("serial")
public class DefaultConfigTableModel extends DefaultTableModel
        implements ConfigTableModel {
    private String[] m_columns;

    /**
     * Create a model with the given column names.
     * @param columns the column names.
     */
    public DefaultConfigTableModel(final String[] columns) {
        super(columns, 0);
        m_columns = columns;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addRow() {
        super.setRowCount(super.getRowCount() + 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeRows(final int[] rows) {
        Arrays.sort(rows);
        for (int i = rows.length - 1; i >= 0; i--) {
            super.removeRow(rows[i]);
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public ConfigTableModel cloneModel() {
        DefaultConfigTableModel clone = new DefaultConfigTableModel(m_columns);
        clone.setRowCount(this.getRowCount());
        for (int r = 0; r < getRowCount(); r++) {
            Vector rowVector = (Vector)dataVector.elementAt(r);
            Vector destVector = (Vector)clone.dataVector.elementAt(r);
            for (int c = 0; c < getColumnCount(); c++) {
                destVector.set(c, rowVector.get(c));
            }
        }
        return clone;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void setTableData(final ConfigTableModel tempModel) {
        DefaultConfigTableModel temp = (DefaultConfigTableModel)tempModel;
        setRowCount(temp.getRowCount());
        for (int r = 0; r < getRowCount(); r++) {
            Vector rowVector = (Vector)temp.dataVector.elementAt(r);
            Vector destVector = (Vector)dataVector.elementAt(r);
            for (int c = 0; c < getColumnCount(); c++) {
                destVector.set(c, rowVector.get(c));
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        setRowCount(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCellEditable(final int rowIndex, final int columnIndex) {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getColumnName(final int column) {
        return m_columns[column];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setColumnName(final int column, final String name) {
        m_columns[column] = name;
        fireTableStructureChanged();
    }



}
