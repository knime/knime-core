/*  
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 *   12.01.2005 (Florian Georg): created
 */
package org.knime.workbench.ui.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.internal.Workbench;
import org.knime.workbench.ui.KNIMEUIPlugin;

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
public class MainPreferencePage extends FieldEditorPreferencePage implements
        IWorkbenchPreferencePage {

    boolean m_apply = false;

    String m_tempPath;

    /**
     * Constructor .
     */
    public MainPreferencePage() {
        super(GRID);

        // we use the pref store of the UI plugin
        setPreferenceStore(KNIMEUIPlugin.getDefault().getPreferenceStore());

        setDescription("KNIME global preferences");

        // get the preference store for the UI plugin
        IPreferenceStore store = KNIMEUIPlugin.getDefault()
                .getPreferenceStore();
        m_tempPath = store.getString(PreferenceConstants.P_TEMP_DIR);
    }

    @Override
    protected void performApply() {
        m_apply = true;
        super.performApply();
    }

    /**
     * Overriden to display a message box in case the temp directory was
     * changed.
     * 
     * @see org.eclipse.jface.preference.FieldEditorPreferencePage#performOk()
     */
    @Override
    public boolean performOk() {

        boolean result = super.performOk();

        checkChanges();

        return result;
    }

    /**
     * Overriden to react when the users applies but then presses cancel.
     * 
     * @see org.eclipse.jface.preference.PreferencePage#performCancel()
     */
    @Override
    public boolean performCancel() {
        boolean result = super.performCancel();

        checkChanges();

        return result;
    }

    private void checkChanges() {
        boolean apply = m_apply;
        m_apply = false;

        if (apply) {
            return;
        }

        // get the preference store for the UI plugin
        IPreferenceStore store = KNIMEUIPlugin.getDefault()
                .getPreferenceStore();
        String currentTmpDir = store.getString(PreferenceConstants.P_TEMP_DIR);
        boolean tempDirChanged = !m_tempPath.equals(currentTmpDir);
        if (tempDirChanged) {

            // reset the directory
            m_tempPath = currentTmpDir;
            MessageBox mb = new MessageBox(Display.getDefault()
                    .getActiveShell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO);
            mb.setText("Restart workbench...");
            mb.setMessage("Changes of the temporary directory become "
                    + "first available after restarting the workbench.\n"
                    + "Do you want to restart the workbench now?");
            if (mb.open() != SWT.YES) {
                return;
            }

            Workbench.getInstance().restart();
        }
    }

    /**
     * Creates the field editors. Field editors are abstractions of the common
     * GUI blocks needed to manipulate various types of preferences. Each field
     * editor knows how to save and restore itself.
     */
    @Override
    public void createFieldEditors() {
        Composite parent = getFieldEditorParent();

        // Open views as JFrame or embedded in an eclipse view
        /* BW: disabled this feature as the eclipse embedded views do not
         * have a menu associated. 
         * NOTE: If you enable this option again, also undo my changes
         * in WorkflowContextMenuProvider*/
//        addField(new RadioGroupFieldEditor(
//                PreferenceConstants.P_CHOICE_VIEWMODE,
//                "Select the mode how &views are opened", 1, new String[][] {
//                        { "External &JFrame",
//                          PreferenceConstants.P_CHOICE_VIEWMODE_JFRAME },
//                        { "Eclipse &view (embedded)",
//                          PreferenceConstants.P_CHOICE_VIEWMODE_VIEW } },
//                parent));

        // Specify the minimum log level for the console
        addField(new RadioGroupFieldEditor(
                PreferenceConstants.P_LOGLEVEL_CONSOLE, "Console Log Level", 4,
                new String[][] {
                        { "&DEBUG", PreferenceConstants.P_LOGLEVEL_DEBUG },

                        { "&INFO", PreferenceConstants.P_LOGLEVEL_INFO },

                        { "&WARN", PreferenceConstants.P_LOGLEVEL_WARN },

                        { "&ERROR", PreferenceConstants.P_LOGLEVEL_ERROR } },
                parent));

        // Specify the minimum log level
        addField(new RadioGroupFieldEditor(
                PreferenceConstants.P_LOGLEVEL_LOG_FILE, "Log File Log Level",
                4, new String[][] {
                        { "&DEBUG", PreferenceConstants.P_LOGLEVEL_DEBUG },

                        { "&INFO", PreferenceConstants.P_LOGLEVEL_INFO },

                        { "&WARN", PreferenceConstants.P_LOGLEVEL_WARN },

                        { "&ERROR", PreferenceConstants.P_LOGLEVEL_ERROR } },
                parent));

        IntegerFieldEditor maxThreadEditor = new IntegerFieldEditor(
                PreferenceConstants.P_MAXIMUM_THREADS,
                "Maximum working threads for all nodes", parent, 3);
        maxThreadEditor.setValidRange(1, Math.max(100, Runtime.getRuntime()
                .availableProcessors() * 4));
        maxThreadEditor.setTextLimit(3);
        addField(maxThreadEditor);

        DirectoryFieldEditor tempDirEditor = new TempDirFieldEditor(
                PreferenceConstants.P_TEMP_DIR,
                "Directory for temporary files\n(you should restart KNIME after"
                        + " changing this value)", parent);
        tempDirEditor.setEmptyStringAllowed(false);
        
        addField(tempDirEditor);

        addField(new BooleanFieldEditor(PreferenceConstants.P_CONFIRM_RESET, 
                "Confirm Node Reset", parent));

        addField(new BooleanFieldEditor(PreferenceConstants.P_CONFIRM_DELETE, 
                "Confirm Node/Connection Deletion", parent));
    }

    /**
     * {@inheritDoc}
     */
    public void init(final IWorkbench workbench) {
        // ignore
    }
}
