/*
 * --------------------------------------------------------------------- *
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.mine.bfn.radial;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.text.ParseException;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.node.mine.bfn.BasisFunctionLearnerNodeDialogPane;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;

/**
 * A dialog for PNN learner to set properties, such as theta minus and plus. 
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class RadialBasisFunctionLearnerNodeDialog 
        extends BasisFunctionLearnerNodeDialogPane {

    private final JSpinner m_thetaMinus;

    private final JSpinner m_thetaPlus;
    
    /**
     * Creates a new {@link org.knime.core.node.NodeDialogPane} for radial 
     * basisfunctions in order to set theta minus and theta plus.
     */
    public RadialBasisFunctionLearnerNodeDialog() {
        super();
        
        // panel with advance settings
        JPanel p = new JPanel(new GridLayout(0, 1));
        
        // theta minus
        m_thetaMinus = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 1.0, 0.1));
        m_thetaMinus.setEditor(new JSpinner.NumberEditor(
                m_thetaMinus, "#.##########"));
        m_thetaMinus.setPreferredSize(new Dimension(100, 25));
        JPanel thetaMinusPanel = new JPanel();
        thetaMinusPanel.setBorder(BorderFactory
                .createTitledBorder(" Theta Minus "));
        thetaMinusPanel.add(m_thetaMinus);
        p.add(thetaMinusPanel);
        
        // theta plus
        m_thetaPlus = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 1.0, 0.1));
        m_thetaPlus.setEditor(new JSpinner.NumberEditor(
                m_thetaPlus, "#.##########"));
        m_thetaPlus.setPreferredSize(new Dimension(100, 25));
        JPanel thetaPlusPanel = new JPanel();
        thetaPlusPanel.setBorder(BorderFactory
                .createTitledBorder(" Theta Plus "));
        thetaPlusPanel.add(m_thetaPlus);
        p.add(thetaPlusPanel);

        m_thetaMinus.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                try {
                    m_thetaMinus.commitEdit();
                } catch (ParseException pe) {
                    // ignore
                }
                double thetaMinus = ((Double)m_thetaMinus.getValue())
                        .doubleValue();

                try {
                    m_thetaPlus.commitEdit();
                } catch (ParseException pe) {
                    // ignore
                }
                double thetaPlus = ((Double)m_thetaPlus.getValue())
                        .doubleValue();

                if (thetaMinus > thetaPlus) {
                    m_thetaPlus.setValue(new Double(thetaMinus));
                }

            }
        });

        m_thetaPlus.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                try {
                    m_thetaMinus.commitEdit();
                } catch (ParseException pe) {
                    // ignore
                }
                double thetaMinus = ((Double)m_thetaMinus.getValue())
                        .doubleValue();

                try {
                    m_thetaPlus.commitEdit();
                } catch (ParseException pe) {
                    // ignore
                }
                double thetaPlus = ((Double)m_thetaPlus.getValue())
                        .doubleValue();

                if (thetaPlus < thetaMinus) {
                    m_thetaMinus.setValue(new Double(thetaPlus));
                }

            }

        });
        
        // add the learner tab to this dialog
        super.addTab("PNN", p);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
        super.loadSettingsFrom(settings, specs);
        // update theta minus
        m_thetaMinus.setValue(new Double(settings.getDouble(
                RadialBasisFunctionFactory.THETA_MINUS,
                RadialBasisFunctionLearnerNodeModel.THETAMINUS)));
        // update theta plus
        m_thetaPlus.setValue(new Double(settings.getDouble(
                RadialBasisFunctionFactory.THETA_PLUS,
                RadialBasisFunctionLearnerNodeModel.THETAPLUS)));
    }

    /**
     * Updates this dialog by retrieving theta minus, theta plus, and the choice
     * of distance function from the underlying model.
     * 
     * @param settings the object to write the settings into
     * @throws InvalidSettingsException if the either theta minus or plus are
     *             out of range [0.0,1.0]
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        super.saveSettingsTo(settings);

        // contains the error message
        StringBuilder errMsg = new StringBuilder();

        // theta minus
        double thetaMinusValue = Double.NaN;
        try {
            m_thetaMinus.commitEdit();
            thetaMinusValue = ((Double)m_thetaMinus.getValue()).doubleValue();
            if (thetaMinusValue < 0.0 || thetaMinusValue > 1.0) {
                errMsg.append("Theta-minus \"" + thetaMinusValue
                        + "\" must be between 0.0 and 1.0\n");
            }
        } catch (ParseException nfe) {
            errMsg.append("Theta-minus \"" + m_thetaMinus.toString()
                    + "\" invalid must be between 0.0 and 1.0\n");
        }

        // theta plus
        double thetaPlusValue = Double.NaN;
        try {
            m_thetaPlus.commitEdit();
            thetaPlusValue = ((Double)m_thetaPlus.getValue()).doubleValue();
            if (thetaPlusValue < 0.0 || thetaPlusValue > 1.0) {
                errMsg.append("Theta-plus \"" + thetaPlusValue
                        + "\" must be between 0.0 and 1.0\n");
            }
        } catch (ParseException nfe) {
            errMsg.append("Theta-plus \"" + m_thetaPlus.toString()
                    + "\" invalid must be between 0.0 and 1.0\n");
        }

        // if error message's length greater zero throw exception
        if (errMsg.length() > 0) {
            throw new InvalidSettingsException(errMsg.toString());
        }

        //
        // everything fine, set values in the model
        // 

        // theta minus
        settings.addDouble(RadialBasisFunctionFactory.THETA_MINUS,
                thetaMinusValue);

        // theta plus
        settings.addDouble(RadialBasisFunctionFactory.THETA_PLUS,
                thetaPlusValue);
    }
}
