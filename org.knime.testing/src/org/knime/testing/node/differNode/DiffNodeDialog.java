/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2011
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
 *
 * History
 *   Jun 23, 2006 (ritmeier): created
 */
package org.knime.testing.node.differNode;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;


/**
 *
 * @author ritmeier, University of Konstanz
 */
public class DiffNodeDialog extends NodeDialogPane implements ActionListener {

    private JComboBox m_differCombo;

    private JPanel m_content;

    private JPanel m_tolerancePanel;

    private static final int m_defaultLowerTolerance = 5;

    private static final int m_defaultUpperTolerance = 0;

    private JSpinner m_lowerToleranceSpinner;

    private JSpinner m_upperToleranceSpinner;

    private int m_loadedLowerTolerance = 0;

    private int m_loadedUpperTolerance = 0;

    private JLabel m_lowerToleranceLable;

    private JLabel m_upperToleranceLable;

    /**
     * enumeration of different evaluators for the test results
     *
     * @author ritmeier, University of Konstanz
     */
    public enum Evaluators {

        /**
         * get a DataTableDiffer.
         */
        TableDiffer() {
            @Override
            public TestEvaluator getInstance() {
                return new DataTableDiffer();
            }
        },
        /**
         * get a DataTableDiffer.
         */
        EmptyTableTest() {
            @Override
            public TestEvaluator getInstance() {
                return new EmptyTableChecker();
            }
        },
        /**
         * get a NegativerDataTableDiffer.
         */
        NegativeDiffer() {
            @Override
            public TestEvaluator getInstance() {
                return new NegativeDataTableDiffer();
            }
        },
        /**
         * get n LearnerScoreComperator
         */
        LearnerScoreComperator() {

            @Override
            public TestEvaluator getInstance() {
                return new LearnerScoreComperator();
            }
        };

        /**
         * Get an instance.
         *
         * @return Returns an instance of TestEvaluator.
         */
        public abstract TestEvaluator getInstance();

    }



    /**
     * Creates a new config dialog for this node.
     */
    public DiffNodeDialog() {
        super();
        addTab("DiffNodeDialog", buildContentPanel());
    }

    /**
     * creates the panels
     *
     * @return Component with content
     */
    private Component buildContentPanel() {
        m_content = new JPanel();
        m_content.setLayout(new BorderLayout());
        m_tolerancePanel = buildTolerancePanel();
        final JComboBox combo = getEvalCombo();
        m_content.add(combo, BorderLayout.NORTH);
        m_content.add(m_tolerancePanel, BorderLayout.SOUTH);

        return m_content;
    }

    private JPanel buildTolerancePanel() {
        final JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(2, 2));

        m_lowerToleranceLable = new JLabel("lower tolerance (error%)");
        panel.add(m_lowerToleranceLable);
        SpinnerModel smin = null;
        if (m_loadedLowerTolerance > 0) {
            smin = new SpinnerNumberModel(m_loadedLowerTolerance, 0, 100, 1);
        } else {
            smin = new SpinnerNumberModel(m_defaultLowerTolerance, 0, 100, 1);
        }
        m_lowerToleranceSpinner = new JSpinner(smin);
        panel.add(m_lowerToleranceSpinner);

        m_upperToleranceLable = new JLabel("upper tolerance (error%)");
        panel.add(m_upperToleranceLable);

        SpinnerModel smax = null;
        if (m_loadedUpperTolerance > 0) {
            smax = new SpinnerNumberModel(m_loadedUpperTolerance, 0, 100, 1);
        } else {
            smax = new SpinnerNumberModel(m_defaultUpperTolerance, 0, 100, 1);
        }
        m_upperToleranceSpinner = new JSpinner(smax);
        panel.add(m_upperToleranceSpinner);

        showSpinners(getEvalCombo().getSelectedItem().equals(
                Evaluators.LearnerScoreComperator));

        return panel;
    }

    private void showSpinners(final boolean b) {

        m_lowerToleranceLable.setVisible(b);
        m_upperToleranceLable.setVisible(b);
        m_lowerToleranceSpinner.setVisible(b);
        m_upperToleranceSpinner.setVisible(b);
    }

    private JComboBox getEvalCombo() {
        if (m_differCombo == null) {
            m_differCombo = new JComboBox(Evaluators.values());
            m_differCombo.addActionListener(this);
        }
        return m_differCombo;
    }

    private int getLowerTollerance() {
        if (m_lowerToleranceSpinner != null) {
            final Object valueObject = m_lowerToleranceSpinner.getValue();
            if (valueObject instanceof Integer) {
                return ((Integer)valueObject).intValue();
            }
        }
        return m_defaultLowerTolerance;
    }

    private int getUpperTollerance() {
        if (m_upperToleranceSpinner != null) {
            final Object valueObject = m_upperToleranceSpinner.getValue();
            if (valueObject instanceof Integer) {
                return ((Integer)valueObject).intValue();
            }
        }
        return m_defaultUpperTolerance;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
            throws NotConfigurableException {
        final String evalString = settings.getString(
                DiffNodeModel.CFGKEY_EVALUATORKEY, Evaluators.TableDiffer
                        .toString());
        Evaluators eval = null;
        try {
            eval = Evaluators.valueOf(evalString);

            getEvalCombo().setSelectedItem(eval);
            if (eval.equals(Evaluators.LearnerScoreComperator)) {
                m_loadedLowerTolerance = settings
                        .getInt(DiffNodeModel.CFGKEY_LOWERTOLERANCEKEY);
                m_loadedUpperTolerance = settings
                        .getInt(DiffNodeModel.CFGKEY_UPPERERTOLERANCEKEY);
            }
            m_lowerToleranceSpinner.setValue(new Integer(m_loadedLowerTolerance));
            m_upperToleranceSpinner.setValue(new Integer(m_loadedUpperTolerance));
        } catch (final IllegalArgumentException e) {
            eval = Evaluators.TableDiffer;
        } catch (final InvalidSettingsException e) {

        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        settings.addString(DiffNodeModel.CFGKEY_EVALUATORKEY,
                ((DiffNodeDialog.Evaluators)getEvalCombo().getSelectedItem())
                        .name());
        if (getEvalCombo().getSelectedItem().equals(
                Evaluators.LearnerScoreComperator)) {
            settings.addInt(DiffNodeModel.CFGKEY_LOWERTOLERANCEKEY,
                    getLowerTollerance());
            settings.addInt(DiffNodeModel.CFGKEY_UPPERERTOLERANCEKEY,
                    getUpperTollerance());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        showSpinners(getEvalCombo().getSelectedItem().equals(
                Evaluators.LearnerScoreComperator));

    }
}
