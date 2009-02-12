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
 * --------------------------------------------------------------------- *
 * 
 * History
 *   12.12.2006 (thiel): created
 */
package org.knime.core.node.util;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.util.SimpleFileFilter;

/**
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public class FileSelectionPanel extends JPanel {
    
    /** default foreground color for editable components. */
    protected static final Color DEFAULT_FG = new JTextField().getForeground();

    /** default background color for editable components. */
    protected static final Color DEFAULT_BG = new JTextField().getBackground();
    
    private final JComboBox m_fileComboBox;

    private StringHistory m_fileHistory;

    private final JButton m_browseButton;

    private final TitledBorder m_border;
    
    private String m_configKey;
    
    private String m_filename;

    /**
     * Constructor that creates a file chooser with an
     * {@link JFileChooser#OPEN_DIALOG} that filters files according to the
     * given extensions. Also non-existing paths are accepted.
     * 
     * @param configKey the model holding the value
     * @param historyID to identify the file history
     * @param validExtensions only show files with those extensions
     */
    public FileSelectionPanel(final String configKey, final String historyID, 
            final String... validExtensions) {
        this(configKey, historyID, JFileChooser.OPEN_DIALOG, validExtensions);
    }

    /**
     * Constructor that creates a file/directory chooser of the given type
     * without a file filter. Also non-existing paths are accepted.
     * 
     * @param configKey the model holding the value
     * @param dialogType {@link JFileChooser#OPEN_DIALOG},
     *            {@link JFileChooser#SAVE_DIALOG} or
     *            {@link JFileChooser#CUSTOM_DIALOG}
     * @param historyID to identify the file history
     * @param directoryOnly <code>true</code> if only directories should be
     *            selectable, otherwise only files can be selected
     */
    public FileSelectionPanel(final String configKey, final String historyID, 
            final int dialogType, final boolean directoryOnly) {
        this(configKey, historyID, dialogType, directoryOnly, new String[0]);
    }

    /**
     * Constructor that creates a file chooser of the given type that filters
     * the files according to the given extensions. Also non-existing paths are
     * accepted.
     * 
     * @param configKey the config key
     * @param dialogType {@link JFileChooser#OPEN_DIALOG},
     *            {@link JFileChooser#SAVE_DIALOG} or
     *            {@link JFileChooser#CUSTOM_DIALOG}
     * @param validExtensions only show files with those extensions
     * @param historyID id for the file history
     */
    public FileSelectionPanel(final String configKey, final String historyID, 
            final int dialogType, final String... validExtensions) {
        this(configKey, historyID, dialogType, false, validExtensions);
    }

    /**
     * Constructor that creates a file or directory chooser of the given type
     * that filters the files according to the given extensions. Also
     * non-existing paths are accepted.
     * 
     * @param configKey the config key.
     * @param dialogType {@link JFileChooser#OPEN_DIALOG},
     *            {@link JFileChooser#SAVE_DIALOG} or
     *            {@link JFileChooser#CUSTOM_DIALOG}
     * @param directoryOnly <code>true</code> if only directories should be
     *            selectable, otherwise only files can be selected
     * @param validExtensions only show files with those extensions
     * @param historyID to identify the file histroy
     */
    public FileSelectionPanel(final String configKey, final String historyID, 
            final int dialogType, final boolean directoryOnly, 
            final String... validExtensions) {
        setLayout(new FlowLayout());
        m_configKey = configKey;
        m_filename = "";

        JPanel p = new JPanel();
        m_fileHistory = StringHistory.getInstance(historyID);
        m_fileComboBox = new JComboBox();
        m_fileComboBox.setPreferredSize(new Dimension(300, m_fileComboBox
                .getPreferredSize().height));
        m_fileComboBox.setRenderer(new ConvenientComboBoxRenderer());

        for (String fileName : m_fileHistory.getHistory()) {
            m_fileComboBox.addItem(fileName);
        }

        m_browseButton = new JButton("Browse...");

        String title = directoryOnly ? "Selected Directory:" : "Selected File:";
        m_border = BorderFactory.createTitledBorder(title);
        p.setBorder(m_border);
        p.add(m_fileComboBox);
        p.add(m_browseButton);
        super.add(p);

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
                        chooser.setFileFilter(new SimpleFileFilter(extension));
                    }
                }
                int returnVal = chooser.showDialog(getParent(), null);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    String newFile;
                    try {
                        newFile =
                                chooser.getSelectedFile().getAbsoluteFile()
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
                    // avoid adding the same string twice...
                    m_fileComboBox.removeItem(newFile);
                    m_fileComboBox.addItem(newFile);
                    m_fileComboBox.setSelectedItem(newFile);
                    revalidate();
                }
            }
        });

        m_fileComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                // transfer the new filename into the settings model
                try {
                    updateModel(true); // don't color the combobox red.
                } catch (InvalidSettingsException ise) {
                    // ignore it here.
                }
            }
        });
    }

    /**
     * Transfers the value from the component into the settings model.
     * 
     * @param noColoring if set true, the component will not be marked red, even
     *            if the entered value was erroneous.
     * @throws InvalidSettingsException if the entered filename is null or
     *             emtpy.
     */
    private void updateModel(final boolean noColoring)
            throws InvalidSettingsException {

        String file = (String)m_fileComboBox.getSelectedItem();
        if ((file != null) && (file.trim().length() > 0)) {
            m_filename = file;
        } else {
            if (!noColoring) {
                showError(m_fileComboBox);
            }
            throw new InvalidSettingsException("Please specify a filename.");
        }
    }

    /**
     * Seems the super.showError doesn't work with comboboxes. This is to
     * replace it with a working version.
     * 
     * @param box the box to color red.
     */
    private void showError(final JComboBox box) {

        String selection = (String)box.getSelectedItem();

        if ((selection == null) || (selection.length() == 0)) {
            box.setBackground(Color.RED);
        } else {
            box.setForeground(Color.RED);
        }
        box.requestFocusInWindow();

        // change the color back as soon as he changes something
        box.addItemListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                box.setForeground(DEFAULT_FG);
                box.setBackground(DEFAULT_BG);
                box.removeItemListener(this);
            }
        });
    }

    /**
     * Updates the components if the components filename is out of syn with the
     * given filename.
     * @param newFile The new filename to set if the components filename is
     * not equal with the given one.
     */
    void updateComponent(final String newFile) {
        // update the component only if model and component are out of sync
        if (newFile != null) {
            File file = new File(newFile);
            String newValue = file.getAbsolutePath();
            boolean update;
            if (newValue == null) {
                update = (m_fileComboBox.getSelectedItem() != null);
            } else {
                update = !newValue.equals(m_fileComboBox.getSelectedItem());
            }
            if (update) {
                // to avoid multiply added items...
                m_fileComboBox.removeItem(newValue);
                m_fileComboBox.addItem(newValue);
                m_fileComboBox.setSelectedItem(newValue);
            }
        }
    }

    /**
     * Enables components if enabled is true, otherwise components will be
     * disabled.
     * @param enabled If true components will be enabled, if false disabled.
     */
    protected void setEnabledComponents(final boolean enabled) {
        m_browseButton.setEnabled(enabled);
        m_fileComboBox.setEnabled(enabled);
    }

    /**
     * Replaces the title displayed in the border that surrounds the editfield
     * and browse button with the specified new title. The default title of the
     * component is "Selected File:" or "Selected Directory:".
     * 
     * @param newTitle the new title to display in the border.
     * 
     * @throws NullPointerException if the new title is null.
     */
    public void setBorderTitle(final String newTitle) {
        if (newTitle == null) {
            throw new NullPointerException("New title to display can't"
                    + " be null.");
        }
        m_border.setTitle(newTitle);
    }   
    
    
    /**
     * Saves all settings to settings object.
     * 
     * @param settings Object to store settings in.
     */
    public void saveSettingsTo(final NodeSettingsWO settings) {
        assert (settings != null);
        settings.addString(m_configKey, m_filename);
    }
    
    /**
     * Method loadSettingsFrom.
     * 
     * @param settings The NodeSettings object of the containing NodeDialogPane.
     * @param specs The DataTableSpec[] of the containing NodeDialogPane.
     * @throws InvalidSettingsException If settings could not be loaded.
     */
    public void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws InvalidSettingsException {
        assert (settings != null && specs != null);
        updateComponent(settings.getString(m_configKey));
    }
}
