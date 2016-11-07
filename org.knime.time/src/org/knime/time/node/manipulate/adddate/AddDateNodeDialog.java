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
 *   Oct 28, 2016 (simon): created
 */
package org.knime.time.node.manipulate.adddate;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter2;
import org.knime.core.node.defaultnodesettings.DialogComponentNumberEdit;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * The node dialog of the node which adds a date to a time cell.
 *
 * @author Simon Schmid, KNIME.com, Konstanz, Germany
 */
class AddDateNodeDialog extends NodeDialogPane {
    private final DialogComponentColumnFilter2 m_dialogCompColFilter;

    private final DialogComponentButtonGroup m_dialogCompReplaceOrAppend;

    private final DialogComponentString m_dialogCompSuffix;

    private final DialogComponentNumberEdit m_dialogCompYear;

    private final DialogComponentStringSelection m_dialogCompMonth;

    private final DialogComponentNumberEdit m_dialogCompDay;

    private final DialogComponentBoolean m_dialogCompZoneBool;

    private final DialogComponentStringSelection m_dialogCompTimeZoneSelec;

    /**
     * Setting up all DialogComponents.
     */
    public AddDateNodeDialog() {
        m_dialogCompColFilter = new DialogComponentColumnFilter2(AddDateNodeModel.createColSelectModel(), 0);

        final SettingsModelString replaceOrAppendModel = AddDateNodeModel.createReplaceAppendStringBool();
        m_dialogCompReplaceOrAppend = new DialogComponentButtonGroup(replaceOrAppendModel, true, null,
            AddDateNodeModel.OPTION_APPEND, AddDateNodeModel.OPTION_REPLACE);

        final SettingsModelString suffixModel = AddDateNodeModel.createSuffixModel(replaceOrAppendModel);
        m_dialogCompSuffix = new DialogComponentString(suffixModel, "Suffix of appended columns: ");

        final SettingsModelIntegerBounded yearModel = AddDateNodeModel.createYearModel();
        m_dialogCompYear = new DialogComponentNumberEdit(yearModel, "Year:", 4);

        final Month[] months = Month.values();
        final String[] monthsStrings = new String[months.length];
        for (int i = 0; i < months.length; i++) {
            monthsStrings[i] = months[i].getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        }

        final SettingsModelString monthModel = AddDateNodeModel.createMonthModel();
        m_dialogCompMonth = new DialogComponentStringSelection(monthModel, "Month:", monthsStrings);

        final SettingsModelInteger dayModel = AddDateNodeModel.createDayModel();
        m_dialogCompDay = new DialogComponentNumberEdit(dayModel, "Day:");

        final SettingsModelBoolean zoneModelBool = AddDateNodeModel.createZoneModelBool();
        m_dialogCompZoneBool = new DialogComponentBoolean(zoneModelBool, "Add time zone");

        final SettingsModelString zoneSelectModel = AddDateNodeModel.createTimeZoneSelectModel(zoneModelBool);
        final Set<String> availableZoneIds = ZoneId.getAvailableZoneIds();
        final String[] availableZoneIdsArray = availableZoneIds.toArray(new String[availableZoneIds.size()]);
        Arrays.sort(availableZoneIdsArray);
        m_dialogCompTimeZoneSelec = new DialogComponentStringSelection(zoneSelectModel, "", availableZoneIdsArray);

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
         * add date settings
         */
        gbc.gridy++;
        final JPanel panelDate = new JPanel(new GridBagLayout());
        panelDate.setBorder(BorderFactory.createTitledBorder("Add Date"));
        final GridBagConstraints gbcDate = new GridBagConstraints();
        //add year
        gbcDate.fill = GridBagConstraints.VERTICAL;
        gbcDate.insets = new Insets(3, 0, 0, 0);
        gbcDate.gridx = 0;
        gbcDate.gridy = 0;
        gbcDate.weighty = 0;
        gbcDate.anchor = GridBagConstraints.WEST;
        panelDate.add(m_dialogCompYear.getComponentPanel(), gbcDate);
        // add month
        gbcDate.gridx++;
        gbcDate.insets = new Insets(0, 0, 0, 0);
        panelDate.add(m_dialogCompMonth.getComponentPanel(), gbcDate);
        // add day
        gbcDate.gridx++;
        gbcDate.insets = new Insets(3, 0, 0, 0);
        gbcDate.weightx = 1;
        panelDate.add(m_dialogCompDay.getComponentPanel(), gbcDate);
        // add time zone selection
        final JPanel panelZoneSelec = new JPanel(new GridBagLayout());
        final GridBagConstraints gbcZS = new GridBagConstraints();
        gbcZS.fill = GridBagConstraints.VERTICAL;
        gbcZS.gridx = 0;
        gbcZS.gridy = 0;
        gbcZS.anchor = GridBagConstraints.WEST;
        panelZoneSelec.add(m_dialogCompZoneBool.getComponentPanel(), gbcZS);
        gbcZS.weightx = 1;
        gbcZS.gridx++;
        panelZoneSelec.add(m_dialogCompTimeZoneSelec.getComponentPanel(), gbcZS);
        gbcDate.gridx = 0;
        gbcDate.gridwidth = 4;
        gbcDate.gridy++;
        panelDate.add(panelZoneSelec, gbcDate);
        panel.add(panelDate, gbc);

        /*
         * add tab
         */
        addTab("Options", panel);

        /*
         * Change listeners
         */

        final ChangeListener dateListener = new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                try {
                    LocalDate.of(yearModel.getIntValue(), Month.valueOf(monthModel.getStringValue().toUpperCase()),
                        dayModel.getIntValue());
                    ((JComponent)m_dialogCompDay.getComponentPanel().getComponent(1)).setBorder(null);
                    ((JComponent)m_dialogCompDay.getComponentPanel().getComponent(1)).updateUI();
                    m_dialogCompDay.setToolTipText(null);
                } catch (DateTimeException exc) {
                    System.out.println(exc);
                    ((JComponent)m_dialogCompDay.getComponentPanel().getComponent(1))
                        .setBorder(BorderFactory.createLineBorder(Color.RED));
                    m_dialogCompDay.setToolTipText(exc.getMessage());
                }
            }
        };

        yearModel.addChangeListener(dateListener);
        monthModel.addChangeListener(dateListener);
        dayModel.addChangeListener(dateListener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_dialogCompColFilter.saveSettingsTo(settings);
        m_dialogCompReplaceOrAppend.saveSettingsTo(settings);
        m_dialogCompSuffix.saveSettingsTo(settings);
        m_dialogCompYear.saveSettingsTo(settings);
        m_dialogCompMonth.saveSettingsTo(settings);
        m_dialogCompDay.saveSettingsTo(settings);
        m_dialogCompZoneBool.saveSettingsTo(settings);
        m_dialogCompTimeZoneSelec.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {
        m_dialogCompColFilter.loadSettingsFrom(settings, specs);
        m_dialogCompReplaceOrAppend.loadSettingsFrom(settings, specs);
        m_dialogCompSuffix.loadSettingsFrom(settings, specs);
        m_dialogCompYear.loadSettingsFrom(settings, specs);
        m_dialogCompMonth.loadSettingsFrom(settings, specs);
        m_dialogCompDay.loadSettingsFrom(settings, specs);
        m_dialogCompZoneBool.loadSettingsFrom(settings, specs);
        m_dialogCompTimeZoneSelec.loadSettingsFrom(settings, specs);
    }

}
