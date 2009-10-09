/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
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
