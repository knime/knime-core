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
 *   Nov 16, 2005 (Kilian Thiel): created
 */
package org.knime.base.node.mine.sota.dialog;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;


/**
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public class SotaNodeDialog extends NodeDialogPane {
    private static final NodeLogger LOGGER = 
        NodeLogger.getLogger(SotaNodeDialog.class);
    
    private SotaSettingsPanel m_settings;
    private SotaFilterColumnPanel m_filterSettings;
    private SotaHierarchicalFuzzySettings m_hierarchicalFuzzyDataSettings;
    
    /**
     * Constructor of SotaNodedialog.
     * Creates new instance of SotaNodeDialog.
     */
    public SotaNodeDialog() {
        super();
        
        m_settings = new SotaSettingsPanel(LOGGER);
        JPanel outerSettingsPanel = new JPanel();
        outerSettingsPanel.add(m_settings);
        outerSettingsPanel.setBorder(new EtchedBorder());
        
        m_filterSettings = new SotaFilterColumnPanel();
        JPanel outerFilterPanel = new JPanel();
        outerFilterPanel.add(m_filterSettings);
        outerFilterPanel.setBorder(new EtchedBorder());        
        
        m_hierarchicalFuzzyDataSettings = 
            new SotaHierarchicalFuzzySettings(LOGGER);
        JPanel outerFuzzyPanel = new JPanel();
        outerFuzzyPanel.add(m_hierarchicalFuzzyDataSettings);
        outerFuzzyPanel.setBorder(new EtchedBorder());
        
        JPanel jp = new JPanel(new GridBagLayout());
        
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.weightx = 10;
        c.weighty = 10;        
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(10, 10, 0, 10);
        jp.add(outerFilterPanel, c);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 10;
        c.weighty = 10;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(10, 10, 10, 10);
        jp.add(outerSettingsPanel, c);
        
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 1;
        c.weightx = 10;
        c.weighty = 10;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(10, 10, 10, 10);
        jp.add(outerFuzzyPanel, c);
                
        addTab("Settings", jp);
    }

    /**
     * @see NodeDialogPane#loadSettingsFrom(NodeSettingsRO, DataTableSpec[])
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        assert (settings != null && specs != null);
        m_settings.loadSettingsFrom(settings, specs);
        m_filterSettings.loadSettingsFrom(settings, specs);
        m_hierarchicalFuzzyDataSettings.loadSettingsFrom(settings, specs);
    }

    /**
     * @see NodeDialogPane#saveSettingsTo(NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        assert (settings != null);
        m_settings.saveSettingsTo(settings);
        m_filterSettings.saveSettingsTo(settings);
        m_hierarchicalFuzzyDataSettings.saveSettingsTo(settings);
    }
}
