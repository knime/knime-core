/* Created on 22.01.2007 10:04:59 by thor
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
        @Override
        public int getColumnCount() {
            return 2 + m_model.getDegree();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getRowCount() {
            if (m_model == null) { return 0; }
            if (m_model.getColumnNames() == null) { return 0; }
            return m_model.getColumnNames().length;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object getValueAt(final int rowIndex, final int columnIndex) {
            if (columnIndex == 0) {
                return m_model.getColumnNames()[rowIndex];
            } else  {
                return FORMATTER.format(m_model.getBetas()
                        [rowIndex * m_model.getDegree() + (columnIndex - 1)]);
            }
        }

        @Override
        public String getColumnName(final int column) {
            if (column == 0) {
                return "Attribute name";
            } else if (column == 1) {
                return "Intercept";
            } else {
                return "Coefficient (x^" + (column - 1) + ")";
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
        m_tableModel.fireTableStructureChanged();
        m_squaredError.setText("  Squared error (per row):  "
                + m_model.getSquaredError());

        for (int i = 0; i < m_tableModel.getColumnCount(); i++) {
            m_table.getTableHeader().getColumnModel().getColumn(i)
                .setMinWidth(110);
        }
    }
}
