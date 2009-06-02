/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   Jun 1, 2009 (wiswedel): created
 */
package org.knime.core.node.workflow;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import org.knime.core.node.util.ScopeVariableTableCellRenderer;

/**
 * View that displays a given {@link ScopeObjectStack} in a table.
 * This view is shown as a separate tab in each node's outport views.
 * @author Bernd Wiswedel, University of Konstanz
 */
final class ScopeObjectStackView extends JPanel {
    
    /** Table displaying name, value and owner of a scope variable. */
    private final JTable m_table;
    
    /** Creates new empty view. */
    public ScopeObjectStackView() {
        super(new BorderLayout());
        m_table = new JTable(new DefaultTableModel() {
            /** {@inheritDoc} */
            @Override
            public Class<?> getColumnClass(final int columnIndex) {
                switch (columnIndex) {
                case 2:
                    return ScopeVariable.class;
                default:
                    return super.getColumnClass(columnIndex);
                }
            }
            /** {@inheritDoc} */
            @Override
            public boolean isCellEditable(final int row, final int column) {
                return false;
            }
        });
        m_table.setDefaultRenderer(ScopeVariable.class, 
                new ScopeVariableTableCellRenderer());
        add(new JScrollPane(m_table), BorderLayout.CENTER);
    }
    
    /** Updates the view to display the given stack.
     * @param stack Whose values are to be displayed. */
    public void update(final ScopeObjectStack stack) {
        Object[][] values = new Object[stack.size()][];
        int loopCount = 0;
        int counter = 0;
        for (ScopeObject s : stack) {
            Object[] obj = new Object[4];
            obj[0] = Integer.valueOf(counter);
            obj[1] = s.getOwner();
            if (s instanceof ScopeVariable) {
                ScopeVariable v = (ScopeVariable)s;
                obj[2] = s;
                Object o;
                switch (v.getType()) {
                case DOUBLE:
                    o = Double.valueOf(v.getDoubleValue());
                    break;
                case INTEGER:
                    o = Integer.valueOf(v.getIntValue());
                    break;
                case STRING:
                    o = v.getStringValue();
                    break;
                default:
                    o = "Unknown Type: " + v.getType();
                }
                obj[3] = o;
            } else {
                obj[2] = "Loop (" + (loopCount++) + ")";
                obj[3] = null;
            }
            values[counter++] = obj;
        }
        String[] colNames = new String[]{"Index", "Owner ID", "Type", "Value"};
        DefaultTableModel model = (DefaultTableModel)m_table.getModel();
        model.setDataVector(values, colNames);
    }

}
