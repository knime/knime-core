/*
 * ------------------------------------------------------------------
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
