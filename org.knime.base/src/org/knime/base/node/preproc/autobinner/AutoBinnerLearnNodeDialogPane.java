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
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.preproc.autobinner;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import org.knime.base.node.preproc.autobinner.AutoBinnerLearnSettings.BinNaming;
import org.knime.base.node.preproc.autobinner.AutoBinnerLearnSettings.Method;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.ColumnFilterPanel;


/**
 * Binner dialog used to specify binning rules.
 *
 * @author Heiko Hofer
 */
final class AutoBinnerLearnNodeDialogPane extends NodeDialogPane {
    private final AutoBinnerLearnSettings m_settings;

    private ColumnFilterPanel m_targetCol;
    private JRadioButton m_methodFixedNumber;
    private JRadioButton m_methodSampleQuantiles;
    private JSpinner m_numBins;
    private JTextField m_sampleQuantiles;
    private JRadioButton m_binNamingNumbered;
    private JRadioButton m_binNamingEdges;
    private JCheckBox m_replaceColumn;
    /**
     * Creates a new binner dialog.
     */
    AutoBinnerLearnNodeDialogPane() {
        m_settings = new AutoBinnerLearnSettings();
        addTab("Auto Binner Settings", createAutoBinnerSettingsTab());
    }

    /**
     * @return
     */
    @SuppressWarnings("unchecked")
    private JPanel createAutoBinnerSettingsTab() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(2, 2, 2, 2);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.weightx = 1;
        c.weighty = 1;

        m_targetCol = new ColumnFilterPanel(true, DoubleValue.class);
        p.add(m_targetCol, c);

        c.gridy++;
        p.add(createMethodUIControls(), c);

        c.gridy++;
        p.add(createBinNamingUIControls(), c);

