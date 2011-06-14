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
 */
package org.knime.base.node.preproc.columnheaderinsert;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionPanel;
import org.knime.core.node.util.DataValueColumnFilter;

/**
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
final class ColumnHeaderInsertNodeDialogPane extends NodeDialogPane {

    private final ColumnSelectionPanel m_lookupColPanel;
    private final ColumnSelectionPanel m_valuePanel;
    private final JCheckBox m_failIfNoMatchChecker;

    /**  */
    @SuppressWarnings("unchecked")
    public ColumnHeaderInsertNodeDialogPane() {
        m_lookupColPanel = new ColumnSelectionPanel((Border)null,
                new DataValueColumnFilter(StringValue.class), false, true);
        m_valuePanel = new ColumnSelectionPanel((Border)null,
                StringValue.class);
        m_failIfNoMatchChecker = new JCheckBox("Fail if no assignment "
                + "in dictionary table");
        addTab("Column Header Insert", initLayout());
    }

    /**
     * @return */
    private JPanel initLayout() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;

        panel.add(new JLabel("Lookup Column"), gbc);
        gbc.gridx += 1;
        panel.add(m_lookupColPanel, gbc);

        gbc.gridy += 1;
        gbc.gridx = 0;
        panel.add(new JLabel("Value Column"), gbc);
        gbc.gridx += 1;
        panel.add(m_valuePanel, gbc);

        gbc.gridx = 0;
        gbc.gridy += 1;
        gbc.gridwidth = 2;
        panel.add(m_failIfNoMatchChecker, gbc);
        return panel;
    }

    /** {@inheritDoc} */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        ColumnHeaderInsertConfig c = new ColumnHeaderInsertConfig();
        c.loadConfigurationInDialog(settings, specs[1]);
        m_lookupColPanel.update(specs[1], c.getLookupColumn());
        if (c.getLookupColumn() == null) {
            m_lookupColPanel.setRowIDSelected();
        }
        m_valuePanel.update(specs[1], c.getValueColumn());
        m_failIfNoMatchChecker.setSelected(c.isFailIfNoMatch());
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        ColumnHeaderInsertConfig c = new ColumnHeaderInsertConfig();
        if (m_lookupColPanel.rowIDSelected()) {
            c.setLookupColumn(null);
        } else {
            c.setLookupColumn(m_lookupColPanel.getSelectedColumn());
        }

        c.setValueColumn(m_valuePanel.getSelectedColumn());
        c.setFailIfNoMatch(m_failIfNoMatchChecker.isSelected());
        c.saveConfiguration(settings);
    }

}
