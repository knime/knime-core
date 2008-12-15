/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 * -------------------------------------------------------------------
 * 
 */
package org.knime.base.node.util.sampledata;

import java.awt.FlowLayout;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
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
 * @author Bernd Wiswedel, University of Konstanz
 */
public class SampleDataNodeDialog extends NodeDialogPane {
    private final JTextField m_clusterCountField;

    private final JTextField m_universeSizeField;

    private final JSpinner m_patternCountSpinner;

    private final JSpinner m_devSpinner;

    private final JSpinner m_noiseSpinner;

    private final JFormattedTextField m_seedField;

    /**
     * Create a new sample data dialog.
     */
    public SampleDataNodeDialog() {
        super();
        m_clusterCountField = new JTextField(8);
        m_universeSizeField = new JTextField(8);
        SpinnerModel spinModel = new SpinnerNumberModel(100, 0,
                Integer.MAX_VALUE, 100);
        m_patternCountSpinner = new JSpinner(spinModel);
        JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor)m_patternCountSpinner
                .getEditor();
        editor.getTextField().setColumns(8);
        spinModel = new SpinnerNumberModel(0.1, 0, Double.MAX_VALUE, 0.01);
        m_devSpinner = new JSpinner(spinModel);
        editor = (JSpinner.DefaultEditor)m_devSpinner.getEditor();
        editor.getTextField().setColumns(8);
        spinModel = new SpinnerNumberModel(0.0, 0.0, 1.0, 0.1);
        m_noiseSpinner = new JSpinner(spinModel);
        editor = (JSpinner.DefaultEditor)m_noiseSpinner.getEditor();
        editor.getTextField().setColumns(8);
        JPanel panel = new JPanel(new GridLayout(0, 1));
        m_seedField = new JFormattedTextField(new Integer(1));
        m_seedField.setColumns(8);
        panel.add(getInFlowLayout(m_clusterCountField, " Cluster Count "));
        panel.add(getInFlowLayout(m_universeSizeField, " Universe Sizes "));
        panel.add(getInFlowLayout(m_patternCountSpinner, " Pattern Count "));
        panel.add(getInFlowLayout(m_devSpinner, " Standard Deviation "));
        panel.add(getInFlowLayout(m_noiseSpinner, " Noise Fraction "));
        panel.add(getInFlowLayout(m_seedField, " Random Seed "));
        addTab("Sample Generator", panel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        double dev = settings.getDouble(SampleDataNodeModel.CFGKEY_DEV, 0.1);
        int patCount = settings
                .getInt(SampleDataNodeModel.CFGKEY_PATCOUNT, 100);
        int[] uniSizes = settings.getIntArray(
                SampleDataNodeModel.CFGKEY_UNISIZE, new int[]{1});
        int[] clusCounts = settings.getIntArray(
                SampleDataNodeModel.CFGKEY_CLUSCOUNT, new int[]{1});
        double no = settings.getDouble(SampleDataNodeModel.CFGKEY_NOISE, 0.0);
        int seed = settings.getInt(SampleDataNodeModel.CFGKEY_SEED, 1);
        m_noiseSpinner.setValue(new Double(no));
        m_devSpinner.setValue(new Double(dev));
        m_patternCountSpinner.setValue(new Integer(patCount));
        m_seedField.setValue(new Integer(seed));
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < uniSizes.length; i++) {
            buf.append((i > 0 ? ", " : "") + uniSizes[i]);
        }
        m_universeSizeField.setText(buf.toString());
        buf = new StringBuffer();
        for (int i = 0; i < clusCounts.length; i++) {
            buf.append((i > 0 ? ", " : "") + clusCounts[i]);
        }
        m_clusterCountField.setText(buf.toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        double dev = ((Double)m_devSpinner.getValue()).doubleValue();
        settings.addDouble(SampleDataNodeModel.CFGKEY_DEV, dev);

        double noise = ((Double)m_noiseSpinner.getValue()).doubleValue();
        settings.addDouble(SampleDataNodeModel.CFGKEY_NOISE, noise);

        int patCount = ((Integer)m_patternCountSpinner.getValue()).intValue();
        settings.addInt(SampleDataNodeModel.CFGKEY_PATCOUNT, patCount);

        int seed = ((Integer)m_seedField.getValue()).intValue();
        settings.addInt(SampleDataNodeModel.CFGKEY_SEED, seed);

        String t = m_clusterCountField.getText();
        int[] tVal = getValues(t);
        settings.addIntArray(SampleDataNodeModel.CFGKEY_CLUSCOUNT, tVal);

        t = m_universeSizeField.getText();
        tVal = getValues(t);
        settings.addIntArray(SampleDataNodeModel.CFGKEY_UNISIZE, tVal);

    }

    /**
     * Takes a component, puts it in a panel with a flowlayout and surrenders it
     * with a titled border.
     */
    private static JPanel getInFlowLayout(final JComponent comp,
            final String borderTitle) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p.setBorder(BorderFactory.createTitledBorder(borderTitle));
        p.add(comp);
        return p;
    }

    private static int[] getValues(final String text)
            throws InvalidSettingsException {
        int[] returnMe;
        try {
            String[] split = text.split("\\s*?,\\s*?");
            returnMe = new int[split.length];
            for (int i = 0; i < returnMe.length; i++) {
                int k = Integer.valueOf(split[i].trim()).intValue();
                returnMe[i] = k;
            }
        } catch (Exception e) {
            throw new InvalidSettingsException(e);
        }
        return returnMe;
    }

}
