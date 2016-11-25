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
package org.knime.time.node.convert.stringtodatetime;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.DataAwareNodeDialogPane;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter2;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.time.node.convert.DateTimeTypes;

/**
 * The node dialog of the node which converts strings to the new date&time types.
 *
 * @author Simon Schmid, KNIME.com, Konstanz, Germany
 */
public class StringToDateTimeNodeDialog extends DataAwareNodeDialogPane {

    private final DialogComponentColumnFilter2 m_dialogCompColFilter;

    private final DialogComponentButtonGroup m_dialogCompReplaceOrAppend;

    private final DialogComponentString m_dialogCompSuffix;

    private final JComboBox<DateTimeTypes> m_typeCombobox;

    private final DialogComponentStringSelection m_dialogCompLocale;

    private final DialogComponentStringSelection m_dialogCompFormatSelect;

    private final JLabel m_typeFormatWarningLabel;

    private final DialogComponentBoolean m_dialogCompCancelOnFail;

    private final SettingsModelString m_formatModel;

    private final JLabel m_previewLabel;

    private DataTableSpec m_spec;

    private BufferedDataTable m_dataTable;

    private String m_preview;

    /**
     * Setting up all DialogComponents.
     */
    public StringToDateTimeNodeDialog() {
        final SettingsModelColumnFilter2 colSelectModel = StringToDateTimeNodeModel.createColSelectModel();
        m_dialogCompColFilter = new DialogComponentColumnFilter2(colSelectModel, 0);

        final SettingsModelString replaceOrAppendModel = StringToDateTimeNodeModel.createReplaceAppendStringBool();
        m_dialogCompReplaceOrAppend = new DialogComponentButtonGroup(replaceOrAppendModel, true, null,
            StringToDateTimeNodeModel.OPTION_APPEND, StringToDateTimeNodeModel.OPTION_REPLACE);

        final SettingsModelString suffixModel = StringToDateTimeNodeModel.createSuffixModel(replaceOrAppendModel);
        m_dialogCompSuffix = new DialogComponentString(suffixModel, "Suffix of appended columns: ");

        m_formatModel = StringToDateTimeNodeModel.createFormatModel();
        m_dialogCompFormatSelect = new DialogComponentStringSelection(m_formatModel, "Date format: ",
            StringToDateTimeNodeModel.createPredefinedFormats(), true);

        final Locale[] availableLocales = Locale.getAvailableLocales();
        final String[] availableLocalesString = new String[availableLocales.length];
        for (int i = 0; i < availableLocales.length; i++) {
            availableLocalesString[i] = availableLocales[i].toString();
        }
        Arrays.sort(availableLocalesString);

        m_dialogCompLocale = new DialogComponentStringSelection(StringToDateTimeNodeModel.createLocaleModel(),
            "Locale: ", availableLocalesString);

        final SettingsModelBoolean cancelOnFailModel = StringToDateTimeNodeModel.createCancelOnFailModel();
        m_dialogCompCancelOnFail = new DialogComponentBoolean(cancelOnFailModel, "Fail on error");

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
         * add type/format selection
         */
        gbc.gridy++;
        final JPanel panelTypeFormat = new JPanel(new GridBagLayout());
        panelTypeFormat.setBorder(BorderFactory.createTitledBorder("Type and Format Selection"));
        final GridBagConstraints gbcTypeFormat = new GridBagConstraints();
        // add label and combo box for type selection
        gbcTypeFormat.insets = new Insets(0, 0, 0, 0);
        gbcTypeFormat.fill = GridBagConstraints.VERTICAL;
        gbcTypeFormat.gridx = 0;
        gbcTypeFormat.gridy = 0;
        gbcTypeFormat.weighty = 0;
        gbcTypeFormat.anchor = GridBagConstraints.WEST;
        m_typeCombobox = new JComboBox<DateTimeTypes>(DateTimeTypes.values());
        final JPanel panelTypeList = new JPanel(new FlowLayout());
        final JLabel labelType = new JLabel("New type: ");
        panelTypeList.add(labelType);
        panelTypeList.add(m_typeCombobox);
        panelTypeFormat.add(panelTypeList, gbcTypeFormat);
        // add format selection
        gbcTypeFormat.insets = new Insets(0, 15, 0, 0);
        gbcTypeFormat.gridx++;
        panelTypeFormat.add(m_dialogCompFormatSelect.getComponentPanel(), gbcTypeFormat);
        // add label and combo box for locale selection
        gbcTypeFormat.insets = new Insets(0, 0, 0, 0);
        gbcTypeFormat.gridx = 0;
        gbcTypeFormat.gridy++;
        panelTypeFormat.add(m_dialogCompLocale.getComponentPanel(), gbcTypeFormat);
        // add label for preview of the first cells content
        m_previewLabel = new JLabel();
        gbcTypeFormat.insets = new Insets(0, 20, 0, 0);
        gbcTypeFormat.gridx++;
        gbcTypeFormat.weightx = 1;
        panelTypeFormat.add(m_previewLabel, gbcTypeFormat);
        // add button to guess type and format
        final JButton guessButton = new JButton("Guess data type and format");
        gbcTypeFormat.insets = new Insets(5, 20, 5, 0);
        gbcTypeFormat.gridx = 1;
        gbcTypeFormat.gridy++;
        gbcTypeFormat.weightx = 0;
        panelTypeFormat.add(guessButton, gbcTypeFormat);
        // add label for warning
        m_typeFormatWarningLabel = new JLabel();
        gbcTypeFormat.gridx = 0;
        gbcTypeFormat.gridy++;
        gbcTypeFormat.weightx = 0;
        gbcTypeFormat.gridwidth = 3;
        gbcTypeFormat.anchor = GridBagConstraints.CENTER;
        m_typeFormatWarningLabel.setForeground(Color.RED);
        panelTypeFormat.add(m_typeFormatWarningLabel, gbcTypeFormat);
        panel.add(panelTypeFormat, gbc);

        /*
         * add cancel on fail selection
         */
        gbc.gridy++;
        final JPanel panelCancelOnFail = new JPanel(new GridBagLayout());
        panelCancelOnFail.setBorder(BorderFactory.createTitledBorder("Abort Execution"));
        final GridBagConstraints gbcCancelOnFail = new GridBagConstraints();
        // add check box
        gbcCancelOnFail.fill = GridBagConstraints.VERTICAL;
        gbcCancelOnFail.gridx = 0;
        gbcCancelOnFail.gridy = 0;
        gbcCancelOnFail.weightx = 1;
        gbcCancelOnFail.weighty = 0;
        gbcCancelOnFail.anchor = GridBagConstraints.WEST;
        panelCancelOnFail.add(m_dialogCompCancelOnFail.getComponentPanel(), gbcCancelOnFail);

        panel.add(panelCancelOnFail, gbc);

        /*
         * add tab
         */
        addTab("Options", panel);

        /*
         * Change and action listeners
         */
        m_typeCombobox.addActionListener(e -> formatListener(m_formatModel.getStringValue()));
        m_dialogCompFormatSelect.getModel().addChangeListener(e -> formatListener(m_formatModel.getStringValue()));
        colSelectModel.addChangeListener(e -> updatePreview(colSelectModel));
        m_typeCombobox.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                final Collection<String> formats = StringToDateTimeNodeModel.createPredefinedFormats();
                if (!formatListener(m_formatModel.getStringValue())) {
                    for (final String format : formats) {
                        if (formatListener(format)) {
                            m_formatModel.setStringValue(format);
                            break;
                        }
                    }
                }
            }
        });
        guessButton.addActionListener(e -> guessFormat(m_preview));
    }

    private void guessFormat(final String preview) {
        final Collection<String> formats = StringToDateTimeNodeModel.createPredefinedFormats();
        for (final String format : formats) {
            final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
            try {
                ZonedDateTime.parse(preview, formatter);
                m_typeCombobox.setSelectedItem(DateTimeTypes.ZONED_DATE_TIME);
                m_formatModel.setStringValue(format);
                break;
            } catch (DateTimeException e) {
            }
            try {
                LocalDateTime.parse(preview, formatter);
                m_typeCombobox.setSelectedItem(DateTimeTypes.LOCAL_DATE_TIME);
                m_formatModel.setStringValue(format);
                break;
            } catch (DateTimeException e) {
            }
            try {
                LocalDate.parse(preview, formatter);
                m_typeCombobox.setSelectedItem(DateTimeTypes.LOCAL_DATE);
                m_formatModel.setStringValue(format);
                break;
            } catch (DateTimeException e) {
            }
            try {
                LocalTime.parse(preview, formatter);
                m_typeCombobox.setSelectedItem(DateTimeTypes.LOCAL_TIME);
                m_formatModel.setStringValue(format);
                break;
            } catch (DateTimeException e) {
            }
        }
    }

    /**
     * @param colSelectModel settings model of the column filter
     */
    private void updatePreview(final SettingsModelColumnFilter2 colSelectModel) {
        final String[] includes = colSelectModel.applyTo(m_spec).getIncludes();
        Arrays.stream(colSelectModel.applyTo(m_spec).getIncludes()).mapToInt(s -> m_spec.findColumnIndex(s)).toArray();
        m_preview = "";
        if (m_dataTable != null) {
            if (!(includes.length == 0 || m_dataTable.size() == 0)) {
                for (final DataRow row : m_dataTable) {
                    final DataCell cell = row.getCell(m_spec.findColumnIndex(includes[0]));
                    if (cell.isMissing()) {
                        continue;
                    } else {
                        m_preview = ((StringValue)cell).getStringValue();
                        break;
                    }
                }
            }
        }
        m_previewLabel.setText("Content of the first cell: " + m_preview);
    }

    /**
     * method for change/action listener of type and date combo boxes.
     */
    private boolean formatListener(final String format) {
        switch ((DateTimeTypes)m_typeCombobox.getSelectedItem()) {
            case LOCAL_DATE: {
                try {
                    final LocalDate now1 = LocalDate.now();
                    final DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern(format);
                    LocalDate.parse(now1.format(formatter1), formatter1);
                    return setTypeFormatWarningNull();
                } catch (DateTimeException exception) {
                    return setTypeFormatWarningMessage(exception, DateTimeTypes.LOCAL_DATE.toString()
                        + " needs a date, but does not support a time, time zone or offset!");
                } catch (IllegalArgumentException exception) {
                    return setTypeFormatWarningMessage(exception, exception.getMessage());
                }
            }
            case LOCAL_TIME: {
                try {
                    final LocalTime now2 = LocalTime.now();
                    final DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern(format);
                    LocalTime.parse(now2.format(formatter2), formatter2);
                    return setTypeFormatWarningNull();
                } catch (DateTimeException exception) {
                    return setTypeFormatWarningMessage(exception, DateTimeTypes.LOCAL_TIME.toString()
                        + " needs a time, but does not support a date, time zone or offset!");
                } catch (IllegalArgumentException exception) {
                    return setTypeFormatWarningMessage(exception, exception.getMessage());
                }
            }
            case LOCAL_DATE_TIME: {
                try {
                    final LocalDateTime now3 = LocalDateTime.now();
                    final DateTimeFormatter formatter3 = DateTimeFormatter.ofPattern(format);
                    final String format2 = now3.format(formatter3);
                    LocalDateTime.parse(format2, formatter3);
                    return setTypeFormatWarningNull();
                } catch (DateTimeException exception) {
                    return setTypeFormatWarningMessage(exception, DateTimeTypes.LOCAL_DATE_TIME.toString()
                        + " needs date and time, but does not support a time zone or offset!");
                } catch (IllegalArgumentException exception) {
                    return setTypeFormatWarningMessage(exception, exception.getMessage());
                }
            }
            case ZONED_DATE_TIME: {
                try {
                    final ZonedDateTime now4 = ZonedDateTime.now();
                    final DateTimeFormatter formatter4 = DateTimeFormatter.ofPattern(format);
                    ZonedDateTime.parse(now4.format(formatter4), formatter4);
                    return setTypeFormatWarningNull();
                } catch (DateTimeException exception) {
                    return setTypeFormatWarningMessage(exception,
                        DateTimeTypes.ZONED_DATE_TIME.toString() + " needs date, time and a time zone or offset!");
                } catch (IllegalArgumentException exception) {
                    return setTypeFormatWarningMessage(exception, exception.getMessage());
                }
            }
            default:
                throw new IllegalStateException("Unhandled date&time type: " + m_typeCombobox.getSelectedItem());
        }
    }

    private boolean setTypeFormatWarningNull() {
        m_typeCombobox.setBorder(null);
        m_dialogCompFormatSelect.setToolTipText(null);
        m_typeCombobox.setToolTipText(null);
        m_typeFormatWarningLabel.setToolTipText(null);
        m_typeFormatWarningLabel.setText("");
        return true;
    }

    private boolean setTypeFormatWarningMessage(final Exception exception, final String message) {
        m_dialogCompFormatSelect.setToolTipText(exception.getMessage());
        m_typeCombobox.setToolTipText(exception.getMessage());
        m_typeFormatWarningLabel.setToolTipText(exception.getMessage());
        m_typeFormatWarningLabel.setText(message);
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
        settings.addString("typeEnum", ((DateTimeTypes)m_typeCombobox.getModel().getSelectedItem()).name());
        m_dialogCompFormatSelect.saveSettingsTo(settings);
        m_dialogCompLocale.saveSettingsTo(settings);
        m_dialogCompCancelOnFail.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        m_dataTable = null;
        m_spec = (DataTableSpec)specs[0];
        m_dialogCompColFilter.loadSettingsFrom(settings, specs);
        m_dialogCompReplaceOrAppend.loadSettingsFrom(settings, specs);
        m_dialogCompSuffix.loadSettingsFrom(settings, specs);
        m_typeCombobox.setSelectedItem(
            DateTimeTypes.valueOf(settings.getString("typeEnum", DateTimeTypes.LOCAL_DATE_TIME.name())));
        m_dialogCompFormatSelect.loadSettingsFrom(settings, specs);
        m_dialogCompLocale.loadSettingsFrom(settings, specs);
        m_dialogCompCancelOnFail.loadSettingsFrom(settings, specs);
        // retrieve potential new values from the StringHistory and add them
        // (if not already present) to the combo box...
        m_dialogCompFormatSelect.replaceListItems(StringToDateTimeNodeModel.createPredefinedFormats(), null);
        // set preview
        updatePreview((SettingsModelColumnFilter2)m_dialogCompColFilter.getModel());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObject[] input)
        throws NotConfigurableException {
        m_dataTable = (BufferedDataTable)input[0];
        m_spec = m_dataTable.getDataTableSpec();
        final DataTableSpec[] specs = new DataTableSpec[]{m_spec};
        m_dialogCompColFilter.loadSettingsFrom(settings, specs);
        m_dialogCompReplaceOrAppend.loadSettingsFrom(settings, specs);
        m_dialogCompSuffix.loadSettingsFrom(settings, specs);
        m_typeCombobox.setSelectedItem(
            DateTimeTypes.valueOf(settings.getString("typeEnum", DateTimeTypes.LOCAL_DATE_TIME.name())));
        m_dialogCompFormatSelect.loadSettingsFrom(settings, specs);
        m_dialogCompLocale.loadSettingsFrom(settings, specs);
        m_dialogCompCancelOnFail.loadSettingsFrom(settings, specs);
        // retrieve potential new values from the StringHistory and add them
        // (if not already present) to the combo box...
        m_dialogCompFormatSelect.replaceListItems(StringToDateTimeNodeModel.createPredefinedFormats(), null);
        // set preview
        updatePreview((SettingsModelColumnFilter2)m_dialogCompColFilter.getModel());
    }

}
