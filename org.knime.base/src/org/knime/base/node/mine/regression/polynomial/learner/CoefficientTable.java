/* Created on 22.01.2007 10:04:59 by thor
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 * ------------------------------------------------------------------- * 
 */
package org.knime.base.node.mine.regression.polynomial.learner;

import java.awt.BorderLayout;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

/**
 * This is the view that shows the coefficients in a table and the squared
 * error per row in a line below the table.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public class CoefficientTable extends JPanel {    
    private PolyRegLearnerNodeModel m_model;    
    private static final NumberFormat FORMATTER = new DecimalFormat("0.0000");
    private final AbstractTableModel m_tableModel = new AbstractTableModel() {
        /**
         * {@inheritDoc}
         */
        public int getColumnCount() {
            return 1 + m_model.getDegree();
        }

        /**
         * {@inheritDoc}
         */
        public int getRowCount() {
            if (m_model == null) { return 0; }
            if (m_model.getColumnNames() == null) { return 0; }
            return m_model.getColumnNames().length;
        }

        /**
         * {@inheritDoc}
         */
        public Object getValueAt(final int rowIndex, final int columnIndex) {
            if (columnIndex == 0) {
                return m_model.getColumnNames()[rowIndex];
            } else  {
                return FORMATTER.format(m_model.getBetas()
                        [rowIndex * m_model.getDegree() + columnIndex]);
            }
        }

        @Override
        public String getColumnName(final int column) {
            if (column == 0) {                
                return "Attribute name";
            } else {                
                return "Coefficient (x^" + column + ")";
            }
        }
    };
    
    private final JTable m_table;
    private final JLabel m_squaredError = new JLabel();
    

    /**
     * Creates a new coefficient table.
     * 
     * @param nodeModel the node model
     */
    public CoefficientTable(final PolyRegLearnerNodeModel nodeModel) {
        super(new BorderLayout());
        m_model = nodeModel;
        m_table = new JTable(m_tableModel);
        
        m_table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        add(new JScrollPane(m_table), BorderLayout.CENTER);        
        add(m_squaredError, BorderLayout.SOUTH);        
    }
    
    /**
     * Updates the table.
     */
    public void update() {
        m_tableModel.fireTableDataChanged();
        m_squaredError.setText("  Squared error (per row):  " 
                + m_model.getSquaredError());
        
        for (int i = 0; i < m_tableModel.getColumnCount(); i++) {
            m_table.getTableHeader().getColumnModel().getColumn(i)
                .setMinWidth(110);
        }
    }
}
