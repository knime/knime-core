/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 * ---------------------------------------------------------------------
 *
 * Created on 2013.11.04. by Gabor Bakos
 */
package org.knime.base.node.mine.util;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;

/**
 * The base class for the predictor nodes' dialogs.
 *
 * @author Gabor Bakos
 * @since 2.9
 */
public class PredictorNodeDialog extends NodeDialogPane {
    /**
     * Name of the main panel.
     */
    private static final String MAIN_PANEL = "MainPanel";

    /**
     *
     */
    private static final int INDENT = 25;

    private static final int GAP = 2;

    private static final int HGAP = GAP;

    private static final int VGAP = GAP;

    private static final int MODEL_PORT = 0;

    private JCheckBox m_overridePred;

    private JTextField m_predColName;

    private JCheckBox m_addProbs;

    private JTextField m_suffix;

    private final SettingsModelBoolean m_overridePredModel;

    private final SettingsModelString m_predictionColModel;

    private final SettingsModelBoolean m_addProbsModel;

    private final SettingsModelString m_suffixModel;

    private JComponent m_lastAdded;

    private final List<DialogComponent> m_dialogComponents = new ArrayList<DialogComponent>();

    private DataColumnSpec m_lastTargetColumn;

    private SpringLayout m_layout = new SpringLayout();

    private SpringLayout m_additionalLayout = new SpringLayout();

    /**
     * Creates the dialog with the {@link PredictorHelper} generated {@link SettingsModel}s.
     *
     * @param addProbabilities The add probabilities {@link SettingsModelBoolean}.
     * @see #PredictorNodeDialog(SettingsModelBoolean, SettingsModelString, SettingsModelBoolean, SettingsModelString)
     */
    protected PredictorNodeDialog(final SettingsModelBoolean addProbabilities) {
        this(PredictorHelper.getInstance().createChangePrediction(), PredictorHelper.getInstance()
            .createPredictionColumn(), addProbabilities, PredictorHelper.getInstance().createSuffix());
    }

