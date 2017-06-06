/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Jan 24, 2017 (simon): created
 */
package org.knime.time.node.manipulate.datetimeshift;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.format.DateTimeParseException;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SwingConstants;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.time.duration.DurationValue;
import org.knime.core.data.time.localdate.LocalDateValue;
import org.knime.core.data.time.localtime.LocalTimeValue;
import org.knime.core.data.time.period.PeriodValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter2;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.time.util.DurationPeriodFormatUtils;
import org.knime.time.util.Granularity;

/**
 * The node dialog of the node which shifts date&time columns.
 *
 * @author Simon Schmid, KNIME.com, Konstanz, Germany
 */
final class DateTimeShiftNodeDialog extends NodeDialogPane {

    private final DialogComponentColumnFilter2 m_dialogCompColFilter;

    private final DialogComponentButtonGroup m_dialogCompReplaceOrAppend;

    private final DialogComponentString m_dialogCompSuffix;

    private final DialogComponentButtonGroup m_dialogCompPeriodSelection;

    private final DialogComponentColumnNameSelection m_dialogCompPeriodOrDurationColSelection;

    private final DialogComponentString m_dialogCompPeriodOrDurationValue;

    private final DialogComponentButtonGroup m_dialogCompNumericalSelection;

    private final DialogComponentColumnNameSelection m_dialogCompNumericalColSelection;

    private final DialogComponentNumber m_dialogCompNumericalValue;

    private final DialogComponentStringSelection m_dialogCompNumericalGranularity;

    private final SettingsModelString m_periodSelectionModel;

    private final SettingsModelString m_numericalSelectionModel;

    private DataTableSpec m_spec;

    final JRadioButton m_periodRadioButton;

    final JRadioButton m_numericalRadioButton;

