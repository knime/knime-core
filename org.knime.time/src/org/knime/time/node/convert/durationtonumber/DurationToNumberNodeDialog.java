/*
 * ------------------------------------------------------------------------
 *
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
 * History
 *   Feb 9, 2017 (simon): created
 */
package org.knime.time.node.convert.durationtonumber;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.time.duration.DurationValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.time.util.Granularity;

/**
 * The node dialog of the node which converts durations to numbers.
 *
 * @author Simon Schmid, KNIME.com, Konstanz, Germany
 */
final class DurationToNumberNodeDialog extends NodeDialogPane {

    private final DialogComponentColumnNameSelection m_dialogCompColSelect;

    private final DialogComponentStringSelection m_dialogCompDurationFieldSelect;

    private final DialogComponentButtonGroup m_dialogCompTypeSelect;

    /**
     * Creates a new dialog.
     */
    @SuppressWarnings("unchecked")
    public DurationToNumberNodeDialog() {
        m_dialogCompColSelect = new DialogComponentColumnNameSelection(DurationToNumberNodeModel.createColSelectModel(),
            "Duration column", 0, true, DurationValue.class);

        m_dialogCompDurationFieldSelect =
            new DialogComponentStringSelection(DurationToNumberNodeModel.createDurationFieldSelectionModel(),
                "Granularity",
                new String[]{Granularity.HOUR.toString(), Granularity.MINUTE.toString(), Granularity.SECOND.toString(),
                    Granularity.MILLISECOND.toString(), Granularity.MICROSECOND.toString(),
                    Granularity.NANOSECOND.toString()});

        m_dialogCompTypeSelect = new DialogComponentButtonGroup(DurationToNumberNodeModel.createTypeSelectionModel(),
            null, true, DataTypeMode.values());

        /*
         * create panel with gbc
         */
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;

        /*
         * add column selection
         */
        final JPanel panelColSelect = new JPanel(new GridBagLayout());
        panelColSelect.setBorder(BorderFactory.createTitledBorder("Column Selection"));
        final GridBagConstraints gbcColSelect = new GridBagConstraints();
        gbcColSelect.insets = new Insets(5, 5, 5, 5);
        gbcColSelect.fill = GridBagConstraints.VERTICAL;
        gbcColSelect.gridx = 0;
        gbcColSelect.gridy = 0;
        gbcColSelect.anchor = GridBagConstraints.WEST;
        gbcColSelect.weightx = 1;
        panelColSelect.add(m_dialogCompColSelect.getComponentPanel(), gbcColSelect);
        panel.add(panelColSelect, gbc);

        /*
         * add field selection and output type selection
         */
        gbc.gridy++;
        gbc.weighty = 1;

        final JPanel panelFieldSelection = new JPanel(new GridBagLayout());
        panelFieldSelection.setBorder(BorderFactory.createTitledBorder("Conversion Settings"));
        panel.add(panelFieldSelection, gbc);

        final GridBagConstraints gbcFieldSelection = new GridBagConstraints();
        gbcFieldSelection.fill = GridBagConstraints.VERTICAL;
        gbcFieldSelection.insets = new Insets(5, 5, 5, 5);
        gbcFieldSelection.gridx = 0;
        gbcFieldSelection.gridy = 0;
        gbcFieldSelection.weightx = 1;
        gbcFieldSelection.anchor = GridBagConstraints.WEST;
        panelFieldSelection.add(m_dialogCompDurationFieldSelect.getComponentPanel(), gbcFieldSelection);
        gbcFieldSelection.weighty = 1;
        gbcFieldSelection.gridy++;
        gbcFieldSelection.insets = new Insets(5, 0, 5, 5);
        panelFieldSelection.add(m_dialogCompTypeSelect.getComponentPanel(), gbcFieldSelection);

        /*
         * add tab
         */
        addTab("Options", panel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_dialogCompColSelect.saveSettingsTo(settings);
        m_dialogCompDurationFieldSelect.saveSettingsTo(settings);
        m_dialogCompTypeSelect.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {
        m_dialogCompColSelect.loadSettingsFrom(settings, specs);
        m_dialogCompDurationFieldSelect.loadSettingsFrom(settings, specs);
        m_dialogCompTypeSelect.loadSettingsFrom(settings, specs);
    }
}
