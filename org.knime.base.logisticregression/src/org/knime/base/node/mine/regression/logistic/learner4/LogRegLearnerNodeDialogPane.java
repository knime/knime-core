/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * -------------------------------------------------------------------
 *
 * History
 *   21.01.2010 (hofer): created
 */
package org.knime.base.node.mine.regression.logistic.learner4;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.EnumSet;

import javax.swing.BorderFactory;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;

import org.knime.base.node.mine.regression.logistic.learner4.LogRegLearnerSettings.LearningRateStrategies;
import org.knime.base.node.mine.regression.logistic.learner4.LogRegLearnerSettings.Prior;
import org.knime.base.node.mine.regression.logistic.learner4.LogRegLearnerSettings.Solver;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.NominalValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.ColumnSelectionPanel;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterPanel;

/**
 * Dialog for the logistic regression learner.
 *
 * @author Heiko Hofer
 * @author Gabor Bakos
 * @author Adrian Nembach, KNIME.com
 * @since 3.1
 */
public final class LogRegLearnerNodeDialogPane extends NodeDialogPane {

    private static int NUMBER_INPUT_FIELD_COLS = 10;

    private DataColumnSpecFilterPanel m_filterPanel;

    private JComboBox<DataCell> m_targetReferenceCategory;

    private JCheckBox m_notSortTarget;

    private ColumnSelectionPanel m_selectionPanel;

    private JCheckBox m_notSortIncludes;

    private DataTableSpec m_inSpec;

    // new in version 3.4
    private JComboBox<Solver> m_solverComboBox;
    private JCheckBox m_lazyCalculationCheckBox;
    private JSpinner m_maxEpochSpinner;
//    private JSpinner m_epsilonSpinner;
    private JTextField m_epsilonField;

    private JComboBox<LearningRateStrategies> m_learningRateStrategyComboBox;
//    private JSpinner m_initialLearningRateSpinner;
    private JTextField m_initialLearningRateField;
    private JSpinner m_learningRateDecaySpinner;

    private JComboBox<Prior> m_priorComboBox;
    private JSpinner m_priorVarianceSpinner;
    private JLabel m_warningPanel;

    /**
     * Create new dialog for linear regression model.
     */
    public LogRegLearnerNodeDialogPane() {
        super();
        // instantiate members
        @SuppressWarnings("unchecked")
        final ColumnSelectionPanel columnSelectionPanel =
            new ColumnSelectionPanel(new EmptyBorder(0, 0, 0, 0), NominalValue.class);
        m_selectionPanel = columnSelectionPanel;
        m_selectionPanel.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                updateTargetCategories((DataCell)m_targetReferenceCategory.getSelectedItem());
            }
        });
        m_warningPanel = new JLabel();
        m_targetReferenceCategory = new JComboBox<>();
        m_filterPanel = new DataColumnSpecFilterPanel(false);
        m_notSortTarget =
                new JCheckBox("Use order from target column domain (only relevant for output representation)");
        m_notSortIncludes = new JCheckBox("Use order from column domain (applies only to nominal columns). "
                + "First value is chosen as reference for dummy variables.");

        m_lazyCalculationCheckBox = new JCheckBox("Perfom calculations lazily");
        m_maxEpochSpinner = new JSpinner(new SpinnerNumberModel(LogRegLearnerSettings.DEFAULT_MAX_EPOCH, 1, Integer.MAX_VALUE, 1));
        m_epsilonField= new JTextField(Double.toString(LogRegLearnerSettings.DEFAULT_EPSILON), NUMBER_INPUT_FIELD_COLS);
