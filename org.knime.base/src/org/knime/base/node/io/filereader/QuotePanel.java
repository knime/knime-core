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
 *   08.12.2005 (ohl): created
 */
package org.knime.base.node.io.filereader;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.knime.core.util.tokenizer.Quote;

/**
 * Dialog for the expert settings of the file reader dialog.
 *
 * @author Peter Ohl, University of Konstanz
 */
public class QuotePanel extends JPanel {

    private JList m_currQuotes = null;

    private JPanel m_qCtrlPanel = null;

    private JPanel m_qEditPanel = null;

    private JTextField m_qEditField = null;

    private JButton m_addButton = null;

    private JButton m_removeButton = null;

    private JScrollPane m_currQuotesScroller;

    private JCheckBox m_qEscBox = null;

    private JPanel m_qEscPanel = null;

    private JPanel m_qListPanel = null;

    private JLabel m_qListHeader = null;

    private JPanel m_qEditLabelPanel = null;

    private JPanel m_quoteEditPanel;

    private JPanel m_textPanel;

    private JPanel m_qErrorPanel;

    private JLabel m_qErrorLabel;

    private FileReaderNodeSettings m_frSettings;

    /**
     * This is the default constructor.
     *
     * @param settings an object containing the already defined quotes
     */
    QuotePanel(final FileReaderNodeSettings settings) {
        initialize();
        loadSettings(settings);

        // we keep a settings object storing the quote objects in
        m_frSettings = new FileReaderNodeSettings();
        overrideSettings(m_frSettings);
    }

    /**
     * This method initializes this.
     */
    private void initialize() {
        this.setSize(520, 375);
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(getTextPanel());
        add(Box.createVerticalStrut(30));
        add(getQuoteEditPanel());
        add(Box.createVerticalStrut(15));
        add(getQErrorPanel());

    }

    /**
     * @return an instance of the quote edit panel (without the text)
     */
    private JPanel getQuoteEditPanel() {
        if (m_quoteEditPanel == null) {
            m_quoteEditPanel = new JPanel();
            m_quoteEditPanel.setLayout(
                    new BoxLayout(m_quoteEditPanel, BoxLayout.X_AXIS));
            m_quoteEditPanel.add(getQEditPanel(), null);
            m_quoteEditPanel.add(getQCtrlPanel(), null);
            m_quoteEditPanel.add(getQListPanel(), null);

        }
        return m_quoteEditPanel;

    }

    private JPanel getTextPanel() {
        if (m_textPanel == null) {
            m_textPanel = new JPanel();
            m_textPanel.setLayout(new BoxLayout(m_textPanel, BoxLayout.X_AXIS));
            Box textBox = Box.createVerticalBox();
            textBox.add(new JLabel("Define quote characters here."));
            textBox.add(new JLabel("Quotes can be multi-character "
                    + "patterns (for example: <quote> )."));
            textBox.add(new JLabel("Escape character (if checked) is always"
                    + " the backslash ('\\')."));
            m_textPanel.add(Box.createHorizontalGlue());
            m_textPanel.add(textBox);
            m_textPanel.add(Box.createHorizontalGlue());
        }
        return m_textPanel;
    }

    /**
     * This method initializes currQuotes.
     *
     * @return javax.swing.JList
     */
    private JScrollPane getCurrQuotes() {
        if (m_currQuotes == null) {
            m_currQuotes = new JList();
            m_currQuotes.setPrototypeCellValue(" \" (esc:'\\') ");
            m_currQuotesScroller = new JScrollPane(m_currQuotes);

        }
        return m_currQuotesScroller;
    }

    /**
     * This method initializes qCtrlPanel.
     *
     * @return javax.swing.JPanel
     */
    private JPanel getQCtrlPanel() {
        if (m_qCtrlPanel == null) {
            m_qCtrlPanel = new JPanel();
            m_qCtrlPanel.setLayout(
                    new BoxLayout(m_qCtrlPanel, BoxLayout.Y_AXIS));
            JPanel buttonBox = new JPanel(new GridLayout(0, 1));
//            getAddButton().setPreferredSize(getRemoveButton().getSize());
//            getAddButton().setMinimumSize(getRemoveButton().getSize());
            buttonBox.add(getAddButton());
            buttonBox.add(getRemoveButton());
            buttonBox.setMaximumSize(new Dimension(120, 50));
            buttonBox.setMinimumSize(new Dimension(120, 50));
            m_qCtrlPanel.add(Box.createVerticalGlue());
            m_qCtrlPanel.add(buttonBox);
            m_qCtrlPanel.add(Box.createVerticalGlue());
        }
        return m_qCtrlPanel;
    }

