/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2013
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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JSpinner.NumberEditor;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.testing.internal.nodes.image.ImageDifferNodeFactory;


/**
 *
 * @author ritmeier, University of Konstanz
 * @deprecated use the new image comparator {@link ImageDifferNodeFactory} and the extension point for difference
 *             checker instead
 */
@Deprecated
public class DiffNodeDialog extends NodeDialogPane implements ActionListener {

    private JComboBox m_differCombo;

    private static final int defaultLowerTolerance = 5;

    private static final int defaultUpperTolerance = 0;

    private JSpinner m_lowerToleranceSpinner;

    private JSpinner m_upperToleranceSpinner;

    private int m_loadedLowerTolerance = 0;

    private int m_loadedUpperTolerance = 0;

    private JLabel m_lowerToleranceLable;

    private JLabel m_upperToleranceLable;

    private JLabel m_epsilonLabel = new JLabel("Relative deviation for floating point values   ");

    private final JSpinner m_epsilonSpinner = new JSpinner(new SpinnerNumberModel(0.001, 0, 1, 0.001));

    /**
     * enumeration of different evaluators for the test results.
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
         * get n LearnerScoreComperator.
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
     * creates the panels.
     *
     * @return Component with content
     */
    private Component buildContentPanel() {
        JPanel content = new JPanel();
        content.setLayout(new BorderLayout());
        JPanel tolerancePanel = buildTolerancePanel();
        final JComboBox combo = getEvalCombo();
        content.add(combo, BorderLayout.NORTH);
        content.add(tolerancePanel, BorderLayout.SOUTH);
        content.add(buildEpsilonPanel(), BorderLayout.SOUTH);

        return content;
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
            smin = new SpinnerNumberModel(defaultLowerTolerance, 0, 100, 1);
        }
        m_lowerToleranceSpinner = new JSpinner(smin);
        panel.add(m_lowerToleranceSpinner);

        m_upperToleranceLable = new JLabel("upper tolerance (error%)");
        panel.add(m_upperToleranceLable);

        SpinnerModel smax = null;
        if (m_loadedUpperTolerance > 0) {
            smax = new SpinnerNumberModel(m_loadedUpperTolerance, 0, 100, 1);
        } else {
            smax = new SpinnerNumberModel(defaultUpperTolerance, 0, 100, 1);
        }
        m_upperToleranceSpinner = new JSpinner(smax);
        panel.add(m_upperToleranceSpinner);

        showToleranceSpinners(getEvalCombo().getSelectedItem().equals(
                Evaluators.LearnerScoreComperator));

        return panel;
    }

    private JPanel buildEpsilonPanel() {
        final JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(1, 2));

        ((NumberEditor) m_epsilonSpinner.getEditor()).getFormat().applyPattern("#0.000");
        panel.add(m_epsilonLabel);
        panel.add(m_epsilonSpinner);
        return panel;
    }
    private void showToleranceSpinners(final boolean b) {
        m_lowerToleranceLable.setVisible(b);
        m_upperToleranceLable.setVisible(b);
        m_lowerToleranceSpinner.setVisible(b);
        m_upperToleranceSpinner.setVisible(b);
    }

    private void showEpsilonSpinner(final boolean b) {
        m_epsilonLabel.setVisible(b);
        m_epsilonSpinner.setVisible(b);
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
        return defaultLowerTolerance;
    }

    private int getUpperTollerance() {
        if (m_upperToleranceSpinner != null) {
            final Object valueObject = m_upperToleranceSpinner.getValue();
            if (valueObject instanceof Integer) {
                return ((Integer)valueObject).intValue();
            }
        }
        return defaultUpperTolerance;
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
            double loadedEpsilon = settings.getDouble(DiffNodeModel.CFGKEY_EPSILON, 0);

            m_lowerToleranceSpinner.setValue(m_loadedLowerTolerance);
            m_upperToleranceSpinner.setValue(m_loadedUpperTolerance);
            m_epsilonSpinner.setValue(loadedEpsilon);
        } catch (final IllegalArgumentException e) {
            eval = Evaluators.TableDiffer;
        } catch (final InvalidSettingsException e) {
            // ignore it
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
        settings.addDouble(DiffNodeModel.CFGKEY_EPSILON,
                (Double)m_epsilonSpinner.getValue());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        showToleranceSpinners(getEvalCombo().getSelectedItem().equals(
                Evaluators.LearnerScoreComperator));
        showEpsilonSpinner(getEvalCombo().getSelectedItem().equals(
                Evaluators.TableDiffer));

    }
}
