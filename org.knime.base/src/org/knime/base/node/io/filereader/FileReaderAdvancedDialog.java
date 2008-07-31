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
 * -------------------------------------------------------------------
 *
 * History
 *   08.12.2005 (ohl): created
 */
package org.knime.base.node.io.filereader;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

/**
 * Dialog for the expert settings of the file reader dialog.
 *
 * @author Peter Ohl, University of Konstanz
 */
public class FileReaderAdvancedDialog extends JDialog {

    private JPanel m_jContentPane;

    private JPanel m_controlPanel = null;

    private JButton m_okButton = null;

    private JButton m_cancelButton = null;

    private JButton m_xmlButton = null;

    private JPanel m_mainPanel = null;

    private boolean m_closedViaReadXML = false;

    private boolean m_closedViaOK = false;

    private boolean m_doAnalyze = false;

    private JTabbedPane m_jTabbedPane = null;

    // this contains the new settings. They must be correct at all time.
    // the dialog must only allow valid modifications. When the dialog closes
    // these settings are taken over (without any validation checks).
    private FileReaderNodeSettings m_settings;

    private QuotePanel m_quotePanel = null;

    private DecSepPanel m_decSepPanel;

    private IgnoreDelimsPanel m_ignoreDelimsPanel;

    private ShortLinesPanel m_shortLinesSupport;

    private UniquifyPanel m_uniquifyPanel;

    private LimitRowsPanel m_limitRowsPanel;

    private CharsetNamePanel m_charsetNamePanel;

    private MissingValuePanel m_missValPanel;

    /*
     * !!! READ THIS: Adding new advanced settings?? Don't forget to copy these
     * settings in the FileAnalyzer from the user settings into the result
     * settings.
     */

    /**
     * This is the default constructor.
     *
     * @param parent the parent frame for all dialogs this dialog opens
     * @param settings the current settings to take over and show in this dialog
     */
    FileReaderAdvancedDialog(final Frame parent,
            final FileReaderNodeSettings settings) {
        super(parent);
        m_settings = settings;
        m_closedViaReadXML = false;
        m_closedViaOK = false;
        setModal(true);

        initialize();
    }

    /**
     * This method initializes this.
     */
    private void initialize() {
        this.setSize(520, 375);
        this.setTitle("Filereader: Advanced Settings");
        this.setContentPane(getJContentPane());
    }

    /**
     * This method initializes jTabbedPane.
     *
     * @return javax.swing.JTabbedPane
     */
    private JTabbedPane getJTabbedPane() {
        if (m_jTabbedPane == null) {
            m_jTabbedPane = new JTabbedPane();
            m_jTabbedPane.addTab("Quote support", null, getQuotePanel(),
                    "Adjust settings for quote characters here");
            m_jTabbedPane.addTab("Decimal Separator", null, getDecSepPanel(),
                    "Set the decimal separator here");
            m_jTabbedPane.addTab("Ignore spaces", null,
                    getIngoreWSatEORPanel(),
                    "Ignore extra whitespaces at end of rows.");
            m_jTabbedPane.addTab("Short Lines", null, getShortLinesPanel(),
                    "Add support for incomplete rows");
            m_jTabbedPane.addTab("unique RowIDs", null, getUniquifyPanel(),
                    "Disable unique making of row IDs");
            m_jTabbedPane.addTab("Limit Rows", null, getLimitRowsPanel(),
                    "Specify the max. number of rows read");
            m_jTabbedPane.addTab("Character decoding", null,
                    getCharsetNamePanel(), "");
            m_jTabbedPane.addTab("Missing Value Pattern", null,
                    getMissValPanel(),
                    "Specify a missing value pattern for string columns");
        }
        return m_jTabbedPane;
    }

    /**
     * Overrides the settings in the passed argument with the settings from this
     * dialog.
     *
     * @param settings the settings to modify. Settings from the dialog will
     *            override settings in the passed object.
     */
    void overrideSettings(final FileReaderNodeSettings settings) {
        m_doAnalyze = false;
        m_doAnalyze |= getQuotePanel().overrideSettings(settings);
        m_doAnalyze |= getDecSepPanel().overrideSettings(settings);
        m_doAnalyze |= getIngoreWSatEORPanel().overrideSettings(settings);
        m_doAnalyze |= getShortLinesPanel().overrideSettings(settings);
        m_doAnalyze |= getUniquifyPanel().overrideSettings(settings);
        m_doAnalyze |= getLimitRowsPanel().overrideSettings(settings);
        m_doAnalyze |= getCharsetNamePanel().overrideSettings(settings);
        m_doAnalyze |= getMissValPanel().overrideSettings(settings);
    }

