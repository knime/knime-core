/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 * -------------------------------------------------------------------
 * 
 * History
 *   Dec 19, 2005 (Kilian Thiel): created
 */
package org.knime.base.node.mine.sota;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.node.mine.sota.logic.SotaManager;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.IntValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.ColumnSelectionPanel;

/**
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public class SotaHierarchicalFuzzySettings extends JPanel {
    private static final long serialVersionUID = 850988272023506129L;

    private JCheckBox m_jchbUseHierarchicalFuzzyData;

    private ColumnSelectionPanel m_columnSelectionPanel;

    private NodeLogger m_logger;
    
    private DialogComponentColumnNameSelection m_classColumn;
    private SettingsModelString m_classColumnModel;
    
    private DialogComponentBoolean m_useOutData;
    private SettingsModelBoolean m_useOutDataModel;

    /**
     * Constructor.
     * 
     * @param logger logger object
     */
    @SuppressWarnings("unchecked")
    public SotaHierarchicalFuzzySettings(final NodeLogger logger) {
        super();
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        m_logger = logger;

        
        m_jchbUseHierarchicalFuzzyData = new JCheckBox();
        m_jchbUseHierarchicalFuzzyData.setText("Use hierarchical fuzzy data");
        m_jchbUseHierarchicalFuzzyData
                .setSelected(SotaManager.USE_HIERARCHICAL_FUZZY_DATA);
        m_jchbUseHierarchicalFuzzyData.addActionListener(
                new SotaHierarchicalFuzzySettingsController());
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        add(m_jchbUseHierarchicalFuzzyData, gbc);
        

        m_columnSelectionPanel = new ColumnSelectionPanel("Hierarchical level",
                IntValue.class);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 20, 5);
        add(m_columnSelectionPanel, gbc);
        
        
        m_useOutDataModel = new SettingsModelBoolean(
                SotaConfigKeys.CFGKEY_USE_CLASS_DATA, 
                SotaNodeModel.DEFAULT_USE_OUTDATA);
        m_useOutDataModel.addChangeListener(new UseOutDataChangeListener());
        m_useOutData = new DialogComponentBoolean(m_useOutDataModel, 
                "Use class column");
        gbc = new GridBagConstraints();
        gbc.gridy = 2;
        gbc.gridx = 0;
        this.add(m_useOutData.getComponentPanel(), gbc);
        
        
        JPanel borderPanel = new JPanel();
        TitledBorder border = BorderFactory.createTitledBorder(
                new EtchedBorder(), "Class column");
        borderPanel.setBorder(border);
        m_classColumnModel = new SettingsModelString(
                SotaConfigKeys.CFGKEY_CLASSCOL, "");
        m_classColumn = new DialogComponentColumnNameSelection(
                m_classColumnModel, "", 0, StringValue.class);
        gbc = new GridBagConstraints();
        gbc.gridy = 3;
        gbc.gridx = 0;
        borderPanel.add(m_classColumn.getComponentPanel());
        this.add(borderPanel, gbc);
    }

    private boolean isHierarchicalFuzzyData() {
        return m_jchbUseHierarchicalFuzzyData.isSelected();
    }

    private String getHierarchicalFuzzyLevelCell() {
        return m_columnSelectionPanel.getSelectedColumn();
    }

    /**
     * Saves all settings to settings object.
     * 
     * @param settings object to store settings in
     * 
     * @throws InvalidSettingsException If components could not save settings
     * to given settings instance.
     */
    public void saveSettingsTo(final NodeSettingsWO settings) 
    throws InvalidSettingsException {
        assert (settings != null);

        settings.addBoolean(SotaConfigKeys.CFGKEY_HIERARCHICAL_FUZZY_DATA,
                isHierarchicalFuzzyData());
        settings.addString(SotaConfigKeys.CFGKEY_HIERARCHICAL_FUZZY_LEVEL,
                getHierarchicalFuzzyLevelCell());
        
        
        m_classColumn.saveSettingsTo(settings);
        m_useOutData.saveSettingsTo(settings);        
    }

    /**
     * Method loadSettingsFrom.
     * 
     * @param settings the NodeSettings object of the containing NodeDialogPane
     * @param specs the DataTableSpec[] of the containing NodeDialogPane
     * 
     * @throws NotConfigurableException If components could not load the 
     * settings from the given settings instance. 
     */
    public void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException  {
        assert (settings != null && specs != null);

        try {
            m_jchbUseHierarchicalFuzzyData.setSelected(settings
                    .getBoolean(SotaConfigKeys.CFGKEY_HIERARCHICAL_FUZZY_DATA));
        } catch (InvalidSettingsException e7) {
            m_logger.debug("Invalid Settings", e7);
        }

        try {
            m_columnSelectionPanel.update(specs[SotaNodeModel.INPORT], settings
                    .getString(SotaConfigKeys.CFGKEY_HIERARCHICAL_FUZZY_LEVEL));
        } catch (InvalidSettingsException e) {
            m_logger.debug("Invalid Settings", e);
        } catch (NotConfigurableException e) {
            m_logger.debug("No IntVal in DataTableSpec");
        }

        enableComboBox();
        
        m_classColumn.loadSettingsFrom(settings, specs);
        m_useOutData.loadSettingsFrom(settings, specs);
    }

    /**
     * Controller of SotaHierarchicalFuzzySettings.
     * 
     * @author Kilian Thiel, University of Konstanz
     */
    class SotaHierarchicalFuzzySettingsController implements ActionListener {

        /**
         * {@inheritDoc}
         */
        public void actionPerformed(final ActionEvent e) {
            enableComboBox();
        }
    }

    private void enableComboBox() {
        if (m_jchbUseHierarchicalFuzzyData.isSelected()) {
            m_columnSelectionPanel.setEnabled(true);
        } else {
            m_columnSelectionPanel.setEnabled(false);
        }
    }
    
    private class UseOutDataChangeListener implements ChangeListener {
        /**
         * {@inheritDoc}
         */
        public void stateChanged(final ChangeEvent e) {
            m_classColumnModel.setEnabled(m_useOutDataModel.getBooleanValue());
        }
    }      
}
