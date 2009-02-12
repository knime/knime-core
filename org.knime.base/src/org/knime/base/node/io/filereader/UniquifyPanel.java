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
 * Panel for the "uniquify row IDs" option.
 * 
 * @author ohl, University of Konstanz
 */
class UniquifyPanel extends JPanel {

    private JCheckBox m_uniquifyRowIDs;

    /**
     * Constructs the panels and loads it with the settings from the passed
     * object.
     * 
     * @param settings containing the settings to show in the panel
     */
    UniquifyPanel(final FileReaderNodeSettings settings) {
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

        m_uniquifyRowIDs = new JCheckBox("generate unique row IDs");

        Box result = Box.createHorizontalBox();
        result.add(Box.createHorizontalGlue());
        result.add(m_uniquifyRowIDs);
        result.add(Box.createHorizontalStrut(5));
        result.add(Box.createHorizontalGlue());
        return result;
    }

    private Container getTextBox() {
        Box result = Box.createVerticalBox();
        result.add(Box.createVerticalGlue());
        result.add(new JLabel(
                "If you check this, the reader checks each row ID"));
        result.add(new JLabel(
                "that it reads from the file and appends a suffix"));
        result.add(new JLabel(
                "if it has read the ID before. Huge files will cause"));
        result.add(new JLabel("it to fail with an out of memory error."));
        result.add(Box.createVerticalStrut(7));
        result.add(new JLabel(
                "If this is not checked and the file reader reads"));
        result.add(new JLabel(
                "rows with identical row IDs, it will refuse to read the"));
        result.add(new JLabel("data and fail during execution."));
        result.add(Box.createVerticalStrut(10));
        result.add(new JLabel(
                "This option is ignored if the file doesn't contain"));
        result.add(new JLabel("row IDs."));

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
        boolean oldVal = settings.uniquifyRowIDs();
        settings.setUniquifyRowIDs(m_uniquifyRowIDs.isSelected());
        return oldVal != settings.uniquifyRowIDs();
    }

    /**
     * Transfers the corresponding values from the passed object into the panel.
     * 
     * @param settings object holding the values to display in the panel
     */
    private void loadSettings(final FileReaderNodeSettings settings) {
        m_uniquifyRowIDs.setSelected(settings.uniquifyRowIDs());
    }
}
