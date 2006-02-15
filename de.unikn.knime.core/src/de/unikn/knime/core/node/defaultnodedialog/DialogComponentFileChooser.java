/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * -------------------------------------------------------------------
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
 *   29.10.2005 (mb): created
 */
package de.unikn.knime.core.node.defaultnodedialog;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;

import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.NodeSettings;

/**
 * A standard component allowing to choose a location (directory) and file name.
 * 
 * @author M. Berthold, University of Konstanz
 */
public class DialogComponentFileChooser extends DialogComponent {

    private final String m_configName;

    private final JTextField m_fileURL;

    private final JButton m_browseButton;

    /**
     * Constructor.
     * 
     * @param configName key for filename in config object
     * @param validExtensions prefer files with those extensions
     */
    public DialogComponentFileChooser(final String configName,
            final String[] validExtensions) {
        m_configName = configName;
        m_fileURL = new JTextField("n/a");
        m_fileURL.setBorder(BorderFactory.createTitledBorder(
                " Selected File "));
        m_fileURL.setColumns(40);
        m_fileURL.setEditable(true);
        m_browseButton = new JButton("Browse...");
        m_browseButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent ae) {
                // sets the path in the file text field.
                JFileChooser chooser = new JFileChooser(m_fileURL.getText());
                chooser.setFileFilter(new SimpleFileFilter(validExtensions));
                int returnVal = chooser.showOpenDialog(getParent());
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    String newFile;
                    try {
                        newFile = chooser.getSelectedFile().getAbsoluteFile()
                                .toString();
                    } catch (Exception e) {
                        newFile = "<Error: Couldn't create URL for file>";
                    }
                    m_fileURL.setText(newFile);
                }
            }
        });

        super.add(m_fileURL);
        super.add(m_browseButton);
    }

    /**
     * Internal helper class filtering out all files not matching extensions.
     */
    public class SimpleFileFilter extends FileFilter {
        private String[] m_validExtensions;

        /**
         * @param exts allowed extensions
         */
        public SimpleFileFilter(final String[] exts) {
            m_validExtensions = exts;
        }

        /**
         * @see java.io.FileFilter#accept(java.io.File)
         */
        public boolean accept(final File f) {
            if (f != null) {
                if (f.isDirectory()) {
                    return true;
                }
                String fileName = f.getName();
                for (String ext : m_validExtensions) {
                    if (fileName.endsWith(ext)) {
                        return true;
                    }
                }
            }
            return false;
        }

        /**
         * @see javax.swing.filechooser.FileFilter#getDescription()
         */
        public String getDescription() {
            String descr = "";
            for (String ext : m_validExtensions) {
                descr = descr + " " + ext.toString();
            }
            return descr;
        }
    }

    /**
     * @see de.unikn.knime.core.node.defaultnodedialog.DialogComponent
     *      #loadSettingsFrom(NodeSettings, DataTableSpec[])
     * 
     * @param settings the NodeSettings object to read settings from
     * @param specs of all input tables
     * @throws InvalidSettingsException if load fails.
     */
    @Override
    public void loadSettingsFrom(final NodeSettings settings,
            final DataTableSpec[] specs) throws InvalidSettingsException {
        assert settings != null;
        String fileName = null;
        try {
            fileName = settings.getString(m_configName);
        } catch (InvalidSettingsException ise) {
            throw new InvalidSettingsException(ise + " Failed to read '"
                    + m_configName + "' value.");
        }
        m_fileURL.setText(fileName);
        /*
         * if (fileName != null) { m_fileLocator = new File(fileName); } else {
         * m_fileLocator = null; }
         */
    }

    /**
     * @see de.unikn.knime.core.node.defaultnodedialog.DialogComponent
     *      #saveSettingsTo(NodeSettings)
     */
    @Override
    public void saveSettingsTo(final NodeSettings settings) {
        settings.addString(m_configName, m_fileURL.getText());
    }

    /**
     * @see de.unikn.knime.core.node.defaultnodedialog.DialogComponent
     *      #setEnabledComponents(boolean)
     */
    @Override
    protected void setEnabledComponents(final boolean enabled) {
        m_browseButton.setEnabled(enabled);
        m_fileURL.setEnabled(enabled);
    }
}
