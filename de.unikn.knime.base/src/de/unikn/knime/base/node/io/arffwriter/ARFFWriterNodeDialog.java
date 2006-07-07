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
 *   17.02.2005 (ohl): created
 */
package de.unikn.knime.base.node.io.arffwriter;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;

import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.NodeDialogPane;
import de.unikn.knime.core.node.NodeSettings;
import de.unikn.knime.core.node.NotConfigurableException;

/**
 * 
 * @author ohl, University of Konstanz
 */
public class ARFFWriterNodeDialog extends NodeDialogPane {

    /** textfield to enter file name. */
    private final JTextField m_textField;

    /**
     * creates a new dialog for the ARFF writer.
     */
    public ARFFWriterNodeDialog() {
        super("ARFF Writer");

        m_textField = new JTextField(20);

        final JPanel panel = new JPanel(new GridLayout(1, 1));
        final JPanel urlPanel = new JPanel(new FlowLayout());
        urlPanel.add(m_textField);
        JButton button = new JButton("Browse...");
        button.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                // file chooser triggered by choose button
                final JFileChooser fileChooser = new JFileChooser();
                fileChooser.setSelectedFile(getFile(m_textField.getText()));
                int r = fileChooser.showDialog(urlPanel, "Select");
                if (r == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile();
                    m_textField.setText(file.getAbsolutePath());
                }
            }
        });
        urlPanel.add(button);
        panel.add(urlPanel);
        addTab("File Chooser", panel);
    }

    /**
     * @see NodeDialogPane#loadSettingsFrom(NodeSettings, DataTableSpec[])
     */
    protected void loadSettingsFrom(final NodeSettings settings,
            final DataTableSpec[] specs) throws NotConfigurableException {

        m_textField.setText(
                settings.getString(ARFFWriterNodeModel.CFGKEY_FILENAME, ""));

    }
    /**
     * @see NodeDialogPane#saveSettingsTo(NodeSettings)
     */
    protected void saveSettingsTo(final NodeSettings settings)
            throws InvalidSettingsException {
        if ((m_textField.getText() == null) 
                || m_textField.getText().equals("")) {
            throw new InvalidSettingsException("Specify a valid file name, "
                    + "or press cancel, please.");
        }
        settings.addString(
                ARFFWriterNodeModel.CFGKEY_FILENAME, m_textField.getText());
    }

    /**
     * Return a file object for the given fileName. It makes sure that if the
     * fileName is not absolute it will be relative to the user's home dir.
     * 
     * @param fileName The file name to convert to a file.
     * @return A file representing fileName.
     */
    private static final File getFile(final String fileName) {
        File f = new File(fileName);
        if (!f.isAbsolute()) {
            f = new File(new File(System.getProperty("user.home")), fileName);
        }
        return f;
    }
}
