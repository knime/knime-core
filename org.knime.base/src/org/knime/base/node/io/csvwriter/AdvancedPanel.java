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
 *   Mar 9, 2007 (ohl): created
 */
package org.knime.base.node.io.csvwriter;

import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * 
 * @author ohl, University of Konstanz
 */
class AdvancedPanel extends JPanel {

    private static final Dimension TEXTFIELDDIM = new Dimension(75, 25);

    private JTextField m_colSeparator = new JTextField("");

    private JTextField m_missValuePattern = new JTextField("");

    /**
     * 
     */
    public AdvancedPanel() {

        JPanel missPanel = new JPanel();
        missPanel.setLayout(new BoxLayout(missPanel, BoxLayout.X_AXIS));
        missPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), "Missing Value Pattern"));
        missPanel.add(new JLabel("Pattern written out for missing values:"));
        missPanel.add(Box.createHorizontalStrut(5));
        missPanel.add(m_missValuePattern);
        m_missValuePattern.setPreferredSize(TEXTFIELDDIM);
        m_missValuePattern.setMaximumSize(TEXTFIELDDIM);
        missPanel.add(Box.createHorizontalGlue());

        JPanel colSepPanel = new JPanel();
        colSepPanel.setLayout(new BoxLayout(colSepPanel, BoxLayout.X_AXIS));
        colSepPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), "Data Separator"));
        colSepPanel.add(new JLabel("Pattern written out between data values:"));
        colSepPanel.add(Box.createHorizontalStrut(5));
        colSepPanel.add(m_colSeparator);
        m_colSeparator.setPreferredSize(TEXTFIELDDIM);
        m_colSeparator.setMaximumSize(TEXTFIELDDIM);
        m_colSeparator.setToolTipText("Use \\t or \\n "
                + "for a tab or newline character.");
        colSepPanel.add(Box.createHorizontalGlue());

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(Box.createVerticalGlue());
        add(colSepPanel);
        add(Box.createVerticalStrut(10));
        add(missPanel);
        add(Box.createVerticalGlue());
        add(Box.createVerticalGlue());

    }

    /**
     * Reads new values from the specified object and puts them into the panel's
     * components.
     * 
     * @param settings object holding the new values to show.
     */
    void loadValuesIntoPanel(final FileWriterSettings settings) {

        // support \t and \n
        String colSep = settings.getColSeparator();
        colSep = FileWriterSettings.escapeString(colSep);

        m_colSeparator.setText(colSep);
        m_missValuePattern.setText(settings.getMissValuePattern());

    }

    /**
     * Writes the current values from the components into the settings object.
     * 
     * @param settings the object to write the values into
     */
    void saveValuesFromPanelInto(final FileWriterSettings settings) {

        // support \t and \n
        String colSep = m_colSeparator.getText();
        colSep = FileWriterSettings.unescapeString(colSep);

        settings.setColSeparator(colSep);
        settings.setMissValuePattern(m_missValuePattern.getText());

    }

}