    /**
     * This method initializes qEditPanel.
     *
     * @return javax.swing.JPanel
     */
    private JPanel getQEditPanel() {
        if (m_qEditPanel == null) {
            m_qEditPanel = new JPanel();
            m_qEditPanel.setBorder(
                    BorderFactory.createTitledBorder(BorderFactory
                    .createEtchedBorder(), "Enter a new quote character:"));
            m_qEditPanel.setLayout(
                    new BoxLayout(m_qEditPanel, BoxLayout.Y_AXIS));
            m_qEditPanel.add(getQEditLabelPanel(), null);
            m_qEditPanel.add(getQEditField(), null);
            m_qEditPanel.add(getQEscPanel(), null);
            m_qEditField.add(Box.createVerticalGlue());
        }
        return m_qEditPanel;
    }

    /**
     * @return the panel containing the error message display
     */
    private JPanel getQErrorPanel() {
        if (m_qErrorPanel == null) {
            m_qErrorPanel = new JPanel();
            m_qErrorPanel.setLayout(
                    new BoxLayout(m_qErrorPanel, BoxLayout.X_AXIS));
            m_qErrorPanel.add(Box.createHorizontalGlue());
            m_qErrorPanel.add(Box.createVerticalStrut(30));
            m_qErrorLabel = new JLabel("");
            m_qErrorLabel.setForeground(Color.RED);
            JScrollPane scpn = new JScrollPane(m_qErrorLabel);
            scpn.setBorder(BorderFactory.createEmptyBorder());
            scpn.setPreferredSize(new Dimension(450, 50));
            m_qErrorPanel.add(scpn);
            m_qErrorPanel.add(Box.createHorizontalGlue());
        }
        return m_qErrorPanel;
    }

    /**
     * Sets the passed message in the error label. It will be displayed in red.
     *
     * @param errMsg the new error message to display
     */
    private void setErrorText(final String errMsg) {
        m_qErrorLabel.setText(errMsg);
    }

    /**
     * Clears any previously displayed error message.
     */
    private void clearErrorText() {
        m_qErrorLabel.setText("");
    }

    /**
     * This method initializes qEditField.
     *
     * @return javax.swing.JTextField
     */
    private JTextField getQEditField() {
        if (m_qEditField == null) {
            m_qEditField = new JTextField();
            m_qEditField.getDocument().addDocumentListener(
                    new DocumentListener() {
                        public void changedUpdate(final DocumentEvent e) {
                            clearErrorText();
                        }

                        public void removeUpdate(final DocumentEvent e) {
                            clearErrorText();
                        }

                        public void insertUpdate(final DocumentEvent e) {
                            clearErrorText();
                        }

                    });
            m_qEditField.setMinimumSize(new Dimension(100, 25));
            m_qEditField.setMaximumSize(new Dimension(100, 25));
            m_qEditField.setPreferredSize(new Dimension(100, 25));
        }
        return m_qEditField;
    }

