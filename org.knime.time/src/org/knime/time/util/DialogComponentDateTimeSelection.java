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
 *   Nov 8, 2016 (simon): created
 */
package org.knime.time.util;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.Locale;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSpinner;
import javax.swing.JSpinner.DateEditor;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.plaf.basic.BasicSpinnerUI;
import javax.swing.text.DateFormatter;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.port.PortObjectSpec;

import com.toedter.calendar.JDateChooser;

/**
 * Provide a standard component for a dialog that allows to select a date, time and/or time zone.
 *
 * @author Simon Schmid, KNIME.com, Konstanz, Germany
 */
public class DialogComponentDateTimeSelection extends DialogComponent {

    private final static String TIME_FORMAT_WITHOUT_MS = "HH:mm:ss";

    private final static String TIME_FORMAT_WITH_MS = "HH:mm:ss.SSS";

    private boolean m_useMillis;

    private final JCheckBox m_dateCheckbox;

    private final JDateChooser m_dateChooser;

    private final JCheckBox m_timeCheckbox;

    private final JSpinner m_timeSpinner;

    private final JCheckBox m_zoneCheckbox;

    private final JComboBox<String> m_zoneComboBox;

    private final DisplayOption m_displayOption;

    private JSpinner.DateEditor m_editor;

    private boolean m_isEditorInitialized;

