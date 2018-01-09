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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *   08.05.2014 (thor): created
 */
package org.knime.base.node.io.database.connection.util;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;
import java.util.TimeZone;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.database.DatabaseConnectionSettings;

/**
 * A panel for setting the timzeone correction for time and date fields. The panel has a {@link GridBagLayout} and uses
 * the protected {@link #m_c} {@link GridBagConstraints} for layouting. You should re-use the constraints when extending
 * this panel.
 *
 * @param <T> a subclass of {@link DatabaseConnectionSettings}
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 * @since 2.10
 */
public class DBTimezonePanel<T extends DatabaseConnectionSettings> extends JPanel {
    private static final long serialVersionUID = 1659548450496165562L;

    /**
     * The settings object from which and to which the panel read/writes the settings.
     */
    protected final T m_settings;

    /**
     * A drop-down list with all available timezones.
     */
    protected final JComboBox<String> m_timezone = new JComboBox<>();

    /**
     * Radio button for no timezone correction.
     */
    protected final JRadioButton m_noTZCorrection = new JRadioButton("No correction (use UTC)");

    /**
     * Radio button for using the current timezone.
     */
    protected final JRadioButton m_useCurrentTZ = new JRadioButton("Use local timezone");

    /**
     * Radio button for using a specific timezone.
     */
    protected final JRadioButton m_useSelectedTZ = new JRadioButton("Use selected timezone  ");

    /**
     * Gridbag constraints object used for layouting the panel.
     */
    protected final GridBagConstraints m_c = new GridBagConstraints();

    /**
     * Creates a new panel.
     *
     * @param settings the settings object the panel should use
     */
    public DBTimezonePanel(final T settings) {
        super(new GridBagLayout());
        m_settings = settings;

        m_c.gridx = 0;
        m_c.gridy = 0;
        m_c.insets = new Insets(2, 2, 2, 2);
        m_c.anchor = GridBagConstraints.WEST;

        String[] timezones = TimeZone.getAvailableIDs();
        Arrays.sort(timezones);
        for (String tz : timezones) {
            m_timezone.addItem(tz);
        }

        m_timezone.setSelectedItem(TimeZone.getDefault().getID());
        m_timezone.setEnabled(false);
        m_useSelectedTZ.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                m_timezone.setEnabled(m_useSelectedTZ.isSelected());
            }
        });

        ButtonGroup bg = new ButtonGroup();
        bg.add(m_noTZCorrection);
        bg.add(m_useCurrentTZ);
        bg.add(m_useSelectedTZ);

        add(m_noTZCorrection, m_c);

        m_c.gridx = 1;
        m_c.weightx = 1;
        m_c.fill = GridBagConstraints.HORIZONTAL;
        add(m_useCurrentTZ, m_c);

        m_c.gridx = 0;
        m_c.gridy++;
        m_c.weightx = 0;
        m_c.fill = GridBagConstraints.NONE;
        add(m_useSelectedTZ, m_c);
        m_c.gridx = 1;
        add(m_timezone, m_c);

        setBorder(BorderFactory.createTitledBorder("Timezone correction"));
    }

    /**
     * Loads the settings into the dialog components.
     *
     * @param specs the incoming port specs.
     * @throws NotConfigurableException if the dialog should not open because necessary information is missing
     */
    public void loadSettings(final PortObjectSpec[] specs) throws NotConfigurableException {
        if ((m_settings.getTimezone() == null) || m_settings.getTimezone().isEmpty()
            || "none".equals(m_settings.getTimezone())) {
            m_noTZCorrection.doClick();
        } else if ("current".equals(m_settings.getTimezone())) {
            m_useCurrentTZ.doClick();
        } else {
            m_useSelectedTZ.doClick();
            m_timezone.setSelectedItem(m_settings.getTimezone());
        }
    }

    /**
     * Saves the component values into the settings object.
     *
     * @throws InvalidSettingsException if some settings are invalid
     */
    public void saveSettings() throws InvalidSettingsException {
        if (m_useSelectedTZ.isSelected() && (m_timezone.getSelectedItem() != null)) {
            m_settings.setTimezone(m_timezone.getSelectedItem().toString());
        } else if (m_useCurrentTZ.isSelected()) {
            m_settings.setTimezone("current");
        } else {
            m_settings.setTimezone("none");
        }
    }

}
