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
 * ------------------------------------------------------------------------
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

    private BooleanFieldEditor m_logDirGLobal;

    /**
     *
     */
    public HeadlessPreferencePage() {
        super(GRID);

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

        addField(new BooleanFieldEditor(HeadlessPreferencesConstants.P_LOG_FILE_LOCATION,
            "Enable per workflow logs", parent));
        m_logDirGLobal = new BooleanFieldEditor(HeadlessPreferencesConstants.P_LOG_GLOBAL_IN_WF_DIR,
            "Log global messages also to workflow log", parent);
        addField(m_logDirGLobal);

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

        addField(new HorizontalLineField(parent));
        addField(new LabelField(parent, "Improve KNIME", SWT.BOLD));
        addField(new LabelField(parent, "Help us improve KNIME by sending us anonymous usage data. Absolutely no other information is sent."));
        BooleanFieldEditor sendAnonymousStatisticsEditor =
            new BooleanFieldEditor(HeadlessPreferencesConstants.P_SEND_ANONYMOUS_STATISTICS,
                "Yes, help improve KNIME.", parent);
        addField(sendAnonymousStatisticsEditor);

    }
    //TK_TODO: Enable disable the global messages in wf dir option depending on the wf option
//
//    /**
//     * {@inheritDoc}
//     */
//    @Override
//    public void propertyChange(final PropertyChangeEvent event) {
//        if (HeadlessPreferencesConstants.P_LOG_FILE_LOCATION.equals(event.getProperty())) {
//            final Boolean enabled = (Boolean)event.getNewValue();
//            m_logDirGLobal.setEnabled(enabled, getFieldEditorParent());
//        }
//        super.propertyChange(event);
//    }

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