//        m_initialLearningRateSpinner = new JSpinner(new SpinnerNumberModel(LogRegLearnerSettings.DEFAULT_INITIAL_LEARNING_RATE,
//            Double.MIN_VALUE, 1e3, 1e-3));
        m_initialLearningRateField = new JTextField(Double.toString(LogRegLearnerSettings.DEFAULT_EPSILON), NUMBER_INPUT_FIELD_COLS);
        m_learningRateDecaySpinner = new JSpinner(new SpinnerNumberModel(LogRegLearnerSettings.DEFAULT_LEARNING_RATE_DECAY,
            Double.MIN_VALUE, 1e3, 1e-3));
        m_priorVarianceSpinner = new JSpinner(new SpinnerNumberModel(LogRegLearnerSettings.DEFAULT_PRIOR_VARIANCE,
            Double.MIN_VALUE, 1e3, 0.1));
        m_priorComboBox = new JComboBox<>(Prior.values());
        m_learningRateStrategyComboBox = new JComboBox<>(LearningRateStrategies.values());
        m_solverComboBox = new JComboBox<>(Solver.values());

        // register listeners
        m_solverComboBox.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(final ItemEvent e) {
                Solver selected = (Solver)m_solverComboBox.getSelectedItem();
                solverChanged(selected);
            }
        });

        m_priorComboBox.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(final ItemEvent e) {
                Prior selected = (Prior)m_priorComboBox.getSelectedItem();
                m_priorVarianceSpinner.setEnabled(selected.hasVariance());
            }
        });

        m_learningRateStrategyComboBox.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(final ItemEvent e) {
                LearningRateStrategies selected = (LearningRateStrategies)m_learningRateStrategyComboBox.getSelectedItem();
                enforceLRSCompatibilities(selected);
            }

        });

        // create tabs
        JPanel settingsPanel = createSettingsPanel();
        addTab("Settings", settingsPanel);
        JPanel advancedSettingsPanel = createAdvancedSettingsPanel();
        addTab("Advanced", advancedSettingsPanel);
    }

    private void enforcePriorCompatibilities(final Prior prior) {
        m_priorVarianceSpinner.setEnabled(prior.hasVariance());
    }

    private void enforceLRSCompatibilities(final LearningRateStrategies lrs) {
        m_learningRateDecaySpinner.setEnabled(lrs.hasDecayRate());
//        m_initialLearningRateSpinner.setEnabled(lrs.hasInitialValue());
        m_initialLearningRateField.setEnabled(lrs.hasInitialValue());
    }

    private void solverChanged(final Solver solver) {

        boolean sgMethod = solver != Solver.IRLS;
        if (sgMethod) {
            setEnabledSGRelated(true);
            m_lazyCalculationCheckBox.setEnabled(solver.supportsLazy());

            ComboBoxModel<Prior> oldPriorModel = m_priorComboBox.getModel();
            EnumSet<Prior> compatiblePriors = solver.getCompatiblePriors();
            Prior oldSelectedPrior = (Prior)oldPriorModel.getSelectedItem();
            m_priorComboBox.setModel(new DefaultComboBoxModel<>(compatiblePriors.toArray(new Prior[compatiblePriors.size()])));
            Prior newSelectedPrior;
            if (compatiblePriors.contains(oldSelectedPrior)) {
                m_priorComboBox.setSelectedItem(oldSelectedPrior);
                newSelectedPrior = oldSelectedPrior;
            } else {
                newSelectedPrior = (Prior)m_priorComboBox.getSelectedItem();
                // TODO warn user that the prior selection changed
            }
            enforcePriorCompatibilities(newSelectedPrior);

            LearningRateStrategies oldSelectedLRS = (LearningRateStrategies)m_learningRateStrategyComboBox.getSelectedItem();
            EnumSet<LearningRateStrategies> compatibleLRS = solver.getCompatibleLearningRateStrategies();
            m_learningRateStrategyComboBox.setModel(new DefaultComboBoxModel<>(
                    compatibleLRS.toArray(new LearningRateStrategies[compatibleLRS.size()])));
            LearningRateStrategies newSelectedLRS = (LearningRateStrategies)m_learningRateStrategyComboBox.getSelectedItem();
            if (compatibleLRS.contains(oldSelectedLRS)) {
                m_learningRateStrategyComboBox.setSelectedItem(oldSelectedLRS);
                newSelectedLRS = oldSelectedLRS;
            } else {
                newSelectedLRS = (LearningRateStrategies)m_learningRateStrategyComboBox.getSelectedItem();
                // TODO warn user that the selected learning rate strategy changed
            }
            enforceLRSCompatibilities(newSelectedLRS);
        } else {
            setEnabledSGRelated(false);
        }
    }

    private void setEnabledSGRelated(final boolean enable) {
        m_lazyCalculationCheckBox.setEnabled(enable);
        m_learningRateStrategyComboBox.setEnabled(enable);
        m_learningRateDecaySpinner.setEnabled(enable);
//        m_initialLearningRateSpinner.setEnabled(enable);
        m_initialLearningRateField.setEnabled(enable);
        m_priorComboBox.setEnabled(enable);
        m_priorVarianceSpinner.setEnabled(enable);
    }

    private JPanel createAdvancedSettingsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.BASELINE_LEADING;
        c.insets = new Insets(5, 5, 0, 0);

        JPanel solverOptionsPanel = createSolverOptionsPanel();
        solverOptionsPanel.setBorder(BorderFactory.createTitledBorder("Solver options"));
        panel.add(solverOptionsPanel, c);

        c.gridy++;
        JPanel learningRateStrategyPanel = createLearningRateStrategyPanel();
        learningRateStrategyPanel.setBorder(BorderFactory.createTitledBorder("Learning rate strategy"));
        panel.add(learningRateStrategyPanel, c);

        c.gridy++;
        JPanel priorPanel = createPriorPanel();
        priorPanel.setBorder(BorderFactory.createTitledBorder("Prior options"));
        panel.add(priorPanel, c);

        return panel;
    }

    private JPanel createSolverOptionsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = makeSettingsConstraints();

        panel.add(new JLabel("Number of epochs:"), c);
        c.gridx++;
        panel.add(m_maxEpochSpinner, c);
        c.gridx = 0;
        c.gridy++;
        panel.add(new JLabel("Epsilon:"), c);
        c.gridx++;
        panel.add(m_epsilonField, c);
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;
        panel.add(m_lazyCalculationCheckBox, c);

        return panel;
    }

    private JPanel createPriorPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = makeSettingsConstraints();

        panel.add(new JLabel("Prior:"), c);
        c.gridx++;
        panel.add(m_priorComboBox, c);
        c.gridx = 0;
        c.gridy++;
        panel.add(new JLabel("Variance:"), c);
        c.gridx++;
        panel.add(m_priorVarianceSpinner, c);

        return panel;
    }

    private GridBagConstraints makeSettingsConstraints() {
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.BASELINE_LEADING;
        c.insets = new Insets(5, 5, 0, 0);
        return c;
    }

    private JPanel createLearningRateStrategyPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = makeSettingsConstraints();

        panel.add(new JLabel("Learning rate strategy:"), c);
        c.gridx++;
        panel.add(m_learningRateStrategyComboBox, c);
        c.gridx = 0;
        c.gridy++;
        panel.add(new JLabel("Initial learning rate:"), c);
        c.gridx++;
