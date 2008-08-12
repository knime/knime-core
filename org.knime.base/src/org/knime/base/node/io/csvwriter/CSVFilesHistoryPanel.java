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
 *   Dec 17, 2005 (wiswedel): created
 */
package org.knime.base.node.io.csvwriter;

import java.awt.Color;
import java.awt.Component;
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
import java.util.Iterator;
import java.util.LinkedHashSet;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import org.knime.core.node.util.ConvenientComboBoxRenderer;
import org.knime.core.node.util.StringHistory;
import org.knime.core.util.SimpleFileFilter;

/**
 * Panel that contains an editable Combo Box showing the file to write to and a
 * button to trigger a file chooser. The elements in the combo are files that
 * have been recently used.
 *
 * @see org.knime.core.node.util.StringHistory
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class CSVFilesHistoryPanel extends JPanel {

    private final JComboBox m_textBox;

    private final JButton m_chooseButton;

    private final JLabel m_warnMsg;

    /**
     * Creates new instance, sets properties, for instance renderer,
     * accordingly.
     */
    public CSVFilesHistoryPanel() {
        m_textBox = new JComboBox(new DefaultComboBoxModel());
        m_textBox.setEditable(true);
        m_textBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        m_textBox.setPreferredSize(new Dimension(300, 25));
        m_textBox.setRenderer(new ConvenientComboBoxRenderer());

        // install listeners to update warn message whenever file name changes
        m_textBox.addItemListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                fileLocationChanged();
            }
        });
        /* install action listeners */
        m_textBox.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(final FocusEvent e) {
                fileLocationChanged();
            }
        });
        Component editor = m_textBox.getEditor().getEditorComponent();
        if (editor instanceof JTextComponent) {
            Document d = ((JTextComponent)editor).getDocument();
            d.addDocumentListener(new DocumentListener() {
                public void changedUpdate(final DocumentEvent e) {
                    fileLocationChanged();
                }

                public void insertUpdate(final DocumentEvent e) {
                    fileLocationChanged();
                }

                public void removeUpdate(final DocumentEvent e) {
                    fileLocationChanged();
                }
            });
        }

        m_chooseButton = new JButton("Browse...");
        m_chooseButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                String newFile = getOutputFileName();
                if (newFile != null) {
                    m_textBox.setSelectedItem(newFile);
                }
            }
        });
        m_warnMsg = new JLabel("");
        // this ensures correct display of the changing label content...
        m_warnMsg.setPreferredSize(new Dimension(350, 25));
        m_warnMsg.setMinimumSize(new Dimension(350, 25));
        m_warnMsg.setForeground(Color.red);

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        Box fileBox = Box.createHorizontalBox();
        fileBox.add(m_textBox);
        fileBox.add(Box.createHorizontalStrut(5));
        fileBox.add(m_chooseButton);
        Box warnBox = Box.createHorizontalBox();
        warnBox.add(m_warnMsg);
        warnBox.add(Box.createHorizontalGlue());
        warnBox.add(Box.createVerticalStrut(25));

        add(fileBox);
        add(warnBox);
        updateHistory();
        fileLocationChanged();
    }

    private void fileLocationChanged() {
        String newMsg = "";
        String selFile = getSelectedFile();
        if (selFile != null && selFile.length() > 0) {
            File newFile = getFile(selFile);
            if (newFile.exists()) {
                if (newFile.isDirectory()) {
                    newMsg = "ERROR: output file can't be a directory";
                } else {
                    newMsg = "Warning: selected file exists.";
                }
            } else {
                if (!newFile.getParentFile().exists()) {
                    newMsg =
                            "Warning: Directory of specified file doesn't exist"
                                    + " and will be created";
                }
            }
        }

        if (newMsg.length() == 0) {
            m_warnMsg.setForeground(m_warnMsg.getBackground());
            m_warnMsg.setText("A long msg to avoid invisibility of label");
        } else {
            m_warnMsg.setForeground(Color.red);
            m_warnMsg.setText(newMsg);
        }

        revalidate();
        repaint();
        invalidate();
        repaint();
    }

    private String getOutputFileName() {
        // file chooser triggered by choose button
        final JFileChooser fileChooser = new JFileChooser();
        fileChooser.setAcceptAllFileFilterUsed(true);
        fileChooser.setFileFilter(new SimpleFileFilter(".csv"));
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        String f = m_textBox.getEditor().getItem().toString();
        File dirOrFile = getFile(f);
        if (dirOrFile.isDirectory()) {
            fileChooser.setCurrentDirectory(dirOrFile);
        } else {
            fileChooser.setSelectedFile(dirOrFile);
        }
        int r = fileChooser.showDialog(CSVFilesHistoryPanel.this, "Save");
        if (r == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (file.exists() && file.isDirectory()) {
                JOptionPane.showMessageDialog(this, "Error: Please specify "
                        + "a file, not a directory.");
                return null;
            }
            return file.getAbsolutePath();
        }
        return null;
    }

    /**
     * Get currently selected file.
     *
     * @return the current file url
     * @see javax.swing.JComboBox#getSelectedItem()
     */
    public String getSelectedFile() {
        return m_textBox.getEditor().getItem().toString();
    }

    /**
     * Set the file url as default.
     *
     * @param url the file to choose
     * @see javax.swing.JComboBox#setSelectedItem(java.lang.Object)
     */
    public void setSelectedFile(final String url) {
        m_textBox.setSelectedItem(url);
    }

    /** Updates the elements in the combo box, reads the file history. */
    public void updateHistory() {
        StringHistory history =
                StringHistory.getInstance(CSVWriterNodeModel.FILE_HISTORY_ID);
        String[] allVals = history.getHistory();
        LinkedHashSet<String> list = new LinkedHashSet<String>();
        for (int i = 0; i < allVals.length; i++) {
            // we used to store URLs in the history. Be backward compatible.
            try {
                String cur = allVals[i];
                File file = textToFile(cur);
                list.add(file.getAbsolutePath());
            } catch (MalformedURLException mue) {
                continue;
            }
        }
        DefaultComboBoxModel comboModel =
                (DefaultComboBoxModel)m_textBox.getModel();
        comboModel.removeAllElements();
        for (Iterator<String> it = list.iterator(); it.hasNext();) {
            comboModel.addElement(it.next());
        }
        // changing the model will also change the minimum size to be
        // quite big. We have a tooltip, we don't need that
        Dimension newMin = new Dimension(0, getPreferredSize().height);
        setMinimumSize(newMin);
    }

    /**
     * Tries to create a File from the passed string.
     *
     * @param url the string to transform into an File
     * @return File if entered value could be properly tranformed
     * @throws MalformedURLException if the value passed was invalid
     */
    private static File textToFile(final String url)
            throws MalformedURLException {
        if ((url == null) || (url.equals(""))) {
            throw new MalformedURLException("Specify a not empty valid URL");
        }

        String file;
        try {
            URL newURL = new URL(url);
            file = newURL.getFile();
        } catch (MalformedURLException e) {
            // see if they specified a file without giving the protocol
            return new File(url);
        }
        if (file == null || file.equals("")) {
            throw new MalformedURLException("Can't get file from file '" + url
                    + "'");
        }
        return new File(file);
    }

    /**
     * Return a file object for the given fileName. It makes sure that if the
     * fileName is not absolute it will be relative to the user's home dir.
     *
     * @param fileName the file name to convert to a file
     * @return a file representing fileName
     */
    public static final File getFile(final String fileName) {
        File f = new File(fileName);
        if (!f.isAbsolute()) {
            f = new File(new File(System.getProperty("user.home")), fileName);
        }
        return f;
    }

}
