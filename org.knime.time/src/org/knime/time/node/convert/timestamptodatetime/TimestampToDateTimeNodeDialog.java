/*
 * ------------------------------------------------------------------------
 *
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
 * History
 *   May 3, 2017 (clemens): created
 */
package org.knime.time.node.convert.timestamptodatetime;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.time.ZoneId;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.commons.lang3.StringUtils;
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

    private final JComboBox<String> m_zoneComboBox;

    private final JComboBox<TimeUnit> m_timeUnitCombobox;

    private final JComboBox<DateTimeType> m_typeCombobox;

    /**
     * Setting up all DialogComponents.
     */
    TimestampToDateTimeNodeDialog() {
        final SettingsModelColumnFilter2 colSelectModel = TimestampToDateTimeNodeModel.createColSelectModel();
        m_dialogCompColFilter = new DialogComponentColumnFilter2(colSelectModel, 0);

        final SettingsModelString replaceOrAppendModel = TimestampToDateTimeNodeModel.createReplaceAppendStringBool();
        m_dialogCompReplaceOrAppend = new DialogComponentButtonGroup(replaceOrAppendModel, true, null,
            TimestampToDateTimeNodeModel.OPTION_APPEND, TimestampToDateTimeNodeModel.OPTION_REPLACE);

        final SettingsModelString suffixModel = TimestampToDateTimeNodeModel.createSuffixModel(replaceOrAppendModel);
        m_dialogCompSuffix = new DialogComponentString(suffixModel, "Suffix of appended columns: ");

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
        final JPanel panelInput = new JPanel(new GridLayout(1,2));
        panelInput.setBorder(BorderFactory.createTitledBorder("Input column"));
        m_timeUnitCombobox = new JComboBox<TimeUnit>(TimestampToDateTimeNodeModel.TIMEUNITS);
        final JLabel labelInputUnit = new JLabel("Unit: ");
        panelInput.add(labelInputUnit);
        panelInput.add(m_timeUnitCombobox);

        panel.add(panelInput, gbc);

        /*
         * add output format selection
         */
        gbc.gridy++;
        final JPanel panelTypeFormat = new JPanel(new GridLayout(2,2));
        panelTypeFormat.setBorder(BorderFactory.createTitledBorder("Output column"));
        //Output type selection
        m_typeCombobox = new JComboBox<DateTimeType>(DateTimeType.values());
        final JLabel labelType = new JLabel("New type: ");
        panelTypeFormat.add(labelType);
        panelTypeFormat.add(m_typeCombobox);
        //Timezone selection
        final JLabel labelZoneInput = new JLabel("Timezone for Zoned Date&Time: ");
        m_zoneComboBox = new JComboBox<String>();
        for (final String id : new TreeSet<String>(ZoneId.getAvailableZoneIds())) {
            m_zoneComboBox.addItem(StringUtils.abbreviateMiddle(id, "..", 29));
        }
        panelTypeFormat.add(labelZoneInput);
        panelTypeFormat.add(m_zoneComboBox);

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
    }

    /**
     * Enable the timezoneinput only if the currently selected DateTimeType is "zoned DateAndTime"
     *  @param dt    the currently selected DateTimeType in the m_typeCombobox
     * */
    private void setTimezoneInputEnableStatus(final DateTimeType dt)
    {
        if(dt.equals(DateTimeType.ZONED_DATE_TIME)) {
            m_zoneComboBox.setEnabled(true);
            //timezoneListener(m_timezoneModel.getStringValue());
        } else {
            m_zoneComboBox.setEnabled(false);
            //setTimezoneWarningNull();
        }
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
        settings.addString("timezone", m_zoneComboBox.getSelectedItem().toString());
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
        m_zoneComboBox.setSelectedItem(settings.getString("timezone", StringUtils.abbreviateMiddle(ZoneId.systemDefault().toString(), "..", 29)));
        setTimezoneInputEnableStatus((DateTimeType)m_typeCombobox.getSelectedItem());
    }

}
