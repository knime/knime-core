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
 *   May 3, 2017 (clemens): created
 */
package org.knime.time.node.convert.timestamptodatetime;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.concurrent.TimeUnit;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter2;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.time.util.DateTimeType;

/**
 * The node dialog of the node which converts timestamps to the new date&time types.
 *
 * @author Clemens von Schwerin, KNIME.com, Konstanz, Germany
 */
final class TimestampToDateTimeNodeDialog extends NodeDialogPane {

    private final DialogComponentColumnFilter2 m_dialogCompColFilter;

    private final DialogComponentButtonGroup m_dialogCompReplaceOrAppend;

    private final DialogComponentString m_dialogCompSuffix;

    private final DialogComponentString m_dialogCompTimezone;

    private final JComboBox<TimeUnit> m_timeUnitCombobox;

    private final JComboBox<DateTimeType> m_typeCombobox;

    private final JLabel m_timezoneWarningLabel;

    private final SettingsModelString m_timezoneModel;

    /**
     * Setting up all DialogComponents.
     */
    public TimestampToDateTimeNodeDialog() {
        final SettingsModelColumnFilter2 colSelectModel = TimestampToDateTimeNodeModel.createColSelectModel();
        m_dialogCompColFilter = new DialogComponentColumnFilter2(colSelectModel, 0);

        final SettingsModelString replaceOrAppendModel = TimestampToDateTimeNodeModel.createReplaceAppendStringBool();
        m_dialogCompReplaceOrAppend = new DialogComponentButtonGroup(replaceOrAppendModel, true, null,
            TimestampToDateTimeNodeModel.OPTION_APPEND, TimestampToDateTimeNodeModel.OPTION_REPLACE);

        final SettingsModelString suffixModel = TimestampToDateTimeNodeModel.createSuffixModel(replaceOrAppendModel);
        m_dialogCompSuffix = new DialogComponentString(suffixModel, "Suffix of appended columns: ");

        m_timezoneModel = TimestampToDateTimeNodeModel.createTimezoneModel();
        m_dialogCompTimezone = new DialogComponentString(m_timezoneModel, "Timezone for Zoned Date&Time: ");

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
         * add input format selection
         */

        gbc.gridy++;
        final JPanel panelInput = new JPanel(new GridBagLayout());
        panelInput.setBorder(BorderFactory.createTitledBorder("Input column"));
        final GridBagConstraints gbcInput = new GridBagConstraints();
        // add label and combo box for type selection
        gbcInput.insets = new Insets(0, 0, 0, 0);
        gbcInput.fill = GridBagConstraints.VERTICAL;
        gbcInput.gridx = 0;
        gbcInput.gridy = 0;
        gbcInput.weighty = 0;
        gbcInput.weightx = 0;
        gbcInput.anchor = GridBagConstraints.WEST;
        m_timeUnitCombobox = new JComboBox<TimeUnit>(TimestampToDateTimeNodeModel.TIMEUNITS);
        final JPanel panelInputUnit = new JPanel(new FlowLayout());
        final JLabel labelInputUnit = new JLabel("Unit: ");
        panelInputUnit.add(labelInputUnit);
        panelInputUnit.add(m_timeUnitCombobox);
        panelInput.add(panelInputUnit, gbcInput);

        panel.add(panelInput, gbc);

        /*
         * add output format selection
         */
        gbc.gridy++;
        final JPanel panelTypeFormat = new JPanel(new GridBagLayout());
        panelTypeFormat.setBorder(BorderFactory.createTitledBorder("Output column"));
        final GridBagConstraints gbcTypeFormat = new GridBagConstraints();
        // add label and combo box for type selection
        gbcTypeFormat.insets = new Insets(0, 0, 0, 0);
        gbcTypeFormat.fill = GridBagConstraints.VERTICAL;
        gbcTypeFormat.gridx = 0;
        gbcTypeFormat.gridy = 0;
        gbcTypeFormat.weighty = 0;
        gbcTypeFormat.weightx = 0;
        gbcTypeFormat.anchor = GridBagConstraints.WEST;
        m_typeCombobox = new JComboBox<DateTimeType>(DateTimeType.values());
        final JPanel panelTypeList = new JPanel(new FlowLayout());
        final JLabel labelType = new JLabel("New type: ");
        panelTypeList.add(labelType);
        panelTypeList.add(m_typeCombobox);
        panelTypeFormat.add(panelTypeList, gbcTypeFormat);
        //Timezone input
        gbcTypeFormat.gridy++;
        panelTypeFormat.add(m_dialogCompTimezone.getComponentPanel(), gbcTypeFormat);
        // add label for warning
        m_timezoneWarningLabel = new JLabel();
        gbcTypeFormat.gridx = 0;
        gbcTypeFormat.gridy++;
        gbcTypeFormat.weightx = 0;
        gbcTypeFormat.gridwidth = 3;
        gbcTypeFormat.anchor = GridBagConstraints.CENTER;
        m_timezoneWarningLabel.setForeground(Color.RED);
        panelTypeFormat.add(m_timezoneWarningLabel, gbcTypeFormat);

        panel.add(panelTypeFormat, gbc);

        /*
         * add tab
         */
        addTab("Options", panel);

        /*
         * add listeners
         */
        m_typeCombobox.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(final ItemEvent e) {
                setTimezoneInputEnableStatus((DateTimeType)e.getItem());
            }
        });
        m_dialogCompTimezone.getModel().addChangeListener(e -> timezoneListener(m_timezoneModel.getStringValue()));
    }

    /**
     * Enable the timezoneinput only if the currently selected DateTimeType is "zoned DateAndTime"
     *  @param dt    the currently selected DateTimeType in the m_typeCombobox
     * */
    private void setTimezoneInputEnableStatus(final DateTimeType dt)
    {
        if(dt.equals(DateTimeType.ZONED_DATE_TIME)) {
            m_dialogCompTimezone.getModel().setEnabled(true);
            timezoneListener(m_timezoneModel.getStringValue());
        } else {
            m_dialogCompTimezone.getModel().setEnabled(false);
            setTimezoneWarningNull();
        }
    }

    /**
     * Is called when the input in the timezone edit changes. If the input is invalid show a warning message,
     * otherwise hide the warning message.
     * @param timezonestr   the current content of the m_dialogCompTimezone
     */
    private void timezoneListener(final String timezonestr)
    {
        if(TimestampToDateTimeNodeModel.validateTimezone(timezonestr))
        {
           setTimezoneWarningNull();
        }else{
           setTimezoneWarningMessage("The entered value does not represent a valid timezone!");
        }
    }

    /**
     * Hide the warning label indicating the validity of the current value in the m_dialogCompTimezone
     * @return true
     */
    private boolean setTimezoneWarningNull() {
        m_typeCombobox.setBorder(null);
        m_dialogCompTimezone.setToolTipText(null);
        m_typeCombobox.setToolTipText(null);
        m_timezoneWarningLabel.setToolTipText(null);
        m_timezoneWarningLabel.setText("");
        return true;
    }

    /**
     * Show the warning label indicating the nonvalidity of the current value in the m_dialogCompTimezone
     * @return false
     */
    private boolean setTimezoneWarningMessage(final String message) {
        m_dialogCompTimezone.setToolTipText(message);
        m_typeCombobox.setToolTipText(message);
        m_timezoneWarningLabel.setToolTipText(message);
        m_timezoneWarningLabel.setText(message);
        m_typeCombobox.setBorder(BorderFactory.createLineBorder(Color.RED));
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_dialogCompColFilter.saveSettingsTo(settings);
        m_dialogCompReplaceOrAppend.saveSettingsTo(settings);
        m_dialogCompSuffix.saveSettingsTo(settings);
        settings.addString("typeEnum", ((DateTimeType)m_typeCombobox.getModel().getSelectedItem()).name());
        settings.addString("unitEnum", (m_timeUnitCombobox.getModel().getSelectedItem()).toString());
        m_dialogCompTimezone.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        m_dialogCompColFilter.loadSettingsFrom(settings, specs);
        m_dialogCompReplaceOrAppend.loadSettingsFrom(settings, specs);
        m_dialogCompSuffix.loadSettingsFrom(settings, specs);
        m_typeCombobox.setSelectedItem(
            DateTimeType.valueOf(settings.getString("typeEnum", DateTimeType.LOCAL_DATE_TIME.name())));
        m_timeUnitCombobox.setSelectedItem(TimeUnit.valueOf(settings.getString("unitEnum", TimeUnit.MILLISECONDS.toString())));
        m_dialogCompTimezone.loadSettingsFrom(settings, specs);
        setTimezoneInputEnableStatus((DateTimeType)m_typeCombobox.getSelectedItem());
    }

}
