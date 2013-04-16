/*
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   11.01.2012 (hofer): created
 */
package org.knime.core.node.util;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;


/**
 * A Panel with a table in the center and buttons at the right to add and
 * remove rows.
 *
 * @author Heiko Hofer
 * @since 2.6
 */
@SuppressWarnings("serial")
public class ConfigTablePanel extends JPanel {
    private final ConfigTableModel m_model;
    private final JTable m_table;
    private JButton m_addButton;
    private JButton m_removeButton;

    /**
     * Create a new component.
     * @param model the model used to initialize the table
     */
    public ConfigTablePanel(final ConfigTableModel model) {
        super(new GridBagLayout());
        m_model = model;

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(0, 0, 0, 5);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.weightx = 1;
        c.weighty = 1;

        m_table = new JTable(m_model);
        m_table.setSelectionMode(
                ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        // Increase row height
        m_table.setRowHeight(m_table.getRowHeight() + 3);
        m_table.getTableHeader().setPreferredSize(new Dimension(
                m_table.getTableHeader().getPreferredSize().width,
                m_table.getRowHeight()));

        Color gridColor = m_table.getGridColor();
        // brighten the grid color
        m_table.setGridColor(new Color((gridColor.getRed() + 255) / 2
            , (gridColor.getGreen() + 255) / 2
            , (gridColor.getBlue() + 255) / 2));

        JScrollPane scroll = new JScrollPane(m_table);
        add(scroll, c);

        c.gridx++;
        c.weightx = 0;
        c.insets = new Insets(0, 0, 0, 0);
        add(createButtonPanel(), c);
    }


    /** The button panel at the right. */
    private JPanel createButtonPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(0, 0, 5, 0);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.weightx = 1;
        c.weighty = 0;

        m_addButton = new JButton("Add");
        m_addButton.addActionListener(createAddButtonListener());
        p.add(m_addButton, c);

        c.gridy++;
        c.insets = new Insets(0, 0, 0, 0);
        m_removeButton = new JButton("Remove");
        m_removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                int[] rows = m_table.getSelectedRows();
                if (rows.length > 0) {
                    commitOpenEditor();
                    m_model.removeRows(rows);
                }
            }
        });
        p.add(m_removeButton, c);

        c.gridy++;
        c.weighty = 1;
        p.add(new JPanel(), c);
        return p;
    }

    /**
     * Create action listener reaction on the Add-Button. Default is to add an
     * empty row.
     * @return the action listener for the Add-Button
     */
    protected ActionListener createAddButtonListener() {
        return new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                m_model.addRow();
            }
        };
    }


    /**
     * Access to underlying table.
     * @return the underlying table
     */
    public JTable getTable() {
        return m_table;
    }

    /**
     * Access to the model.
     * @return the table model.
     */
    protected ConfigTableModel getModel() {
        return m_model;
    }

    /**
     * Commits pending edits.
     */
    protected void commitOpenEditor() {
        if (null != m_table.getCellEditor()) {
            m_table.getCellEditor().stopCellEditing();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEnabled(final boolean enabled) {
        super.setEnabled(enabled);
        m_table.setEnabled(enabled);
        m_addButton.setEnabled(enabled);
        m_removeButton.setEnabled(enabled);
    }

}
