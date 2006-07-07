/* 
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
 *   2006-05-26 (tm): reviewed
 */
package de.unikn.knime.core.node.defaultnodedialog;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;


import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.NodeSettings;
import de.unikn.knime.core.util.SimpleFileFilter;

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
     * Constructor that creates a file chooser with an
     * {@link JFileChooser#OPEN_DIALOG} that filters files according to the
     * given extensions.
     * 
     * @param configName key for filename in config object
     * @param validExtensions only show files with those extensions
     */
    public DialogComponentFileChooser(final String configName,
            final String... validExtensions) {
       this(configName, JFileChooser.OPEN_DIALOG, validExtensions); 
    }
    
    /**
     * Constructor that creates a file chooser of the given type without a file
     * filter.
     * 
     * @param configName key for filename in config object
     * @param dialogType {@link JFileChooser#OPEN_DIALOG},
     *  {@link JFileChooser#SAVE_DIALOG} or {@link JFileChooser#CUSTOM_DIALOG}
     * @param directoryOnly <code>true</code> if only directories should be
     * selectable, otherwise only files can be selected
     */
    public DialogComponentFileChooser(final String configName,
            final int dialogType, final boolean directoryOnly) {
        this(configName, dialogType, directoryOnly, new String[0]);
    }
    
    /**
     * Constructor that creates a file chooser of the given type that filters
     * the files according to the given extensions.
     * 
     * @param configName key for filename in config object
     * @param dialogType {@link JFileChooser#OPEN_DIALOG},
     *  {@link JFileChooser#SAVE_DIALOG} or {@link JFileChooser#CUSTOM_DIALOG}
     * @param validExtensions only show files with those extensions
     */
    public DialogComponentFileChooser(final String configName,
            final int dialogType, final String... validExtensions) {
        this(configName, dialogType, false, validExtensions);
    }

    /**
     * Constructor that creates a file chooser of the given type that filters
     * the files according to the given extensions.
     * 
     * @param configName key for filename in config object
     * @param dialogType {@link JFileChooser#OPEN_DIALOG},
     *  {@link JFileChooser#SAVE_DIALOG} or {@link JFileChooser#CUSTOM_DIALOG}
     * @param directoryOnly <code>true</code> if only directories should be
     *  selectable, otherwise only files can be selected
     * @param validExtensions only show files with those extensions
     */
    public DialogComponentFileChooser(final String configName,
            final int dialogType, final boolean directoryOnly,
            final String... validExtensions) {
        setLayout(new FlowLayout());
        m_configName = configName;
        JPanel p = new JPanel();
        m_fileURL = new JTextField("n/a");
        Border b = BorderFactory.createTitledBorder(" Selected File ");
        p.setBorder(b);
        p.add(m_fileURL);
        
        m_fileURL.setColumns(40);
        m_fileURL.setEditable(true);
        m_browseButton = new JButton("Browse...");
        m_browseButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent ae) {
                // sets the path in the file text field.
                JFileChooser chooser = new JFileChooser(m_fileURL.getText());
                chooser.setDialogType(dialogType);
                if (directoryOnly) {
                    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                } else {
                    // if extensions are defined
                    if (validExtensions != null && validExtensions.length > 0) {
                        // disable "All Files" selection
                        chooser.setAcceptAllFileFilterUsed(false);
                    }
                    // set file filter for given extensions
                    chooser.setFileFilter(
                            new SimpleFileFilter(validExtensions));
                }
                int returnVal = chooser.showDialog(getParent(), null);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    String newFile;
                    try {
                        newFile = chooser.getSelectedFile().getAbsoluteFile()
                                .toString();
                        // if ile selection and only on extension available
                        if (!directoryOnly && validExtensions.length == 1) {
                            // and the file names has no this extension
                            if (!newFile.endsWith(validExtensions[0])) {
                                // then append it
                                newFile += validExtensions[0];
                            }
                        }
                    } catch (SecurityException se) {
                        newFile = "<Error: " + se.getMessage() + ">";
                    }
                    m_fileURL.setText(newFile);
                }
            }
        });

        super.add(p);
        super.add(m_browseButton);
    }

    /**
     * @see de.unikn.knime.core.node.defaultnodedialog.DialogComponent
     *      #loadSettingsFrom(NodeSettings, DataTableSpec[])
     * 
     * @param settings the NodeSettings object to read settings from
     * @param specs specs of all input tables
     * @throws InvalidSettingsException if the settings could not be read
     */
    @Override
    public void loadSettingsFrom(final NodeSettings settings,
            final DataTableSpec[] specs) throws InvalidSettingsException {
        assert settings != null;
        String fileName = "";
        try {
            fileName = settings.getString(m_configName);
        } finally {
            m_fileURL.setText(fileName);
        }
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
        String file = m_fileURL.getText().trim();
        if (file.length() == 0) {
            file = null;
        }
        settings.addString(m_configName, file);
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
