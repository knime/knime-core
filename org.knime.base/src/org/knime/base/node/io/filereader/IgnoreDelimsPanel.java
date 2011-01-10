/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 *   19.07.2006 (ohl): created
 */
package org.knime.base.node.io.filereader;

import java.awt.Container;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.knime.core.util.tokenizer.Delimiter;

/**
 * Dialog panel for the expert dialog of the filereader. Panel for the flag
 * "ignore extra delimiters at the end of the rows".
 *
 * @author Peter Ohl, University of Konstanz
 *
 */
public class IgnoreDelimsPanel extends JPanel {

    private JCheckBox m_ignoreThem;

    /**
     * Constructs the panels and loads it with the settings from the passed
     * object.
     *
     * @param settings containing the settings to show in the panel
     */
    IgnoreDelimsPanel(final FileReaderNodeSettings settings) {
        initialize();
        loadSettings(settings);
    }

    private void initialize() {
        this.setSize(520, 375);
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(Box.createVerticalStrut(20));
        add(Box.createVerticalGlue());
        add(getTextBox());
        add(Box.createVerticalStrut(10));
        add(getPanel());
        add(Box.createVerticalGlue());
        add(Box.createVerticalStrut(20));
    }

    private Container getPanel() {

        m_ignoreThem = new JCheckBox("Ignore extra delimiters at end of rows");
        Box result = Box.createHorizontalBox();
        result.add(Box.createHorizontalGlue());
        result.add(m_ignoreThem);
        result.add(Box.createHorizontalStrut(5));
        result.add(Box.createHorizontalGlue());
        return result;
    }

    private Container getTextBox() {
        Box result = Box.createVerticalBox();
        result.add(Box.createVerticalGlue());
        result.add(new JLabel("Check this to ignore additional whitespaces"));
        result.add(new JLabel("at the end of each row. Otherwise a missing"));
        result.add(new JLabel("cell will be introduced."));
        result.add(Box.createVerticalStrut(5));
        result.add(new JLabel("Note: With this set, missing values at the"));
        result.add(new JLabel("\t\tend of a row must be quoted (e.g. \"\")."));
        result.add(Box.createVerticalStrut(3));
        result.add(new JLabel("This setting is ignored if a delimiter other"));
        result.add(new JLabel("than space or tab is selected."));

        result.add(Box.createVerticalGlue());
        return result;
    }

    /**
     * Checks the current values in the panel.
     *
     * @return null, if settings are okay and can be applied. An error message
     *         if not.
     */
    String checkSettings() {
        return null;
    }

    /**
     * Transfers the current settings from the panel in the passed object.
     * Overwriting the corresponding values in the object.
     *
     * @param settings the settings object to fill in the currently set values
     * @return true if the new settings are different from the one passed in.
     */
    boolean overrideSettings(final FileReaderNodeSettings settings) {

        boolean ignoreEm = m_ignoreThem.isSelected();

        if (ignoreEm != settings.ignoreDelimsAtEORUserValue()) {
            // set the user set value - only if he changed it.

            settings.setIgnoreDelimsAtEndOfRowUserValue(ignoreEm);

            // and set he actual flag, if the delimiter is a whitespace (THIS
            // DEPENDS on delimiters are being set before this is called!!!!)
            // (!)
            for (Delimiter delim : settings.getAllDelimiters()) {
                String delStr = delim.getDelimiter();
                if (!settings.isRowDelimiter(delStr)) {
                    if (delStr.equals(" ") || delStr.equals("\t")) {
                        settings.setIgnoreEmptyTokensAtEndOfRow(ignoreEm);
                        break;
                    }

                }

            }

            // also fix the delimiter settings
            // I guess that is what they would expect...?
            settings.setDelimiterUserSet(true);

            // need to re-analyze file with settings changed
            return true;
        }

        return false; // no need to re-analyze, no settings changed here.

    }

    /**
     * Transfers the corresponding values from the passed object into the panel.
     *
     * @param settings object holding the values to display in the panel
     */
    private void loadSettings(final FileReaderNodeSettings settings) {

        if (settings.ignoreDelimsAtEORUserSet()) {
            m_ignoreThem.setSelected(settings.ignoreDelimsAtEORUserValue());
        } else {
            m_ignoreThem.setSelected(settings.ignoreEmptyTokensAtEndOfRow());
        }
    }
}
