/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
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
