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
 * -------------------------------------------------------------------
 * 
 * History
 *   Nov 16, 2005 (Kilian Thiel): created
 */
package org.knime.base.node.mine.sota;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.ParseException;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.knime.base.node.mine.sota.distances.DistanceManagerFactory;
import org.knime.base.node.mine.sota.logic.SotaManager;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public class SotaSettingsPanel extends JPanel {
    private final double m_learningrateINCR = 0.01;

    private final double m_variabilityINCR = 0.01;

    private final double m_resourceINCR = 0.01;

    private final double m_errorINCR = 0.01;

    private JSpinner m_jsLearningRateWinner;

    private JSpinner m_jsLearningRateSister;

    private JSpinner m_jsLearningRateAncestor;

    private JSpinner m_jsVariability;

    private JSpinner m_jsResource;

    private JSpinner m_jsError;

    private JCheckBox m_jchbUseVariability;

    private NodeLogger m_logger;

    private JComboBox m_jcbDistance;

    private int m_width = 8;

    /**
     * Constructor.
     * 
     * @param logger the NodeLogger object to use for logging
     */
    public SotaSettingsPanel(final NodeLogger logger) {
        super(new GridBagLayout());
        m_logger = logger;

        GridBagConstraints gbc = new GridBagConstraints();
        
        // Winner Learningrate
        add(new JLabel("Winner learningrate"), getLabelGBC(0, 0));        
        
        m_jsLearningRateWinner = new JSpinner(new SpinnerNumberModel(
                SotaManager.LR_WINNER, SotaManager.LR_WINNER_MIN,
                SotaManager.LR_WINNER_MAX, m_learningrateINCR));        
        JSpinner.DefaultEditor editor 
            = (JSpinner.DefaultEditor)m_jsLearningRateWinner.getEditor();
        editor.getTextField().setColumns(m_width);
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(10, 0, 0, 10);
        add(m_jsLearningRateWinner, gbc);

        // Sister Learninrate  
        add(new JLabel("Sister learningrate"), getLabelGBC(0, 1));   
        
        m_jsLearningRateSister = new JSpinner(new SpinnerNumberModel(
                SotaManager.LR_SISTER, SotaManager.LR_SISTER_MIN,
                SotaManager.LR_SISTER_MAX, m_learningrateINCR));
        editor = (JSpinner.DefaultEditor)m_jsLearningRateSister.getEditor();
        editor.getTextField().setColumns(m_width);
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(10, 0, 0, 10);
        add(m_jsLearningRateSister, gbc);

        // Ancestor Learningrate
        add(new JLabel("Ancestor learningrate"), getLabelGBC(0, 2));   
        
        m_jsLearningRateAncestor = new JSpinner(new SpinnerNumberModel(
                SotaManager.LR_ANCESTOR, SotaManager.LR_ANCESTOR_MIN,
                SotaManager.LR_ANCESTOR_MAX, m_learningrateINCR));
        editor = (JSpinner.DefaultEditor)m_jsLearningRateAncestor.getEditor();
        editor.getTextField().setColumns(m_width);
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(10, 0, 0, 10);
        add(m_jsLearningRateAncestor, gbc);

        // Minimal Variability
        add(new JLabel("Minimal variability"), getLabelGBC(0, 3));
        
        m_jsVariability = new JSpinner(new SpinnerNumberModel(
                SotaManager.MIN_VARIABILITY, SotaManager.MIN_VARIABILITY_MIN,
                SotaManager.MIN_VARIABILITY_MAX, m_variabilityINCR));
        editor = (JSpinner.DefaultEditor)m_jsVariability.getEditor();
        editor.getTextField().setColumns(m_width);
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(10, 0, 0, 10);
        add(m_jsVariability, gbc);
        
        // Minimal Resource
        add(new JLabel("Minimal resource"), getLabelGBC(0, 4));
        
        m_jsResource = new JSpinner(new SpinnerNumberModel(
                SotaManager.MIN_RESOURCE, SotaManager.MIN_RESOURCE_MIN,
                SotaManager.MIN_RESOURCE_MAX, m_resourceINCR));
        editor = (JSpinner.DefaultEditor)m_jsResource.getEditor();
        editor.getTextField().setColumns(m_width);
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 4;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(10, 0, 0, 10);
        add(m_jsResource, gbc);
        
        // Minimal Error
        add(new JLabel("Minimal error"), getLabelGBC(0, 5));
        
        m_jsError = new JSpinner(new SpinnerNumberModel(SotaManager.MIN_ERROR,
                SotaManager.MIN_ERROR_MIN, SotaManager.MIN_ERROR_MAX,
                m_errorINCR));
        editor = (JSpinner.DefaultEditor)m_jsError.getEditor();
        editor.getTextField().setColumns(m_width);
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 5;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(10, 0, 0, 10);
        add(m_jsError, gbc);
        
        // Use Variability
        add(new JLabel("Use variability"), getLabelGBC(0, 6));
        
        m_jchbUseVariability = new JCheckBox();
        m_jchbUseVariability.setSelected(SotaManager.USE_VARIABILITY);
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 6;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(10, 0, 0, 10);
        add(m_jchbUseVariability, gbc);
        
        // Distance Metrik
        add(new JLabel("Dictance metric"), getLabelGBC(0, 7));

        m_jcbDistance = new JComboBox();
        m_jcbDistance.addItem(DistanceManagerFactory.EUCLIDEAN_DIST);
        m_jcbDistance.addItem(DistanceManagerFactory.COS_DIST);
        m_jcbDistance.setSelectedItem(DistanceManagerFactory.EUCLIDEAN_DIST);
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 7;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(10, 0, 0, 10);
        add(m_jcbDistance, gbc);
    }

    private GridBagConstraints getLabelGBC(final int x, final int y) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.insets = new Insets(10, 10, 0, 10);
        return gbc;
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
            m_jsLearningRateWinner.getModel().setValue(
                    new Double(settings
                            .getDouble(SotaConfigKeys.CFGKEY_LR_WINNER)));
        } catch (InvalidSettingsException e1) {
            m_logger.debug("Invalid Settings", e1);
        }

        try {
            m_jsLearningRateSister.getModel().setValue(
                    new Double(settings
                            .getDouble(SotaConfigKeys.CFGKEY_LR_SISTER)));
        } catch (InvalidSettingsException e2) {
            m_logger.debug("Invalid Settings", e2);
        }

        try {
            m_jsLearningRateAncestor.getModel().setValue(
                    new Double(settings
                            .getDouble(SotaConfigKeys.CFGKEY_LR_ANCESTOR)));
        } catch (InvalidSettingsException e3) {
            m_logger.debug("Invalid Settings", e3);
        }

        try {
            m_jsError.getModel().setValue(
                    new Double(settings
                            .getDouble(SotaConfigKeys.CFGKEY_MIN_ERROR)));
        } catch (InvalidSettingsException e4) {
            m_logger.debug("Invalid Settings", e4);
        }

        try {
            m_jsVariability.getModel().setValue(
                    new Double(settings
                            .getDouble(SotaConfigKeys.CFGKEY_VARIABILITY)));
        } catch (InvalidSettingsException e5) {
            m_logger.debug("Invalid Settings", e5);
        }

        try {
            m_jsResource.getModel().setValue(
                    new Double(settings
                            .getDouble(SotaConfigKeys.CFGKEY_RESOURCE)));
        } catch (InvalidSettingsException e6) {
            m_logger.debug("Invalid Settings", e6);
        }

        try {
            m_jchbUseVariability.setSelected(settings
                    .getBoolean(SotaConfigKeys.CFGKEY_USE_VARIABILITY));
        } catch (InvalidSettingsException e7) {
            m_logger.debug("Invalid Settings", e7);
        }

        try {
            m_jcbDistance.setSelectedItem(settings
                    .getString(SotaConfigKeys.CFGKEY_USE_DISTANCE));
        } catch (InvalidSettingsException e8) {
            m_logger.debug("Invalid Settings", e8);
        }
    }

    /**
     * Saves all settings to settings object.
     * 
     * @param settings object to store settings in
     */
    public void saveSettingsTo(final NodeSettingsWO settings) {
        assert (settings != null);

        settings.addDouble(SotaConfigKeys.CFGKEY_LR_WINNER,
                getWinnerLearningrate());
        settings.addDouble(SotaConfigKeys.CFGKEY_LR_SISTER,
                getSisterLearningrate());
        settings.addDouble(SotaConfigKeys.CFGKEY_LR_ANCESTOR,
                getAncestorLearningrate());

        settings.addDouble(SotaConfigKeys.CFGKEY_MIN_ERROR, getMinimumError());
        settings.addDouble(SotaConfigKeys.CFGKEY_VARIABILITY,
                getMinimumVariability());
        settings.addDouble(SotaConfigKeys.CFGKEY_RESOURCE, 
                getMinimumResource());

        settings.addBoolean(SotaConfigKeys.CFGKEY_USE_VARIABILITY,
                isUseVariability());

        settings.addString(SotaConfigKeys.CFGKEY_USE_DISTANCE,
                getDistanceMetric());
    }

    /**
     * Get selected value of Winner learningrate.
     * 
     * @return the selected value of Winner learningrate
     */
    private double getWinnerLearningrate() {
        try {
            m_jsLearningRateWinner.commitEdit();
        } catch (ParseException e) {
            // if the spinner has the focus, the currently edited value
            // might not be commited. Now it is!
        }
        SpinnerNumberModel snm = (SpinnerNumberModel)m_jsLearningRateWinner
                .getModel();
        return snm.getNumber().doubleValue();
    }

    /**
     * Get selected value of Sister learningrate.
     * 
     * @return the selected value of Sister learningrate
     */
    private double getSisterLearningrate() {
        try {
            m_jsLearningRateSister.commitEdit();
        } catch (ParseException e) {
            // if the spinner has the focus, the currently edited value
            // might not be commited. Now it is!
        }
        SpinnerNumberModel snm = (SpinnerNumberModel)m_jsLearningRateSister
                .getModel();
        return snm.getNumber().doubleValue();
    }

    /**
     * Get selected value of Ancestor learningrate.
     * 
     * @return the selected value of Ancestor learningrate
     */
    private double getAncestorLearningrate() {
        try {
            m_jsLearningRateAncestor.commitEdit();
        } catch (ParseException e) {
            // if the spinner has the focus, the currently edited value
            // might not be commited. Now it is!
        }
        SpinnerNumberModel snm = (SpinnerNumberModel)m_jsLearningRateAncestor
                .getModel();
        return snm.getNumber().doubleValue();
    }

    /**
     * Get selected value of minimum error.
     * 
     * @return the selected value of minimum error
     */
    private double getMinimumError() {
        try {
            m_jsError.commitEdit();
        } catch (ParseException e) {
            // if the spinner has the focus, the currently edited value
            // might not be commited. Now it is!
        }
        SpinnerNumberModel snm = (SpinnerNumberModel)m_jsError.getModel();
        return snm.getNumber().doubleValue();
    }

    /**
     * Get selected value of minimum variability.
     * 
     * @return the selected value of minimum variability
     */
    private double getMinimumVariability() {
        try {
            m_jsVariability.commitEdit();
        } catch (ParseException e) {
            // if the spinner has the focus, the currently edited value
            // might not be commited. Now it is!
        }
        SpinnerNumberModel snm = (SpinnerNumberModel)m_jsVariability.getModel();
        return snm.getNumber().doubleValue();
    }

    /**
     * Get selected value of minimum resource.
     * 
     * @return the selected value of minimum resource
     */
    private double getMinimumResource() {
        try {
            m_jsResource.commitEdit();
        } catch (ParseException e) {
            // if the spinner has the focus, the currently edited value
            // might not be commited. Now it is!
        }
        SpinnerNumberModel snm = (SpinnerNumberModel)m_jsResource.getModel();
        return snm.getNumber().doubleValue();
    }

    /**
     * Returns true if use-variability checkbox is selected, else false.
     * 
     * @return True if use-variability checkbox is selected, else false
     */
    private boolean isUseVariability() {
        return m_jchbUseVariability.isSelected();
    }

    /**
     * Returns the selected distance metric.
     * 
     * @return The selected distance metric.
     */
    private String getDistanceMetric() {
        return (String)m_jcbDistance.getSelectedItem();
    }
}
