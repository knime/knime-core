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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   12.05.2010 (hofer): created
 */
package org.knime.base.node.viz.crosstable;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionComboxBox;
import org.knime.core.node.util.ColumnSelectionPanel;
import org.knime.core.node.util.DataValueColumnFilter;

/**
 * This is the dialog for the Crosstab node.
 *
 * @author Heiko Hofer
 */
public class CrosstabNodeDialog extends NodeDialogPane {
    private ColumnSelectionComboxBox m_rowVarColumn;
    private ColumnSelectionComboxBox m_colVarColumn;
    private ColumnSelectionPanel m_weightColumn;
    private JCheckBox m_enableHiliting;

    /**
     * Creates a new dialog.
     */
    public CrosstabNodeDialog() {
        super();

        JPanel settings = createSettingsPanel();
        addTab("Settings", settings);
    }

    @SuppressWarnings("unchecked")
    private JPanel createSettingsPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(8, 3, 3, 3);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.fill = GridBagConstraints.BOTH;

        p.add(new JLabel("Row variable:"), c);
        c.gridx++;
        c.weightx = 1;
        m_rowVarColumn = new ColumnSelectionComboxBox(
                StringValue.class, DoubleValue.class);
        m_rowVarColumn.setBorder(null);
        p.add(m_rowVarColumn, c);

        c.insets = new Insets(3, 3, 3, 3);
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        p.add(new JLabel("Column variable:"), c);
        c.gridx++;
        c.weightx = 1;
        m_colVarColumn = new ColumnSelectionComboxBox(
                StringValue.class, DoubleValue.class);
        m_colVarColumn.setBorder(null);
        p.add(m_colVarColumn, c);

        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        p.add(new JLabel("Weight column:"), c);
        c.gridx++;
        c.weightx = 1;
        m_weightColumn = new ColumnSelectionPanel((Border)null,
                new DataValueColumnFilter(DoubleValue.class),
                true);
        m_weightColumn.setBorder(null);
        p.add(m_weightColumn, c);

        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;
        m_enableHiliting = new JCheckBox("Enable hiliting");
        p.add(m_enableHiliting, c);


        c.gridy++;
        c.gridx = 0;
        c.weighty = 1;
        p.add(new JPanel(), c);

        return p;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        CrosstabNodeSettings s = new CrosstabNodeSettings();

        s.setRowVarColumn(m_rowVarColumn.getSelectedColumn());
        s.setColVarColumn(m_colVarColumn.getSelectedColumn());
        s.setWeightColumn(m_weightColumn.getSelectedColumn());
        s.setEnableHiliting(m_enableHiliting.isSelected());

        s.saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        CrosstabNodeSettings s = new CrosstabNodeSettings();
        s.loadSettingsDialog(settings, null);

        m_rowVarColumn.update(specs[0], s.getRowVarColumn());
        m_colVarColumn.update(specs[0], s.getColVarColumn());
        m_weightColumn.update(specs[0], s.getWeightColumn());
        m_enableHiliting.setSelected(s.getEnableHiliting());
    }

}
