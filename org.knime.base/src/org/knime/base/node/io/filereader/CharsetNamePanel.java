/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 *   21.08.2007 (ohl): created
 */
package org.knime.base.node.io.filereader;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.nio.charset.Charset;
import java.util.Enumeration;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Implements the tab panel for the character set settings (in the advanced
 * settings dialog).
 * 
 * @author Peter Ohl, University of Konstanz
 */
public class CharsetNamePanel extends JPanel {

    // action command for the "default" button
    private static final String DEFAULT_LABEL = "default";

    // action command for the "enter your own char set name" button
    private static final String CUSTOM_LABEL = "user defined";

    private static final Color TEXTFIELD_FG = new JTextField().getForeground();

    private ButtonGroup m_group;

    private JRadioButton m_default;

    private JRadioButton m_iso8859;

    private JRadioButton m_utf8;

    private JRadioButton m_utf16le;

    private JRadioButton m_utf16be;

    private JRadioButton m_utf16;

    private JRadioButton m_custom;

    private JTextField m_customName;

    /**
     * Creates a panel to select the character set name and initializes it from
     * the passed object.
     * 
     * @param settings the settings to initialize to panel from
     */
    CharsetNamePanel(final FileReaderSettings settings) {
        this.setSize(520, 375);
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(Box.createVerticalStrut(20));
        add(Box.createVerticalGlue());
        add(getTextBox());
        add(Box.createVerticalStrut(10));
        add(getSelectionPanel());
        add(Box.createVerticalGlue());
        add(Box.createVerticalStrut(20));

        loadSettings(settings);

    }

