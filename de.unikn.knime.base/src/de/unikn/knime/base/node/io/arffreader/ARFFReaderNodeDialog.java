/* -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   11.02.2005 (ohl): created
 */
package de.unikn.knime.base.node.io.arffreader;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;

import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.NodeDialogPane;
import de.unikn.knime.core.node.NodeSettingsRO;
import de.unikn.knime.core.node.NodeSettingsWO;
import de.unikn.knime.core.node.NotConfigurableException;

/**
 * Contains the dialog for the ARFF file reader.
 * 
 * @author ohl, University of Konstanz
 */
public class ARFFReaderNodeDialog extends NodeDialogPane {

    private static final int VERT_SPACE = 5;

    private static final int HORIZ_SPACE = 5;

    private static final int COMPONENT_HEIGHT = 25;

    private static final int TEXTFIELD_WIDTH = 350;

    private JTextField m_url;

    private JLabel m_urlError;

    private JTextField m_rowPrefix;

    private static final int TABWIDTH = 600;

    /**
     * Creates a new ARFF file reader dialog.
     */
    public ARFFReaderNodeDialog() {
        super("ARFF File Reader");

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
        fPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.
                createEtchedBorder(), "Enter location of ARFF file:"));
        Container fileBox = Box.createHorizontalBox();
        fileBox.add(Box.createHorizontalStrut(20));
        fileBox.add(new JLabel("valid URL:"));
        fileBox.add(Box.createHorizontalStrut(HORIZ_SPACE));

        sumOfCompHeigth += COMPONENT_HEIGHT;

        m_url = new JTextField();
        m_url.setMaximumSize(new Dimension(TEXTFIELD_WIDTH, COMPONENT_HEIGHT));
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

        Container prefixBox = Box.createHorizontalBox();
        JPanel prefixPanel = new JPanel();
        prefixPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.
                createEtchedBorder(), "Row IDs are build from prefix + "
                + "row number"));
        prefixPanel.setLayout(new BoxLayout(prefixPanel, BoxLayout.X_AXIS));

        prefixBox.add(Box.createHorizontalStrut(20));
        prefixBox.add(new JLabel("RowID prefix:"));
        prefixBox.add(Box.createHorizontalStrut(HORIZ_SPACE));
        m_rowPrefix = new JTextField();
        m_rowPrefix.setMaximumSize(new Dimension(TEXTFIELD_WIDTH / 2,
                COMPONENT_HEIGHT));
        prefixBox.add(m_rowPrefix);

        prefixPanel.add(prefixBox);
        innerBox.add(prefixPanel);
        innerBox.add(Box.createVerticalStrut(VERT_SPACE));
        sumOfCompHeigth += COMPONENT_HEIGHT + (2 * VERT_SPACE);

        innerBox.setMaximumSize(new Dimension(TABWIDTH, sumOfCompHeigth));

        filePanel.add(innerBox);
        filePanel.add(Box.createGlue());

        // set a filter to the filename that fills the error text label
        m_url.getDocument().addDocumentListener(new DocumentListener() {
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

        return filePanel;
    }

    /**
     * updates the file error label whenever the fileURL entered changed.
     */
    protected void updateFileError() {
        URL urli = null;
        String text = null;

        if ((m_url.getText() == null) || m_url.getText().equals("")) {
            text = "";
            m_urlError.setText("Specify valid file location");
            return;
        }

        try {
            urli = ARFFReaderNodeModel.stringToURL(m_url.getText());
            if (urli != null) {
                try {
                    InputStream is = urli.openStream();
                    if (is != null) {
                        text = null;
                    } else {
                        text = "Can't open: " + urli.toString();
                    }
                } catch (IOException ioe) {
                    text = "Can't open: " + urli.toString();
                }
            } else {
                text = "Invalid URL";
            }
        } catch (Exception e) {
            text = "Invalid file location";
        }

        if (text == null) {
            m_urlError.setText("");
        } else {
            m_urlError.setText(text);
            m_urlError.setVisible(true);
        }

    }

    /**
     * pops up the file selection dialog and sets the selected file path into
     * the m_url text field.
     */
    protected void popupFileChooser() {
        // before opening the dialog, try extracting the file that might
        // be specified already:
        String startingDir = "";
        try {
            URL newURL = ARFFReaderNodeModel.stringToURL(m_url.getText());
            if (newURL.getProtocol().equals("file")) {
                File tmpFile = new File(newURL.getPath());
                startingDir = tmpFile.getAbsolutePath();
            }
        } catch (Exception e) {
            // no valid path - start in the default dir of the file chooser
        }

        JFileChooser chooser;
        chooser = new JFileChooser(startingDir);
        chooser.setFileFilter(new ARFFReaderFileFilter());
        int returnVal = chooser.showOpenDialog(getPanel().getParent());
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            String path;
            try {
                path = chooser.getSelectedFile().getAbsoluteFile().toURL().
                        toString();
            } catch (Exception e) {
                path = "<Error: Couldn't create URL for file>";
            }
            m_url.setText(path);
        }
        // user canceled - do nothing.
    }

    /**
     * @see NodeDialogPane#loadSettingsFrom(NodeSettingsRO, DataTableSpec[])
     */
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        m_url.setText(settings.
                getString(ARFFReaderNodeModel.CFGKEY_FILEURL, ""));
        m_rowPrefix.setText(settings.getString(
                ARFFReaderNodeModel.CFGKEY_ROWPREFIX, ""));

    }

    /**
     * @see NodeDialogPane#saveSettingsTo(NodeSettingsWO)
     */
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        updateFileError();
        if (!m_urlError.getText().equals("")) {
            throw new InvalidSettingsException("Specify valid file location. "
                    + "Or press 'Cancel'.");
        }
        settings.addString(ARFFReaderNodeModel.CFGKEY_FILEURL, m_url.getText());
        settings.addString(ARFFReaderNodeModel.CFGKEY_ROWPREFIX, m_rowPrefix.
                getText());
    }

    /**
     * FileFilter for the ARFFReader file chooser dialog.
     * 
     * @author ohl, University of Konstanz
     */
    private class ARFFReaderFileFilter extends FileFilter {

        /**
         * @see javax.swing.filechooser.FileFilter#accept(java.io.File)
         */
        public boolean accept(final File f) {
            if (f != null) {
                if (f.isDirectory()) {
                    return true;
                }
                String lastFive = f.getName().substring(
                        f.getName().length() - 5, f.getName().length());
                return lastFive.equalsIgnoreCase(".arff");
            }
            return true;

        }

        /**
         * @see javax.swing.filechooser.FileFilter#getDescription()
         */
        public String getDescription() {
            return "ARFF data files (*.arff)";
        }

    }

}
