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
 *   12.01.2005 (Florian Georg): created
 */
package de.unikn.knime.workbench.ui.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import de.unikn.knime.workbench.ui.KNIMEUIPlugin;

/**
 * This class represents a preference page that is contributed to the
 * Preferences dialog. By subclassing <samp>FieldEditorPreferencePage </samp>,
 * we can use the field support built into JFace that allows us to create a page
 * that is small and knows how to save, restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They are stored in the
 * preference store that belongs to the main plug-in class. That way,
 * preferences can be accessed directly via the preference store.
 * 
 * @author Florian Georg, University of Konstanz
 */
public class MainPreferencePage extends FieldEditorPreferencePage
        implements IWorkbenchPreferencePage {

    /**
     * Constructor
     * .
     */
    public MainPreferencePage() {
        super(GRID);

        // we use the pref store of the UI plugin
        setPreferenceStore(KNIMEUIPlugin.getDefault()
                .getPreferenceStore());

        setDescription("Konstanz Information Miner global preferences");
    }

    /**
     * Creates the field editors. Field editors are abstractions of the common
     * GUI blocks needed to manipulate various types of preferences. Each field
     * editor knows how to save and restore itself.
     */
    public void createFieldEditors() {
        Composite parent = getFieldEditorParent();

        // Path to datasets TODO: should be provided to the nodes?
        addField(new DirectoryFieldEditor(
                PreferenceConstants.P_DATAFILES_PATH,
                "Default &path to datasets:", parent));

        // Enable nodes that are flagged "experimental" ?
        // TODO: introduce this flag in the extension point
        addField(new BooleanFieldEditor(
                PreferenceConstants.P_FLAG_ENABLE_EXPERIMENTAL,
                "&Enable experimental stuff", parent));

        // Open views as JFrame or embedded in an eclipse view
        // TODO realize this !
        addField(new RadioGroupFieldEditor(
                PreferenceConstants.P_CHOICE_VIEWMODE,
                "Select the mode how &views are opened",
                1,
                new String[][] {
                        {
                                "External &JFrame",
                                PreferenceConstants.P_CHOICE_VIEWMODE_JFRAME},
                        {
                                "Eclipse &view (embedded)",
                                PreferenceConstants.P_CHOICE_VIEWMODE_VIEW}},
                parent));

        // Specify the minimum log level
        addField(new RadioGroupFieldEditor(
                PreferenceConstants.P_LOGLEVEL,
                "Minimum log-level",
                1,
                new String[][] {
                        {"&DEBUG",
                                PreferenceConstants.P_LOGLEVEL_DEBUG},

                        {"&INFO", PreferenceConstants.P_LOGLEVEL_INFO},

                        {"&WARN", PreferenceConstants.P_LOGLEVEL_WARN},

                        {"&ERROR",
                                PreferenceConstants.P_LOGLEVEL_ERROR}},
                parent));

        // Enter a pattern for formating the log messages
        addField(new StringFieldEditor(
                PreferenceConstants.P_PATTERN_LAYOUT,
                "Logging pattern layout (log4j):", parent));

        // redirect output to logfile
        addField(new BooleanFieldEditor(
                PreferenceConstants.P_FLAG_FILE_LOGGER,
                "&Redirect output to logfile", parent));

        // logfile name
        addField(new FileFieldEditor(
                PreferenceConstants.P_FILE_LOGGER_FILENAME,
                "&Logfile", parent));

    }

    /**
     * @see org.eclipse.ui.IWorkbenchPreferencePage
     *      #init(org.eclipse.ui.IWorkbench)
     */
    public void init(final IWorkbench workbench) {
        // ignore
    }

}