    private Container getSelectionPanel() {
 
        m_group = new ButtonGroup();
        /*
         * use action commands that are valid charset names (we use them later
         * directly as parameter). Except for "default" and "user defined".
         */
        m_default = new JRadioButton(DEFAULT_LABEL);
        m_default.setToolTipText("uses the default decoding set by the "
                + "operating system");
        m_default.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                buttonsChanged();
            }
        });
        m_group.add(m_default);
        Box defaultBox = Box.createHorizontalBox();
        defaultBox.add(Box.createHorizontalStrut(20));
        defaultBox.add(m_default);
        defaultBox.add(Box.createHorizontalGlue());
        
        m_iso8859 = new JRadioButton("ISO-8859-1");
        m_iso8859.setToolTipText("ISO Latin Alphabet No. 1, "
                + "a.k.a. ISO-LATIN-1");
        m_iso8859.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                buttonsChanged();
            }
        });
        m_group.add(m_iso8859);
        Box iso8859Box = Box.createHorizontalBox();
        iso8859Box.add(Box.createHorizontalStrut(20));
        iso8859Box.add(m_iso8859);
        iso8859Box.add(Box.createHorizontalGlue());
        
        m_utf8 = new JRadioButton("UTF-8");
        m_utf8.setToolTipText("Eight-bit UCS Transformation Format");
        m_utf8.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                buttonsChanged();
            }
        });
        m_group.add(m_utf8);
        Box utf8Box = Box.createHorizontalBox();
        utf8Box.add(Box.createHorizontalStrut(20));
        utf8Box.add(m_utf8);
        utf8Box.add(Box.createHorizontalGlue());
        
        m_utf16le = new JRadioButton("UTF-16LE");
        m_utf16le.setToolTipText("Sixteen-bit UCS Transformation Format, "
                + "little-endian byte order");
        m_utf16le.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                buttonsChanged();
            }
        });
        m_group.add(m_utf16le);
        Box utf16leBox = Box.createHorizontalBox();
        utf16leBox.add(Box.createHorizontalStrut(20));
        utf16leBox.add(m_utf16le);
        utf16leBox.add(Box.createHorizontalGlue());
        
        m_utf16be = new JRadioButton("UTF-16BE");
        m_utf16be.setToolTipText("Sixteen-bit UCS Transformation Format, "
                + "big-endian byte order");
        m_utf16be.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                buttonsChanged();
            }
        });
        m_group.add(m_utf16be);
        Box utf16beBox = Box.createHorizontalBox();
        utf16beBox.add(Box.createHorizontalStrut(20));
        utf16beBox.add(m_utf16be);
        utf16beBox.add(Box.createHorizontalGlue());
        
        m_utf16 = new JRadioButton("UTF-16");
        m_utf16.setToolTipText("Sixteen-bit UCS Transformation Format, "
                + "byte order identified by an optional "
                + "byte-order mark in the file");
        m_utf16.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                buttonsChanged();
            }
        });
        m_group.add(m_utf16);
        Box utf16Box = Box.createHorizontalBox();
        utf16Box.add(Box.createHorizontalStrut(20));
        utf16Box.add(m_utf16);
        utf16Box.add(Box.createHorizontalGlue());

        m_custom = new JRadioButton(CUSTOM_LABEL);
        m_custom.setToolTipText("Enter a valid charset name supported by "
                + "the Java Virtual Machine");
        m_custom.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                buttonsChanged();
            }
        });
        m_group.add(m_custom);

        m_customName = new JTextField(20);
        m_customName.setPreferredSize(new Dimension(250, 25));
        m_customName.setMaximumSize(new Dimension(250, 25));
        m_customName.getDocument().addDocumentListener(new DocumentListener() {
            public void removeUpdate(final DocumentEvent e) {
                checkCustomCharsetName();
            }

            public void insertUpdate(final DocumentEvent e) {
                checkCustomCharsetName();
            }

            public void changedUpdate(final DocumentEvent e) {
                checkCustomCharsetName();
            }
        });
        Box customBox = Box.createHorizontalBox();
        customBox.add(Box.createHorizontalStrut(20));
        customBox.add(m_custom);
        customBox.add(Box.createHorizontalStrut(5));
        customBox.add(m_customName);
        customBox.add(Box.createHorizontalGlue());

        
        JPanel result = new JPanel();
        result.setLayout(new BoxLayout(result, BoxLayout.Y_AXIS));
        result.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), "Select a character set for decoding:"));

        result.add(defaultBox);
        result.add(iso8859Box);
        result.add(utf8Box);
        result.add(utf16leBox);
        result.add(utf16beBox);
        result.add(utf16Box);
        result.add(customBox);
        return result;
    }

    /**
     * Sets the enable status according to the current selection.
     */
    private void buttonsChanged() {
        m_customName.setEnabled(m_custom.isSelected());
        checkCustomCharsetName();
    }

    /**
     * Tests the entered charset name (if the textfield is enabled), and colors
     * the textfield in case of an error.
     * 
     * @return true if the entered charset name is supported or the textfield is
     *         disabled.
     */
    private boolean checkCustomCharsetName() {
        if (!m_custom.isSelected()) {
            return true;
        }

        String cs = m_customName.getText();
        try {
            if (Charset.isSupported(cs)) {
                m_customName.setForeground(TEXTFIELD_FG);
                return true;
            } else {
                m_customName.setForeground(Color.RED);
                return false;
            }
        } catch (IllegalArgumentException iae) {
            m_customName.setForeground(Color.RED);
            return false;
        } 
    }

    private Container getTextBox() {
        Box result = Box.createHorizontalBox();
        return result;
    }

    private void loadSettings(final FileReaderSettings settings) {
        String csName = settings.getCharsetName();

        if (csName == null) {
            // the default
            m_default.setSelected(true);
        } else {
            boolean foundIt = false;
            Enumeration<AbstractButton> buttons = m_group.getElements();
            while (buttons.hasMoreElements()) {
                AbstractButton b = buttons.nextElement();
                if (csName.equals(b.getActionCommand())) {
                    foundIt = true;
                    b.setSelected(true);
                    break;
                }
            }
            if (!foundIt) {
                m_custom.setSelected(true);
                m_customName.setText(csName);
            }
        }

    }

    /**
     * Checks if the settings in the panel are good for applying them.
     * 
     * @return null if all settings are okay, or an error message if settings
     *         can't be taken over.
     * 
     */
    String checkSettings() {

        boolean foundIt = false;
        Enumeration<AbstractButton> buttons = m_group.getElements();
        while (buttons.hasMoreElements()) {
            AbstractButton b = buttons.nextElement();
            if (b.isSelected()) {
                foundIt = true;
                if (CUSTOM_LABEL.equals(b.getActionCommand())) {
                    if ((m_custom.getText() == null)
                            || (m_custom.getText().length() == 0)) {
                        return "Please enter a character set name";
                    }
                    if (!checkCustomCharsetName()) {
                        return "The entered character set name is not supported"
                                + " by this Java VM";
                    }
                }
                break;
            }
        }
        if (!foundIt) {
            return "Please select a character set";
        }
        return null;
    }

    /**
     * Writes the current settings of the panel into the passed settings object.
     * 
     * @param settings the object to write settings in
     * @return true if the new settings are different from the one passed in.
     */
    boolean overrideSettings(final FileReaderSettings settings) {
        String oldCSN = settings.getCharsetName();
        String newCSN = null;

        Enumeration<AbstractButton> buttons = m_group.getElements();
        while (buttons.hasMoreElements()) {
            AbstractButton b = buttons.nextElement();
            if (b.isSelected()) {

                newCSN = b.getActionCommand();

                if (CUSTOM_LABEL.equals(newCSN)) {
                    newCSN = m_customName.getText();
                } else if (DEFAULT_LABEL.equals(newCSN)) {
                    newCSN = null;
                }
                settings.setCharsetName(newCSN);
                break;
            }
        }

        if (oldCSN == null) {
            return (newCSN != null);
        } else {
            return !oldCSN.equals(newCSN);
        }

    }
}
