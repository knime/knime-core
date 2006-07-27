/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any quesions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *   08.12.2005 (ohl): created
 */
package de.unikn.knime.base.node.io.filereader;

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

    private QuotePanel m_quotePanel = null;

    private boolean m_closedViaReadXML = false;

    private boolean m_closedViaOK = false;

    private JTabbedPane m_jTabbedPane = null;

    // this contains the new settings. They must be correct at all time.
    // the dialog must only allow valid modifications. When the dialog closes
    // these settings are taken over (without any validation checks).
    private FileReaderNodeSettings m_settings;

    private DecSepPanel m_decSepPanel;

    private IgnoreDelimsPanel m_ignoreDelimsPanel;

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
     * Overrides the settings in the passed argument with the settings from this
     * dialog.
     * 
     * @param settings the settings to modify. Settings from the dialog will
     *            override settings in the passed object.
     */
    void overrideSettings(final FileReaderNodeSettings settings) {
        getQuotePanel().overrideSettings(settings);
        getDecSepPanel().overrideSettings(settings);
        getIngoreWSatEORPanel().overrideSettings(settings);
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
                                    + "settings and read new setings "
                                    + "from the file.\n" + "DO YOU WANT THIS?",
                            "Override confirm", JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE, null) == JOptionPane.YES_OPTION) {
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

    private IgnoreDelimsPanel getIngoreWSatEORPanel() {
        if (m_ignoreDelimsPanel == null) {
            m_ignoreDelimsPanel = new IgnoreDelimsPanel(m_settings);
        }
        return m_ignoreDelimsPanel;
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
            m_jTabbedPane.addTab("Ingore spaces", null,
                    getIngoreWSatEORPanel(),
                    "Ignore extra whitespaces at end of rows.");
        }
        return m_jTabbedPane;
    }

    /**
     * The main.
     * 
     * @param args the args of main.
     */
    public static void main(final String[] args) {
        FileReaderAdvancedDialog dlg = new FileReaderAdvancedDialog(null,
                new FileReaderNodeSettings());
        dlg.setVisible(true);
        dlg.dispose();
    }
}
