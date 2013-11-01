/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
package org.knime.base.node.mine.bfn;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import org.knime.base.node.mine.util.PredictorHelper;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;


/**
 * A dialog to apply data to basis functions. Can be used to set a name for the
 * new, applied column.
 *
 * @author Thomas Gabriel, University of Konstanz
 * @since 2.9
 */
public class BasisFunctionPredictor2NodeDialog extends NodeDialogPane {

    /** Prediction column. */
    private final JTextField m_apply = new JTextField();

    /** Prediction column override checkbox. */
    private final JCheckBox m_override = new JCheckBox(PredictorHelper.CHANGE_PREDICTION_COLUMN_NAME);

    /** Probability columns' suffices. */
    private final JTextField m_suffix = new JTextField();

    private final JRadioButton m_dftButton;

    private final JRadioButton m_setButton;

    private final JCheckBox m_ignButton;

    private final JSpinner m_dontKnow;

    private final JCheckBox m_appendProp;

    /** Key for the applied column: <i>apply_column</i>. */
    public static final String APPLY_COLUMN = PredictorHelper.CFGKEY_PREDICTION_COLUMN;

    /** Key for don't know probability for the unknown class. */
    public static final String DONT_KNOW_PROP = "dont_know_prop";

    /** Config key if don't know should be ignored. */
    public static final String CFG_DONT_KNOW_IGNORE = "ignore_dont_know";

    /** Config key if class probabilities should be appended to the table. */
    public static final String CFG_CLASS_PROPS = "append_class_probabilities";

    private DataColumnSpec m_lastTrainingColumn;

