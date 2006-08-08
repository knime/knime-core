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
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.mine.cluster.fuzzycmeans;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.HashSet;
import java.util.Set;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;

import org.knime.base.node.util.FilterColumnPanel;

/**
 * Dialog for {@link FuzzyClusterNodeModel}- allows to adjust number of
 * clusters and other properties.
 * 
 * @author Michael Berthold, University of Konstanz
 * @author Nicolas Cebron, University of Konstanz
 */
public class FuzzyClusterNodeDialog extends NodeDialogPane {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(FuzzyClusterNodeDialog.class);

    /*
     * private members holding new values, for number of clusters,
     * maximum number of iterations and fuzzifier.
     */
    private JSpinner m_nrClustersSpinner;

    private JSpinner m_maxNrIterationsSpinner;

    private JSpinner m_fuzzifierSpinner;

    private JSpinner m_deltaSpinner;
    
    private JSpinner m_lambdaSpinner;

    /*
     * The maximum number of clusters.
     */
    private static final int MAXNRCLUSTERS = 9999;

    /*
     * The maximum value of the fuzzifier.
     */
    private static final double MAXFUZZIFIER = 10;

    /*
     * Panel to select the columns to use in the clustering process.
     */
    private FilterColumnPanel m_filterpanel;

    /*
     * JRadioButton for selection to provide delta value
     */
    private final JRadioButton m_providedeltaRB = new JRadioButton("set delta");

    /*
     * JRadioButton for selection to NOT provide delta value
     */
    private final JRadioButton m_notprovidedeltaRB = new JRadioButton(
            "Autom. delta, lambda:");

    private final Checkbox m_noisecheck = new Checkbox("Induce noise cluster",
            false);

    /*
     * The tab's name.
     */
    private static final String TAB = "Fuzzy c-means";

    /*
     * The tab2's name.
     */
    private static final String TAB2 = "Included Columns";

