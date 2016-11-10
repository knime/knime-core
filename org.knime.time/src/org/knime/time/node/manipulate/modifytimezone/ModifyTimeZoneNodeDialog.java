/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *   Oct 28, 2016 (simon): created
 */
package org.knime.time.node.manipulate.modifytimezone;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Set;

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
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterPanel;
import org.knime.core.node.util.filter.column.DataTypeColumnFilter;

/**
 * The node dialog of the node which modifies a time zone.
 *
 * @author Simon Schmid, KNIME.com, Konstanz, Germany
 */
class ModifyTimeZoneNodeDialog extends NodeDialogPane {

    private final DataColumnSpecFilterPanel m_dialogCompColFilter;

    private final DialogComponentButtonGroup m_dialogCompReplaceOrAppend;

    private final DialogComponentString m_dialogCompSuffix;

    private final DialogComponentStringSelection m_dialogCompTimeZoneSelec;

    private final DialogComponentButtonGroup m_dialogCompModifySelect;

    private DataTableSpec m_spec;

    private boolean m_includeLocalDateTime;

    /**
     * Setting up all DialogComponents.
     */
    ModifyTimeZoneNodeDialog() {
        m_includeLocalDateTime = true;
        m_dialogCompColFilter = new DataColumnSpecFilterPanel();

        final SettingsModelString replaceOrAppendModel = ModifyTimeZoneNodeModel.createReplaceAppendStringBool();
        m_dialogCompReplaceOrAppend = new DialogComponentButtonGroup(replaceOrAppendModel, true, null,
            ModifyTimeZoneNodeModel.OPTION_APPEND, ModifyTimeZoneNodeModel.OPTION_REPLACE);

        final SettingsModelString suffixModel = ModifyTimeZoneNodeModel.createSuffixModel(replaceOrAppendModel);
        m_dialogCompSuffix = new DialogComponentString(suffixModel, "Suffix of appended columns: ");

        final SettingsModelString modifySelectModel = ModifyTimeZoneNodeModel.createModifySelectModel();
        m_dialogCompModifySelect =
            new DialogComponentButtonGroup(modifySelectModel, true, null, ModifyTimeZoneNodeModel.MODIFY_OPTION_SET,
                ModifyTimeZoneNodeModel.MODIFY_OPTION_SHIFT, ModifyTimeZoneNodeModel.MODIFY_OPTION_REMOVE);

        final SettingsModelString zoneSelectModel = ModifyTimeZoneNodeModel.createTimeZoneSelectModel();
        final Set<String> availableZoneIds = ZoneId.getAvailableZoneIds();
        final String[] availableZoneIdsArray = availableZoneIds.toArray(new String[availableZoneIds.size()]);
        Arrays.sort(availableZoneIdsArray);
        m_dialogCompTimeZoneSelec =
            new DialogComponentStringSelection(zoneSelectModel, "Time zone: ", availableZoneIdsArray);

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
         * add time zone selection
         */
        gbc.gridy++;
        final JPanel panelZoneSelec = new JPanel(new GridBagLayout());
        panelZoneSelec.setBorder(BorderFactory.createTitledBorder("Time Zone Selection"));
        final GridBagConstraints gbcZS = new GridBagConstraints();
        gbcZS.fill = GridBagConstraints.VERTICAL;
        gbcZS.gridx = 0;
        gbcZS.gridy = 0;
        gbcZS.anchor = GridBagConstraints.WEST;
        panelZoneSelec.add(m_dialogCompModifySelect.getComponentPanel(), gbcZS);
        gbcZS.gridx++;
        gbcZS.insets = new Insets(0, 30, 0, 0);
        gbcZS.weightx = 1;
        panelZoneSelec.add(m_dialogCompTimeZoneSelec.getComponentPanel(), gbcZS);
        panel.add(panelZoneSelec, gbc);

        /*
         * add tab
         */
        addTab("Options", panel);

        modifySelectModel.addChangeListener(e -> {
            boolean isSetTz = modifySelectModel.getStringValue().equals(ModifyTimeZoneNodeModel.MODIFY_OPTION_SET);
            boolean isRemoveTz =
                modifySelectModel.getStringValue().equals(ModifyTimeZoneNodeModel.MODIFY_OPTION_REMOVE);

            zoneSelectModel.setEnabled(!isRemoveTz);
            m_includeLocalDateTime = isSetTz;

            final DataTypeColumnFilter filter = m_includeLocalDateTime ? ModifyTimeZoneNodeModel.ZONED_AND_LOCAL_FILTER
                : ModifyTimeZoneNodeModel.ZONED_FILTER;
            final DataColumnSpecFilterConfiguration filterConfiguration =
                ModifyTimeZoneNodeModel.createDCFilterConfiguration(filter);
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
        boolean includeLocalDateTime = ((SettingsModelString)m_dialogCompModifySelect.getModel()).getStringValue()
            .equals(ModifyTimeZoneNodeModel.MODIFY_OPTION_SET);
        DataColumnSpecFilterConfiguration filterConfiguration =
            ModifyTimeZoneNodeModel.createDCFilterConfiguration(includeLocalDateTime
                ? ModifyTimeZoneNodeModel.ZONED_AND_LOCAL_FILTER : ModifyTimeZoneNodeModel.ZONED_FILTER);
        m_dialogCompColFilter.saveConfiguration(filterConfiguration);
        filterConfiguration.saveConfiguration(settings);
        m_dialogCompReplaceOrAppend.saveSettingsTo(settings);
        m_dialogCompSuffix.saveSettingsTo(settings);
        m_dialogCompTimeZoneSelec.saveSettingsTo(settings);
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
        m_dialogCompTimeZoneSelec.loadSettingsFrom(settings, specs);
        m_dialogCompModifySelect.loadSettingsFrom(settings, specs);
        m_includeLocalDateTime = ((SettingsModelString)m_dialogCompModifySelect.getModel()).getStringValue()
            .equals(ModifyTimeZoneNodeModel.MODIFY_OPTION_SET);
        final DataColumnSpecFilterConfiguration filterConfiguration =
            ModifyTimeZoneNodeModel.createDCFilterConfiguration(m_includeLocalDateTime
                ? ModifyTimeZoneNodeModel.ZONED_AND_LOCAL_FILTER : ModifyTimeZoneNodeModel.ZONED_FILTER);
        filterConfiguration.loadConfigurationInDialog(settings, specs[0]);
        m_dialogCompColFilter.loadConfiguration(filterConfiguration, specs[0]);
    }
}
