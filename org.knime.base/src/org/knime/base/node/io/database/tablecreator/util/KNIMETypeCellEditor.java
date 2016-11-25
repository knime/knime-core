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
 *   Jan 8, 2016 (budiyanto): created
 */
package org.knime.base.node.io.database.tablecreator.util;

import java.awt.Component;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;

import org.knime.core.data.DataType;

/**
 * Cell Editor to edit the Knime data type in the table
 *
 * @author Budi Yanto, KNIME.com
 */
class KNIMETypeCellEditor extends DefaultCellEditor {

    private static final long serialVersionUID = 1L;

    private final JComboBox<DataType> m_comboBox;

    private Collection<DataType> m_sortedDataTypes;

    private final Set<DataType> m_dataTypeHash;

    private DataType m_oldValue;

    /**
     * Creates a new instance of KnimeTypeCellEditor
     *
     * @param knimeTypes the knime data types used to initialize the cell editor
     */
    KNIMETypeCellEditor(final Collection<DataType> knimeTypes) {
        this(new JComboBox<DataType>(), knimeTypes);
    }

    private KNIMETypeCellEditor(final JComboBox<DataType> comboBox, final Collection<DataType> knimeTypes) {
        super(comboBox);
        m_comboBox = comboBox;
        m_dataTypeHash = new HashSet<>();
        m_sortedDataTypes = knimeTypes;
        m_comboBox.setRenderer(new KnimeTypeListCellRenderer());
    }

    void setKNIMETypes(final Collection<DataType> knimeTypes) {
        m_sortedDataTypes = knimeTypes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Component getTableCellEditorComponent(final JTable table, final Object value, final boolean isSelected,
        final int row, final int column) {
        m_oldValue = (DataType)value;

        m_comboBox.removeAllItems();
        for (DataType dt : m_sortedDataTypes) {
            if (dt.equals(value) || !m_dataTypeHash.contains(dt)) {
                m_comboBox.addItem(dt);
            }
        }
        return super.getTableCellEditorComponent(table, value, isSelected, row, column);
    }

    DataType getNextItem() {
        for (DataType dt : m_sortedDataTypes) {
            if (m_dataTypeHash.add(dt)) {
                return dt;
            }
        }
        return null;
    }

    boolean isItemAvailable() {
        return m_sortedDataTypes.size() == m_dataTypeHash.size();
    }

    void removeItemFromHash(final DataType item) {
        m_dataTypeHash.remove(item);
    }

    void removeAllFromHash() {
        m_dataTypeHash.clear();
    }

    void addItemToHash(final DataType item) {
        m_dataTypeHash.add(item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getCellEditorValue() {
        DataType newValue = (DataType)super.getCellEditorValue();
        if (!newValue.equals(m_oldValue)) {
            m_dataTypeHash.add(newValue);
            m_dataTypeHash.remove(m_oldValue);
        }

        return newValue;
    }

    /**
     * List cell renderer for the Knime data type
     *
     * @author Budi Yanto, KNIME.com
     */
    private class KnimeTypeListCellRenderer extends JLabel implements ListCellRenderer<DataType> {

        private static final long serialVersionUID = 1L;

        private KnimeTypeListCellRenderer() {
            setOpaque(true);
            setVerticalAlignment(CENTER);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Component getListCellRendererComponent(final JList<? extends DataType> list, final DataType value,
            final int index, final boolean isSelected, final boolean cellHasFocus) {

            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }
            setText(value.toString());
            setIcon(value.getIcon());

            return this;
        }
    }

}
