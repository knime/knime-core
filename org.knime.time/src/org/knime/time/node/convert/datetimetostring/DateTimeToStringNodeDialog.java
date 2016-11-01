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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.StringValue;
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
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.StringHistory;

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

    private DataTableSpec m_spec;

    /**
     * Predefined date formats.
     */
    public static final Collection<String> PREDEFINED_FORMATS = createPredefinedFormats();

    /**
     * Key for the string history to re-use user entered date formats.
     */
    static final String FORMAT_HISTORY_KEY = "string_to_date_formats";

    static final String OPTION_APPEND = "Append selected columns";

    static final String OPTION_REPLACE = "Replace selected columns";

    /**
     * Setting up all DialogComponents.
     */
    public DateTimeToStringNodeDialog() {
        final SettingsModelColumnFilter2 colSelectModel = createColSelectModel();
        m_dialogCompColFilter = new DialogComponentColumnFilter2(colSelectModel, 0);

        final SettingsModelString replaceOrAppendModel = createReplaceAppendStringBool();
        m_dialogCompReplaceOrAppend =
            new DialogComponentButtonGroup(replaceOrAppendModel, true, null, OPTION_APPEND, OPTION_REPLACE);

        final SettingsModelString suffixModel = createSuffixModel();
        m_dialogCompSuffix = new DialogComponentString(suffixModel, "Suffix of appended columns: ");

        m_formatModel = createFormatModel();
        m_dialogCompFormatSelect =
            new DialogComponentStringSelection(m_formatModel, "Date format: ", PREDEFINED_FORMATS, true);

        final Locale[] availableLocales = Locale.getAvailableLocales();
        final String[] availableLocalesString = new String[availableLocales.length];
        for (int i = 0; i < availableLocales.length; i++) {
            availableLocalesString[i] = availableLocales[i].toString();
        }
        Arrays.sort(availableLocalesString);

        m_dialogCompLocale =
            new DialogComponentStringSelection(createLocaleModel(), "Locale: ", availableLocalesString);

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
        // add label for warning
        final JLabel typeFormatLabel = new JLabel();
        gbcTypeFormat.gridx = 0;
        gbcTypeFormat.gridy++;
        gbcTypeFormat.weightx = 0;
        gbcTypeFormat.gridwidth = 3;
        gbcTypeFormat.insets = new Insets(0, 4, 0, 0);
        typeFormatLabel.setForeground(Color.RED);
        panelTypeFormat.add(typeFormatLabel, gbcTypeFormat);
        panel.add(panelTypeFormat, gbc);
        /*
         * add tab
         */
        addTab("Options", panel);

        /*
         * Change and action listeners
         */
        replaceOrAppendModel.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(final ChangeEvent e) {
                if (replaceOrAppendModel.getStringValue().equals(OPTION_APPEND)) {
                    suffixModel.setEnabled(true);
                } else {
                    suffixModel.setEnabled(false);
                }
            }
        });

        final ChangeListener listener = new ChangeListener() {

            @Override
            public void stateChanged(final ChangeEvent e) {
                final String[] includes = colSelectModel.applyTo(m_spec).getIncludes();

                final String format = m_formatModel.getStringValue();
                int i = 0;
                try {
                    for (String include : includes) {
                        final DataType type = m_spec.getColumnSpec(include).getType();
                        if (type.equals(LocalDateCellFactory.TYPE)) {
                            final LocalDate now1 = LocalDate.now();
                            final DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern(format);
                            LocalDate.parse(now1.format(formatter1), formatter1);
                        } else if (type.equals(LocalTimeCellFactory.TYPE)) {
                            final LocalTime now2 = LocalTime.now();
                            final DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern(format);
                            LocalTime.parse(now2.format(formatter2), formatter2);
                        } else if (type.equals(LocalDateTimeCellFactory.TYPE)) {
                            final LocalDateTime now3 = LocalDateTime.now();
                            final DateTimeFormatter formatter3 = DateTimeFormatter.ofPattern(format);
                            String format2 = now3.format(formatter3);
                            LocalDateTime.parse(format2, formatter3);
                        } else if (type.equals(ZonedDateTimeCellFactory.TYPE)) {
                            final ZonedDateTime now4 = ZonedDateTime.now();
                            final DateTimeFormatter formatter4 = DateTimeFormatter.ofPattern(format);
                            ZonedDateTime.parse(now4.format(formatter4), formatter4);
                        } else {
                            throw new IllegalStateException("Unhandled date&time type: " + type.getName());
                        }
                        i++;
                    }
                    m_dialogCompFormatSelect.setToolTipText(null);
                    ((JComponent)m_dialogCompFormatSelect.getComponentPanel().getComponent(1)).setBorder(null);
                    typeFormatLabel.setText("");
                } catch (IllegalArgumentException ex) {
                    m_dialogCompFormatSelect.setToolTipText(ex.getMessage());
                    typeFormatLabel.setText("Date format is not valid: " + ex.getMessage());
                    ((JComponent)m_dialogCompFormatSelect.getComponentPanel().getComponent(1))
                        .setBorder(BorderFactory.createLineBorder(Color.RED));
                } catch (UnsupportedTemporalTypeException ex) {
                    m_dialogCompFormatSelect.setToolTipText(ex.getMessage());
                    typeFormatLabel.setText("Data type of column '" + includes[i]
                        + "' is not compatible with date format: " + ex.getMessage());
                    ((JComponent)m_dialogCompFormatSelect.getComponentPanel().getComponent(1))
                        .setBorder(BorderFactory.createLineBorder(Color.RED));
                }
            }
        };
        colSelectModel.addChangeListener(listener);
        m_formatModel.addChangeListener(listener);

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
        m_dialogCompFormatSelect.replaceListItems(createPredefinedFormats(), null);
    }

    /**
     * @return a set of all predefined formats plus the formats added by the user
     */
    private static Collection<String> createPredefinedFormats() {
        // unique values
        Set<String> formats = new LinkedHashSet<String>();
        formats.add("yyyy-MM-dd;HH:mm:ss.S");
        formats.add("dd.MM.yyyy;HH:mm:ss.S");
        formats.add("yyyy-MM-dd'T'HH:mm:ss.SSS");
        formats.add("yyyy-MM-dd;HH:mm:ssVV");
        formats.add("dd.MM.yyyy;HH:mm:ssVV");
        formats.add("yyyy-MM-dd'T'HH:mm:ss.SSSVV");
        formats.add("yyyy-MM-dd'T'HH:mm:ss.SSSVV'['zzzz']'");
        formats.add("yyyy/dd/MM");
        formats.add("dd.MM.yyyy");
        formats.add("yyyy-MM-dd");
        formats.add("HH:mm:ss");
        // check also the StringHistory....
        String[] userFormats = StringHistory.getInstance(FORMAT_HISTORY_KEY).getHistory();
        for (String userFormat : userFormats) {
            formats.add(userFormat);
        }
        return formats;
    }

    /** @return the column select model, used in both dialog and model. */
    @SuppressWarnings("unchecked")
    static SettingsModelColumnFilter2 createColSelectModel() {
        return new SettingsModelColumnFilter2("col_select", StringValue.class);
    }

    /** @return the string model, used in both dialog and model. */
    static SettingsModelString createReplaceAppendStringBool() {
        return new SettingsModelString("replace_or_append", DateTimeToStringNodeDialog.OPTION_REPLACE);
    }

    /** @return the string model, used in both dialog and model. */
    static SettingsModelString createSuffixModel() {
        final SettingsModelString settingsModelString = new SettingsModelString("suffix", "(Date&Time)");
        settingsModelString.setEnabled(false);
        return settingsModelString;
    }

    /** @return the string select model, used in both dialog and model. */
    static SettingsModelString createFormatModel() {
        return new SettingsModelString("date_format", "yyyy-MM-dd;HH:mm:ss.S");
    }

    /** @return the boolean model, used in both dialog and model. */
    static SettingsModelBoolean createCancelOnFailModel() {
        return new SettingsModelBoolean("cancel_on_fail", true);
    }

    /** @return the string select model, used in both dialog and model. */
    static SettingsModelString createLocaleModel() {
        return new SettingsModelString("locale", Locale.getDefault().toString());
    }
}
