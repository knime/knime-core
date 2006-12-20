/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 *   04.05.2006 (ohl): created
 */
package org.knime.base.node.io.filereader;

import java.awt.Container;
import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Implements the tab panel for the decimal separator settings (in the advaced
 * settings dialog).
 * 
 * @author Peter Ohl, University of Konstanz
 */
public class DecSepPanel extends JPanel {

    private JTextField m_decSep;

    /**
     * Creates a panel to set the decimal separator and initializes it from the
     * passed object.
     * 
     * @param settings the settings to initialize to panel from
     */
    DecSepPanel(final FileReaderSettings settings) {
        this.setSize(520, 375);
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(Box.createVerticalStrut(20));
        add(Box.createVerticalGlue());
        add(getTextBox());
        add(Box.createVerticalStrut(10));
        add(getPanel());
        add(Box.createVerticalGlue());
        add(Box.createVerticalStrut(20));

        loadSettings(settings);

    }

    private Container getPanel() {
        Box result = Box.createHorizontalBox();
        result.add(Box.createHorizontalGlue());
        result.add(new JLabel("Decimal separator character:"));
        result.add(Box.createHorizontalStrut(5));
        m_decSep = new JTextField(1);
        m_decSep.setPreferredSize(new Dimension(35, 25));
        m_decSep.setMinimumSize(new Dimension(35, 25));
        m_decSep.setMaximumSize(new Dimension(35, 25));
        result.add(m_decSep);
        result.add(Box.createHorizontalGlue());
        return result;
    }

    private Container getTextBox() {
        Box result = Box.createHorizontalBox();
        result.add(Box.createHorizontalGlue());
        result.add(new JLabel("Enter a new separator "
                + "(used for double columns only)"));
        result.add(Box.createHorizontalGlue());
        return result;
    }

    private void loadSettings(final FileReaderSettings settings) {
        m_decSep.setText("" + settings.getDecimalSeparator());
    }

    /**
     * Writes the current settings of the panel into the passed settings object.
     * 
     * @param settings the object to write settings in
     */
    void overrideSettings(final FileReaderSettings settings) {
        settings.setDecimalSeparator(m_decSep.getText().charAt(0));
    }
}