    /**
     *
     */
    @SuppressWarnings("unchecked")
    public DateTimeShiftNodeDialog() {

        m_dialogCompColFilter = new DialogComponentColumnFilter2(DateTimeShiftNodeModel.createColSelectModel(), 0);

        final SettingsModelString replaceOrAppendModel = DateTimeShiftNodeModel.createReplaceAppendModel();
        m_dialogCompReplaceOrAppend = new DialogComponentButtonGroup(replaceOrAppendModel, true, null,
            DateTimeShiftNodeModel.OPTION_APPEND, DateTimeShiftNodeModel.OPTION_REPLACE);

        final SettingsModelString suffixModel = DateTimeShiftNodeModel.createSuffixModel(replaceOrAppendModel);
        m_dialogCompSuffix = new DialogComponentString(suffixModel, "Suffix of appended columns: ");

        m_periodSelectionModel = DateTimeShiftNodeModel.createPeriodSelectionModel();
        m_dialogCompPeriodSelection =
            new DialogComponentButtonGroup(m_periodSelectionModel, null, true, DurationMode.values());

        m_dialogCompPeriodOrDurationColSelection = new DialogComponentColumnNameSelection(
            DateTimeShiftNodeModel.createPeriodColSelectModel(m_periodSelectionModel), "", 0, false, PeriodValue.class,
            DurationValue.class);

        m_dialogCompPeriodOrDurationValue = new DialogComponentString(
            DateTimeShiftNodeModel.createPeriodValueModel(m_periodSelectionModel), "", false, 30);

        m_numericalSelectionModel = DateTimeShiftNodeModel.createNumericalSelectionModel();
        m_dialogCompNumericalSelection =
            new DialogComponentButtonGroup(m_numericalSelectionModel, null, true, NumericalMode.values());

        m_dialogCompNumericalColSelection = new DialogComponentColumnNameSelection(
            DateTimeShiftNodeModel.createNumericalColSelectModel(m_numericalSelectionModel), "", 0, false,
            IntValue.class, LongValue.class);

        m_dialogCompNumericalValue = new DialogComponentNumber(
            DateTimeShiftNodeModel.createNumericalValueModel(m_numericalSelectionModel), "", 1, 30);

        final SettingsModelString granularityModel = DateTimeShiftNodeModel.createNumericalGranularityModel();
        m_dialogCompNumericalGranularity =
            new DialogComponentStringSelection(granularityModel, "Granularity", Granularity.strings());

        /*
         * create panel with gbc
         */
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 1;

        /*
         * add column filter
         */
        panel.add(m_dialogCompColFilter.getComponentPanel(), gbc);

        /*
         * add replace/append selection
         */
        gbc.gridy++;
        gbc.weighty = 0;
        final JPanel panelReplace = new JPanel(new GridBagLayout());
        panelReplace.setBorder(BorderFactory.createTitledBorder("Replace/Append Selection"));
        final GridBagConstraints gbcReplaceAppend = new GridBagConstraints();
        // add check box
        gbcReplaceAppend.fill = GridBagConstraints.VERTICAL;
        gbcReplaceAppend.gridx = 0;
        gbcReplaceAppend.gridy = 0;
        gbcReplaceAppend.weighty = 0;
        gbcReplaceAppend.anchor = GridBagConstraints.WEST;
        panelReplace.add(m_dialogCompReplaceOrAppend.getComponentPanel(), gbcReplaceAppend);
        // add suffix text field
        gbcReplaceAppend.gridx++;
        gbcReplaceAppend.weightx = 1;
        gbcReplaceAppend.insets = new Insets(2, 10, 0, 0);
        panelReplace.add(m_dialogCompSuffix.getComponentPanel(), gbcReplaceAppend);

        panel.add(panelReplace, gbc);

        /*
         * add period/duration selection
         */
        gbc.gridy++;
        gbc.insets = new Insets(7, 7, 7, 7);
        final JPanel panelPeriodOrNumerical = new JPanel(new GridBagLayout());
        panelPeriodOrNumerical.setBorder(BorderFactory.createTitledBorder("Shift Value Selection"));
        final GridBagConstraints gbcPeriodOrNumerical = new GridBagConstraints();
        // add radio button to choose period instead of numerical value
        gbcPeriodOrNumerical.fill = GridBagConstraints.BOTH;
        gbcPeriodOrNumerical.gridx = 0;
        gbcPeriodOrNumerical.gridy = 0;
        gbcPeriodOrNumerical.weighty = 0;
        gbcPeriodOrNumerical.anchor = GridBagConstraints.WEST;
        m_periodRadioButton = new JRadioButton("Use Duration");
        m_periodRadioButton.setSelected(true);
        panelPeriodOrNumerical.add(m_periodRadioButton, gbcPeriodOrNumerical);

        // add radio buttons to choose between a column or value
        final JPanel panelPeriodSelection = new JPanel(new GridBagLayout());
        panelPeriodSelection.setBorder(BorderFactory.createTitledBorder(""));
        final GridBagConstraints gbcPeriodSelection = new GridBagConstraints();
        gbcPeriodSelection.fill = GridBagConstraints.VERTICAL;
        gbcPeriodSelection.gridx = 0;
        gbcPeriodSelection.gridy = 0;
        gbcPeriodSelection.weighty = 0;
        gbcPeriodSelection.anchor = GridBagConstraints.WEST;
        gbcPeriodSelection.insets = new Insets(5, 5, 5, 5);
        panelPeriodSelection.add(m_dialogCompPeriodSelection.getComponentPanel().getComponent(0), gbcPeriodSelection);

        // add column selection and text field for period/duration value
        final JPanel panelPeriodColumnValue = new JPanel(new GridBagLayout());
        final GridBagConstraints gbcPeriodColumnValue = new GridBagConstraints();
        gbcPeriodColumnValue.fill = GridBagConstraints.VERTICAL;
        gbcPeriodColumnValue.gridx = 0;
        gbcPeriodColumnValue.gridy = 0;
        gbcPeriodColumnValue.weightx = 1;
        gbcPeriodColumnValue.anchor = GridBagConstraints.WEST;
        m_dialogCompPeriodOrDurationColSelection.getComponentPanel().getComponent(1)
            .setPreferredSize(new Dimension(350, 30));
        panelPeriodColumnValue.add(m_dialogCompPeriodOrDurationColSelection.getComponentPanel(), gbcPeriodColumnValue);
        gbcPeriodColumnValue.gridy++;
        panelPeriodColumnValue.add(m_dialogCompPeriodOrDurationValue.getComponentPanel(), gbcPeriodColumnValue);
        gbcPeriodSelection.weightx = 1;
        gbcPeriodSelection.gridx++;
        panelPeriodSelection.add(panelPeriodColumnValue, gbcPeriodSelection);

        // add warning label for period/duration
        final JLabel warningLabelPeriod = new JLabel();
        warningLabelPeriod.setForeground(Color.RED);
        warningLabelPeriod.setPreferredSize(new Dimension(500, new JLabel(" ").getPreferredSize().height));
        gbcPeriodSelection.gridx = 0;
        gbcPeriodSelection.gridy++;
        gbcPeriodSelection.gridwidth = 2;
        gbcPeriodSelection.insets = new Insets(0, 10, 5, 0);
        panelPeriodSelection.add(warningLabelPeriod, gbcPeriodSelection);

        gbcPeriodOrNumerical.gridy++;
        gbcPeriodOrNumerical.weightx = 1;
        gbcPeriodOrNumerical.insets = new Insets(5, 25, 5, 5);
        panelPeriodOrNumerical.add(panelPeriodSelection, gbcPeriodOrNumerical);

        /*
         * add numerical value selection
         */
        gbcPeriodOrNumerical.gridy++;
        gbcPeriodOrNumerical.insets = new Insets(10, 0, 0, 0);
        m_numericalRadioButton = new JRadioButton("Use Numerical");
        panelPeriodOrNumerical.add(m_numericalRadioButton, gbcPeriodOrNumerical);

        // add radio buttons to choose between a column or value
        final JPanel panelNumericalSelection = new JPanel(new GridBagLayout());
        panelNumericalSelection.setBorder(BorderFactory.createTitledBorder(""));
        final GridBagConstraints gbcNumericalSelection = new GridBagConstraints();
        gbcNumericalSelection.fill = GridBagConstraints.VERTICAL;
        gbcNumericalSelection.gridx = 0;
        gbcNumericalSelection.gridy = 0;
        gbcNumericalSelection.weighty = 0;
        gbcNumericalSelection.anchor = GridBagConstraints.WEST;
        gbcNumericalSelection.insets = new Insets(5, 5, 5, 5);
        panelNumericalSelection.add(m_dialogCompNumericalSelection.getComponentPanel().getComponent(0),
            gbcNumericalSelection);

        // add column selection and text field for numerical value
        final JPanel panelNumericalColumnValue = new JPanel(new GridBagLayout());
        final GridBagConstraints gbcNumericalColumnValue = new GridBagConstraints();
        gbcNumericalColumnValue.fill = GridBagConstraints.VERTICAL;
        gbcNumericalColumnValue.gridx = 0;
        gbcNumericalColumnValue.gridy = 0;
        gbcNumericalColumnValue.weightx = 1;
        gbcNumericalColumnValue.anchor = GridBagConstraints.WEST;
        m_dialogCompNumericalColSelection.getComponentPanel().getComponent(1).setPreferredSize(new Dimension(350, 30));
        panelNumericalColumnValue.add(m_dialogCompNumericalColSelection.getComponentPanel(), gbcNumericalColumnValue);
        gbcNumericalColumnValue.gridy++;
        panelNumericalColumnValue.add(m_dialogCompNumericalValue.getComponentPanel(), gbcNumericalColumnValue);
        ((JSpinner.DefaultEditor)((JSpinner)m_dialogCompNumericalValue.getComponentPanel().getComponent(1)).getEditor())
            .getTextField().setHorizontalAlignment(SwingConstants.LEFT);
        gbcNumericalSelection.weightx = 1;
        gbcNumericalSelection.gridx++;
        gbcNumericalSelection.insets = new Insets(0, 48, 0, 0);
        panelNumericalSelection.add(panelNumericalColumnValue, gbcNumericalSelection);

        gbcNumericalSelection.gridy++;
        gbcNumericalSelection.gridx = 0;
        gbcNumericalSelection.gridwidth = 2;
        gbcNumericalSelection.insets = new Insets(5, 5, 0, 0);
        panelNumericalSelection.add(m_dialogCompNumericalGranularity.getComponentPanel(), gbcNumericalSelection);

        // add warning label for numerical
        final JLabel warningLabelNumerical = new JLabel();
        warningLabelNumerical.setForeground(Color.RED);
        warningLabelNumerical.setPreferredSize(new Dimension(500, new JLabel(" ").getPreferredSize().height));
        gbcNumericalSelection.gridx = 0;
        gbcNumericalSelection.gridy++;
        gbcNumericalSelection.gridwidth = 2;
        gbcNumericalSelection.insets = new Insets(5, 10, 5, 0);
        panelNumericalSelection.add(warningLabelNumerical, gbcNumericalSelection);

        gbcPeriodOrNumerical.gridy++;
        gbcPeriodOrNumerical.weightx = 1;
        gbcPeriodOrNumerical.insets = new Insets(5, 25, 5, 5);
        panelPeriodOrNumerical.add(panelNumericalSelection, gbcPeriodOrNumerical);

        final ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(m_periodRadioButton);
        buttonGroup.add(m_numericalRadioButton);
        panel.add(panelPeriodOrNumerical, gbc);

        /*
         * add tab
         */
        addTab("Options", panel);

        /*
         * change listeners
         */
        m_periodRadioButton.addActionListener(l -> {
            m_periodSelectionModel.setEnabled(m_periodRadioButton.isSelected());
            m_numericalSelectionModel.setEnabled(m_numericalRadioButton.isSelected());
            granularityModel.setEnabled(m_numericalRadioButton.isSelected());
        });
        m_numericalRadioButton.addActionListener(l -> {
            m_periodSelectionModel.setEnabled(m_periodRadioButton.isSelected());
            m_numericalSelectionModel.setEnabled(m_numericalRadioButton.isSelected());
            granularityModel.setEnabled(m_numericalRadioButton.isSelected());
        });

        m_dialogCompColFilter.getModel().addChangeListener(l -> {
            warningLabelPeriod.setText(checkPeriodOrDurationSettings());
            warningLabelNumerical.setText(checkNumericalSettings());
        });
        m_periodSelectionModel.addChangeListener(l -> warningLabelPeriod.setText(checkPeriodOrDurationSettings()));
        m_dialogCompPeriodOrDurationColSelection.getModel()
            .addChangeListener(l -> warningLabelPeriod.setText(checkPeriodOrDurationSettings()));
        m_dialogCompPeriodOrDurationValue.getModel()
            .addChangeListener(l -> warningLabelPeriod.setText(checkPeriodOrDurationSettings()));
        m_numericalSelectionModel.addChangeListener(l -> warningLabelNumerical.setText(checkNumericalSettings()));
        m_dialogCompNumericalColSelection.getModel()
            .addChangeListener(l -> warningLabelNumerical.setText(checkNumericalSettings()));
        m_dialogCompNumericalValue.getModel()
            .addChangeListener(l -> warningLabelNumerical.setText(checkNumericalSettings()));
        m_dialogCompNumericalGranularity.getModel()
            .addChangeListener(l -> warningLabelNumerical.setText(checkNumericalSettings()));
    }