    /**
     * Creates the dialog with custom {@link SettingsModel}s.
     *
     * @param overridePrediction Use custom prediction column name?
     * @param customPrediction Use this name.
     * @param addProbabilities Add probability columns?
     * @param probabilitySuffix With this suffix.
     *
     */
    protected PredictorNodeDialog(final SettingsModelBoolean overridePrediction,
        final SettingsModelString customPrediction, final SettingsModelBoolean addProbabilities,
        final SettingsModelString probabilitySuffix) {
        m_overridePredModel = overridePrediction;
        m_predictionColModel = customPrediction;
        m_addProbsModel = addProbabilities;
        m_suffixModel = probabilitySuffix;
        JPanel mainPanel = new JPanel(m_layout);
        mainPanel.setName(MAIN_PANEL);
        addTab("Options", mainPanel);

        //Dummy control only for SpringLayout.
        final JPanel additionalControls = new JPanel(m_additionalLayout);
        //        additionalControls.setName(MAIN_PANEL);
        mainPanel.add(additionalControls);
        getLayout().putConstraint(SpringLayout.WEST, additionalControls, 0, SpringLayout.WEST, mainPanel);
        getLayout().putConstraint(SpringLayout.NORTH, additionalControls, 0, SpringLayout.NORTH, mainPanel);
        getLayout().putConstraint(SpringLayout.EAST, additionalControls, 0, SpringLayout.EAST, mainPanel);
        m_lastAdded = additionalControls;
        addOtherControls(mainPanel);
        m_overridePred = new JCheckBox(PredictorHelper.CHANGE_PREDICTION_COLUMN_NAME);
        mainPanel.add(m_overridePred);
        m_predColName = new JTextField();
        mainPanel.add(m_predColName);
        m_addProbs = new JCheckBox(PredictorHelper.APPEND_COLUMNS_WITH_NORMALIZED_CLASS_DISTRIBUTION);
        mainPanel.add(m_addProbs);
        final JLabel labelSuffix = new JLabel(PredictorHelper.SUFFIX_FOR_PROBABILITY_COLUMNS);
        mainPanel.add(labelSuffix);
        m_suffix = new JTextField();
        labelSuffix.setLabelFor(m_suffix);
        mainPanel.add(m_suffix);
        //The suffix label is left to the suffix field.
        getLayout().putConstraint(SpringLayout.WEST, labelSuffix, INDENT, SpringLayout.WEST, m_addProbs);
        getLayout().putConstraint(SpringLayout.NORTH, labelSuffix, VGAP, SpringLayout.SOUTH, m_addProbs);
        getLayout().putConstraint(SpringLayout.WEST, m_suffix, HGAP, SpringLayout.EAST, labelSuffix);
        constraints(m_lastAdded);
        //Listen for model changes.
        overridePrediction.addChangeListener(listenerForBooleanValues(m_overridePred, overridePrediction));
        customPrediction.addChangeListener(listenerForStringValues(m_predColName, null, customPrediction));
        customPrediction.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                if (!customPrediction.isEnabled()) {
                    PredictorHelper ph = PredictorHelper.getInstance();
                    DataColumnSpec lastTargetColumn = getLastTargetColumn();
                    customPrediction.setStringValue(ph.computePredictionDefault(lastTargetColumn == null ? ""
                        : lastTargetColumn.getName()));
                }
            }
        });
        addProbabilities.addChangeListener(listenerForBooleanValues(m_addProbs, addProbabilities));
        probabilitySuffix.addChangeListener(listenerForStringValues(m_suffix, labelSuffix, probabilitySuffix));

        //UI changes change the model.
        m_overridePred.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                overridePrediction.setBooleanValue(m_overridePred.isSelected());
                customPrediction.setEnabled(overridePrediction.getBooleanValue());
            }
        });
        m_addProbs.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                addProbabilities.setBooleanValue(m_addProbs.isSelected());
                boolean add = addProbabilities.getBooleanValue();
                m_suffix.setEnabled(add);
                labelSuffix.setEnabled(add);
            }
        });
        m_predColName.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                m_predictionColModel.setStringValue(m_predColName.getText());
                m_predictionColModel.setEnabled(m_predColName.isEnabled());
            }
        });
        m_suffix.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                m_suffixModel.setStringValue(m_suffix.getText());
                m_suffixModel.setEnabled(m_suffix.isEnabled());
            }
        });
        addControlsOnNewTabs();
    }

    /**
     * @param lastAdded The reference for the previous controls (the last one on the same tab).
     */
    protected void constraints(final JComponent lastAdded) {
        Component parent = lastAdded.getParent();
        Component lastRef = lastAdded;
        getLayout().putConstraint(SpringLayout.WEST, m_overridePred, 0, SpringLayout.WEST, parent);
        getLayout().putConstraint(SpringLayout.NORTH, m_overridePred, VGAP, SpringLayout.SOUTH, lastRef);

        lastRef = m_overridePred;
        getLayout().putConstraint(SpringLayout.WEST, m_predColName, INDENT, SpringLayout.WEST, lastAdded);
        getLayout().putConstraint(SpringLayout.NORTH, m_predColName, 0, SpringLayout.SOUTH, lastRef);
        getLayout().putConstraint(SpringLayout.EAST, m_predColName, -HGAP, SpringLayout.EAST, parent);

        lastRef = m_predColName;
        getLayout().putConstraint(SpringLayout.WEST, m_addProbs, 0, SpringLayout.WEST, lastAdded);
        getLayout().putConstraint(SpringLayout.NORTH, m_addProbs, VGAP, SpringLayout.SOUTH, lastRef);

        lastRef = m_addProbs;
        getLayout().putConstraint(SpringLayout.NORTH, m_suffix, 0, SpringLayout.SOUTH, lastRef);
        getLayout().putConstraint(SpringLayout.EAST, m_suffix, -HGAP, SpringLayout.EAST, parent);
    }

    /**
     * Default listener for a {@link SettingsModelBoolean} with a {@link JCheckBox} to update.
     *
     * @param comp The {@link JCheckBox}.
     * @param model The model.
     * @return The {@link ChangeListener} that updates the UI according to the model.
     */
    protected ChangeListener listenerForBooleanValues(final JCheckBox comp, final SettingsModelBoolean model) {
        ChangeListener ret = new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                comp.setEnabled(model.isEnabled());
                comp.setSelected(model.getBooleanValue());
            }
        };
        return ret;
    }

    /**
     * Default listener for a {@link SettingsModelString} with a {@link JTextField} (and optionally a {@link JLabel}) to
     * update.
     *
     * @param field The {@link JTextField}.
     * @param label The possibly {@code null} value to update for enabledness changes.
     * @param model The model.
     * @return The {@link ChangeListener} that updates the UI according to the model.
     */
    protected ChangeListener listenerForStringValues(final JTextField field, final JLabel label,
        final SettingsModelString model) {
        ChangeListener ret = new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                field.setEnabled(model.isEnabled());
                field.setText(model.getStringValue());
                if (label != null) {
                    label.setEnabled(model.isEnabled());
                }
            }
        };
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        for (DialogComponent comp : m_dialogComponents) {
            comp.saveSettingsTo(settings);
        }
        m_overridePredModel.setBooleanValue(m_overridePred.isSelected());
        m_overridePredModel.saveSettingsTo(settings);
        m_predictionColModel.setEnabled(m_predColName.isEnabled());
        m_predictionColModel.setStringValue(m_predColName.getText());
        if (m_predColName.isEnabled() && m_predColName.getText().isEmpty()) {
            throw new InvalidSettingsException("Please specify a prediction column name.");
        }
        m_predictionColModel.saveSettingsTo(settings);
        m_addProbsModel.setBooleanValue(m_addProbs.isSelected());
        m_addProbsModel.saveSettingsTo(settings);
        m_suffixModel.setEnabled(m_suffix.isEnabled());
        m_suffixModel.setStringValue(m_suffix.getText());
        m_suffixModel.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        extractTargetColumn(specs);
        for (DialogComponent comp : m_dialogComponents) {
            comp.loadSettingsFrom(settings, specs);
        }
        try {
            m_overridePredModel.loadSettingsFrom(settings);
        } catch (InvalidSettingsException ex) {
            getLogger().debug(ex.getMessage(), ex);
            m_overridePredModel.setEnabled(true);
            m_overridePredModel.setBooleanValue(false);
        }
        try {
            m_predictionColModel.loadSettingsFrom(settings);
        } catch (InvalidSettingsException ex) {
            getLogger().debug(ex.getMessage(), ex);
            m_predictionColModel.setEnabled(m_overridePredModel.getBooleanValue());
            m_predictionColModel.setStringValue("");
        }
        m_overridePred.getModel().setSelected(m_overridePredModel.getBooleanValue());
        try {
            m_addProbsModel.loadSettingsFrom(settings);
        } catch (InvalidSettingsException ex) {
            getLogger().debug(ex.getMessage(), ex);
            m_addProbsModel.setEnabled(true);
            m_addProbsModel.setBooleanValue(true);
        }
        m_addProbs.getModel().setSelected(m_addProbsModel.getBooleanValue());
        try {
            m_suffixModel.loadSettingsFrom(settings);
        } catch (InvalidSettingsException ex) {
            getLogger().debug(ex.getMessage(), ex);
            m_suffixModel.setEnabled(m_addProbsModel.getBooleanValue());
            m_suffixModel.setStringValue("");
        }
        for (ActionListener listener : m_addProbs.getActionListeners()) {
            listener.actionPerformed(null);
        }
        for (ActionListener listener : m_overridePred.getActionListeners()) {
            listener.actionPerformed(null);
        }
    }

    /**
     * Updates the last target column property based on input. The default implementation assumes a
     * {@link PMMLPortObjectSpec} on the first {@code 0} port with a single target column name. Please override if this
     * is not the case.
     *
     * @param specs The {@link PortObjectSpec}s containing the model specific info.
     * @see #setLastTargetColumn(DataColumnSpec)
     */
    protected void extractTargetColumn(final PortObjectSpec[] specs) {
        if (specs[MODEL_PORT] instanceof PMMLPortObjectSpec) {
            PMMLPortObjectSpec spec = (PMMLPortObjectSpec)specs[MODEL_PORT];
            setLastTargetColumn(spec.getTargetCols().iterator().next());
        } else if (specs[MODEL_PORT] == null) {
            setLastTargetColumn(null);
        } else {
            throw new IllegalStateException("Please implement this method properly for the class:\n" + this.getClass());
        }
    }

    /**
     * Override this if you want to add additional controls on the same tab before the common controls. <br>
     * You can specify their arrangement using the {@link #getLayout() SpringLayout}. An easier option for
     * {@link DialogComponent}s is the {@link #addDialogComponent(JPanel, DialogComponent)} method.
     * <br>
     * This method is called once in the constructor.
     *
     * @param panel The panel where you can add the controls.
     */
    protected void addOtherControls(final JPanel panel) {

    }

    /**
     * Adds the {@link DialogComponent#getComponentPanel() dialog's panel} to the {@code panel} left aligned, below the
     * previous control.
     *
     * @param panel The to be parent panel.
     * @param component A {@link DialogComponent}.
     * @return The {@link DialogComponent} argument.
     */
    protected final DialogComponent addDialogComponent(final JPanel panel, final DialogComponent component) {
        panel.add(component.getComponentPanel());
        m_dialogComponents.add(component);
        m_additionalLayout.putConstraint(SpringLayout.WEST, panel, 0, SpringLayout.WEST, component.getComponentPanel());
        m_additionalLayout.putConstraint(MAIN_PANEL.equals(m_lastAdded.getName()) ? SpringLayout.NORTH
            : SpringLayout.SOUTH, m_lastAdded, VGAP, SpringLayout.NORTH, component.getComponentPanel());
        m_lastAdded = component.getComponentPanel();
        return component;
    }

    /**
     * Add controls on a new tab, called after the first tab is arranged in the constructor.
     */
    protected void addControlsOnNewTabs() {

    }

    /**
     * @return the lastTargetColumn
     */
    protected DataColumnSpec getLastTargetColumn() {
        return m_lastTargetColumn;
    }

    /**
     * @param lastTargetColumn the lastTargetColumn to set
     */
    protected void setLastTargetColumn(final DataColumnSpec lastTargetColumn) {
        this.m_lastTargetColumn = lastTargetColumn;
    }

    /**
     * @return the layout (of the main tab's panel)
     */
    public SpringLayout getLayout() {
        return m_layout;
    }

    /**
     * @return the lastAdded (additional control)
     */
    protected final JComponent getLastAdded() {
        return m_lastAdded;
    }

    /**
     * @param lastAdded the lastAdded (additional control) to set
     */
    protected final void setLastAdded(final JComponent lastAdded) {
        this.m_lastAdded = lastAdded;
    }
}
