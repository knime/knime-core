/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *   Oct 28, 2016 (simon): created
 */
package org.knime.time.node.manipulate.modifydate;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterPanel;
import org.knime.core.node.util.filter.column.DataTypeColumnFilter;
import org.knime.time.util.DialogComponentDateTimeSelection;
import org.knime.time.util.SettingsModelDateTime;
import org.knime.time.util.DialogComponentDateTimeSelection.DisplayOption;

/**
 * The node dialog of the node which modifies date.
 *
 * @author Simon Schmid, KNIME.com, Konstanz, Germany
 */
final class ModifyDateNodeDialog extends NodeDialogPane {
    private final DataColumnSpecFilterPanel m_dialogCompColFilter;

    private final DialogComponentButtonGroup m_dialogCompReplaceOrAppend;

    private final DialogComponentString m_dialogCompSuffix;

    private final DialogComponentDateTimeSelection m_dialogCompDate;

    private final DialogComponentDateTimeSelection m_dialogCompTimeZone;

    private final DialogComponentButtonGroup m_dialogCompModifySelect;

    private DataTableSpec m_spec;

    private boolean m_filterOnlyLocalDate;

    /**
     * Setting up all DialogComponents.
     */
    public ModifyDateNodeDialog() {
        m_filterOnlyLocalDate = true;
        m_dialogCompColFilter = new DataColumnSpecFilterPanel();

        final SettingsModelString replaceOrAppendModel = ModifyDateNodeModel.createReplaceAppendStringBool();
        m_dialogCompReplaceOrAppend = new DialogComponentButtonGroup(replaceOrAppendModel, true, null,
            ModifyDateNodeModel.OPTION_APPEND, ModifyDateNodeModel.OPTION_REPLACE);

        final SettingsModelString suffixModel = ModifyDateNodeModel.createSuffixModel(replaceOrAppendModel);
        m_dialogCompSuffix = new DialogComponentString(suffixModel, "Suffix of appended columns: ");

        final SettingsModelDateTime dateModel = ModifyDateNodeModel.createDateModel();
        m_dialogCompDate = new DialogComponentDateTimeSelection(dateModel, null, DisplayOption.SHOW_DATE_ONLY);

        final SettingsModelDateTime timeZoneModel = ModifyDateNodeModel.createTimeZoneModel();
        m_dialogCompTimeZone =
            new DialogComponentDateTimeSelection(timeZoneModel, null, DisplayOption.SHOW_TIMEZONE_ONLY_OPTIONAL);


        final SettingsModelString modifySelectModel = ModifyDateNodeModel.createModifySelectModel();
        m_dialogCompModifySelect =
            new DialogComponentButtonGroup(modifySelectModel, true, null, ModifyDateNodeModel.MODIFY_OPTION_APPEND,
                ModifyDateNodeModel.MODIFY_OPTION_CHANGE, ModifyDateNodeModel.MODIFY_OPTION_REMOVE);

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
        gbc.weighty = 1;

        /*
         * add column filter
         */
        panel.add(m_dialogCompColFilter, gbc);

        /*
         * add replace/append selection
         */
        gbc.gridy++;
        gbc.weighty = 0;
        final JPanel panelReplace = new JPanel(new GridBagLayout());
        panelReplace.setBorder(BorderFactory.createTitledBorder("Replace/Append Selection"));
        final GridBagConstraints gbcReplaceAppend = new GridBagConstraints();
        // add check box
        gbcReplaceAppend.fill = GridBagConstraints.VERTICAL;
        gbcReplaceAppend.gridx = 0;
        gbcReplaceAppend.gridy = 0;
        gbcReplaceAppend.weighty = 0;
        gbcReplaceAppend.anchor = GridBagConstraints.WEST;
        panelReplace.add(m_dialogCompReplaceOrAppend.getComponentPanel(), gbcReplaceAppend);
        // add suffix text field
        gbcReplaceAppend.gridx++;
        gbcReplaceAppend.weightx = 1;
        gbcReplaceAppend.insets = new Insets(2, 10, 0, 0);
        panelReplace.add(m_dialogCompSuffix.getComponentPanel(), gbcReplaceAppend);

        panel.add(panelReplace, gbc);

        /*
         * add date and operation settings
         */
        gbc.gridy++;
        final JPanel panelSelection = new JPanel(new GridBagLayout());
        panelSelection.setBorder(BorderFactory.createTitledBorder("Date Selection"));
        final GridBagConstraints gbcSelection = new GridBagConstraints();
        gbcSelection.fill = GridBagConstraints.VERTICAL;
        gbcSelection.gridx = 0;
        gbcSelection.gridy = 0;
        gbcSelection.weighty = 0;
        gbcSelection.anchor = GridBagConstraints.WEST;
        /*
         * build operation selection panel
         */
        panelSelection.add(m_dialogCompModifySelect.getComponentPanel(), gbcSelection);
        /*
         * build date panel
         */
        final JPanel panelDate = new JPanel(new GridBagLayout());
        final GridBagConstraints gbcDate = new GridBagConstraints();
        gbcDate.fill = GridBagConstraints.VERTICAL;
        gbcDate.gridx = 0;
        gbcDate.gridy = 0;
        gbcDate.weightx = 1;
        gbcDate.anchor = GridBagConstraints.WEST;
        panelDate.add(m_dialogCompDate.getComponentPanel(), gbcDate);
        // add month
        gbcDate.gridy++;
        panelDate.add(m_dialogCompTimeZone.getComponentPanel(), gbcDate);

        gbcSelection.gridx++;
        gbcSelection.insets = new Insets(0, 50, 0, 0);
        gbcSelection.gridwidth = 2;
        gbcSelection.weightx = 1;
        panelSelection.add(panelDate, gbcSelection);
        panel.add(panelSelection, gbc);

        /*
         * add tab
         */
        addTab("Options", panel);

        /*
         * Change listeners
         */
        // change listener for column filter
        modifySelectModel.addChangeListener(e -> {
            boolean isAppendOption =
                modifySelectModel.getStringValue().equals(ModifyDateNodeModel.MODIFY_OPTION_APPEND);
            boolean isRemoveOption =
                modifySelectModel.getStringValue().equals(ModifyDateNodeModel.MODIFY_OPTION_REMOVE);

            dateModel.setEnabled(!isRemoveOption);
            timeZoneModel.setEnabled(isAppendOption);

            m_filterOnlyLocalDate = isAppendOption;

            final DataTypeColumnFilter filter =
                m_filterOnlyLocalDate ? ModifyDateNodeModel.LOCAL_TIME_FILTER : ModifyDateNodeModel.DATE_TIME_FILTER;
            final DataColumnSpecFilterConfiguration filterConfiguration =
                    ModifyDateNodeModel.createDCFilterConfiguration(filter);
            m_dialogCompColFilter.saveConfiguration(filterConfiguration);
            final DataColumnSpecFilterConfiguration tempConfiguration =
                new DataColumnSpecFilterConfiguration(filterConfiguration.getConfigRootName());
            final NodeSettings tempSettings = new NodeSettings("tempSettings");
            tempConfiguration.saveConfiguration(tempSettings);
            filterConfiguration.loadConfigurationInDialog(tempSettings, m_spec);
            m_dialogCompColFilter.loadConfiguration(filterConfiguration, m_spec);
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        boolean filterOnlyLocalTime = ((SettingsModelString)m_dialogCompModifySelect.getModel()).getStringValue()
            .equals(ModifyDateNodeModel.MODIFY_OPTION_APPEND);
        DataColumnSpecFilterConfiguration filterConfiguration = ModifyDateNodeModel.createDCFilterConfiguration(
            filterOnlyLocalTime ? ModifyDateNodeModel.LOCAL_TIME_FILTER : ModifyDateNodeModel.DATE_TIME_FILTER);
        m_dialogCompColFilter.saveConfiguration(filterConfiguration);
        filterConfiguration.saveConfiguration(settings);
        m_dialogCompReplaceOrAppend.saveSettingsTo(settings);
        m_dialogCompSuffix.saveSettingsTo(settings);
        m_dialogCompDate.saveSettingsTo(settings);
        m_dialogCompTimeZone.saveSettingsTo(settings);
        m_dialogCompModifySelect.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {
        m_spec = specs[0];
        m_dialogCompReplaceOrAppend.loadSettingsFrom(settings, specs);
        m_dialogCompSuffix.loadSettingsFrom(settings, specs);
        m_dialogCompDate.loadSettingsFrom(settings, specs);
        m_dialogCompTimeZone.loadSettingsFrom(settings, specs);
        m_dialogCompModifySelect.loadSettingsFrom(settings, specs);
        m_filterOnlyLocalDate = ((SettingsModelString)m_dialogCompModifySelect.getModel()).getStringValue()
            .equals(ModifyDateNodeModel.MODIFY_OPTION_APPEND);
        final DataColumnSpecFilterConfiguration filterConfiguration = ModifyDateNodeModel.createDCFilterConfiguration(
            m_filterOnlyLocalDate ? ModifyDateNodeModel.LOCAL_TIME_FILTER : ModifyDateNodeModel.DATE_TIME_FILTER);
        filterConfiguration.loadConfigurationInDialog(settings, specs[0]);
        m_dialogCompColFilter.loadConfiguration(filterConfiguration, specs[0]);
    }

}
