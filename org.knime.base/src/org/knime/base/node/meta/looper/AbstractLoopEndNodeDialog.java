/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 */
package org.knime.base.node.meta.looper;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.Enumeration;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.knime.base.node.meta.looper.AbstractLoopEndNodeSettings.RowKeyPolicy;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;

/**
 * This is the dialog for the loop end node.
 *
 * @author Thorsten Meinl, University of Konstanz
 * @since 2.9
 * @param <T> type of settings model
 */
public abstract class AbstractLoopEndNodeDialog<T extends AbstractLoopEndNodeSettings> extends NodeDialogPane {

    private final JCheckBox m_addIterationColumn = new JCheckBox("Add iteration column");

    private final ButtonGroup m_rowKeyPolicy = new ButtonGroup();

    private final T m_settings;

    private GridBagConstraints m_gbc;

    private JPanel m_panel;

    /**
     * Creates a new dialog.
     * @param settings a new settings object
     */
    public AbstractLoopEndNodeDialog(final T settings) {
        m_settings = settings;
        m_panel = new JPanel(new GridBagLayout());

        m_gbc = new GridBagConstraints();

        m_gbc.gridx = 0;
        m_gbc.gridy = 0;
        m_gbc.anchor = GridBagConstraints.WEST;
        m_gbc.insets = new Insets(0, 0, 5, 0);

        JPanel rkPolicyPanel = new JPanel(new GridLayout(RowKeyPolicy.values().length, 1));
        rkPolicyPanel.setBorder(BorderFactory.createTitledBorder("Row key policy"));
        for (RowKeyPolicy p : RowKeyPolicy.values()) {
            JRadioButton rButton = new JRadioButton(p.label());
            rButton.setActionCommand(p.name());
            m_rowKeyPolicy.add(rButton);
            rkPolicyPanel.add(rButton);
        }
        m_panel.add(rkPolicyPanel, m_gbc);
        m_gbc.gridy++;

        m_panel.add(m_addIterationColumn, m_gbc);

        addTab("Standard settings", m_panel);
    }

    /**
     * Adds the given component to the panel.
     *
     * @param component The component to add
     * @since 2.9
     */
    protected void addComponent(final Component component) {
        m_gbc.gridy++;
        m_panel.add(component, m_gbc);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_settings.addIterationColumn(m_addIterationColumn.isSelected());
        m_settings.rowKeyPolicy(RowKeyPolicy.valueOf(m_rowKeyPolicy.getSelection().getActionCommand()));
        addToSettings(m_settings);
        m_settings.saveSettings(settings);
    }

    /**
     * Enables subclasses to add there settings to the settings object.
     *
     * @param settings The settings object
     */
    protected abstract void addToSettings(final T settings);

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        m_settings.loadSettings(settings);
        m_addIterationColumn.setSelected(m_settings.addIterationColumn());
        RowKeyPolicy p = m_settings.rowKeyPolicy();
        for (Enumeration<AbstractButton> e = m_rowKeyPolicy.getElements(); e.hasMoreElements();) {
            AbstractButton b = e.nextElement();
            b.setSelected(b.getActionCommand().equals(p.name()));
        }
        loadFromSettings(m_settings);
    }

    /**
     * Enables subclasses to load there settings from the settings object.
     *
     * @param settings The settings object
     */
    protected abstract void loadFromSettings(final T settings);

}