    /**
     * checks the settings for period/duration selection
     *
     * @return the error message
     */
    private String checkPeriodOrDurationSettings() {
        final String[] includes =
            ((SettingsModelColumnFilter2)m_dialogCompColFilter.getModel()).applyTo(m_spec).getIncludes();
        // if duration/period column/value is selected
        if (m_periodSelectionModel.isEnabled()) {
            // if column shall be used
            if (m_periodSelectionModel.getStringValue().equals(DurationMode.Column.name())) {
                // if no column is selected
                if (m_dialogCompPeriodOrDurationColSelection.getSelectedAsSpec() == null) {
                    return "A column must be selected!";
                } else {
                    // if a period column is selected
                    if (m_dialogCompPeriodOrDurationColSelection.getSelectedAsSpec().getType()
                        .isCompatible(PeriodValue.class)) {
                        for (final String include : includes) {
                            if (m_spec.getColumnSpec(include).getType().isCompatible(LocalTimeValue.class)) {
                                return "A date-based duration cannot be applied on a time!";
                            }
                        }
                    }
                    // if a duration column is selected
                    else {
                        for (final String include : includes) {
                            if (m_spec.getColumnSpec(include).getType().isCompatible(LocalDateValue.class)) {
                                return "A time-based duration cannot be applied on a date!";
                            }
                        }
                    }

                }
            }
            // if a static value shall be used
            else {
                try {
                    // if a period is written
                    DurationPeriodFormatUtils.parsePeriod(
                        ((SettingsModelString)m_dialogCompPeriodOrDurationValue.getModel()).getStringValue());
                    for (final String include : includes) {
                        if (m_spec.getColumnSpec(include).getType().isCompatible(LocalTimeValue.class)) {
                            return "A date-based duration cannot be applied on a time!";
                        }
                    }
                } catch (DateTimeParseException e) {
                    try {
                        // if a duration is written
                        DurationPeriodFormatUtils.parseDuration(
                            ((SettingsModelString)m_dialogCompPeriodOrDurationValue.getModel()).getStringValue());
                        for (final String include : includes) {
                            if (m_spec.getColumnSpec(include).getType().isCompatible(LocalDateValue.class)) {
                                return "A time-based duration cannot be applied on a date!";
                            }
                        }
                    } catch (DateTimeParseException e2) {
                        // if a neither a period nor a duration is written
                        return "Value does not represent a duration!";
                    }
                }
            }
        }
        return "";
    }

