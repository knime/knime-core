/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   Mar 19, 2007 (ohl): created
 */
package org.knime.base.node.io.filereader;

import java.awt.Container;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * 
 * @author ohl, University of Konstanz
 */
class ShortLinesPanel extends JPanel {

    private JCheckBox m_allowShortLines;

    /**
     * Constructs the panels and loads it with the settings from the passed
     * object.
     * 
     * @param settings containing the settings to show in the panel
     */
    ShortLinesPanel(final FileReaderNodeSettings settings) {
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

        m_allowShortLines = new JCheckBox("allow short lines");

        Box result = Box.createHorizontalBox();
        result.add(Box.createHorizontalGlue());
        result.add(m_allowShortLines);
        result.add(Box.createHorizontalStrut(5));
        result.add(Box.createHorizontalGlue());
        return result;
    }

    private Container getTextBox() {
        Box result = Box.createVerticalBox();
        result.add(Box.createVerticalGlue());
        result.add(new JLabel("Check this to read in lines with too few data"));
        result.add(new JLabel("elements. They are padded to full column "));
        result.add(new JLabel("count with missing values then."));
        result.add(Box.createVerticalStrut(5));
        result.add(new JLabel("For some spreadsheet applications you need to"));
        result.add(new JLabel("set this in order to read exported data in."));
        result.add(Box.createVerticalStrut(3));
        result.add(new JLabel("By default (if unchecked), files with short"));
        result.add(new JLabel("lines are rejected by the file reader."));

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
        boolean oldValue = settings.getSupportShortLines();
        settings.setSupportShortLines(m_allowShortLines.isSelected());
        return oldValue != settings.getSupportShortLines();
    }

    /**
     * Transfers the corresponding values from the passed object into the panel.
     * 
     * @param settings object holding the values to display in the panel
     */
    private void loadSettings(final FileReaderNodeSettings settings) {
        m_allowShortLines.setSelected(settings.getSupportShortLines());
    }
}
