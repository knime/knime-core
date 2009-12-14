/* This source code, its documentation and all appendant files
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
 */
package org.knime.workbench.ui.preferences;

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
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.NodeLogger.LEVEL;
import org.knime.workbench.core.KNIMECorePlugin;
import org.knime.workbench.core.preferences.HeadlessPreferencesConstants;

/**
 *
 * @author Fabian Dill, University of Konstanz
 */
public class HeadlessPreferencePage extends FieldEditorPreferencePage implements
        IWorkbenchPreferencePage {

    private boolean m_apply = false;

    private String m_tempPath;

    /**
     *
     */
    public HeadlessPreferencePage() {
        super(GRID);

        // setDescription("KNIME global preferences");

        // get the preference store for the UI plugin
        IPreferenceStore store =
                KNIMECorePlugin.getDefault().getPreferenceStore();
        m_tempPath = store.getString(HeadlessPreferencesConstants.P_TEMP_DIR);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void createFieldEditors() {
        Composite parent = getFieldEditorParent();

        // Specify the minimum log level for log file
        addField(new RadioGroupFieldEditor(
                HeadlessPreferencesConstants.P_LOGLEVEL_LOG_FILE,
                "Log File Log Level",
                4, new String[][] {
                        {"&DEBUG", LEVEL.DEBUG.name()},

                        {"&INFO", LEVEL.INFO.name()},

                        {"&WARN", LEVEL.WARN.name()},

                        {"&ERROR", LEVEL.ERROR.name()} },
                parent));

        // number threads
        IntegerFieldEditor maxThreadEditor = new IntegerFieldEditor(
                HeadlessPreferencesConstants.P_MAXIMUM_THREADS,
                "Maximum working threads for all nodes", parent, 3);
        maxThreadEditor.setValidRange(1, Math.max(100, Runtime.getRuntime()
                .availableProcessors() * 4));
        maxThreadEditor.setTextLimit(3);
        addField(maxThreadEditor);


        // temp dir
        DirectoryFieldEditor tempDirEditor = new TempDirFieldEditor(
                HeadlessPreferencesConstants.P_TEMP_DIR,
                "Directory for temporary files\n(you should restart KNIME after"
                        + " changing this value)", parent);
        tempDirEditor.setEmptyStringAllowed(false);

        addField(tempDirEditor);

    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected void performApply() {
        m_apply = true;
        super.performApply();
    }

    /**
     * Overriden to display a message box in case the temp directory was
     * changed.
     *
     * {@inheritDoc}
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
     * {@inheritDoc}
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
        IPreferenceStore store =
                KNIMECorePlugin.getDefault().getPreferenceStore();
        String currentTmpDir =
                store.getString(HeadlessPreferencesConstants.P_TEMP_DIR);
        boolean tempDirChanged = !m_tempPath.equals(currentTmpDir);
        if (tempDirChanged) {

            // reset the directory
            m_tempPath = currentTmpDir;
            MessageBox mb =
                    new MessageBox(Display.getDefault().getActiveShell(),
                            SWT.ICON_QUESTION | SWT.YES | SWT.NO);
            mb.setText("Restart workbench...");
            mb.setMessage("Changes of the temporary directory become "
                    + "first available after restarting the workbench.\n"
                    + "Do you want to restart the workbench now?");
            if (mb.open() != SWT.YES) {
                return;
            }

            PlatformUI.getWorkbench().restart();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(final IWorkbench workbench) {
        // we use the pref store of the UI plugin
        setPreferenceStore(KNIMECorePlugin.getDefault()
                .getPreferenceStore());
    }

}