    /**
     * Before the dialog closes it calls this method. The dialog disappears only
     * if this method returns null. If it returns an error message it stays open
     * and displays the message.
     */
    private String checkSettings() {
        StringBuilder result = new StringBuilder();
        String panelMsg = null;

        panelMsg = getQuotePanel().checkSettings();
        if (panelMsg != null) {
            result.append('\n');
            result.append("Quote Support: ");
            result.append(panelMsg);
        }
        panelMsg = getDecSepPanel().checkSettings();
        if (panelMsg != null) {
            result.append('\n');
            result.append("Decimal Separator: ");
            result.append(panelMsg);
        }
        panelMsg = getMissValPanel().checkSettings();
        if (panelMsg != null) {
            result.append("\n");
            result.append("Missing Value: ");
            result.append(panelMsg);
        }
        panelMsg = getIngoreWSatEORPanel().checkSettings();
        if (panelMsg != null) {
            result.append('\n');
            result.append("Ignore Spaces: ");
            result.append(panelMsg);
        }
        panelMsg = getShortLinesPanel().checkSettings();
        if (panelMsg != null) {
            result.append('\n');
            result.append("Short Lines: ");
            result.append(panelMsg);
        }
        panelMsg = getUniquifyPanel().checkSettings();
        if (panelMsg != null) {
            result.append('\n');
            result.append("unique RowIDs: ");
            result.append(panelMsg);
        }
        panelMsg = getLimitRowsPanel().checkSettings();
        if (panelMsg != null) {
            result.append('\n');
            result.append("Limit Rows: ");
            result.append(panelMsg);
        }
        panelMsg = getCharsetNamePanel().checkSettings();
        if (panelMsg != null) {
            result.append('\n');
            result.append("Character decoding: ");
            result.append(panelMsg);
        }
        if (result.length() > 0) {
            return result.toString();
        } else {
            return null;
        }

    }

    /**
     * This method initializes jContentPane.
     *
     * @return javax.swing.JPanel
     */
    private JPanel getJContentPane() {
        if (m_jContentPane == null) {
            m_jContentPane = new JPanel();
            m_jContentPane.setLayout(new BoxLayout(m_jContentPane,
                    BoxLayout.Y_AXIS));
            m_jContentPane.add(getMainPanel(), null);
            m_jContentPane.add(getControlPanel(), null);
        }
        return m_jContentPane;
    }

    /**
     * This method initializes controlPanel.
     *
     * @return javax.swing.JPanel
     */
    private JPanel getControlPanel() {
        if (m_controlPanel == null) {
            m_controlPanel = new JPanel();
            m_controlPanel.setLayout(new BoxLayout(m_controlPanel,
                    BoxLayout.X_AXIS));
            m_controlPanel.add(Box.createHorizontalGlue());
            m_controlPanel.add(Box.createHorizontalGlue());
            m_controlPanel.add(getXmlButton(), null);
            m_controlPanel.add(Box.createHorizontalStrut(10));
            m_controlPanel.add(getOkButton(), null);
            m_controlPanel.add(Box.createHorizontalStrut(5));
            m_controlPanel.add(getCancelButton(), null);
            m_controlPanel.add(Box.createHorizontalGlue());

        }
        return m_controlPanel;
    }

