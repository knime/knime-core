/* 
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
 * -------------------------------------------------------------------
 * 
 * History
 *   24.05.2006 (gabriel): created
 */
package org.knime.base.node.viz.property.color;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.DataColumnSpecListCellRenderer;

/**
 * Dialog to select column to apply colors to.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class ColorAppenderNodeDialogPane extends NodeDialogPane {
    /**
     * Columns selection to apply colors.
     */
    private final JComboBox m_columns;

    /**
     * Create color appender dialog pane.
     */
    public ColorAppenderNodeDialogPane() {
        super();
        m_columns = new JComboBox();
        m_columns.setPreferredSize(new Dimension(150, 20));
        m_columns.setRenderer(new DataColumnSpecListCellRenderer());
        JPanel p = new JPanel(new FlowLayout());
        p.setBorder(BorderFactory.createTitledBorder(" Append colors to "));
        p.add(m_columns, BorderLayout.CENTER);
        super.addTab("Column Selection", p);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        m_columns.removeAllItems();
        if (specs[0].getNumColumns() == 0) {
            throw new NotConfigurableException("Please provide table with"
                    + " at least one column to select.");
        }
        for (int i = 0; i < specs[0].getNumColumns(); i++) {
            DataColumnSpec cspec = specs[0].getColumnSpec(i);
            m_columns.addItem(cspec);
        }
        String selColumn = settings.getString(
                ColorNodeModel.SELECTED_COLUMN, null);
        if (selColumn != null) {
            DataColumnSpec cspec = specs[0].getColumnSpec(selColumn);
            m_columns.setSelectedItem(cspec);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        Object o = m_columns.getSelectedItem();
        if (o == null) {
            throw new InvalidSettingsException("No column selected.");
        }
        settings.addString(ColorNodeModel.SELECTED_COLUMN,
                ((DataColumnSpec)o).getName());
    }
}
