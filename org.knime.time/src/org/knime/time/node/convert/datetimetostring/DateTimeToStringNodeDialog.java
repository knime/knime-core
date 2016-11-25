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
 *   Oct 19, 2016 (simon): created
 */
package org.knime.time.node.convert.datetimetostring;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.commons.lang3.LocaleUtils;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.time.localdate.LocalDateCellFactory;
import org.knime.core.data.time.localdatetime.LocalDateTimeCellFactory;
import org.knime.core.data.time.localtime.LocalTimeCellFactory;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeCellFactory;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter2;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.time.node.convert.DateTimeTypes;

/**
 * The node dialog of the node which converts strings to the new date&time types.
 *
 * @author Simon Schmid, KNIME.com, Konstanz, Germany
 */
public class DateTimeToStringNodeDialog extends NodeDialogPane {

    private final DialogComponentColumnFilter2 m_dialogCompColFilter;

    private final DialogComponentButtonGroup m_dialogCompReplaceOrAppend;

    private final DialogComponentString m_dialogCompSuffix;

    private final DialogComponentStringSelection m_dialogCompFormatSelect;

    private final DialogComponentStringSelection m_dialogCompLocale;

    private final SettingsModelString m_formatModel;

    private SettingsModelString m_localeModel;

    private final JLabel m_previewLabel;

    private final JLabel m_typeFormatWarningLabel;

    private DataTableSpec m_spec;

    private final ZonedDateTime m_currentZDT;

    /**
     * Setting up all DialogComponents.
     */
    public DateTimeToStringNodeDialog() {
        m_currentZDT = ZonedDateTime.now();

        final SettingsModelColumnFilter2 colSelectModel = DateTimeToStringNodeModel.createColSelectModel();
        m_dialogCompColFilter = new DialogComponentColumnFilter2(colSelectModel, 0);

        final SettingsModelString replaceOrAppendModel = DateTimeToStringNodeModel.createReplaceAppendStringBool();
        m_dialogCompReplaceOrAppend = new DialogComponentButtonGroup(replaceOrAppendModel, true, null,
            DateTimeToStringNodeModel.OPTION_APPEND, DateTimeToStringNodeModel.OPTION_REPLACE);

        final SettingsModelString suffixModel = DateTimeToStringNodeModel.createSuffixModel(replaceOrAppendModel);
        m_dialogCompSuffix = new DialogComponentString(suffixModel, "Suffix of appended columns: ");

        m_formatModel = DateTimeToStringNodeModel.createFormatModel();
        m_dialogCompFormatSelect = new DialogComponentStringSelection(m_formatModel, "Date format: ",
            DateTimeToStringNodeModel.createPredefinedFormats(), true);

        final Locale[] availableLocales = Locale.getAvailableLocales();
        final String[] availableLocalesString = new String[availableLocales.length];
        for (int i = 0; i < availableLocales.length; i++) {
            availableLocalesString[i] = availableLocales[i].toString();
        }
        Arrays.sort(availableLocalesString);

        m_localeModel = DateTimeToStringNodeModel.createLocaleModel();
        m_dialogCompLocale = new DialogComponentStringSelection(m_localeModel, "Locale: ", availableLocalesString);

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
        gbcReplaceAppend.anchor = GridBagConstraints.WEST;
        panelReplace.add(m_dialogCompReplaceOrAppend.getComponentPanel(), gbcReplaceAppend);
        // add suffix text field
        gbcReplaceAppend.gridx++;
        gbcReplaceAppend.weightx = 1;
        gbcReplaceAppend.insets = new Insets(2, 10, 0, 0);
        panelReplace.add(m_dialogCompSuffix.getComponentPanel(), gbcReplaceAppend);

        panel.add(panelReplace, gbc);

        /*
         * add type/format selection
         */
        gbc.gridy++;
        final JPanel panelTypeFormat = new JPanel(new GridBagLayout());
        panelTypeFormat.setBorder(BorderFactory.createTitledBorder("Type and Format Selection"));
        final GridBagConstraints gbcTypeFormat = new GridBagConstraints();
        // add label and combo box for type selection
        gbcTypeFormat.fill = GridBagConstraints.VERTICAL;
        gbcTypeFormat.gridx = 0;
        gbcTypeFormat.gridy = 0;
        gbcTypeFormat.anchor = GridBagConstraints.WEST;
        gbcTypeFormat.insets = new Insets(3, 0, 0, 0);
        panelTypeFormat.add(m_dialogCompFormatSelect.getComponentPanel(), gbcTypeFormat);
        // add label and combo box for locale selection
        gbcTypeFormat.gridx++;
        gbcTypeFormat.weightx = 1;
        gbcTypeFormat.insets = new Insets(0, 20, 0, 0);
        panelTypeFormat.add(m_dialogCompLocale.getComponentPanel(), gbcTypeFormat);
        // add label for preview of the first cells content
        m_previewLabel = new JLabel();
        gbcTypeFormat.insets = new Insets(0, 5, 0, 0);
        gbcTypeFormat.gridx = 0;
        gbcTypeFormat.weightx = 0;
        gbcTypeFormat.gridy++;
        panelTypeFormat.add(m_previewLabel, gbcTypeFormat);
        // add label for warning
        m_typeFormatWarningLabel = new JLabel();
        gbcTypeFormat.gridx = 0;
        gbcTypeFormat.gridy++;
        gbcTypeFormat.weightx = 0;
        gbcTypeFormat.gridwidth = 3;
        gbcTypeFormat.insets = new Insets(6, 4, 0, 0);
        m_typeFormatWarningLabel.setForeground(Color.RED);
        panelTypeFormat.add(m_typeFormatWarningLabel, gbcTypeFormat);
        panel.add(panelTypeFormat, gbc);
        /*
         * add tab
         */
        addTab("Options", panel);

        /*
         * Change listeners
         */
        final ChangeListener listener = new ChangeListener() {

            @Override
            public void stateChanged(final ChangeEvent e) {
                final String[] includes = colSelectModel.applyTo(m_spec).getIncludes();

                final String format = m_formatModel.getStringValue();
                int i = 0;
                for (String include : includes) {
                    final DataType type = m_spec.getColumnSpec(include).getType();
                    if (type.equals(LocalDateCellFactory.TYPE)) {
                        try {
                            final LocalDate now1 = LocalDate.now();
                            final DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern(format);
                            now1.format(formatter1);
                            setTypeFormatWarningNull();
                        } catch (DateTimeException exception) {
                            setTypeFormatWarningMessage(exception,
                                "'" + includes[i] + "' is a " + DateTimeTypes.LOCAL_DATE.toString()
                                    + " and only contains a date, but the format contains time fields!");
                        } catch (IllegalArgumentException exception) {
                            setTypeFormatWarningMessage(exception, exception.getMessage());
                        }
                    } else if (type.equals(LocalTimeCellFactory.TYPE)) {
                        try {
                            final LocalTime now2 = LocalTime.now();
                            final DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern(format);
                            now2.format(formatter2);
                            setTypeFormatWarningNull();
                        } catch (DateTimeException exception) {
                            setTypeFormatWarningMessage(exception,
                                "'" + includes[i] + "' is a " + DateTimeTypes.LOCAL_TIME.toString()
                                    + " and only contains a time, but the format contains date fields!");
                        } catch (IllegalArgumentException exception) {
                            setTypeFormatWarningMessage(exception, exception.getMessage());
                        }
                    } else if (type.equals(LocalDateTimeCellFactory.TYPE)) {
                        try {
                            final LocalDateTime now3 = LocalDateTime.now();
                            final DateTimeFormatter formatter3 = DateTimeFormatter.ofPattern(format);
                            now3.format(formatter3);
                            setTypeFormatWarningNull();
                        } catch (DateTimeException exception) {
                            setTypeFormatWarningMessage(exception, "'" + includes[i] + "' is a "
                                + DateTimeTypes.LOCAL_DATE_TIME.toString()
                                + " and only contains a date and a time, but the format contains time zone fields!");
                        } catch (IllegalArgumentException exception) {
                            setTypeFormatWarningMessage(exception, exception.getMessage());
                        }
                    } else if (type.equals(ZonedDateTimeCellFactory.TYPE)) {
                        try {
                            final ZonedDateTime now4 = ZonedDateTime.now();
                            final DateTimeFormatter formatter4 = DateTimeFormatter.ofPattern(format);
                            now4.format(formatter4);
                            setTypeFormatWarningNull();
                        } catch (DateTimeException exception) {
                            setTypeFormatWarningMessage(exception,
                                "'" + includes[i] + "' is a " + DateTimeTypes.ZONED_DATE_TIME.toString()
                                    + " and only contains a date, time, and time zone or offset!");
                        } catch (IllegalArgumentException exception) {
                            setTypeFormatWarningMessage(exception, exception.getMessage());
                        }
                    } else {
                        throw new IllegalStateException("Unhandled date&time type: " + type.getName());
                    }
                    i++;
                }
            }
        };
        colSelectModel.addChangeListener(listener);
        m_formatModel.addChangeListener(listener);
        m_formatModel.addChangeListener(e -> updatePreview());
        m_localeModel.addChangeListener(e -> updatePreview());

    }

