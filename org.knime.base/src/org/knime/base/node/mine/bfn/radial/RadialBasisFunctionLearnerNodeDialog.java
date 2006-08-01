/*
 * --------------------------------------------------------------------- *
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
 * If you have any quesions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.mine.bfn.radial;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.ParseException;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.node.mine.bfn.BasisFunctionLearnerNodeDialogPanel;
import org.knime.base.node.mine.bfn.BasisFunctionLearnerNodeModel;
import org.knime.base.node.mine.bfn.BasisFunctionLearnerTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;

/**
 * A dialog for radial basisfunction learner to set the following properties:
 * theta minus, theta plus, and a distance measurement.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
class RadialBasisFunctionLearnerNodeDialog extends NodeDialogPane {
    private final BasisFunctionLearnerNodeDialogPanel m_basicsPanel;

    private final JSpinner m_thetaMinus;

    private final JSpinner m_thetaPlus;

    /**
     * Creates a new {@link NodeDialogPane} for radial basis functions in order
     * to set theta minus, theta plus, and a choice of distance function.
     */
    RadialBasisFunctionLearnerNodeDialog() {
        super();
        // panel with model specific settings
        m_basicsPanel = new BasisFunctionLearnerNodeDialogPanel();
        super.addTab(m_basicsPanel.getName(), m_basicsPanel);
        // panel with advance settings
        GridBagLayout gbl = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = GridBagConstraints.REMAINDER;
        JPanel p = new JPanel(gbl);
        // theta minus
        c.insets = new Insets(15, 15, 5, 15);
        m_thetaMinus = new JSpinner();
        JSpinner.DefaultEditor editorMinus = (JSpinner.DefaultEditor)m_thetaMinus
                .getEditor();
        editorMinus.getTextField().setColumns(8);
        m_thetaMinus.setModel(new SpinnerNumberModel(0.0, 0.0,
                Double.POSITIVE_INFINITY, 0.1));
        m_thetaMinus.setPreferredSize(new Dimension(100, 25));
        JPanel thetaMinusPanel = new JPanel();
        thetaMinusPanel.setBorder(BorderFactory
                .createTitledBorder(" Theta Minus "));
        thetaMinusPanel.add(m_thetaMinus);
        gbl.setConstraints(thetaMinusPanel, c);
        p.add(thetaMinusPanel);
        // theta plus
        c.insets = new Insets(5, 15, 5, 15);
        m_thetaPlus = new JSpinner();
        JSpinner.DefaultEditor editorPlus = (JSpinner.DefaultEditor)m_thetaPlus
                .getEditor();
        editorPlus.getTextField().setColumns(8);
        m_thetaPlus.setModel(new SpinnerNumberModel(0.0, 0.0,
                Double.POSITIVE_INFINITY, 0.1));
        m_thetaPlus.setPreferredSize(new Dimension(100, 25));
        JPanel thetaPlusPanel = new JPanel();
        thetaPlusPanel.setBorder(BorderFactory
                .createTitledBorder(" Theta Plus "));
        thetaPlusPanel.add(m_thetaPlus);
        gbl.setConstraints(thetaPlusPanel, c);
        p.add(thetaPlusPanel);
        super.addTab(" Learner ", p);

        m_thetaMinus.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                try {
                    m_thetaMinus.commitEdit();
                } catch (ParseException pe) {
                    // empty
                }
                double thetaMinus = ((Double)m_thetaMinus.getValue())
                        .doubleValue();

                try {
                    m_thetaPlus.commitEdit();
                } catch (ParseException pe) {
                    // leer.
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
                    // nothing.
                }
                double thetaMinus = ((Double)m_thetaMinus.getValue())
                        .doubleValue();

                try {
                    m_thetaPlus.commitEdit();
                } catch (ParseException pe) {
                    // nichts.
                }
                double thetaPlus = ((Double)m_thetaPlus.getValue())
                        .doubleValue();

                if (thetaPlus < thetaMinus) {
                    m_thetaMinus.setValue(new Double(thetaPlus));
                }

            }

        });
    } // RadialBasisFunctionDialog()

    /**
     * @see NodeDialogPane#loadSettingsFrom(NodeSettingsRO, DataTableSpec[])
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        // update target columns
        m_basicsPanel.setTargetColumns(settings.getString(
                BasisFunctionLearnerNodeModel.TARGET_COLUMN, null), specs[0]);
        // update choice of distance function
        m_basicsPanel.setDistance(settings.getInt(
                BasisFunctionLearnerNodeModel.DISTANCE, 0));
        // update theta minus
        m_thetaMinus.setValue(new Double(settings.getDouble(
                RadialBasisFunctionFactory.THETA_MINUS,
                RadialBasisFunctionLearnerNodeModel.THETAMINUS)));
        // update theta plus
        m_thetaPlus.setValue(new Double(settings.getDouble(
                RadialBasisFunctionFactory.THETA_PLUS,
                RadialBasisFunctionLearnerNodeModel.THETAPLUS)));
        // set missing replacement value
        int missing = settings.getInt(BasisFunctionLearnerTable.MISSING, 0);
        m_basicsPanel.setMissing(missing);
        // shrink after commit
        boolean shrinkAfterCommit = settings.getBoolean(
                BasisFunctionLearnerNodeModel.SHRINK_AFTER_COMMIT, false);
        m_basicsPanel.setShrinkAfterCommit(shrinkAfterCommit);
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
        assert (settings != null);

        // contains the error message if theta minus and/or theta plus
        // is out of range
        StringBuffer errMsg = new StringBuffer();

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

        // distance
        int distance = m_basicsPanel.getDistance();
        if (distance < 0
                || distance > BasisFunctionLearnerNodeModel.DISTANCES.length) {
            errMsg.append("Select a distance measure: " + distance);
        }

        // if error message's length greater zero throw exception
        if (errMsg.length() > 0) {
            throw new InvalidSettingsException(errMsg.toString());
        }

        //
        // everthing fine, set values in the model
        // 

        // set target column
        settings.addString(BasisFunctionLearnerNodeModel.TARGET_COLUMN,
                m_basicsPanel.getSelectedTargetColumn().getName());

        // theta minus
        settings.addDouble(RadialBasisFunctionFactory.THETA_MINUS,
                thetaMinusValue);

        // theta plus
        settings.addDouble(RadialBasisFunctionFactory.THETA_PLUS,
                thetaPlusValue);

        // distance
        settings.addInt(BasisFunctionLearnerNodeModel.DISTANCE, distance);

        // missing
        settings.addInt(BasisFunctionLearnerTable.MISSING, m_basicsPanel
                .getMissing());

        // shrink after commit
        settings.addBoolean(BasisFunctionLearnerNodeModel.SHRINK_AFTER_COMMIT,
                m_basicsPanel.isShrinkAfterCommit());

    }
}
