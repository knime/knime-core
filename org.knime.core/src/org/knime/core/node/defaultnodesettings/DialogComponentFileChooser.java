/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * -------------------------------------------------------------------
 *
 * History
 *   29.10.2005 (mb): created
 *   2006-05-26 (tm): reviewed
 */
package org.knime.core.node.defaultnodesettings;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.FlowVariableModelButton;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.ConvenientComboBoxRenderer;
import org.knime.core.node.util.StringHistory;
import org.knime.core.util.SimpleFileFilter;

/**
 * A standard component allowing to choose a location(directory) and/or file
 * name.
 *
 * @author M. Berthold, University of Konstanz
 */
public class DialogComponentFileChooser extends DialogComponent {

    private final JComboBox m_fileComboBox;

    private final StringHistory m_fileHistory;

    private final JButton m_browseButton;

    private final TitledBorder m_border;

    private final List<SimpleFileFilter> m_fileFilter;

    private FlowVariableModelButton m_fvmButton;

    /**
     * Constructor that creates a file chooser with an
     * {@link JFileChooser#OPEN_DIALOG} that filters files according to the
     * given extensions. Also non-existing paths are accepted.
     *
     * @param stringModel the model holding the value
     * @param historyID to identify the file history
     * @param validExtensions only show files with those extensions. An entry
     * in this array may contain the <code>|</code> character between two
     * file extensions that will be shown in one item of the file type
     * combo box. This means that one item allows for more than one file type.
     * Specify extension including the dot &quot;.&quot;.
     */
    public DialogComponentFileChooser(final SettingsModelString stringModel,
            final String historyID, final String... validExtensions) {
        this(stringModel, historyID, JFileChooser.OPEN_DIALOG, validExtensions);
    }

    /**
     * Constructor that creates a file/directory chooser of the given type
     * without a file filter. Also non-existing paths are accepted.
     *
     * @param stringModel the model holding the value
     * @param dialogType {@link JFileChooser#OPEN_DIALOG},
     *            {@link JFileChooser#SAVE_DIALOG} or
     *            {@link JFileChooser#CUSTOM_DIALOG}
     * @param historyID to identify the file history
     * @param directoryOnly <code>true</code> if only directories should be
     *            selectable, otherwise only files can be selected
     */
    public DialogComponentFileChooser(final SettingsModelString stringModel,
            final String historyID, final int dialogType,
            final boolean directoryOnly) {
        this(stringModel, historyID, dialogType, directoryOnly, new String[0]);
    }

    /**
     * Constructor that creates a file chooser of the given type that filters
     * the files according to the given extensions. Also non-existing paths are
     * accepted.
     *
     * @param stringModel the model holding the value
     * @param historyID id for the file history
     * @param dialogType {@link JFileChooser#OPEN_DIALOG},
     *            {@link JFileChooser#SAVE_DIALOG} or
     *            {@link JFileChooser#CUSTOM_DIALOG}
     * @param validExtensions only show files with those extensions. An entry
     * in this array may contain the <code>|</code> character between two
     * file extensions that will be shown in one item of the file type
     * combo box. This means that one item allows for more than one file type.
     * Specify extension including the dot &quot;.&quot;.
     */
    public DialogComponentFileChooser(final SettingsModelString stringModel,
            final String historyID, final int dialogType,
            final String... validExtensions) {
        this(stringModel, historyID, dialogType, false, validExtensions);
    }

    /**
     * Constructor that creates a file or directory chooser of the given type
     * that filters the files according to the given extensions. Also
     * non-existing paths are accepted.
     *
     * @param stringModel the model holding the value
     * @param historyID to identify the file history
     * @param dialogType {@link JFileChooser#OPEN_DIALOG},
     *            {@link JFileChooser#SAVE_DIALOG} or
     *            {@link JFileChooser#CUSTOM_DIALOG}
     * @param directoryOnly <code>true</code> if only directories should be
     *            selectable, otherwise only files can be selected
     * @param validExtensions only show files with those extensions. An entry
     * in this array may contain the <code>|</code> character between two
     * file extensions that will be shown in one item of the file type
     * combo box. This means that one item allows for more than one file type.
     * Specify extension including the dot &quot;.&quot;.
     */
    public DialogComponentFileChooser(final SettingsModelString stringModel,
            final String historyID, final int dialogType,
            final boolean directoryOnly, final String... validExtensions) {
        this(stringModel, historyID, dialogType, directoryOnly,
                null, validExtensions);
    }