    private void updatePreview() {
        try {
            final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(m_formatModel.getStringValue(),
                LocaleUtils.toLocale(m_localeModel.getStringValue()));
            final String result = m_currentZDT.format(formatter);
            m_previewLabel.setText("Preview: " + result);
        } catch (IllegalArgumentException e) {
            m_previewLabel.setText("Preview: ");
        }
    }

    private void setTypeFormatWarningNull() {
        m_dialogCompFormatSelect.setToolTipText(null);
        m_typeFormatWarningLabel.setToolTipText(null);
        ((JComponent)m_dialogCompFormatSelect.getComponentPanel().getComponent(1)).setBorder(null);
        m_typeFormatWarningLabel.setText("");
    }

    private void setTypeFormatWarningMessage(final Exception exception, final String message) {
        m_dialogCompFormatSelect.setToolTipText(exception.getMessage());
        m_typeFormatWarningLabel.setToolTipText(exception.getMessage());
        m_typeFormatWarningLabel.setText(message);
        ((JComponent)m_dialogCompFormatSelect.getComponentPanel().getComponent(1))
            .setBorder(BorderFactory.createLineBorder(Color.RED));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_dialogCompColFilter.saveSettingsTo(settings);
        m_dialogCompReplaceOrAppend.saveSettingsTo(settings);
        m_dialogCompSuffix.saveSettingsTo(settings);
        m_dialogCompFormatSelect.saveSettingsTo(settings);
        m_dialogCompLocale.saveSettingsTo(settings);
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
        m_dialogCompFormatSelect.loadSettingsFrom(settings, specs);
        m_dialogCompLocale.loadSettingsFrom(settings, specs);
        // retrieve potential new values from the StringHistory and add them
        // (if not already present) to the combo box...
        m_dialogCompFormatSelect.replaceListItems(DateTimeToStringNodeModel.createPredefinedFormats(), null);
        // update preview
        updatePreview();
    }

}
