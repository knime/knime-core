/* 
 * -------------------------------------------------------------------
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
 *   29.10.2005 (mb): created
 *   2006-05-26 (tm): reviewed
 */
package org.knime.core.node.defaultnodedialog;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.border.Border;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.ConvenientComboBoxRenderer;
import org.knime.core.node.util.StringHistory;
import org.knime.core.util.SimpleFileFilter;



/**
 * A standard component allowing to choose a location (directory) and file name.
 * 
 * @author M. Berthold, University of Konstanz
 * @deprecated use classes in org.knime.core.node.defaultnodesettings instead
 */
public class DialogComponentFileChooser extends DialogComponent {

    private final String m_configName;
    
    private final JComboBox m_fileComboBox;
    
    private StringHistory m_fileHistory;

    private final JButton m_browseButton;
    
    /**
     * Constructor that creates a file chooser with an
     * {@link JFileChooser#OPEN_DIALOG} that filters files according to the
     * given extensions.
     * 
     * @param configName key for filename in config object
     * @param historyID to identify the file history
     * @param validExtensions only show files with those extensions
     */
    public DialogComponentFileChooser(final String configName, 
            final String historyID, 
            final String... validExtensions) {
       this(configName, historyID, JFileChooser.OPEN_DIALOG, validExtensions); 
    }
    
    /**
     * Constructor that creates a file chooser of the given type without a file
     * filter.
     * 
     * @param configName key for filename in config object
     * @param dialogType {@link JFileChooser#OPEN_DIALOG},
     *  {@link JFileChooser#SAVE_DIALOG} or {@link JFileChooser#CUSTOM_DIALOG}
     *  @param historyID to identify the file history
     * @param directoryOnly <code>true</code> if only directories should be
     * selectable, otherwise only files can be selected
     */
    public DialogComponentFileChooser(final String configName, 
            final String historyID,
            final int dialogType, final boolean directoryOnly) {
        this(configName, historyID, dialogType, directoryOnly, new String[0]);
    }
    
    /**
     * Constructor that creates a file chooser of the given type that filters
     * the files according to the given extensions.
     * 
     * @param configName key for filename in config object
     * @param dialogType {@link JFileChooser#OPEN_DIALOG},
     *  {@link JFileChooser#SAVE_DIALOG} or {@link JFileChooser#CUSTOM_DIALOG}
     * @param validExtensions only show files with those extensions
     * @param historyID id for the file history
     */
    public DialogComponentFileChooser(final String configName,
            final String historyID, final int dialogType, 
            final String... validExtensions) {
        this(configName, historyID, dialogType, false, validExtensions);
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
     * @param historyID to identify the file histroy
     */
    public DialogComponentFileChooser(final String configName,
            final String historyID,
            final int dialogType,
            final boolean directoryOnly,
            final String... validExtensions) {
        setLayout(new FlowLayout());
        m_configName = configName;
        JPanel p = new JPanel();
        
        m_fileHistory = StringHistory.getInstance(historyID);
        m_fileComboBox = new JComboBox();
        m_fileComboBox.setPreferredSize(new Dimension(300, 
                m_fileComboBox.getPreferredSize().height));
        m_fileComboBox.setRenderer(new ConvenientComboBoxRenderer());
        for (String fileName : m_fileHistory.getHistory()) {
            m_fileComboBox.addItem(fileName);
        }
        m_fileComboBox.addItemListener(new ItemListener() {

            /**
             * {@inheritDoc}
             */
            public void itemStateChanged(final ItemEvent e) {
                String fileName = (String)m_fileComboBox.getSelectedItem();
                if (fileName != null) {
                    m_fileHistory.add(fileName);
                }
            }
            
        });
        Border b = BorderFactory.createTitledBorder(" Selected File ");
        p.setBorder(b);
        p.add(m_fileComboBox);
        
        m_browseButton = new JButton("Browse...");
        m_browseButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent ae) {
                // sets the path in the file text field.
                String selectedFile = (String)m_fileComboBox.getSelectedItem();
                if (selectedFile == null) {
                    selectedFile = (String)m_fileComboBox.getItemAt(0);
                }
                JFileChooser chooser = new JFileChooser(selectedFile);
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
                    for (String extension : validExtensions) {
                        chooser.setFileFilter(
                                new SimpleFileFilter(extension));
                    }
                }
                int returnVal = chooser.showDialog(getParent(), null);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    String newFile;
                    try {
                        newFile = chooser.getSelectedFile().getAbsoluteFile()
                                .toString();
                        // if file selection and only on extension available
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
                    List<String> files = Arrays.asList(
                            m_fileHistory.getHistory());
                    if (!files.contains(newFile)) {
                        m_fileComboBox.addItem(newFile);
                    }
                    m_fileComboBox.setSelectedItem(newFile);
                    revalidate();
                }
            }
        });

        super.add(p);
        super.add(m_browseButton);
    }

    /**
     * @see org.knime.core.node.defaultnodedialog.DialogComponent
     *      #loadSettingsFrom(NodeSettingsRO, DataTableSpec[])
     * 
     * @param settings the NodeSettings object to read settings from
     * @param specs specs of all input tables
     * @throws InvalidSettingsException if the settings could not be read
     */
    @Override
    public void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws InvalidSettingsException {
        assert settings != null;
        String fileName = "";
        try {
            fileName = settings.getString(m_configName);
        } finally {
            m_fileComboBox.setSelectedItem(fileName);
        }
        /*
         * if (fileName != null) { m_fileLocator = new File(fileName); } else {
         * m_fileLocator = null; }
         */
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) {
        String file = ((String)m_fileComboBox.getSelectedItem());
        if (file == null || file.trim().length() == 0) {
            file = null;
        }
        settings.addString(m_configName, file);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setEnabledComponents(final boolean enabled) {
        m_browseButton.setEnabled(enabled);
        m_fileComboBox.setEnabled(enabled);
    }
    
}