    /**
     * Constructor that creates a file or directory chooser of the given type
     * that filters the files according to the given extensions. Also
     * non-existing paths are accepted.
     *
     * @param stringModel the model holding the value
     * @param historyID to identify the file history
     * @param dialogType {@link JFileChooser#OPEN_DIALOG},
     *            {@link JFileChooser#SAVE_DIALOG} or
     *            {@link JFileChooser#CUSTOM_DIALOG}
     * @param directoryOnly <code>true</code> if only directories should be
     *            selectable, otherwise only files can be selected
     * @param fvm model exposed to choose from available flow variables
     * @param validExtensions only show files with those extensions. An entry
     * in this array may contain the <code>|</code> character between two
     * file extensions that will be shown in one item of the file type
     * combo box. This means that one item allows for more than one file type.
     * Specify extension including the dot &quot;.&quot;.
     */
    public DialogComponentFileChooser(final SettingsModelString stringModel,
            final String historyID, final int dialogType,
            final boolean directoryOnly, final FlowVariableModel fvm,
            final String... validExtensions) {
        super(stringModel);

        getComponentPanel().setLayout(new FlowLayout());

        final JPanel p = new JPanel();
        m_fileHistory = StringHistory.getInstance(historyID);
        m_fileComboBox = new JComboBox();
        m_fileComboBox.setPreferredSize(new Dimension(300, m_fileComboBox
                .getPreferredSize().height));
        m_fileComboBox.setRenderer(new ConvenientComboBoxRenderer());
        m_fileComboBox.setEditable(true);

        for (final String fileName : m_fileHistory.getHistory()) {
            m_fileComboBox.addItem(fileName);
        }

        m_browseButton = new JButton("Browse...");

        final String title =
            directoryOnly ? "Selected Directory:" : "Selected File:";
        m_border = BorderFactory.createTitledBorder(title);
        p.setBorder(m_border);
        p.add(m_fileComboBox);
        p.add(m_browseButton);
        getComponentPanel().add(p);

        if (validExtensions != null) {
            m_fileFilter =
                new ArrayList<SimpleFileFilter>(validExtensions.length);
            for (final String extension : validExtensions) {
                if (extension.indexOf('|') > 0) {
                    m_fileFilter.add(new SimpleFileFilter(
                            extension.split("\\|")));
                } else {
                    m_fileFilter.add(new SimpleFileFilter(extension));
                }
            }
        } else {
            m_fileFilter = new ArrayList<SimpleFileFilter>(0);
        }

        m_browseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent ae) {
                // sets the path in the file text field.
                final String selectedFile =
                    m_fileComboBox.getEditor().getItem().toString();
                final JFileChooser chooser = new JFileChooser(selectedFile);
                chooser.setDialogType(dialogType);
                if (directoryOnly) {
                    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                } else {
                    // if extensions are defined
                    if (m_fileFilter != null
                            && m_fileFilter.size() > 0) {
                        // disable "All Files" selection
                        chooser.setAcceptAllFileFilterUsed(false);
                        // set the file filter for the given extensions
                        for (final FileFilter filter : m_fileFilter) {
                            chooser.setFileFilter(filter);
                        }
                        //set the first filter as default filter
                        chooser.setFileFilter(m_fileFilter.get(0));
                    }
                }
                final int returnVal =
                        chooser.showDialog(getComponentPanel().getParent(),
                                null);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    String newFile;
                    try {
                        newFile =
                                chooser.getSelectedFile().getAbsoluteFile()
                                        .toString();
                        //check if the user has added the extension
                        if (!directoryOnly && m_fileFilter != null) {
                            boolean extensionFound = false;
                            for (final SimpleFileFilter filter : m_fileFilter) {
                                final String[] extensions =
                                    filter.getValidExtensions();
                                for (final String extension : extensions) {
                                    if (newFile.endsWith(extension)) {
                                        extensionFound = true;
                                        break;
                                    }
                                }
                                if (extensionFound) {
                                    break;
                                }
                            }
                            //otherwise add the extension of the selected
                            //FileFilter
                            if (!extensionFound) {
                                final FileFilter fileFilter =
                                    chooser.getFileFilter();
                                if (fileFilter != null
                                        && fileFilter
                                        instanceof SimpleFileFilter) {
                                    final SimpleFileFilter filter =
                                        (SimpleFileFilter)fileFilter;
                                    final String[] extensions =
                                        filter.getValidExtensions();
                                    if (extensions != null
                                            && extensions.length > 0) {
                                        //append the first extension
                                        newFile = newFile + extensions[0];
                                    }
                                }
                            }
                        }
                    } catch (final SecurityException se) {
                        newFile = "<Error: " + se.getMessage() + ">";
                    }
                    // avoid adding the same string twice...
                    m_fileComboBox.removeItem(newFile);
                    m_fileComboBox.addItem(newFile);
                    m_fileComboBox.setSelectedItem(newFile);
                    getComponentPanel().revalidate();
                }
            }
        });

        // add variable editor button if so desired
        if (fvm != null) {
            fvm.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(final ChangeEvent evt) {
                     getModel().setEnabled(!fvm.isVariableReplacementEnabled());
                }
            });
            m_fvmButton = new FlowVariableModelButton(fvm);
            p.add(m_fvmButton);
        } else {
            m_fvmButton = null;
        }

        m_fileComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                filenameChanged();            }
        });
        m_fileComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                filenameChanged();
            }
        });

        /* install action listeners */
        // set stuff to update preview when file location changes
        m_fileComboBox.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(final FocusEvent e) {
                filenameChanged();
            }
            /**
             * {@inheritDoc}
             */
            @Override
            public void focusGained(final FocusEvent e) {
                filenameChanged();
            }
        });
        final Component editor =
            m_fileComboBox.getEditor().getEditorComponent();
        if (editor instanceof JTextComponent) {
            final Document d = ((JTextComponent)editor).getDocument();
            d.addDocumentListener(new DocumentListener() {
                @Override
                public void changedUpdate(final DocumentEvent e) {
                    filenameChanged();
                }

                @Override
                public void insertUpdate(final DocumentEvent e) {
                    filenameChanged();
                }

                @Override
                public void removeUpdate(final DocumentEvent e) {
                    filenameChanged();
                }
            });
        }


        getModel().prependChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                updateComponent();
            }
        });

        //call this method to be in sync with the settings model
        updateComponent();
    }

    // called by all action/change listeners to transfer the new filename into
    // the settings model. (And ignore any invalid situations.)
    private void filenameChanged() {
        // transfer the new filename into the settings model
        try {
            clearError(m_fileComboBox);
            updateModel(true); // don't color the combobox red.
        } catch (final InvalidSettingsException ise) {
            // ignore it here.
        }
    }

    /**
     * Transfers the value from the component into the settings model.
     *
     * @param noColoring if set true, the component will not be marked red, even
     *            if the entered value was erroneous.
     * @throws InvalidSettingsException if the entered filename is null or
     *             empty.
     */
    private void updateModel(final boolean noColoring)
            throws InvalidSettingsException {

        final String file = m_fileComboBox.getEditor().getItem().toString();
        if ((file != null) && (file.trim().length() > 0)) {

            try {
                ((SettingsModelString)getModel()).setStringValue(file);
            } catch (final RuntimeException e) {
                // if value was not accepted by setter method
                if (!noColoring) {
                    showError(m_fileComboBox);
                }
                throw new InvalidSettingsException(e);
            }

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

        if (!getModel().isEnabled()) {
            // don't flag an error in disabled components.
            return;
        }
        final String selection = box.getEditor().getItem().toString();

        if ((selection == null) || (selection.length() == 0)) {
            box.setBackground(Color.RED);
        } else {
            box.setForeground(Color.RED);
        }
        box.requestFocusInWindow();

        // change the color back as soon as he changes something
        box.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                box.setForeground(DEFAULT_FG);
                box.setBackground(DEFAULT_BG);
                box.removeItemListener(this);
            }
        });
    }

    /**
     * Sets the coloring of the specified component back to normal.
     *
     * @param box the component to clear the error status for.
     */
    protected void clearError(final JComboBox box) {
        box.setForeground(DEFAULT_FG);
        box.setBackground(DEFAULT_BG);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateComponent() {

        clearError(m_fileComboBox);

        // update the component only if model and component are out of sync
        final SettingsModelString model = (SettingsModelString)getModel();
        final String newValue = model.getStringValue();
        boolean update;
        if (newValue == null) {
            update = (m_fileComboBox.getSelectedItem() != null);
        } else {
            final String file = m_fileComboBox.getEditor().getItem().toString();
            update = !newValue.equals(file);
        }
        if (update) {
            // to avoid multiply added items...
            m_fileComboBox.removeItem(newValue);
            m_fileComboBox.addItem(newValue);
            m_fileComboBox.setSelectedItem(newValue);
        }

        // also update the enable status
        setEnabledComponents(model.isEnabled());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsBeforeSave()
            throws InvalidSettingsException {
        // just in case we didn't get notified about the last change...
        updateModel(false); // mark the erroneous component red.
        // store the saved filename in the history
        m_fileHistory.add(((SettingsModelString)getModel()).getStringValue());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs)
            throws NotConfigurableException {
        // we're always good - independent of the incoming spec
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
     * {@inheritDoc}
     */
    @Override
    public void setToolTipText(final String text) {
        m_browseButton.setToolTipText(text);
        m_fileComboBox.setToolTipText(text);
    }

}
