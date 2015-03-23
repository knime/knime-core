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
 *   16.02.2015 (tibuch): created
 */
package org.knime.base.node.preproc.draganddroppanel;

import java.util.List;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.util.Pair;

/**
 *
 * @author tibuch
 */
public class DNDSelectionConfiguration {

    private DialogComponentButtonGroup m_radioButtons;

    private AllColumnConfiguration m_allConfig;

    private ManualSelectionConfiguration m_manualConfig;

    private TypeSelectionConfiguration m_typeConfig;

    private SettingsModelString m_radioButtonModel = new SettingsModelString("selectionType", "All Columns");

    private AllColumnPanel m_allColumn;

    private ManualSelectionPanel m_manual;

    private TypeSelectionPanel m_type;




    public DNDSelectionConfiguration(final ConfigurationDialogFactory fac) {
        m_manualConfig = new ManualSelectionConfiguration(fac);
        m_typeConfig = new TypeSelectionConfiguration(fac);
        m_allConfig = new AllColumnConfiguration(fac);


        m_allColumn = new AllColumnPanel(getAllConifg());

        m_manual = new ManualSelectionPanel(getManualConfig());

        m_type = new TypeSelectionPanel(getTypeConfig());


        m_radioButtons = new DialogComponentButtonGroup(m_radioButtonModel, false, null,
            "All Columns", "Manual Selection", "Type Selection");
    }

    public void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {
        m_manual.loadSettingsFrom(settings, specs);
        NodeSettings nsro;
        try {
            nsro = (NodeSettings)settings.getNodeSettings("typeSelectionSettings");
        } catch (InvalidSettingsException e) {
            nsro = new NodeSettings("typeSelectionSettings");
        }
        m_type.loadSettingsFrom(nsro, specs);


        m_radioButtons.loadSettingsFrom(settings, specs);

    }

    public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_manualConfig.validateSettings(settings);
        NodeSettings nsro;
        try {
            nsro = (NodeSettings)settings.getNodeSettings("typeSelectionSettings");
        } catch (InvalidSettingsException e) {
            nsro = new NodeSettings("typeSelectionSettings");
        }
        m_typeConfig.validateSettings(settings);

        m_radioButtonModel.validateSettings(settings);
    }

    /**
     * @param settings asdf
     * @throws InvalidSettingsException asdf
     */
    public void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_manualConfig.loadValidatedSettingsFrom(settings);
        NodeSettings nsro;
        try {
            nsro = (NodeSettings)settings.getNodeSettings("typeSelectionSettings");
        } catch (InvalidSettingsException e) {
            nsro = new NodeSettings("typeSelectionSettings");
        }
        m_typeConfig.loadValidatedSettingsFrom(nsro);


        m_radioButtonModel.loadSettingsFrom(settings);
        }

    public void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException  {
        m_manual.saveSettingsTo(settings);
        NodeSettings nswo = new NodeSettings("typeSelectionSettings");
        m_type.saveSettingsTo(nswo);
        settings.addNodeSettings(nswo);

        m_radioButtonModel.saveSettingsTo(settings);
    }

    public void saveSettings(final NodeSettingsWO settings) {
        m_manualConfig.saveSettings(settings);
        NodeSettings nswo = new NodeSettings("typeSelectionSettings");
        m_typeConfig.saveSettings(nswo);
        settings.addNodeSettings(nswo);

        m_radioButtonModel.saveSettingsTo(settings);
    }

    /**
     * @return the manualConfig
     */
    public ManualSelectionConfiguration getManualConfig() {
        return m_manualConfig;
    }

    /**
     * @return the typeConfig
     */
    public TypeSelectionConfiguration getTypeConfig() {
        return m_typeConfig;
    }

    public List<Pair<String, PaneConfigurationDialog>> configure(final DataTableSpec spec) {
        if (m_radioButtonModel.getStringValue().equals("Manual Selection")) {
            return m_manualConfig.configure(spec);
          // TODO: implement allconfig configuration
//        } else if (m_radioButtonModel.getStringValue().equals("All Columns")) {
//            return m_allConfig.configure(spec);
        } else {
            return m_typeConfig.configure(spec);
        }
    }

    /**
     * @return the radioButtons
     */
    public DialogComponentButtonGroup getRadioButtons() {
        return m_radioButtons;
    }

    /**
     * @return the manual
     */
    public ManualSelectionPanel getManual() {
        return m_manual;
    }

    /**
     * @return the type
     */
    public TypeSelectionPanel getType() {
        return m_type;
    }

    /**
     * @return the allConifg
     */
    public AllColumnConfiguration getAllConifg() {
        return m_allConfig;
    }

    /**
     * @param allConifg the allConifg to set
     */
    public void setAllConifg(final AllColumnConfiguration allConifg) {
        m_allConfig = allConifg;
    }

    /**
     * @param manual the manual to set
     */
    public void setManual(final ManualSelectionPanel manual) {
        m_manual = manual;
    }

    /**
     * @return the allColumn
     */
    public AllColumnPanel getAllColumn() {
        return m_allColumn;
    }

    /**
     * @param allColumn the allColumn to set
     */
    public void setAllColumn(final AllColumnPanel allColumn) {
        m_allColumn = allColumn;
    }


}