    /**
     * This method initializes okButton.
     *
     * @return javax.swing.JButton
     */
    private JButton getOkButton() {
        if (m_okButton == null) {
            m_okButton = new JButton();
            m_okButton.setText("Ok");
            m_okButton.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    final String errMsg = checkSettings();
                    if (errMsg != null) {
                        JOptionPane.showMessageDialog(
                                FileReaderAdvancedDialog.this, errMsg,
                                "Invalid Settings", JOptionPane.ERROR_MESSAGE);
                        // dialog stays open
                        return;
                    }
                    m_closedViaOK = true;
                    setVisible(false);
                }
            });
        }
        return m_okButton;
    }

    /**
     * This method initializes cancelButton.
     *
     * @return javax.swing.JButton
     */
    private JButton getCancelButton() {
        if (m_cancelButton == null) {
            m_cancelButton = new JButton();
            m_cancelButton.setText("Cancel");
            m_cancelButton.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    setVisible(false);
                }
            });
        }
        return m_cancelButton;
    }

    /**
     * This method initializes xmlButton.
     *
     * @return javax.swing.JButton
     */
    private JButton getXmlButton() {
        if (m_xmlButton == null) {
            m_xmlButton = new JButton();
            m_xmlButton.setText("Read from XML file...");
            m_xmlButton.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    if (JOptionPane.showConfirmDialog(
                            FileReaderAdvancedDialog.this,
                            "This is for reading the old "
                                    + "XML file format!!\n"
                                    + "It will erase all current "
                                    + "settings and read new settings "
                                    + "from the file.\n" + "DO YOU WANT THIS?",
                            "Confirm override", JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE, null)
                            == JOptionPane.YES_OPTION) {
                        // we just close the dialog and expect the main dlg
                        // to read our result.
                        m_closedViaReadXML = true;
                        setVisible(false);
                        // the above will return from the modal dialog
                    }
                }
            });
        }
        return m_xmlButton;
    }

    /**
     * After the dialog closes this will return <code>true</code> if the user
     * closed the dialog with the (confirmed) "read from XML file" button).
     *
     * @return <code>true</code> if the dialog was closed via the "read from
     *         XML file" button
     */
    boolean closedViaReadXML() {
        return m_closedViaReadXML;
    }

    /**
     * After the dialog closes this will return <code>true</code> if the user
     * okayed the dialog and settings should be taken over.
     *
     * @return <code>true</code> if the dialog was closed through the OK
     *         button
     */
    boolean closedViaOk() {
        return m_closedViaOK;
    }

    /**
     * @return true if one of the settings changed that needs a re-analyze of
     *         the file to become affective
     */
    boolean needsReAnalyze() {
        return m_doAnalyze;
    }

    /**
     * This method initializes mainPanel which contains all tabs.
     *
     * @return javax.swing.JPanel
     */
    private JPanel getMainPanel() {
        if (m_mainPanel == null) {
            m_mainPanel = new JPanel();
            m_mainPanel.add(getJTabbedPane(), null);
        }
        return m_mainPanel;
    }

    /**
     * This method initializes quotePanel.
     *
     * @return javax.swing.JPanel
     */
    private QuotePanel getQuotePanel() {
        if (m_quotePanel == null) {
            m_quotePanel = new QuotePanel(m_settings);
        }
        return m_quotePanel;
    }

    /**
     * @return the panel for the decimal separator settings
     */
    private DecSepPanel getDecSepPanel() {
        if (m_decSepPanel == null) {
            m_decSepPanel = new DecSepPanel(m_settings);
        }
        return m_decSepPanel;

    }

    private MissingValuePanel getMissValPanel() {
        if (m_missValPanel == null) {
            m_missValPanel = new MissingValuePanel(m_settings);
        }
        return m_missValPanel;
    }

    private IgnoreDelimsPanel getIngoreWSatEORPanel() {
        if (m_ignoreDelimsPanel == null) {
            m_ignoreDelimsPanel = new IgnoreDelimsPanel(m_settings);
        }
        return m_ignoreDelimsPanel;
    }

    private ShortLinesPanel getShortLinesPanel() {
        if (m_shortLinesSupport == null) {
            m_shortLinesSupport = new ShortLinesPanel(m_settings);
        }
        return m_shortLinesSupport;
    }

    private UniquifyPanel getUniquifyPanel() {
        if (m_uniquifyPanel == null) {
            m_uniquifyPanel = new UniquifyPanel(m_settings);
        }
        return m_uniquifyPanel;
    }

    private LimitRowsPanel getLimitRowsPanel() {
        if (m_limitRowsPanel == null) {
            m_limitRowsPanel = new LimitRowsPanel(m_settings);
        }
        return m_limitRowsPanel;
    }

    private CharsetNamePanel getCharsetNamePanel() {
        if (m_charsetNamePanel == null) {
            m_charsetNamePanel = new CharsetNamePanel(m_settings);
        }
        return m_charsetNamePanel;
    }

    /**
     * The main.
     *
     * @param args the args of main.
     */
    public static void main(final String[] args) {
        FileReaderAdvancedDialog dlg =
                new FileReaderAdvancedDialog(null,
                        new FileReaderNodeSettings());
        dlg.setVisible(true);
        dlg.dispose();
    }
}
