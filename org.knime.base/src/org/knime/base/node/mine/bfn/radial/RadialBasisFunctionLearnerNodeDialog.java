/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