//        panel.add(m_initialLearningRateSpinner, c);
        panel.add(m_initialLearningRateField, c);
        c.gridx = 0;
        c.gridy++;
        panel.add(new JLabel("Decay rate:"), c);
        c.gridx++;
        panel.add(m_learningRateDecaySpinner, c);

        return panel;
    }

    private JPanel createSettingsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.BASELINE_LEADING;
        c.insets = new Insets(5, 5, 0, 0);
        JPanel northPanel = createTargetOptionsPanel();
        northPanel.setBorder(BorderFactory.createTitledBorder("Target"));
//        panel.add(northPanel, BorderLayout.NORTH);
        panel.add(northPanel, c);
        c.gridy++;

        JPanel centerPanel = createSolverPanel();
        centerPanel.setBorder(BorderFactory.createTitledBorder("Solver"));
//        panel.add(centerPanel, BorderLayout.WEST);
        panel.add(centerPanel, c);
        c.gridy++;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.BOTH;

        JPanel southPanel = createIncludesPanel();
        southPanel.setBorder(BorderFactory.createTitledBorder("Values"));
//        panel.add(southPanel, BorderLayout.SOUTH);
        panel.add(southPanel, c);
        return panel;
    }

    private JPanel createSolverPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = makeSettingsConstraints();
        p.add(new JLabel("Select solver:"), c);
        c.gridx++;
        c.weightx = 1;
        p.add(m_solverComboBox, c);
        return p;
    }

    /**
     * Create options panel for the target.
     */
    private JPanel createTargetOptionsPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = makeSettingsConstraints();

        p.add(new JLabel("Target Column:"), c);

        c.gridx++;

        p.add(m_selectionPanel, c);

        c.gridx = 0;
        c.gridy++;
        p.add(new JLabel("Reference Category:"), c);

        c.gridx++;
        p.add(m_targetReferenceCategory, c);

        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 3;
        c.weightx = 1;
        p.add(m_notSortTarget, c);

        return p;
    }

    /**
     * Create options panel for the included columns.
     */
    private JPanel createIncludesPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.BASELINE_LEADING;
        c.insets = new Insets(5, 5, 0, 0);


        p.add(m_filterPanel, c);

        c.gridy++;

        p.add(m_notSortIncludes, c);

        return p;
    }

    /**
     * Update list of target categories.
     */
    private void updateTargetCategories(final DataCell selectedItem) {
        m_targetReferenceCategory.removeAllItems();

        String selectedColumn = m_selectionPanel.getSelectedColumn();
        if (selectedColumn != null) {
            DataColumnSpec colSpec = m_inSpec.getColumnSpec(selectedColumn);
            if (null != colSpec) {
                // select last as default
                DataCell newSelectedItem = null;
                DataCell lastItem = null;
                final DataColumnDomain domain = colSpec.getDomain();
                if (domain.hasValues()) {
                    for (DataCell cell : domain.getValues()) {
                        m_targetReferenceCategory.addItem(cell);
                        lastItem = cell;
                        if (cell.equals(selectedItem)) {
                            newSelectedItem = selectedItem;
                        }
                    }
                    if (newSelectedItem == null) {
                        newSelectedItem = lastItem;
                    }
                    m_targetReferenceCategory.getModel().setSelectedItem(newSelectedItem);
                }
            }
        }
        m_targetReferenceCategory.setEnabled(m_targetReferenceCategory.getModel().getSize() > 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO s, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        final LogRegLearnerSettings settings = new LogRegLearnerSettings();
        m_inSpec = (DataTableSpec)specs[0];
        settings.loadSettingsForDialog(s, m_inSpec);
        final DataColumnSpecFilterConfiguration config = settings.getIncludedColumns();
        //config.loadConfigurationInDialog(s, m_inSpec);
        m_filterPanel.loadConfiguration(config, m_inSpec);

        String target = settings.getTargetColumn();

        m_selectionPanel.update(m_inSpec, target);
        //m_filterPanel.updateWithNewConfiguration(config); is not enough, we have to reload things as selection update might change the UI
        m_filterPanel.loadConfiguration(config, m_inSpec);
        // must hide the target from filter panel
        // updating m_filterPanel first does not work as the first
        // element in the spec will always be in the exclude list.
        String selected = m_selectionPanel.getSelectedColumn();
        if (null == selected) {
            for (DataColumnSpec colSpec : m_inSpec) {
                if (colSpec.getType().isCompatible(NominalValue.class)) {
                    selected = colSpec.getName();
                    break;
                }
            }
        }
        if (selected != null) {
            DataColumnSpec colSpec = m_inSpec.getColumnSpec(selected);
            m_filterPanel.hideNames(colSpec);
        }

        updateTargetCategories(settings.getTargetReferenceCategory());

        m_notSortTarget.setSelected(!settings.getSortTargetCategories());
        m_notSortIncludes.setSelected(!settings.getSortIncludesCategories());
        m_solverComboBox.setSelectedItem(settings.getSolver());
        m_maxEpochSpinner.setValue(settings.getMaxEpoch());
        m_lazyCalculationCheckBox.setSelected(settings.isPerformLazy());
        double epsilon = settings.getEpsilon();
        m_epsilonField.setText(Double.toString(epsilon));
        m_learningRateStrategyComboBox.setSelectedItem(settings.getLearningRateStrategy());
        m_learningRateDecaySpinner.setValue(settings.getLearningRateDecay());
//        m_initialLearningRateSpinner.setValue(settings.getInitialLearningRate());
        m_initialLearningRateField.setText(Double.toString(settings.getInitialLearningRate()));
        m_priorComboBox.setSelectedItem(settings.getPrior());
        m_priorVarianceSpinner.setValue(settings.getPriorVariance());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO s) throws InvalidSettingsException {
        final LogRegLearnerSettings settings = new LogRegLearnerSettings();
        final DataColumnSpecFilterConfiguration config = LogRegLearnerNodeModel.createDCSFilterConfiguration();
        m_filterPanel.saveConfiguration(config);
        //config.saveConfiguration(s);
        settings.setIncludedColumns(config);
        settings.setTargetColumn(m_selectionPanel.getSelectedColumn());
        settings.setTargetReferenceCategory((DataCell)m_targetReferenceCategory.getSelectedItem());
        settings.setSortTargetCategories(!m_notSortTarget.isSelected());
        settings.setSortIncludesCategories(!m_notSortIncludes.isSelected());
        settings.setSolver((Solver)m_solverComboBox.getSelectedItem());
        settings.setMaxEpoch((int)m_maxEpochSpinner.getValue());
        settings.setPerformLazy(m_lazyCalculationCheckBox.isSelected());
        try {
            String str = m_epsilonField.getText();
            double epsilon = Double.valueOf(str);
            settings.setEpsilon(epsilon);
        } catch (NumberFormatException nfe) {
            throw new InvalidSettingsException("Please provide a valid value for epsilon.");
        }
        settings.setLearningRateStrategy((LearningRateStrategies)m_learningRateStrategyComboBox.getSelectedItem());
        settings.setLearningRateDecay((double)m_learningRateDecaySpinner.getValue());
//        settings.setInitialLearningRate((double)m_initialLearningRateSpinner.getValue());
        try {
            String str = m_initialLearningRateField.getText();
            double lr = Double.valueOf(str);
            settings.setInitialLearningRate(lr);
        } catch (NumberFormatException nfe) {
            throw new InvalidSettingsException("Please provide a valid value for the initial learning rate.");
        }
        settings.setPrior((Prior)m_priorComboBox.getSelectedItem());
        settings.setPriorVariance((double)m_priorVarianceSpinner.getValue());

        settings.saveSettings(s);
    }
}