    /**
     * This method initializes addButton.
     *
     * @return javax.swing.JButton
     */
    private JButton getAddButton() {
        if (m_addButton == null) {
            m_addButton = new JButton();
            m_addButton.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    addNewQuote();
                }
            });
            m_addButton.setText("Add >");
        }
        return m_addButton;
    }

    /**
     * This method initializes removeButton.
     *
     * @return javax.swing.JButton
     */
    private JButton getRemoveButton() {
        if (m_removeButton == null) {
            m_removeButton = new JButton();
            m_removeButton.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    removeSelectedQuote();
                }
            });
            m_removeButton.setText("< Remove");
        }
        return m_removeButton;
    }

    /**
     * Called when user presses the "Add"-button.
     */
    private void addNewQuote() {

        String newQuote = getQEditField().getText().trim();
        if (newQuote.length() < 1) {
            return;
        }

        try {

            if (m_qEscBox.isSelected()) {
                m_frSettings.addQuotePattern(newQuote, newQuote, '\\');
            } else {
                m_frSettings.addQuotePattern(newQuote, newQuote);
            }
            // transfer new quotes into the panel
            loadSettings(m_frSettings);
            // clean edit field
            m_qEditField.setText("");

        } catch (IllegalArgumentException iae) {
            setErrorText("Not added! (" + iae.getMessage() + ")");
        }

    }

    private void removeSelectedQuote() {
        String selQuote = (String)m_currQuotes.getSelectedValue();
        if (selQuote == null) {
            return;
        }
        String quote = getQuotePattern(selQuote);
        m_frSettings.removeQuotePattern(quote, quote);

        // display the new quotes list
        loadSettings(m_frSettings);

        // put the deleted quote into the editfield
        m_qEscBox.setSelected(getEscCharacter(selQuote) != -1);
        getQEditField().setText(getQuotePattern(selQuote));
    }

    /**
     * Currently, defined quotes are displayed surrounded by spaces, and the esc
     * character added in parantheses. (For example: double quotes will be
     * displayed like ' " (esc:'\')' (without the surounding ticks)).
     *
     * @param listEntry a String from the list model of the current quote list.
     * @return the quote characters contained in the passed string
     * @see #getListEntry(Quote)
     */
    private String getQuotePattern(final String listEntry) {
        return listEntry.trim().split(" ")[0];
    }

    private int getEscCharacter(final String listEntry) {
        int escSequence = listEntry.lastIndexOf("(esc: '");
        if ((escSequence < 0) || (listEntry.length() < escSequence + 8)) {
            return -1;
        }

        return listEntry.charAt(escSequence + 7);
    }

    /**
     * Creates the string to display in the JList for current quotes. Must work
     * together with the above method that extracts the quote pattern from the
     * string.
     *
     * @param quote the quote to display in the JList.
     * @return a string to display in a JList for this quote.
     * @see #getQuotePattern(String)
     * @see #getEscCharacter(String)
     */
    private String getListEntry(final Quote quote) {
        String result = " " + quote.getLeft() + " ";
        if (!quote.getLeft().equals(quote.getRight())) {
            result = result + " " + quote.getRight() + " ";
        }
        if (quote.hasEscapeChar()) {
            result = result + " (esc: '" + quote.getEscape() + "')";
        }
        return result;
    }

    /**
     * This method initializes qEscBox.
     *
     * @return javax.swing.JCheckBox
     */
    private JCheckBox getQEscBox() {
        if (m_qEscBox == null) {
            m_qEscBox = new JCheckBox();
            m_qEscBox.setText("support esc character ('\\')");
        }
        return m_qEscBox;
    }

    /**
     * This method initializes qEscPanel.
     *
     * @return javax.swing.JPanel
     */
    private JPanel getQEscPanel() {
        if (m_qEscPanel == null) {
            m_qEscPanel = new JPanel();
            m_qEscPanel.add(getQEscBox(), null);
        }
        return m_qEscPanel;
    }

    /**
     * This method initializes qListPanel.
     *
     * @return javax.swing.JPanel
     */
    private JPanel getQListPanel() {
        if (m_qListPanel == null) {
            m_qListHeader = new JLabel();
            m_qListHeader.setText("currently set quotes");
            m_qListPanel = new JPanel();
            m_qListPanel.setLayout(
                    new BoxLayout(m_qListPanel, BoxLayout.Y_AXIS));
            m_qListPanel.add(m_qListHeader, null);
            m_qListPanel.add(getCurrQuotes(), null);
        }
        return m_qListPanel;
    }

    /**
     * This method initializes qEditLabelPanel.
     *
     * @return javax.swing.JPanel
     */
    private JPanel getQEditLabelPanel() {
        if (m_qEditLabelPanel == null) {
            m_qEditLabelPanel = new JPanel();
            m_qEditLabelPanel.setLayout(new BoxLayout(m_qEditLabelPanel,
                    BoxLayout.X_AXIS));
            m_qEditLabelPanel.add(Box.createHorizontalGlue());
        }
        return m_qEditLabelPanel;
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
     * Deletes all quotes defined in the passed object, reads the currently
     * listed quotes from the JList and adds them to the settings object.
     *
     * @param settings the settings object to replace the quotes in with the
     *            quotes currently defined in the panel
     * @return true if the new settings are different from the one passed in.
     */
    boolean overrideSettings(final FileReaderNodeSettings settings) {

        // save'm to decide whether the new settings are different
        Vector<Quote> oldQuotes = settings.getAllQuotes();

        settings.removeAllQuotes();
        for (int i = 0; i < m_currQuotes.getModel().getSize(); i++) {
            String lEntry = (String)m_currQuotes.getModel().getElementAt(i);
            String quotes = getQuotePattern(lEntry);
            int escChar = getEscCharacter(lEntry);
            if (escChar != -1) {
                settings.addQuotePattern(quotes, quotes, (char)escChar);
            } else {
                settings.addQuotePattern(quotes, quotes);
            }
        }
        // fix the settings.
        settings.setQuoteUserSet(true);

        // decide whether we need to re-analyze the file (whether we have
        // new quote settings)
        Vector<Quote> newQuotes = settings.getAllQuotes();
        if (newQuotes.size() != oldQuotes.size()) {
            // need to re-analyze with different quotes.
            return true;
        }
        for (Quote q : newQuotes) {
            if (!oldQuotes.contains(q)) {
                return true;
            }
        }
        return false;


    }

    /**
     * Transfers the values from the specified object into the components of the
     * panel.
     *
     * @param settings the object containing the settings to display
     */
    private void loadSettings(final FileReaderNodeSettings settings) {

        // take over the quotes defined in the settings object into our JList
        final Vector<String> newModel = new Vector<String>();
        for (Quote q : settings.getAllQuotes()) {
            newModel.add(getListEntry(q));
        }
        m_currQuotes.setModel(new AbstractListModel() {
            public int getSize() {
                return newModel.size();
            }

            public Object getElementAt(final int index) {
                return newModel.get(index);
            }
        });

        // also clear the edit field
        getQEditField().setText("");

        clearErrorText();
    }
}