    /**
     * checks the settings for numerical selection
     *
     * @return the error message
     */
    private String checkNumericalSettings() {
        final String[] includes =
            ((SettingsModelColumnFilter2)m_dialogCompColFilter.getModel()).applyTo(m_spec).getIncludes();
        // if numerical column/value is selected
        if (m_numericalSelectionModel.isEnabled()) {
            // if column shall be used and no column is selected
            if (m_numericalSelectionModel.getStringValue().equals(NumericalMode.Column.name())) {
                if (m_dialogCompNumericalColSelection.getSelectedAsSpec() == null) {
                    return "A column must be selected!";
                }
            }
            // check if granularity goes with included columns
            final String granularity =
                ((SettingsModelString)m_dialogCompNumericalGranularity.getModel()).getStringValue();
            if (!Granularity.fromString(granularity).isPartOfDate()) {
                for (final String include : includes) {
                    if (m_spec.getColumnSpec(include).getType().isCompatible(LocalDateValue.class)) {
                        return granularity + " can not be applied on a date!";
                    }
                }
            } else {
                for (final String include : includes) {
                    if (m_spec.getColumnSpec(include).getType().isCompatible(LocalTimeValue.class)) {
                        return granularity + " can not be applied on a time!";
                    }
                }
            }
        }
        return "";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        if (!checkPeriodOrDurationSettings().equals("") || !checkNumericalSettings().equals("")) {
            throw new InvalidSettingsException(checkPeriodOrDurationSettings() + checkNumericalSettings());
        }
        m_dialogCompColFilter.saveSettingsTo(settings);
        m_dialogCompReplaceOrAppend.saveSettingsTo(settings);
        m_dialogCompSuffix.saveSettingsTo(settings);
        m_dialogCompPeriodSelection.saveSettingsTo(settings);
        m_dialogCompPeriodOrDurationColSelection.saveSettingsTo(settings);
        m_dialogCompPeriodOrDurationValue.saveSettingsTo(settings);
        m_dialogCompNumericalSelection.saveSettingsTo(settings);
        m_dialogCompNumericalColSelection.saveSettingsTo(settings);
        m_dialogCompNumericalValue.saveSettingsTo(settings);
        m_dialogCompNumericalGranularity.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {
        m_spec = specs[0];
        m_dialogCompColFilter.loadSettingsFrom(settings, specs);
        m_dialogCompReplaceOrAppend.loadSettingsFrom(settings, specs);
        m_dialogCompSuffix.loadSettingsFrom(settings, specs);
        m_dialogCompPeriodSelection.loadSettingsFrom(settings, specs);
        m_dialogCompPeriodOrDurationColSelection.loadSettingsFrom(settings, specs);
        m_dialogCompPeriodOrDurationValue.loadSettingsFrom(settings, specs);
        m_dialogCompNumericalSelection.loadSettingsFrom(settings, specs);
        m_dialogCompNumericalColSelection.loadSettingsFrom(settings, specs);
        m_dialogCompNumericalValue.loadSettingsFrom(settings, specs);
        m_dialogCompNumericalGranularity.loadSettingsFrom(settings, specs);
        m_periodRadioButton.setSelected(m_dialogCompPeriodSelection.getModel().isEnabled());
        m_numericalRadioButton.setSelected(!m_dialogCompPeriodSelection.getModel().isEnabled());
    }

}
