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
 * -------------------------------------------------------------------
 *
 * History
 *    27.08.2008 (Tobias Koetter): created
 */

package org.knime.base.node.preproc.groupby.dialogutil;

import org.knime.core.data.DataColumnSpec;

import org.knime.base.node.preproc.groupby.aggregation.AggregationMethod;

import java.awt.Component;

import javax.swing.DefaultCellEditor;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableModel;
import javax.swing.tree.DefaultTreeCellEditor;


/**
 * Extends the {@link DefaultTreeCellEditor} class to provide the
 * {@link AggregationMethodComboBox} as cell editor. It passes the
 * {@link DataColumnSpec} of the selected method to the combo box to
 * display only compatible aggregation methods.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class AggregationMethodTableCellEditor extends DefaultCellEditor
implements TableCellEditor {

    private static final long serialVersionUID = 1415862346615703238L;

    private final TableModel m_model;


    /**Constructor for class AggregationMethodTableCellEditor.
     * @param model the {@link TableModel} to get the type of the column
     * that should be changed
     */
    public AggregationMethodTableCellEditor(final TableModel model) {
        super(new AggregationMethodComboBox());
        m_model = model;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Component getTableCellEditorComponent(final JTable table,
            final Object value, final boolean isSelected, final int row,
            final int column) {
        final Object valueAt = m_model.getValueAt(row, 0);
        final Object methodVal = m_model.getValueAt(row, 1);
        if (valueAt instanceof DataColumnSpec) {
            final AggregationMethod method;
                if (methodVal instanceof AggregationMethod) {
                    method = (AggregationMethod)methodVal;
                } else {
                    method = null;
                }
            final DataColumnSpec spec = (DataColumnSpec)valueAt;
            getBox().update(spec, method);
        }
        return super.getTableCellEditorComponent(table, valueAt,
                isSelected, row, column);
    }

    /**
     * @return the {@link AggregationMethodComboBox}
     */
    AggregationMethodComboBox getBox() {
        return (AggregationMethodComboBox)getComponent();
    }
}