    /**
     * Creates a new predictor dialog to set a name for the applied column.
     */
    public BasisFunctionPredictor2NodeDialog() {
        super();
        // panel with advance settings
        JPanel p = new JPanel(new GridLayout(3, 1));

        // add apply column
        m_apply.setPreferredSize(new Dimension(175, 25));
        JPanel normPanel = new JPanel();
        normPanel.setBorder(BorderFactory.createTitledBorder(" Choose Name "));
        normPanel.add(m_override);
        normPanel.add(m_apply);

        // append class probabilities
        m_appendProp = new JCheckBox("Append Class Columns", true);
        m_appendProp.setPreferredSize(new Dimension(175, 25));
        JPanel propPanel = new JPanel();
        propPanel.setBorder(
                BorderFactory.createTitledBorder(" Class Probabilities "));
        propPanel.add(m_appendProp);
        propPanel.add(new JLabel("Suffix for probability columns"));
        m_suffix.setPreferredSize(new Dimension(175, 25));
        propPanel.add(m_suffix);

        // add don't know probability
        m_dftButton = new JRadioButton("Default ", true);
        m_setButton = new JRadioButton("Use ");
        m_ignButton = new JCheckBox("Ignore ", true);
        m_ignButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                selectionChanged();
            }
        });
        ButtonGroup bg = new ButtonGroup();
        bg.add(m_dftButton);
        bg.add(m_setButton);
        m_dontKnow = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 1.0, 0.1));
        m_dontKnow.setEditor(new JSpinner.NumberEditor(
                m_dontKnow, "#.##########"));
        m_dontKnow.setPreferredSize(new Dimension(75, 25));
        JPanel dontKnowPanel = new JPanel(new BorderLayout());
        dontKnowPanel.setBorder(BorderFactory
                .createTitledBorder(" Don't Know Class "));
        dontKnowPanel.add(m_ignButton, BorderLayout.NORTH);
        JPanel dftPanel = new JPanel(new FlowLayout());
        dftPanel.setBorder(BorderFactory
                .createTitledBorder(""));
        dftPanel.add(m_dftButton);
        dftPanel.add(m_setButton);
        dftPanel.add(m_dontKnow);
        dontKnowPanel.add(dftPanel, BorderLayout.CENTER);
        p.add(dontKnowPanel);

        m_dftButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                m_dontKnow.setEnabled(false);
            }
        });

        m_setButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                m_dontKnow.setEnabled(true);
            }
        });
        m_override.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                boolean selected = m_override.isSelected();
                m_apply.setEnabled(selected);
                if (!selected) {
                    final String predictionDefault =
                        PredictorHelper.getInstance().computePredictionDefault(m_lastTrainingColumn.getName());
                    m_apply.setText(predictionDefault);
                }
            }});
        p.add(normPanel);
        p.add(propPanel);
        super.addTab("Prediction Column", p);
    }

    private void selectionChanged() {
        if (m_ignButton.isSelected()) {
            m_dftButton.setEnabled(false);
            m_setButton.setEnabled(false);
            m_dontKnow.setEnabled(false);
        } else {
            m_dftButton.setEnabled(true);
            m_setButton.setEnabled(true);
            if (m_dftButton.isSelected()) {
                m_dontKnow.setEnabled(false);
            } else {
                m_dontKnow.setEnabled(true);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        PredictorHelper ph = PredictorHelper.getInstance();
        // prediction column name
        String apply;
        try {
            final SettingsModelString prediction = ph.createPredictionColumn();
            final SettingsModelBoolean overridePred = ph.createChangePrediction();
            apply = prediction.getStringValue();
            overridePred.loadSettingsFrom(settings);
            prediction.loadSettingsFrom(settings);
            boolean enabled = overridePred.getBooleanValue();
            //m_override.setSelected(!enabled);
            m_override.setSelected(enabled);
        } catch (InvalidSettingsException e) {
            throw new NotConfigurableException(e.getMessage(), e);
        }
        m_apply.setText(apply);
        if (settings.getBoolean(CFG_DONT_KNOW_IGNORE, false)) {
            m_ignButton.setSelected(true);
            m_dontKnow.setValue(new Double(0.0));
        } else {
            m_ignButton.setSelected(false);
            double value = settings.getDouble(DONT_KNOW_PROP, -1.0);
            if (value < 0.0) {
                m_dftButton.setSelected(true);
                m_dontKnow.setValue(new Double(0.0));
            } else {
                m_setButton.setSelected(true);
                m_dontKnow.setValue(new Double(value));
            }
        }
        m_appendProp.setSelected(settings.getBoolean(CFG_CLASS_PROPS, true));
        m_lastTrainingColumn = specs[0].getColumnSpec(specs[0].getNumColumns() - 5);
        selectionChanged();
        for (final ActionListener listener: m_override.getActionListeners()) {
            listener.actionPerformed(null);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        // prediction column name
        String s = m_apply.getText().trim();
        if (s.length() == 0) {
            throw new InvalidSettingsException("Empty name not allowed.");
        }
        //settings.addString(APPLY_COLUMN, s);
        if (m_ignButton.isSelected()) {
            settings.addBoolean(CFG_DONT_KNOW_IGNORE, true);
            settings.addDouble(DONT_KNOW_PROP, 0.0);
        } else {
            settings.addBoolean(CFG_DONT_KNOW_IGNORE, false);
            if (m_dftButton.isSelected()) {
                settings.addDouble(DONT_KNOW_PROP, -1.0);
            } else {
                Double value = (Double)m_dontKnow.getValue();
                settings.addDouble(DONT_KNOW_PROP, value.doubleValue());
            }
        }
        settings.addBoolean(CFG_CLASS_PROPS, m_appendProp.isSelected());
        PredictorHelper predictorHelper = PredictorHelper.getInstance();
        final SettingsModelString predictionColumn = predictorHelper.createPredictionColumn();
        final SettingsModelBoolean overridePred = predictorHelper.createChangePrediction();
        overridePred.setBooleanValue(!m_override.isSelected());
        predictionColumn.setStringValue(s);
        predictionColumn.saveSettingsTo(settings);
        overridePred.saveSettingsTo(settings);
        final SettingsModelString suffix = predictorHelper.createSuffix();
        suffix.setStringValue(m_suffix.getText());
        suffix.saveSettingsTo(settings);
    }
}
