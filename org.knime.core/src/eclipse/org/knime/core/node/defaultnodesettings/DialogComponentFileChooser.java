/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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

import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.FilesHistoryPanel;
import org.knime.core.node.util.FilesHistoryPanel.LocationValidation;

/**
 * A standard component allowing to choose a location(directory) and/or file
 * name.
 *
 * @author M. Berthold, University of Konstanz
 */
public class DialogComponentFileChooser extends DialogComponent {
    private final TitledBorder m_border;

    private final FilesHistoryPanel m_filesPanel;

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

        getComponentPanel().setLayout(new BoxLayout(getComponentPanel(), BoxLayout.X_AXIS));
        int selectionMode;
        LocationValidation locationValidation;
        if (directoryOnly) {
            selectionMode = JFileChooser.DIRECTORIES_ONLY;
            if (dialogType == JFileChooser.SAVE_DIALOG) {
                locationValidation = LocationValidation.DirectoryOutput;
            } else {
                locationValidation = LocationValidation.DirectoryInput;
            }
        } else {
            selectionMode = JFileChooser.FILES_AND_DIRECTORIES;
            if (dialogType == JFileChooser.SAVE_DIALOG) {
                locationValidation = LocationValidation.FileOutput;
            } else {
                locationValidation = LocationValidation.FileInput;
            }
        }

        m_filesPanel = new FilesHistoryPanel(fvm, historyID, locationValidation, validExtensions);
        m_filesPanel.setSelectMode(selectionMode);
        m_filesPanel.setDialogType(dialogType);
        m_filesPanel.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                try {
                    ((SettingsModelString)getModel()).setStringValue(m_filesPanel.getSelectedFile());
                } catch (Exception ex) {
                    NodeLogger.getLogger(DialogComponentFileChooser.class).error(
                        "Could not store selected file in settings: " + ex.getMessage(), ex);
                }
            }
        });


        final String title = directoryOnly ? "Selected Directory:" : "Selected File:";
        m_border = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), title);
        getComponentPanel().setBorder(m_border);
        getComponentPanel().setMaximumSize(new Dimension(Integer.MAX_VALUE, 74));
        getComponentPanel().add(m_filesPanel);
        getComponentPanel().add(Box.createHorizontalGlue());

        getModel().prependChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                updateComponent();
            }
        });

        //call this method to be in sync with the settings model
        updateComponent();
    }

    /**
     * Sets the coloring of the specified component back to normal.
     *
     * @param box the component to clear the error status for.
     * @deprecated This method does nothing any more
     */
    @Deprecated
    protected void clearError(final JComboBox<?> box) {
       // does nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateComponent() {
        // update the component only if model and component are out of sync
        final SettingsModelString model = (SettingsModelString)getModel();
        final String newValue = model.getStringValue();
        boolean update;
        if (newValue == null) {
            update = !m_filesPanel.getSelectedFile().isEmpty();
        } else {
            update = !newValue.equals(m_filesPanel.getSelectedFile());
        }
        if (update) {
            m_filesPanel.setSelectedFile(newValue);
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
        // nothing to validate, this component accepts all values in compliance with the noding guidelines
        m_filesPanel.addToHistory();
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
        m_filesPanel.setEnabled(enabled);
    }

    /**
     * Replaces the title displayed in the border that surrounds the editfield
     * and browse button with the specified new title. The default title of the
     * component is "Selected File:" or "Selected Directory:".
     *
     * @param newTitle the new title to display in the border.
     *
     * @throws IllegalArgumentException if the new title is <code>null</code>
     */
    public void setBorderTitle(final String newTitle) {
        if (newTitle == null) {
            throw new IllegalArgumentException("New title to display must not be null.");
        }
        m_border.setTitle(newTitle);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setToolTipText(final String text) {
        m_filesPanel.setToolTipText(text);
    }

    /**
     * Adds a change listener to the file choose that gets notified whenever the entered file name changes.
     *
     * @param cl a change listener
     * @since 2.11
     */
    public void addChangeListener(final ChangeListener cl) {
        m_filesPanel.addChangeListener(cl);
    }

    /**
     * Sets if this file panel should allow remote URLs. In case they are not allowed and the user enters a non-local
     * URL an error message is shown. The default is to allow remote URLs
     *
     * @param b <code>true</code> if remote URLs are allowed, <code>false</code> otherwise
     * @since 2.11
     */
    public void setAllowRemoteURLs(final boolean b) {
        m_filesPanel.setAllowRemoteURLs(b);
    }

    /**
     * Sets the dialog type to SAVE {@link JFileChooser#SAVE_DIALOG}, whereby it also forces the given file extension
     * when the user enters a path in the text field that does not end with either the argument extension or any
     * extension specified in the constructor (ignoring case).
     * Calling this method will overwrite the dialog type set in the constructor.
     *
     * @param forcedExtension optional parameter to force a file extension to be appended to the selected
     *        file name, e.g. ".txt" (<code>null</code> and an empty string do not force any extension)
     * @since 3.3
     */
    public void setDialogTypeSaveWithExtension(final String forcedExtension) {
        m_filesPanel.setDialogTypeSaveWithExtension(forcedExtension);
    }
}