    /**
     * Constructor - set name of fuzzy c-means cluster node.
     */
    public FuzzyClusterNodeDialog() {
        super();

        // create panel content for special property-tab
        JPanel clusterPropPane = new JPanel();
        clusterPropPane.setLayout(new GridLayout(4, 2));
        SpinnerNumberModel nrclustersmodel = new SpinnerNumberModel(3, 1,
                MAXNRCLUSTERS, 1);
        m_nrClustersSpinner = new JSpinner(nrclustersmodel);
        JLabel nrClustersLabel = new JLabel("Number of clusters: ");
        clusterPropPane.add(nrClustersLabel);
        clusterPropPane.add(m_nrClustersSpinner);
        SpinnerNumberModel nrmaxiterationsmodel = new SpinnerNumberModel(99, 1,
                9999, 1);
        m_maxNrIterationsSpinner = new JSpinner(nrmaxiterationsmodel);
        JLabel maxNrIterationsLabel = new JLabel("Max. number of iterations: ");
        clusterPropPane.add(maxNrIterationsLabel);
        clusterPropPane.add(m_maxNrIterationsSpinner);
        JLabel fuzzifierLabel = new JLabel("Fuzzifier: ");
        SpinnerNumberModel fuzzifiermodel = new SpinnerNumberModel(2.0, 1.0,
                10.0, .1);
        m_fuzzifierSpinner = new JSpinner(fuzzifiermodel);
        clusterPropPane.add(fuzzifierLabel);
        clusterPropPane.add(m_fuzzifierSpinner);

        JPanel noisepanel = new JPanel();
        noisepanel.setLayout(new GridLayout(3, 2));
        // RadioButtons for choosing delta
        ButtonGroup group = new ButtonGroup();
        group.add(m_providedeltaRB);
        m_providedeltaRB.setEnabled(false);
        m_providedeltaRB.addItemListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                boolean state = m_providedeltaRB.isSelected();
                m_deltaSpinner.setEnabled(state);
                m_lambdaSpinner.setEnabled(false);
            }
        });
        group.add(m_notprovidedeltaRB);
        m_notprovidedeltaRB.setEnabled(false);
        m_notprovidedeltaRB.setSelected(true);
        m_notprovidedeltaRB.addItemListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                boolean state = m_notprovidedeltaRB.isSelected();
                m_deltaSpinner.setEnabled(false);
                m_lambdaSpinner.setEnabled(state);
            }
        });
        
        m_noisecheck.addItemListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                boolean state = m_noisecheck.getState();
                m_providedeltaRB.setEnabled(state);
                m_notprovidedeltaRB.setEnabled(state);
                m_deltaSpinner.setEnabled(state);
                m_lambdaSpinner.setEnabled(state);
                if (state && m_notprovidedeltaRB.isSelected()) {
                    m_deltaSpinner.setEnabled(false);
                    m_lambdaSpinner.setEnabled(true);
                }
                if (state && m_providedeltaRB.isSelected()) {
                    m_deltaSpinner.setEnabled(true);
                    m_lambdaSpinner.setEnabled(false);
                }
            }
        });
        noisepanel.add(m_noisecheck);
        noisepanel.add(new JLabel());
        noisepanel.add(m_providedeltaRB, BorderLayout.WEST);
        SpinnerNumberModel deltaSpinnermodel = new SpinnerNumberModel(.2, .0,
                1.0, .01);
        m_deltaSpinner = new JSpinner(deltaSpinnermodel);
        m_deltaSpinner.setEnabled(false);
        noisepanel.add(m_deltaSpinner);
        noisepanel.add(m_notprovidedeltaRB, BorderLayout.WEST);
        SpinnerNumberModel lambdaSpinnermodel = new SpinnerNumberModel(.1, .0,
                1.0, .01);
        m_lambdaSpinner = new JSpinner(lambdaSpinnermodel);
        m_lambdaSpinner.setEnabled(false);
        noisepanel.add(m_lambdaSpinner);
        clusterPropPane.add(noisepanel);

        super.addTab(TAB, clusterPropPane);
        m_filterpanel = new FilterColumnPanel(DoubleValue.class);
        super.addTab(TAB2, m_filterpanel);
    }

    /**
     * Loads the settings from the model, Number of Clusters and
     * maximum number of Iterations.
     * @see NodeDialogPane#loadSettingsFrom(NodeSettingsRO, DataTableSpec[])
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        assert (settings != null && specs != null);
        if (specs[0].getNumColumns() <= 0) {
            throw new NotConfigurableException("No input data");
        }
        if (settings.containsKey(FuzzyClusterNodeModel.NRCLUSTERS_KEY)) {
            try {
                int tempnrclusters = settings
                        .getInt(FuzzyClusterNodeModel.NRCLUSTERS_KEY);
                if ((1 < tempnrclusters) && (tempnrclusters < MAXNRCLUSTERS)) {
                    m_nrClustersSpinner.setValue(tempnrclusters);
                } else {
                    throw new InvalidSettingsException(
                            "Value out of range for number of"
                                    + " clusters, must be in [1,9999]");
                }
            } catch (InvalidSettingsException e) {
                LOGGER.debug("Invalid Settings", e);
            }
        }

        if (settings.containsKey(FuzzyClusterNodeModel.MAXITERATIONS_KEY)) {
            try {
                int tempmaxiter = settings
                        .getInt(FuzzyClusterNodeModel.MAXITERATIONS_KEY);
                if ((1 <= tempmaxiter) && (tempmaxiter < MAXNRCLUSTERS)) {
                    m_maxNrIterationsSpinner.setValue(tempmaxiter);
                } else {
                    throw new InvalidSettingsException("Value out of range "
                            + "for maximum number of iterations, must be in "
                            + "[1,9999]");
                }
            } catch (InvalidSettingsException e) {
                LOGGER.debug("Invalid Settings", e);
            }
        }

        if (settings.containsKey(FuzzyClusterNodeModel.FUZZIFIER_KEY)) {
            try {
                double tempfuzzifier = settings
                        .getDouble(FuzzyClusterNodeModel.FUZZIFIER_KEY);
                if ((1 < tempfuzzifier) && (tempfuzzifier < MAXFUZZIFIER)) {
                    m_fuzzifierSpinner.setValue(tempfuzzifier);
                } else {
                    throw new InvalidSettingsException("Value out of range "
                            + "for fuzzifier, must be in " + "[>1,10]");
                }
            } catch (InvalidSettingsException e) {
                LOGGER.debug("Invalid Settings", e);
            }
        }

        if (settings.containsKey(FuzzyClusterNodeModel.NOISE_KEY)) {
            try {
                boolean noise = settings
                        .getBoolean(FuzzyClusterNodeModel.NOISE_KEY);
                if (noise) {
                    m_noisecheck.setState(noise);
                    if (settings
                          .containsKey(FuzzyClusterNodeModel.DELTAVALUE_KEY)) {
                        double delta = settings
                              .getDouble(FuzzyClusterNodeModel.DELTAVALUE_KEY);
                        if (delta > 0) {
                            m_providedeltaRB.setEnabled(true);
                            m_notprovidedeltaRB.setEnabled(true);
                            m_providedeltaRB.setSelected(true);
                            m_deltaSpinner.setEnabled(true);
                            m_deltaSpinner.setValue(delta);
                        } 
                    }
                    if (settings.containsKey(
                            FuzzyClusterNodeModel.LAMBDAVALUE_KEY)) {
                        double lambda = settings.getDouble(
                                FuzzyClusterNodeModel.LAMBDAVALUE_KEY);
                        if (lambda > 0) {
                            m_notprovidedeltaRB.setEnabled(true);
                            m_providedeltaRB.setEnabled(true);
                            m_notprovidedeltaRB.setSelected(true);
                            m_lambdaSpinner.setEnabled(true);
                            m_lambdaSpinner.setValue(lambda);
                        }
                    }
                } 
            } catch (InvalidSettingsException e) {
                LOGGER.debug("Invalid Settings", e);
            }
        } else {
            m_providedeltaRB.setEnabled(false);
            m_notprovidedeltaRB.setEnabled(false);
        }
        if (specs[FuzzyClusterNodeModel.INPORT] == null) {
            // settings can't be evaluated against the spec
            return;
        }
        FilterColumnPanel p = (FilterColumnPanel)getTab(TAB2);
        if (settings.containsKey(FuzzyClusterNodeModel.INCLUDELIST_KEY)) {
            String[] columns = settings.getStringArray(
                    FuzzyClusterNodeModel.INCLUDELIST_KEY, new String[0]);
            HashSet<String> list = new HashSet<String>();
            for (int i = 0; i < columns.length; i++) {
                if (specs[FuzzyClusterNodeModel.INPORT]
                        .containsName(columns[i])) {
                    list.add(columns[i]);
                }
            }
            // set include list on the panel
            p.update(specs[FuzzyClusterNodeModel.INPORT], false, list);
        } else {
            p.update(specs[FuzzyClusterNodeModel.INPORT], true, new String[]{});
        }
    }

    /**
     * Save the settings from the dialog, Number of Clusters and
     * maximum number of Iterations. 
     * 
     * @see NodeDialogPane#saveSettingsTo(NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        assert (settings != null);

        int tempnrclusters = (Integer)m_nrClustersSpinner.getValue();
        int tempmaxiter = (Integer)m_maxNrIterationsSpinner.getValue();
        double tempfuzzifier = (Double)m_fuzzifierSpinner.getValue();
        if ((1 <= tempnrclusters) && (tempnrclusters < MAXNRCLUSTERS)) {
            settings.addInt(FuzzyClusterNodeModel.NRCLUSTERS_KEY,
                    tempnrclusters);
        } else {
            throw new InvalidSettingsException(
                    "Value out of range for number of"
                            + " clusters, must be in [1,9999]");
        }
        if ((1 <= tempmaxiter) && (tempmaxiter < MAXNRCLUSTERS)) {
            settings.addInt(FuzzyClusterNodeModel.MAXITERATIONS_KEY,
                    tempmaxiter);
        } else {
            throw new InvalidSettingsException("Value out of range "
                    + "for maximum number of iterations, must be in "
                    + "[1,9999]");
        }
        if ((1 < tempfuzzifier) && (tempfuzzifier < MAXFUZZIFIER)) {
            settings.addDouble(FuzzyClusterNodeModel.FUZZIFIER_KEY,
                    tempfuzzifier);
        } else {
            throw new InvalidSettingsException("Value out of range "
                    + "for fuzzifier, must be in " + "[>1,10]");
        }
        m_filterpanel = (FilterColumnPanel)getTab(TAB2);
        Set<String> list = m_filterpanel.getIncludedColumnList();
        settings.addStringArray(FuzzyClusterNodeModel.INCLUDELIST_KEY, list
                .toArray(new String[0]));

        settings.addBoolean(FuzzyClusterNodeModel.NOISE_KEY, m_noisecheck
                .getState());
        if (m_providedeltaRB.isSelected()) {
            settings.addDouble(FuzzyClusterNodeModel.DELTAVALUE_KEY,
                    (Double)m_deltaSpinner.getValue());
        } else {
            settings.addDouble(FuzzyClusterNodeModel.DELTAVALUE_KEY, -1);
        }
        if (m_notprovidedeltaRB.isSelected()) {
            settings.addDouble(FuzzyClusterNodeModel.LAMBDAVALUE_KEY,
                    (Double)m_lambdaSpinner.getValue());
        } else {
            settings.addDouble(FuzzyClusterNodeModel.LAMBDAVALUE_KEY, -1);
        }

    }
}
