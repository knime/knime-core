/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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
 * -------------------------------------------------------------------
 * 
 * History
 *   Dec 19, 2005 (Kilian Thiel): created
 */
package org.knime.base.node.mine.sota.dialog;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;

import org.knime.base.node.mine.sota.SotaConfigKeys;
import org.knime.base.node.mine.sota.SotaManager;
import org.knime.base.node.mine.sota.SotaNodeModel;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.IntValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
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

    /**
     * Constructor.
     * 
     * @param logger logger object
     */
    public SotaHierarchicalFuzzySettings(final NodeLogger logger) {
        super();
        setLayout(new GridBagLayout());
        setBorder(new EtchedBorder());

        m_logger = logger;

        m_jchbUseHierarchicalFuzzyData = new JCheckBox();
        m_jchbUseHierarchicalFuzzyData
                .setSelected(SotaManager.USE_HIERARCHICAL_FUZZY_DATA);
        m_jchbUseHierarchicalFuzzyData.addActionListener(
                new SotaHierarchicalFuzzySettingsController());

        m_columnSelectionPanel = new ColumnSelectionPanel("Hierarchical level",
                IntValue.class);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        add(new JLabel("Use hierarchical fuzzy data"), gbc);

        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        add(m_jchbUseHierarchicalFuzzyData, gbc);

        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        add(m_columnSelectionPanel, gbc);
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
     */
    public void saveSettingsTo(final NodeSettingsWO settings) {
        assert (settings != null);

        settings.addBoolean(SotaConfigKeys.CFGKEY_HIERARCHICAL_FUZZY_DATA,
                isHierarchicalFuzzyData());
        settings.addString(SotaConfigKeys.CFGKEY_HIERARCHICAL_FUZZY_LEVEL,
                getHierarchicalFuzzyLevelCell());
    }

    /**
     * Method loadSettingsFrom.
     * 
     * @param settings the NodeSettings object of the containing NodeDialogPane
     * @param specs the DataTableSpec[] of the containing NodeDialogPane
     */
    public void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) {
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
    }

    /**
     * Controller of SotaHierarchicalFuzzySettings.
     * 
     * @author Kilian Thiel, University of Konstanz
     */
    class SotaHierarchicalFuzzySettingsController implements ActionListener {

        /**
         * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
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
}