        c.gridy++;
        m_replaceColumn = new JCheckBox("Replace target column(s)");
        p.add(m_replaceColumn, c);
        return p;
    }

    private JPanel createMethodUIControls() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(2, 2, 2, 2);
        c.weightx = 0;


        c.gridy = 0;
        c.gridx = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        m_methodFixedNumber = new JRadioButton("Fixed number of bins");
        m_methodFixedNumber.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                m_numBins.setEnabled(m_methodFixedNumber.isSelected());
                m_sampleQuantiles.setEnabled(
                        !m_methodFixedNumber.isSelected());
            }
        });
        p.add(m_methodFixedNumber, c);

        c.gridy++;
        JPanel numBinsPanel = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 5, 0));
        numBinsPanel.add(new JLabel("Number of bins:"));
        m_numBins = new JSpinner(
                new SpinnerNumberModel(5, 1, Integer.MAX_VALUE, 1));
        numBinsPanel.add(m_numBins);
        numBinsPanel.setBorder(BorderFactory.createEmptyBorder(0, 17, 0, 0));
        p.add(numBinsPanel, c);

        c.gridy++;
        c.gridx = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        m_methodSampleQuantiles = new JRadioButton("Sample quantiles");
        m_methodSampleQuantiles.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                m_numBins.setEnabled(!m_methodSampleQuantiles.isSelected());
                m_sampleQuantiles.setEnabled(
                        m_methodSampleQuantiles.isSelected());
            }
        });
        p.add(m_methodSampleQuantiles, c);

        c.gridy++;
        JPanel quantilesPanel = new JPanel(new GridBagLayout());
        int gridy = c.gridy;
        c.gridy = 0;
        c.gridwidth = 1;
        quantilesPanel.add(new JLabel("Quantiles (comma separated):"), c);
        c.gridx++;
        c.gridwidth = 1;
        c.weightx = 1;
        m_sampleQuantiles = new JTextField();
        quantilesPanel.add(m_sampleQuantiles, c);
        quantilesPanel.setBorder(BorderFactory.createEmptyBorder(0, 17, 0, 0));
        c.gridy = gridy;
        c.gridx = 0;
        c.gridwidth = 1;
        p.add(quantilesPanel, c);

        ButtonGroup method = new ButtonGroup();
        method.add(m_methodFixedNumber);
        method.add(m_methodSampleQuantiles);

        p.setBorder(BorderFactory.createTitledBorder("Binning Method"));
        return p;
    }

    private JPanel createBinNamingUIControls() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(2, 2, 2, 2);

        c.gridy = 0;
        c.gridx = 0;
        c.weightx = 0;

        c.gridwidth = 2;

        m_binNamingNumbered = new JRadioButton("Numbered");
        p.add(m_binNamingNumbered, c);

        c.gridx = 2;
        c.gridwidth = 1;
        c.weightx = 1;
        c.insets = new Insets(6, 2, 2, 2);
        JLabel exampleNumbered = new JLabel("e.g.: Bin 1, Bin 2, Bin 3");
        exampleNumbered.setEnabled(false);
        p.add(exampleNumbered, c);
        c.weightx = 0;
        c.insets = new Insets(2, 2, 2, 2);

        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 1;
        m_binNamingEdges = new JRadioButton("Borders");
        p.add(m_binNamingEdges, c);

        c.gridx = 1;
        c.gridwidth = 2;
        c.weightx = 1;
        c.insets = new Insets(6, 2, 2, 2);
        JLabel exampleEdges = new JLabel("e.g.: [-10,0], (0,10], (10,20]");
        exampleEdges.setEnabled(false);
        p.add(exampleEdges, c);
        c.weightx = 0;
        c.insets = new Insets(2, 2, 2, 2);

        ButtonGroup method = new ButtonGroup();
        method.add(m_binNamingNumbered);
        method.add(m_binNamingEdges);

        p.setBorder(BorderFactory.createTitledBorder("Bin Naming"));
        return p;
    }

    /**
     * @param settings to read intervals from
     * @param specs The input table spec
     * @see NodeDialogPane#loadSettingsFrom(NodeSettingsRO, PortObjectSpec[])
     * @throws NotConfigurableException if the spec contains no columns
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
        m_settings.loadSettingsForDialog(settings);

        m_targetCol.update((DataTableSpec)specs[0], false,
                m_settings.getTargetColumn());
        m_targetCol.setKeepAllSelected(m_settings.getIncludeAll());
        m_methodFixedNumber.setSelected(
                m_settings.getMethod().equals(Method.fixedNumber));
        m_methodSampleQuantiles.setSelected(
                m_settings.getMethod().equals(Method.sampleQuantiles));
        m_numBins.setEnabled(!m_methodSampleQuantiles.isSelected());
        m_sampleQuantiles.setEnabled(
                m_methodSampleQuantiles.isSelected());
        m_numBins.setValue(m_settings.getBinCount());
        m_sampleQuantiles.setText(implode(m_settings.getSampleQuantiles()));
        m_binNamingNumbered.setSelected(
                m_settings.getBinNaming().equals(BinNaming.numbered));
        m_binNamingEdges.setSelected(
                m_settings.getBinNaming().equals(BinNaming.edges));
        m_replaceColumn.setSelected(m_settings.getReplaceColumn());
    }

    private String implode(final double[] values) {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < values.length - 1; i++) {
            str.append(Double.toString(values[i]));
            str.append(", ");
        }
        if (values.length > 0) {
            str.append(values[values.length - 1]);
        }
        return str.toString();
    }

    /**
     * @param settings write intervals to
     * @throws InvalidSettingsException if a bin name is empty
     * @see NodeDialogPane#saveSettingsTo(NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        m_settings.setTargetColumn(
                m_targetCol.getIncludedColumnSet().toArray(new String[0]));
        m_settings.setIncludeAll(m_targetCol.isKeepAllSelected());
        Method method = m_methodFixedNumber.isSelected() ? Method.fixedNumber
                : Method.sampleQuantiles;
        m_settings.setMethod(method);
        m_settings.setBinCount(((Number)m_numBins.getValue()).intValue());
        try {
            List<Double> sampleQuantiles = explode(m_sampleQuantiles.getText());
            double[] sq = normalizeSampleQuantiles(sampleQuantiles);
            m_settings.setSampleQuantiles(sq);
        } catch(NumberFormatException e) {
            throw new InvalidSettingsException(
                    "Cannot parse the list of quantiles. Changes are "
                    + "not applied.");
        }
        BinNaming binNaming = m_binNamingNumbered.isSelected()
            ? BinNaming.numbered : BinNaming.edges;
        m_settings.setBinNaming(binNaming);
        m_settings.setReplaceColumn(m_replaceColumn.isSelected());

        m_settings.saveSettings(settings);
    }


    private List<Double> explode(final String str) {
        StringTokenizer tokenizer = new StringTokenizer(str, ", \t");
        int tokenCount = tokenizer.countTokens();
        List<Double> values = new ArrayList<Double>();
        for (int i = 0; i < tokenCount; i++) {
            values.add(Double.valueOf(tokenizer.nextToken()));
        }
        return values;
    }

    private double[] normalizeSampleQuantiles(final List<Double> sampleQuantiles) {
        // remove elements which are either < 0 or > 1
        for (Iterator<Double> iter = sampleQuantiles.iterator();
            iter.hasNext(); ) {
            Double d = iter.next();
            if (d < 0 || d > 1) {
                iter.remove();
            }
        }
        Collections.sort(sampleQuantiles);
        // remove equal elements
        double prev = -1.0;
        double curr;
        for (Iterator<Double> iter = sampleQuantiles.iterator();
            iter.hasNext(); ) {
            curr = iter.next();
            if (prev == curr) {
                iter.remove();
            } else {
                prev = curr;
            }
        }
        // add zero if not present
        if (sampleQuantiles.get(0) != 0) {
            sampleQuantiles.add(0, 0.0);
        }
        // add one if not present
        if (sampleQuantiles.get(sampleQuantiles.size() - 1) != 1) {
            sampleQuantiles.add(1.0);
        }
        double[] sq = new double[sampleQuantiles.size()];
        for (int i = 0; i < sq.length; i++) {
            sq[i] = sampleQuantiles.get(i);
        }
        return sq;
    }
}
