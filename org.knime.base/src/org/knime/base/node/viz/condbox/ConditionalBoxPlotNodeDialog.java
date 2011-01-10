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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   Feb 25, 2008 (sellien): created
 */
package org.knime.base.node.viz.condbox;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.NominalValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionComboxBox;

/**
 * This class provides the configuration dialog for the conditional box plot
 * node.
 *
 * @author Stephan Sellien, University of Konstanz
 *
 */
public class ConditionalBoxPlotNodeDialog extends NodeDialogPane {
    private final ConditionalBoxPlotSettings m_settings =
            new ConditionalBoxPlotSettings();

    @SuppressWarnings("unchecked")
    private ColumnSelectionComboxBox m_nominalColumn =
            new ColumnSelectionComboxBox((Border)null, NominalValue.class);

    @SuppressWarnings("unchecked")
    private ColumnSelectionComboxBox m_numericColumn =
            new ColumnSelectionComboxBox((Border)null, DoubleValue.class);

    private JCheckBox m_showMissingValues =
            new JCheckBox("Show missing values", false);

    /**
     * Creates the configuration dialog.
     */
    public ConditionalBoxPlotNodeDialog() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(2, 2, 2, 2);
        c.gridx = 0;
        c.gridy = 0;
        p.add(new JLabel("Nominal column   "), c);
        c.gridx++;
        p.add(m_nominalColumn, c);

        c.gridx = 0;
        c.gridy = 1;
        p.add(new JLabel("Numeric column   "), c);
        c.gridx++;
        p.add(m_numericColumn, c);

        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 2;
        p.add(m_showMissingValues, c);
        c.gridwidth = 1;

        addTab("Standard Settings", p);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {

        try {
            m_settings.loadSettings(settings);
        } catch (InvalidSettingsException ex) {
            // ignore it and use defaults
        }

        m_nominalColumn.update(specs[0], m_settings.nominalColumn());
        if (!m_nominalColumn.getSelectedColumn().equals(
                m_settings.nominalColumn())) {
            m_nominalColumn.setSelectedIndex(0);
        }
        m_numericColumn.update(specs[0], m_settings.numericColumn());
        if (!m_numericColumn.getSelectedColumn().equals(
                m_settings.numericColumn())) {
            m_numericColumn.setSelectedIndex(0);
        }
        m_showMissingValues.setSelected(m_settings.showMissingValues());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        m_settings.nominalColumn(m_nominalColumn.getSelectedColumn());
        m_settings.numericColumn(m_numericColumn.getSelectedColumn());
        m_settings.showMissingValues(m_showMissingValues.isSelected());
        m_settings.saveSettings(settings);
    }

}
