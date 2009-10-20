/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   Mar 8, 2007 (ohl): created
 */
package org.knime.base.node.io.csvwriter;

import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.node.io.csvwriter.FileWriterSettings.quoteMode;

/**
 * 
 * @author ohl, University of Konstanz
 */
class QuotePanel extends JPanel {

    private static final Dimension TEXTFIELDDIM = new Dimension(100, 25);

    private JTextField m_leftQuote;

    private JTextField m_rightQuote;

    private JTextField m_quoteReplacement;

    private JTextField m_sepReplacement;

    private JRadioButton m_ifNeeded;

    private JRadioButton m_always;

    private JRadioButton m_string;

    private JRadioButton m_never;

    /**
     * 
     */
    public QuotePanel() {

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(createQuotePanel());
        add(Box.createVerticalStrut(5));
        add(createWhenPanel());
        add(Box.createVerticalGlue());
        add(Box.createVerticalGlue());
    }

    private JPanel createQuotePanel() {

        Box textBox = Box.createHorizontalBox();
        textBox.add(new JLabel("Specify the quote patterns that will be "
                + "put around quoted data:"));
        textBox.add(Box.createHorizontalGlue()); // make it left aligned.

        Box patternBox = Box.createHorizontalBox();
        patternBox.add(Box.createHorizontalStrut(50));
        patternBox.add(new JLabel("left quote:"));
        patternBox.add(Box.createHorizontalStrut(4));
        m_leftQuote = new JTextField("", 6);
        m_leftQuote.setPreferredSize(TEXTFIELDDIM);
        m_leftQuote.setMaximumSize(TEXTFIELDDIM);
        patternBox.add(m_leftQuote);
        patternBox.add(Box.createHorizontalStrut(25));
        patternBox.add(new JLabel("right quote:"));
        patternBox.add(Box.createHorizontalStrut(4));
        m_rightQuote = new JTextField("", 6);
        m_rightQuote.setPreferredSize(TEXTFIELDDIM);
        m_rightQuote.setMaximumSize(TEXTFIELDDIM);
        patternBox.add(m_rightQuote);
        patternBox.add(Box.createHorizontalGlue());
        patternBox.add(Box.createHorizontalGlue());

        JPanel quotePanel = new JPanel();
        quotePanel.setLayout(new BoxLayout(quotePanel, BoxLayout.Y_AXIS));
        quotePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), "Quote Pattern"));
        quotePanel.add(textBox);
        quotePanel.add(patternBox);

        return quotePanel;
    }

    private JPanel createWhenPanel() {

        final int leftInset = 50;

        Box textBox = Box.createHorizontalBox();
        textBox.add(new JLabel("When should the data be put in quotes:"));
        textBox.add(Box.createHorizontalGlue()); // make it left aligned.

        m_sepReplacement = new JTextField("", 6);
        m_sepReplacement.setPreferredSize(TEXTFIELDDIM);
        m_sepReplacement.setMaximumSize(TEXTFIELDDIM);
        m_sepReplacement.setToolTipText("Use \\t or \\n "
                + "for a tab or newline character.");
        m_quoteReplacement = new JTextField("", 6);
        m_quoteReplacement.setPreferredSize(TEXTFIELDDIM);
        m_quoteReplacement.setMaximumSize(TEXTFIELDDIM);

        m_always = new JRadioButton("always");
        m_always.setToolTipText("The missing value pattern is never put"
                + " into quotes");
        m_always.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                selectionChanged();
            }
        });
        m_ifNeeded = new JRadioButton("if needed");
        m_ifNeeded.setToolTipText("If the data contains the separator or"
                + " equals the missing value pattern."
                + " Right quotes inside the data must be replaced.");
        m_ifNeeded.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                selectionChanged();
            }
        });

        m_string = new JRadioButton("non-numerical only");
        m_string.setToolTipText("Floating point and integer numbers stay"
                + " without quotes");
        m_string.setSelected(true);
        m_string.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                selectionChanged();
            }
        });

        m_never = new JRadioButton("never");
        m_never.setToolTipText("The separator must be replaced, if it occurs "
                + "in the data written.");
        m_never.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                selectionChanged();
            }
        });
        // put the radios in a group
        ButtonGroup group = new ButtonGroup();
        group.add(m_always);
        group.add(m_ifNeeded);
        group.add(m_string);
        group.add(m_never);

        Box alwaysBox = Box.createHorizontalBox();
        alwaysBox.add(Box.createHorizontalStrut(leftInset));
        alwaysBox.add(m_always);
        alwaysBox.add(Box.createHorizontalGlue());

        Box neededBox = Box.createHorizontalBox();
        neededBox.add(Box.createHorizontalStrut(leftInset));
        neededBox.add(m_ifNeeded);
        neededBox.add(Box.createHorizontalGlue());

        Box stringBox = Box.createHorizontalBox();
        stringBox.add(Box.createHorizontalStrut(leftInset));
        stringBox.add(m_string);
        stringBox.add(Box.createHorizontalGlue());

        Box neverBox = Box.createHorizontalBox();
        neverBox.add(Box.createHorizontalStrut(leftInset));
        neverBox.add(m_never);
        neverBox.add(Box.createHorizontalGlue());

        Box replQuoteBox = Box.createHorizontalBox();
        replQuoteBox.add(Box.createHorizontalStrut(leftInset + 20));
        replQuoteBox.add(new JLabel("replace right quote in data with"));
        replQuoteBox.add(Box.createHorizontalStrut(5));
        replQuoteBox.add(m_quoteReplacement);
        replQuoteBox.add(Box.createHorizontalGlue());

        Box replSepBox = Box.createHorizontalBox();
        replSepBox.add(Box.createHorizontalStrut(leftInset + 20));
        replSepBox.add(new JLabel("replace separator in data with"));
        replSepBox.add(Box.createHorizontalStrut(5));
        replSepBox.add(m_sepReplacement);
        replSepBox.add(Box.createHorizontalGlue());

        JPanel whenPanel = new JPanel();
        whenPanel.setLayout(new BoxLayout(whenPanel, BoxLayout.Y_AXIS));
        whenPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), "Quote Mode"));
        whenPanel.add(textBox);
        whenPanel.add(alwaysBox);
        whenPanel.add(neededBox);
        whenPanel.add(stringBox);
        whenPanel.add(neverBox);
        whenPanel.add(replQuoteBox);
        whenPanel.add(replSepBox);

        return whenPanel;
    }

    /**
     * Updates the values in the components from the passed settings object.
     * 
     * @param settings the object holding the values to load.
     */
    void loadValuesIntoPanel(final FileWriterSettings settings) {
        m_leftQuote.setText(settings.getQuoteBegin());
        m_rightQuote.setText(settings.getQuoteEnd());

        m_quoteReplacement.setText(settings.getQuoteReplacement());
        // support \n and \t
        String sepRepl = settings.getSeparatorReplacement();
        sepRepl = FileWriterSettings.escapeString(sepRepl);
        m_sepReplacement.setText(sepRepl);

        quoteMode mode = settings.getQuoteMode();
        m_always.setSelected(mode == quoteMode.ALWAYS);
        m_ifNeeded.setSelected(mode == quoteMode.IF_NEEDED);
        m_string.setSelected(mode == quoteMode.STRINGS);
        m_never.setSelected(mode == quoteMode.REPLACE);

        selectionChanged();

    }

    /**
     * Disables or Enables the the replacement textboxes depending on the quote
     * mode.
     */
    private void selectionChanged() {
        m_quoteReplacement.setEnabled(!m_never.isSelected());
        m_sepReplacement.setEnabled(m_never.isSelected());
    }

    /**
     * Saves the current values from the panel into the passed object.
     * 
     * @param settings the object to write the values into
     */
    void saveValuesFromPanelInto(final FileWriterSettings settings) {
        settings.setQuoteBegin(m_leftQuote.getText());
        settings.setQuoteEnd(m_rightQuote.getText());

        settings.setQuoteReplacement(m_quoteReplacement.getText());
        settings.setSeparatorReplacement(m_sepReplacement.getText());

        if (m_always.isSelected()) {
            settings.setQuoteMode(quoteMode.ALWAYS);
        } else if (m_ifNeeded.isSelected()) {
            settings.setQuoteMode(quoteMode.IF_NEEDED);
        } else if (m_string.isSelected()) {
            settings.setQuoteMode(quoteMode.STRINGS);
        } else if (m_never.isSelected()) {
            settings.setQuoteMode(quoteMode.REPLACE);
        } else {
            // one of them should have been selected...
            assert false;
            settings.setQuoteMode(quoteMode.STRINGS);
        }

    }

}
