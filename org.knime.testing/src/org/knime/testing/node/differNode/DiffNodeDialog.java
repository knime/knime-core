/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
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
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;


/**
 * 
 * @author ritmeier, University of Konstanz
 */
public class DiffNodeDialog extends NodeDialogPane implements ActionListener {

    private JComboBox m_differCombo;

    private JPanel m_content;

    private JPanel m_tollerancePanel;

    private static final int m_defaultLowerTollerance = 5;

    private static final int m_defaultUpperTollerance = 0;

    private JSpinner m_lowerTolleranceSpinner;

    private JSpinner m_upperTolleranceSpinner;

    private int m_loadedLowerTollerance = -1;

    private int m_loadedUpperTollerance = -1;

    private JLabel m_lowerTolleranceLable;

    private JLabel m_upperTolleranceLable;

    /**
     * enumeration of differnd evaluators for the test results
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
        m_tollerancePanel = buildTollerancePanel();
        JComboBox combo = getEvalCombo();
        m_content.add(combo, BorderLayout.NORTH);
        m_content.add(m_tollerancePanel, BorderLayout.SOUTH);

        return m_content;
    }

    private JPanel buildTollerancePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(2, 2));

        m_lowerTolleranceLable = new JLabel("lower Tollerance");
        panel.add(m_lowerTolleranceLable);
        SpinnerModel smin = null;
        if (m_loadedLowerTollerance > 0) {
            smin = new SpinnerNumberModel(m_loadedLowerTollerance, 0, 100, 1);
        } else {
            smin = new SpinnerNumberModel(m_defaultLowerTollerance, 0, 100, 1);
        }
        m_lowerTolleranceSpinner = new JSpinner(smin);
        panel.add(m_lowerTolleranceSpinner);

        m_upperTolleranceLable = new JLabel("upper Tollerance");
        panel.add(m_upperTolleranceLable);

        SpinnerModel smax = null;
        if (m_loadedUpperTollerance > 0) {
            smax = new SpinnerNumberModel(m_loadedUpperTollerance, 0, 100, 1);
        } else {
            smax = new SpinnerNumberModel(m_defaultUpperTollerance, 0, 100, 1);
        }
        m_upperTolleranceSpinner = new JSpinner(smax);
        panel.add(m_upperTolleranceSpinner);

        showSpinners(getEvalCombo().getSelectedItem().equals(
                Evaluators.LearnerScoreComperator));

        return panel;
    }

    private void showSpinners(boolean b) {
        
        m_lowerTolleranceLable.setVisible(b);
        m_upperTolleranceLable.setVisible(b);
        m_lowerTolleranceSpinner.setVisible(b);
        m_upperTolleranceSpinner.setVisible(b);
    }

    private JComboBox getEvalCombo() {
        if (m_differCombo == null) {
            m_differCombo = new JComboBox(Evaluators.values());
            m_differCombo.addActionListener(this);
        }
        return m_differCombo;
    }

    private int getLowerTollerance() {
        if (m_lowerTolleranceSpinner != null) {
            Object valueObject = m_lowerTolleranceSpinner.getValue();
            if (valueObject instanceof Integer)
                return ((Integer)valueObject).intValue();
        }
        return m_defaultLowerTollerance;
    }

    private int getUpperTollerance() {
        if (m_upperTolleranceSpinner != null) {
            Object valueObject = m_upperTolleranceSpinner.getValue();
            if (valueObject instanceof Integer)
                return ((Integer)valueObject).intValue();
        }
        return m_defaultUpperTollerance;
    }

    /**
     * @see org.knime.core.node.NodeDialogPane#loadSettingsFrom(org.knime.core.node.NodeSettings,
     *      org.knime.core.data.DataTableSpec[])
     */
    @Override
    protected void loadSettingsFrom(NodeSettingsRO settings, DataTableSpec[] specs)
            throws NotConfigurableException {
        String evalString = settings.getString(
                DiffNodeModel.CFGKEY_EVALUATORKEY, Evaluators.TableDiffer
                        .toString());
        Evaluators eval = null;
        try {
            eval = Evaluators.valueOf(evalString);

            getEvalCombo().setSelectedItem(eval);
            if (eval.equals(Evaluators.LearnerScoreComperator)) {
                m_loadedLowerTollerance = settings
                        .getInt(DiffNodeModel.CFGKEY_LOWERTOLLERANCEKEY);
                m_loadedUpperTollerance = settings
                        .getInt(DiffNodeModel.CFGKEY_UPPERERTOLLERANCEKEY);
            }
            m_lowerTolleranceSpinner.setValue(new Integer(m_loadedLowerTollerance));
            m_upperTolleranceSpinner.setValue(new Integer(m_loadedUpperTollerance));
        } catch (IllegalArgumentException e) {
            eval = Evaluators.TableDiffer;
        } catch (InvalidSettingsException e) {

        }

    }

    /**
     * @see org.knime.core.node.NodeDialogPane#saveSettingsTo(org.knime.core.node.NodeSettings)
     */
    @Override
    protected void saveSettingsTo(NodeSettingsWO settings)
            throws InvalidSettingsException {
        settings.addString(DiffNodeModel.CFGKEY_EVALUATORKEY,
                ((DiffNodeDialog.Evaluators)getEvalCombo().getSelectedItem())
                        .name());
        if (getEvalCombo().getSelectedItem().equals(
                Evaluators.LearnerScoreComperator)) {
            settings.addInt(DiffNodeModel.CFGKEY_LOWERTOLLERANCEKEY,
                    getLowerTollerance());
            settings.addInt(DiffNodeModel.CFGKEY_UPPERERTOLLERANCEKEY,
                    getUpperTollerance());
        }
    }

    public void actionPerformed(ActionEvent e) {
        showSpinners(getEvalCombo().getSelectedItem().equals(
                Evaluators.LearnerScoreComperator));

    }
}