    /**
     * Constructor puts for the date, time and time zone a checkbox and chooser into the panel according to display
     * option.
     *
     * @param model the model that stores the values for this component
     * @param label label for the border of the dialog, if <code>null</code> no border is created
     * @param displayOption an option of {@link DisplayOption}, defines which components are put into the panel
     */
    public DialogComponentDateTimeSelection(final SettingsModelDateTime model, final String label,
        final DisplayOption displayOption) {
        super(model);
        m_isEditorInitialized = false;
        m_displayOption = displayOption;

        final JPanel panel = new JPanel(new GridBagLayout());
        if (label != null) {
            panel.setBorder(BorderFactory.createTitledBorder(label));
        }

        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(5, 5, 5, 5);

        /*
         * === add date components ===
         */
        final String dateLabel = "Date:";
        m_dateCheckbox = new JCheckBox(dateLabel, true);
        final Date value = Date.from(model.getZonedDateTime().toInstant());
        m_dateChooser = new JDateChooser("yyyy-MM-dd", "####-##-##", '_');
        // fixes bug AP-5865
        setPopupProperty(m_dateChooser);

        if (value != null) {
            m_dateChooser.setDate(value);
        }
        for (final Component comp : m_dateChooser.getComponents()) {
            if (comp instanceof JTextField) {
                ((JTextField)comp).setColumns(100);
            }
        }
        if (m_displayOption.isShowDate()) {
            model.setUseDate(true);
            if (m_displayOption.isShowDateOptional()) {
                panel.add(m_dateCheckbox, gbc);
            } else {
                panel.add(new JLabel(dateLabel), gbc);
            }
            gbc.gridx++;
            gbc.ipadx = 20;
            if (!m_displayOption.isShowTime() && !m_displayOption.isShowZone()) {
                gbc.weightx = 1;
            }
            panel.add(m_dateChooser, gbc);
        } else {
            model.setUseDate(false);
        }

        /*
         * === add time components ===
         */
        final String timeLabel = "Time:";
        m_timeCheckbox = new JCheckBox(timeLabel, true);
        m_timeSpinner = new JSpinner(new SpinnerDateModel());
        m_timeSpinner.setUI(new TimeSpinnerUI());
        m_editor = isUseMillis() ? new JSpinner.DateEditor(m_timeSpinner, TIME_FORMAT_WITH_MS)
            : new JSpinner.DateEditor(m_timeSpinner, TIME_FORMAT_WITHOUT_MS);
        m_editor.getTextField().setColumns(6);

        m_timeSpinner.setEditor(m_editor);
        if (value != null) {
            m_timeSpinner.setValue(value);
        }
        if (m_displayOption.isShowTime()) {
            model.setUseTime(true);
            gbc.weightx = 0;
            if (m_displayOption.isShowDate()) {
                gbc.gridx++;
                gbc.insets = new Insets(0, 15, 0, 0);
            }
            gbc.ipadx = 0;
            if (m_displayOption.isShowTimeOptional()) {
                panel.add(m_timeCheckbox, gbc);
            } else {
                panel.add(new JLabel(timeLabel), gbc);
            }
            gbc.gridx++;
            gbc.ipadx = 20;
            gbc.weightx = 1;
            gbc.insets = new Insets(0, 7, 0, 5);
            panel.add(m_timeSpinner, gbc);
        } else {
            model.setUseTime(false);
        }

        /*
         * === add time zone components ===
         */
        final String zoneLabel = "Time Zone:";
        m_zoneCheckbox = new JCheckBox(zoneLabel, true);
        m_zoneComboBox = new JComboBox<String>();
        for (final String id : new TreeSet<String>(ZoneId.getAvailableZoneIds())) {
            m_zoneComboBox.addItem(id);
        }
        final ZoneId zone = model.getZone();
        if (zone != null) {
            m_zoneComboBox.setSelectedItem(zone.getId());
        }
        if (m_displayOption.isShowZone()) {
            model.setUseZone(true);
            final JPanel zonePanel = new JPanel();
            gbc.gridx = 0;
            gbc.gridy++;
            if (m_displayOption.isShowZoneOptional()) {
                zonePanel.add(m_zoneCheckbox);
            } else {
                zonePanel.add(new JLabel(zoneLabel));
            }
            zonePanel.add(m_zoneComboBox);
            gbc.gridwidth = 4;
            gbc.insets = new Insets(0, 0, 0, 0);
            gbc.ipadx = 0;
            gbc.weightx = 1;
            panel.add(zonePanel, gbc);
        } else {
            model.setUseZone(false);
        }

        getComponentPanel().add(panel);

        /*
         * === add action and change listeners ===
         */
        m_dateCheckbox.addActionListener(e -> {
            m_dateChooser.setEnabled(m_dateCheckbox.isSelected() && model.isEnabled());
            model.setUseDate(m_dateCheckbox.isSelected());
        });
        m_dateChooser.addPropertyChangeListener("date", e -> {
            model.setLocalDate(m_dateChooser.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        });
        m_timeCheckbox.addActionListener(e -> {
            m_timeSpinner.setEnabled(m_timeCheckbox.isSelected() && model.isEnabled());
            model.setUseTime(m_timeCheckbox.isSelected());
        });
        m_timeSpinner.addChangeListener(e -> {
            final DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern(isUseMillis() ? TIME_FORMAT_WITH_MS : TIME_FORMAT_WITHOUT_MS);
            model.setLocalTime(LocalTime.parse(
                ((DateEditor)m_timeSpinner.getEditor()).getFormat().format(m_timeSpinner.getValue()), formatter));

        });
        m_zoneCheckbox.addActionListener(e -> {
            m_zoneComboBox.setEnabled(m_zoneCheckbox.isSelected() && model.isEnabled());
            model.setUseZone(m_zoneCheckbox.isSelected());
        });
        m_zoneComboBox.addActionListener(e -> {
            model.setZone(ZoneId.of((String)m_zoneComboBox.getSelectedItem()));
        });
        model.addChangeListener(e -> {
            if (!m_isEditorInitialized) {
                ((DateFormatter)m_editor.getTextField().getFormatter()).setFormat(new SimpleDateFormat(
                    model.useMillis() ? TIME_FORMAT_WITH_MS : TIME_FORMAT_WITHOUT_MS, Locale.getDefault()));
                setUseMillis(model.useMillis());
                m_isEditorInitialized = true;
            }
            updateComponent();
        });
        m_editor.getTextField().addFocusListener(new FocusAdapter() {
            /**
             * {@inheritDoc}
             */
            @Override
            public void focusLost(final FocusEvent e) {
                updateSpinnerFormat();
            }

        });
    }

    private void setPopupProperty(final JDateChooser dateChooser){
        try {
            // Java will fetch a static field that is public, if you
            // declare it to be non-static or give it the wrong scope, it
            // automatically retrieves the static field from a super
            // class/interface (from which super interface it gets it,
            // depends pretty much on the order after the "extends ..."
            // statement) If this field has the wrong type, a coding
            // problem is reported.
            final Field typeField = JDateChooser.class.getDeclaredField("popup");
            typeField.setAccessible(true);
            ((JPopupMenu)typeField.get(dateChooser)).setFocusable(false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    /**
     * Allows the user to determine by himself whether milliseconds shall be used or not. Checks if the text in the text
     * field of the time spinner contains milliseconds or not and updates the format of the spinner editor accordingly.
     *
     */
    private void updateSpinnerFormat() {
        final JFormattedTextField field = (JFormattedTextField)m_editor.getComponent(0);
        try {
            DateTimeFormatter.ofPattern(TIME_FORMAT_WITH_MS).parse(field.getText());
            if (!m_useMillis) {
                setUseMillis(true);
                ((DateFormatter)m_editor.getTextField().getFormatter())
                    .setFormat(new SimpleDateFormat(TIME_FORMAT_WITH_MS, Locale.getDefault()));
                updateModel();
            }
        } catch (final DateTimeParseException iae) {
            try {
                DateTimeFormatter.ofPattern(TIME_FORMAT_WITHOUT_MS).parse(field.getText());
                if (m_useMillis) {
                    setUseMillis(false);
                    ((DateFormatter)m_editor.getTextField().getFormatter())
                        .setFormat(new SimpleDateFormat(TIME_FORMAT_WITHOUT_MS, Locale.getDefault()));
                    updateModel();
                }
            } catch (final DateTimeParseException iae2) {
            }
        }
    }

    /**
     * @param used sets the boolean of the date component check box
     */
    public void setDateUsed(final boolean used) {
        m_dateCheckbox.setSelected(used);
    }

    /**
     * @param used sets the boolean of the time component check box
     */
    public void setTimeUsed(final boolean used) {
        m_timeCheckbox.setSelected(used);
    }

    /**
     * @param used sets the boolean of the zone component check box
     */
    public void setZoneUsed(final boolean used) {
        m_zoneCheckbox.setSelected(used);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateComponent() {
        final SettingsModelDateTime model = (SettingsModelDateTime)getModel();
        m_dateChooser.setDate(Date.from(model.getLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant()));
        m_dateCheckbox.setSelected(model.useDate());
        m_timeSpinner.setValue(Date
            .from(model.getLocalTime().atDate(LocalDate.of(1970, 1, 1)).atZone(ZoneId.systemDefault()).toInstant()));
        m_timeCheckbox.setSelected(model.useTime());

        m_zoneComboBox.setSelectedItem(model.getZone().getId());
        m_zoneCheckbox.setSelected(model.useZone());
        setEnabledComponents(model.isEnabled());
    }

    /**
     * Transfers the current value from the component into the model.
     */
    private void updateModel() {
        final SettingsModelDateTime model = (SettingsModelDateTime)getModel();
        model.setLocalDate(m_dateChooser.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        final DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern(isUseMillis() ? TIME_FORMAT_WITH_MS : TIME_FORMAT_WITHOUT_MS);
        model
            .setLocalTime(LocalTime.parse(((DateEditor)m_timeSpinner.getEditor()).getTextField().getText(), formatter));
        model.setZone(ZoneId.of((String)m_zoneComboBox.getSelectedItem()));

        model.setUseDate(m_dateCheckbox.isSelected());
        model.setUseTime(m_timeCheckbox.isSelected());
        model.setUseZone(m_zoneCheckbox.isSelected());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsBeforeSave() throws InvalidSettingsException {
        updateModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs) throws NotConfigurableException {
        //  nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setEnabledComponents(final boolean enabled) {
        m_dateCheckbox.setEnabled(enabled);
        m_dateChooser.setEnabled(m_dateCheckbox.isSelected() && enabled);
        m_timeCheckbox.setEnabled(enabled);
        m_timeSpinner.setEnabled(m_timeCheckbox.isSelected() && enabled);
        m_zoneCheckbox.setEnabled(enabled);
        m_zoneComboBox.setEnabled(m_zoneCheckbox.isSelected() && enabled);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setToolTipText(final String text) {
        getComponentPanel().setToolTipText(text);
    }

    /**
     * @return if milliseconds are used
     */
    public boolean isUseMillis() {
        return m_useMillis;
    }

    /**
     * @param useMillis the useMillis to set
     */
    public void setUseMillis(final boolean useMillis) {
        m_useMillis = useMillis;
    }

    /**
     * Defines which elements of the dialog component shall be shown.
     *
     * @author Simon Schmid, KNIME.com, Konstanz, Germany
     */
    public enum DisplayOption {

            /**
             * Only the date chooser is shown.
             */
            SHOW_DATE_ONLY(true, false, false, false, false, false),
            /**
             * Only the date chooser and its checkbox is shown.
             */
            SHOW_DATE_ONLY_OPTIONAL(true, true, false, false, false, false),
            /**
             * Only the time spinner is shown.
             */
            SHOW_TIME_ONLY(false, false, true, false, false, false),
            /**
             * Only the time spinner and its checkbox is shown.
             */
            SHOW_TIME_ONLY_OPTIONAL(false, false, true, true, false, false),
            /**
             * Only the time zone combobox is shown.
             */
            SHOW_TIMEZONE_ONLY(false, false, false, false, true, false),
            /**
             * Only the time zone combobox and its checkbox is shown.
             */
            SHOW_TIMEZONE_ONLY_OPTIONAL(false, false, false, false, true, true),
            /**
             * Both date chooser and time spinner are shown.
             */
            SHOW_DATE_AND_TIME(true, false, true, false, false, false),
            /**
             * Both date chooser and time spinner are shown.
             */
            SHOW_DATE_AND_TIME_OPTIONAL(true, true, true, true, false, false),
            /**
             * Both date chooser and time spinner plus time zone are shown.
             */
            SHOW_DATE_AND_TIME_AND_TIMEZONE(true, false, true, false, true, false),
            /**
             * Both date chooser and time spinner are shown with an individual checkbox to activate them.
             */
            SHOW_DATE_AND_TIME_AND_TIMEZONE_OPTIONAL(true, true, true, true, true, true);

        private final boolean m_showDate;

        private final boolean m_showDateOptional;

        private final boolean m_showTime;

        private final boolean m_showTimeOptional;

        private final boolean m_showZone;

        private final boolean m_showZoneOptional;

        DisplayOption(final boolean showDate, final boolean showDateOptional, final boolean showTime,
            final boolean showTimeOptional, final boolean showZone, final boolean showZoneOptional) {
            m_showDate = showDate;
            m_showDateOptional = showDateOptional;
            m_showTime = showTime;
            m_showTimeOptional = showTimeOptional;
            m_showZone = showZone;
            m_showZoneOptional = showZoneOptional;
        }

        /**
         * @return the showDate
         */
        public boolean isShowDate() {
            return m_showDate;
        }

        /**
         * @return the showDateOptional
         */
        public boolean isShowDateOptional() {
            return m_showDateOptional;
        }

        /**
         * @return the showTime
         */
        public boolean isShowTime() {
            return m_showTime;
        }

        /**
         * @return the showTimeOptional
         */
        public boolean isShowTimeOptional() {
            return m_showTimeOptional;
        }

        /**
         * @return the showZone
         */
        public boolean isShowZone() {
            return m_showZone;
        }

        /**
         * @return the showZoneOptional
         */
        public boolean isShowZoneOptional() {
            return m_showZoneOptional;
        }

    }

    /**
     * Solves the problem that the text field of a spinner does not lose the focus when the buttons are pressed.
     *
     */
    private class TimeSpinnerUI extends BasicSpinnerUI {
        @Override
        protected Component createNextButton() {
            final JButton btnUp = (JButton)super.createNextButton();
            btnUp.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent ae) {
                    updateSpinnerFormat();
                }
            });
            return btnUp;
        }

        @Override
        protected Component createPreviousButton() {
            final JButton btnDown = (JButton)super.createPreviousButton();
            btnDown.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent ae) {
                    updateSpinnerFormat();
                }
            });
            return btnDown;
        }
    }
}
