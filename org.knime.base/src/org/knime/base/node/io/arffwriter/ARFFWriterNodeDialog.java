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
 *   11.02.2005 (ohl): created
 */
package org.knime.base.node.io.arffwriter;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import org.knime.base.node.io.arffreader.ARFFReaderNodeModel;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ConvenientComboBoxRenderer;

/**
 * Contains the dialog for the ARFF file writer.
 * 
 * @author Peter Ohl, University of Konstanz
 */
public class ARFFWriterNodeDialog extends NodeDialogPane implements
        ItemListener {

    private static final int VERT_SPACE = 5;

    private static final int HORIZ_SPACE = 5;

    private static final int COMPONENT_HEIGHT = 25;

    private static final int TEXTFIELD_WIDTH = 350;

    private JComboBox m_url;

    private JLabel m_urlError;

    private static final int TABWIDTH = 600;

    /**
     * Creates a new ARFF file reader dialog.
     */
    public ARFFWriterNodeDialog() {
        super();

        addTab("Specify ARFF file", createFilePanel());
    }

    private JPanel createFilePanel() {
        int sumOfCompHeigth = 0;

        JPanel filePanel = new JPanel();
        filePanel.setLayout(new BoxLayout(filePanel, BoxLayout.Y_AXIS));
        filePanel.add(Box.createGlue());

        JPanel innerBox = new JPanel();
        innerBox.setLayout(new BoxLayout(innerBox, BoxLayout.Y_AXIS));
        innerBox.add(Box.createVerticalStrut(COMPONENT_HEIGHT + VERT_SPACE));
        sumOfCompHeigth += COMPONENT_HEIGHT + VERT_SPACE;

        JPanel fPanel = new JPanel();
        fPanel.setLayout(new BoxLayout(fPanel, BoxLayout.X_AXIS));
        fPanel
                .setBorder(BorderFactory.createTitledBorder(BorderFactory
                        .createEtchedBorder(),
                        "Enter location to write ARFF file to:"));
        Container fileBox = Box.createHorizontalBox();
        fileBox.add(Box.createHorizontalStrut(20));
        fileBox.add(new JLabel("valid location:"));
        fileBox.add(Box.createHorizontalStrut(HORIZ_SPACE));

        sumOfCompHeigth += COMPONENT_HEIGHT;

        m_url = new JComboBox();
        m_url.setMaximumSize(new Dimension(TEXTFIELD_WIDTH, COMPONENT_HEIGHT));
        m_url.setMinimumSize(new Dimension(TEXTFIELD_WIDTH, COMPONENT_HEIGHT));
        m_url
                .setPreferredSize(new Dimension(TEXTFIELD_WIDTH,
                        COMPONENT_HEIGHT));
        m_url.setEditable(true);
        m_url.setRenderer(new ConvenientComboBoxRenderer());
        fileBox.add(m_url);
        fileBox.add(Box.createHorizontalStrut(HORIZ_SPACE));

        JButton browse = new JButton("Browse...");
        browse.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                // sets the path in the file text field.
                popupFileChooser();
            }
        });
        fileBox.add(browse);
        fileBox.add(Box.createHorizontalStrut(VERT_SPACE));
        sumOfCompHeigth += COMPONENT_HEIGHT + VERT_SPACE;

        fPanel.add(fileBox);
        innerBox.add(fPanel);
        innerBox.add(Box.createVerticalStrut(VERT_SPACE));
        sumOfCompHeigth += VERT_SPACE;

        Container errBox = Box.createHorizontalBox();
        errBox.add(Box.createHorizontalGlue());
        // force certain height even if textfield is empty
        errBox.add(Box.createVerticalStrut(COMPONENT_HEIGHT));
        m_urlError = new JLabel("");
        m_urlError.setForeground(Color.RED);
        errBox.add(m_urlError);
        errBox.add(Box.createHorizontalGlue());
        errBox.add(Box.createHorizontalGlue());
        innerBox.add(errBox);
        innerBox.add(Box.createVerticalStrut(VERT_SPACE));
        sumOfCompHeigth += COMPONENT_HEIGHT + VERT_SPACE;

        innerBox.add(Box.createVerticalStrut(VERT_SPACE));
        sumOfCompHeigth += COMPONENT_HEIGHT + (2 * VERT_SPACE);

        innerBox.setMaximumSize(new Dimension(TABWIDTH, sumOfCompHeigth));

        filePanel.add(innerBox);
        filePanel.add(Box.createGlue());

        // set a filter to the filename that fills the error text label
        Component editor = m_url.getEditor().getEditorComponent();
        if (editor instanceof JTextComponent) {
            Document d = ((JTextComponent)editor).getDocument();
            d.addDocumentListener(new DocumentListener() {
                public void changedUpdate(final DocumentEvent e) {
                    updateFileError();
                }

                public void insertUpdate(final DocumentEvent e) {
                    updateFileError();
                }

                public void removeUpdate(final DocumentEvent e) {
                    updateFileError();
                }
            });
        }
        // itemListener must be this in order to be able to remove and reset it.
        m_url.addItemListener(this);
        m_url.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(final FocusEvent e) {
                updateFileError();
            }
        });

        return filePanel;
    }

    /**
     * {@inheritDoc}
     */
    public void itemStateChanged(final ItemEvent e) {
        updateFileError();
    }

    /**
     * Updates the file error label whenever the fileURL entered changed.
     */
    protected void updateFileError() {
        String urlInput = m_url.getEditor().getItem().toString();
        URL urli = null;
        String text = null;

        if ((urlInput == null) || urlInput.equals("")) {
            text = "";
            m_urlError.setText("Specify valid file location");
            return;
        }

        try {
            urli = ARFFReaderNodeModel.stringToURL(urlInput);
            if (urli != null) {
                try {
                    if (urli.getProtocol().equalsIgnoreCase("FILE")) {
                        // Can only check files
                        // if we have a file location check its existence
                        File f = new File(urli.getPath());
                        if (f.isDirectory()) {
                            m_urlError.setText("\"" + f.getAbsolutePath()
                                    + "\" is a directory.");
                        }
                        if (f.exists() && !f.canWrite()) {
                            text = "Cannot write to file \""
                                    + f.getAbsolutePath() + "\".";
                        }
                    }
                } catch (Exception e) {
                    text = "Invalid file location";
                }
            }
        } catch (MalformedURLException mue) {
            text = "Malformed URL.";
        }
        if (text == null) {
            m_urlError.setText("");
        } else {
            m_urlError.setText(text);
            m_urlError.setVisible(true);
        }
    }

    /**
     * Pops up the file selection dialog and sets the selected file path into
     * the m_url text field.
     */
    protected void popupFileChooser() {
        // before opening the dialog, try extracting the file that might
        // be specified already:
        String startingDir = "";
        try {
            URL newURL = ARFFReaderNodeModel.stringToURL(m_url.getEditor()
                    .getItem().toString());
            if (newURL.getProtocol().equals("file")) {
                File tmpFile = new File(newURL.getPath());
                startingDir = tmpFile.getAbsolutePath();
            }
        } catch (Exception e) {
            // no valid path - start in the default dir of the file chooser
        }

        JFileChooser chooser;
        chooser = new JFileChooser(startingDir);
        chooser.setAcceptAllFileFilterUsed(true);
        chooser.setFileFilter(new ARFFReaderNodeModel.ARFFFileFilter());
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int returnVal = chooser.showSaveDialog(getPanel().getParent());
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            String path;
            try {
                path = chooser.getSelectedFile().getAbsoluteFile().toURI()
                        .toURL().toString();
                if (!path.toLowerCase().endsWith(".arff")) {
                    path += ".arff";
                }
            } catch (Exception e) {
                path = "<Error: Couldn't create URL for file>";
            }
            m_url.setSelectedItem(path);
            updateFileError();
        }
        // user canceled - do nothing.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        updateFileError();
        if (!m_urlError.getText().equals("")) {
            throw new InvalidSettingsException("Specify valid file location. "
                    + "Or press 'Cancel'.");
        }

        settings.addString(ARFFWriterNodeModel.CFGKEY_FILENAME, m_url
                .getEditor().getItem().toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {

        // set the file history for the combo box.
        // disconnect the ItemChangelistener first
        m_url.removeItemListener(this);
        m_url.removeAllItems();
        for (String str : ARFFReaderNodeModel.getFileHistory(
        /* removeNotExistingFiles */false)) {
            m_url.addItem(str);
        }

        // set the selected file
        m_url.setSelectedItem(settings.getString(
                ARFFWriterNodeModel.CFGKEY_FILENAME, ""));

        m_url.addItemListener(this);

        updateFileError();
    }
}
