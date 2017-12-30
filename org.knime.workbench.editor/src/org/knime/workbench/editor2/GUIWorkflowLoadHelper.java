/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *
 */
package org.knime.workbench.editor2;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.workflow.Credentials;
import org.knime.core.node.workflow.FileWorkflowPersistor.LoadVersion;
import org.knime.core.node.workflow.WorkflowContext;
import org.knime.core.node.workflow.WorkflowLoadHelper;
import org.knime.core.util.Version;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.knime.workbench.ui.masterkey.CredentialVariablesDialog;
import org.knime.workbench.ui.preferences.PreferenceConstants;

/**
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
class GUIWorkflowLoadHelper extends WorkflowLoadHelper {

    private static final Version CURRENT_VERSION = new Version(KNIMEConstants.VERSION);

    private static final String LOAD_FUTURE_FLOW_TEXT =
        "If you choose to proceed you may have issues loading the workflow, it may miss some nodes or "
            + "connections, or node configurations may not be correct either now or when you save it later.\n\n"
            + "How do you want to proceed?";

    private final Display m_display;
    private final String m_workflowName;
    /** The updated(!) credentials. Stored as member so that modified workflow credentials can be passed into
     * credential quickform nodes (given they have the same identifier) - prevents multiple prompts for the 'same'
     * credentials key. It's indirectly related to AP-5974.
     */
    private final Map<String, Credentials> m_promptedCredentials = new HashMap<>();


    private static WorkflowContext createWorkflowContext(final URI uri, final File workflowDirectory,
        final File mountpointRoot) {
        if (workflowDirectory == null) {
            return null;
        } else {
            return new WorkflowContext.Factory(workflowDirectory)
                .setMountpointRoot(mountpointRoot)
                .setMountpointURI(uri)
                .createContext();
        }
    }

    /**
     * @param display Display host.
     * @param workflowName Name of the workflow (dialog title)
     * @param uri the workflow's URI from the explorer
     * @param workflowDirectory directory of the workflow that should be loaded; maybe <code>null</code> if not known
     * @param mountpointRoot root directory of the mount point in which the workflow to the loaded is contained; maybe
     *            <code>null</code> if not known
     */
    GUIWorkflowLoadHelper(final Display display, final String workflowName, final URI uri, final File workflowFile,
        final File mountpointRoot) {
        this(display, workflowName, uri, workflowFile, mountpointRoot, false);
    }

    /**
     * @param display Display host.
     * @param workflowName Name of the workflow (dialog title)
     * @param uri the workflow's URI from the explorer
     * @param workflowDirectory directory of the workflow that should be loaded; maybe <code>null</code> if not known
     * @param mountpointRoot root directory of the mount point in which the workflow to the loaded is contained; maybe
     *            <code>null</code> if not known
     * @param isTemplate Whether the loaded workflow is a reference to a template (don't load data)
     */
    GUIWorkflowLoadHelper(final Display display, final String workflowName, final URI uri, final File workflowDirectory,
        final File mountpointRoot, final boolean isTemplate) {
        super(isTemplate, createWorkflowContext(uri, workflowDirectory, mountpointRoot));
        m_display = display;
        m_workflowName = workflowName;
    }

    /** @return the display */
    public Display getDisplay() {
        return m_display;
    }

    /** {@inheritDoc} */
    @Override
    public List<Credentials> loadCredentials(final List<Credentials> credentialsList) {
        // set the ones that were already prompted for into the result list ... don't prompt them again
        final List<Credentials> newCredentialsList = new ArrayList<Credentials>();
        final List<Credentials> credentialsToBePromptedList = new ArrayList<Credentials>();
        for (Credentials c : credentialsList) {
            Credentials updatedCredentials = m_promptedCredentials.get(c.getName());
            if (updatedCredentials != null) {
                newCredentialsList.add(updatedCredentials);
            } else {
                credentialsToBePromptedList.add(c);
            }
        }
        // prompt for details for the credentials that haven't been prompted for
        if (!credentialsToBePromptedList.isEmpty()) {
            // run sync'ly in UI thread
            m_display.syncExec(new Runnable() {
                @Override
                public void run() {
                    CredentialVariablesDialog dialog = new CredentialVariablesDialog(
                        m_display.getActiveShell(), credentialsToBePromptedList, m_workflowName);
                    if (dialog.open() == Window.OK) {
                        List<Credentials> updateCredentialsList = dialog.getCredentials();
                        newCredentialsList.addAll(updateCredentialsList);
                        updateCredentialsList.stream().filter(c -> StringUtils.isNotEmpty(c.getPassword()))
                            .forEach(c -> m_promptedCredentials.put(c.getName(), c));
                    } else {
                        newCredentialsList.addAll(credentialsToBePromptedList);
                    }
                }
            });
        }
        return newCredentialsList;
    }

    @Override
    public UnknownKNIMEVersionLoadPolicy getUnknownKNIMEVersionLoadPolicy(final LoadVersion workflowKNIMEVersion,
        final Version createdByKNIMEVersion, final boolean isNightlyBuild) {

        final boolean isFuture = createdByKNIMEVersion != null && !CURRENT_VERSION.isSameOrNewer(createdByKNIMEVersion);
        assert isFuture || isNightlyBuild : "Only to be called for 'future' workflows or those created by a nightly";

        if (isNightlyBuild && !isFuture) {
            // nightly builds in the same or older version will show an info dialog with a toggle button
            return shouldLoadNightlyBuildWorkflow(workflowKNIMEVersion, createdByKNIMEVersion);
        } else {
            return shouldLoadFutureVersionWorkflow(workflowKNIMEVersion, createdByKNIMEVersion, isNightlyBuild);
        }
    }

    /** Called from {@link #getUnknownKNIMEVersionLoadPolicy(LoadVersion, Version, boolean)} when loading a
     * workflow created by a future version of KNIME AP. */
    private UnknownKNIMEVersionLoadPolicy shouldLoadFutureVersionWorkflow(final LoadVersion workflowKNIMEVersion,
        final Version createdByKNIMEVersion, final boolean isNightlyBuild) {
        StringBuilder e = new StringBuilder("Your version of KNIME Analytics Platform (");
        e.append(CURRENT_VERSION);
        e.append(") version does not support reading workflows created by a newer release");
        e.append(" of KNIME Analytics Platform (");
        e.append(createdByKNIMEVersion);
        if (isNightlyBuild) {
            e.append("-nightly");
        }
        e.append(").\n\n");

        e.append("You should upgrade to the latest version of KNIME Analytics Platform to open this workflow.\n\n");
        e.append(LOAD_FUTURE_FLOW_TEXT);

        String cancel = "&Cancel";
        String loadAnyway = "&Load Anyway";
        final String[] labels = new String[] {cancel, loadAnyway};
        final AtomicReference<UnknownKNIMEVersionLoadPolicy> result =
                new AtomicReference<>(UnknownKNIMEVersionLoadPolicy.Abort);
        m_display.syncExec(new Runnable() {
            @Override
            public void run() {
                MessageDialog dialog = new MessageDialog(m_display.getActiveShell(),
                    "Workflow version not supported by KNIME Analytics Platform version",
                    null, e.toString(), MessageDialog.WARNING, labels, 0);
                switch (dialog.open()) {
                    case 0:           // 0 button: Cancel
                    case SWT.DEFAULT: // cancel by 'esc'
                        result.set(UnknownKNIMEVersionLoadPolicy.Abort);
                        break;
                    default:
                        result.set(UnknownKNIMEVersionLoadPolicy.Try);
                }
            }
        });
        return result.get();
    }

    /** Called from {@link #getUnknownKNIMEVersionLoadPolicy(LoadVersion, Version, boolean)} when loading a
     * workflow created by a current or old nightly build version. Requires special treatment as per AP-8642. */
    private UnknownKNIMEVersionLoadPolicy shouldLoadNightlyBuildWorkflow(final LoadVersion workflowKNIMEVersion,
        final Version createdByKNIMEVersion) {
        final IPreferenceStore store = KNIMEUIPlugin.getDefault().getPreferenceStore();
        boolean confirmLoadNightlyBuild = store.getBoolean(PreferenceConstants.P_CONFIRM_LOAD_NIGHTLY_BUILD_WORKFLOW);
        if (!confirmLoadNightlyBuild) {
            return UnknownKNIMEVersionLoadPolicy.Try;
        }

        StringBuilder e = new StringBuilder("You are attempting to load a workflow created with a nightly build (");
        if (createdByKNIMEVersion != null) {
            e.append(createdByKNIMEVersion);
        } else {
            e.append("<unknown>");
        }
        e.append("-nightly). This can lead to issues with the officially released version of some nodes.\n\n");
        e.append(LOAD_FUTURE_FLOW_TEXT);

        // must not change to something else than this as MessageDialogWithToggle has some special magic to re-index
        // buttons -- see org.eclipse.jface.dialogs.MessageDialogWithToggle.createButtonsForButtonBar(Composite)
        // must use jface button label as per org.eclipse.jface.dialogs.MessageDialogWithToggle.mapButtonLabelToButtonID
        // (this is very different to the behavior of MessageDialog(NoToggle))
        String cancel = IDialogConstants.CANCEL_LABEL;
        String loadAnyway = "&Load Anyway";
        final String[] labels = new String[] {cancel, loadAnyway};
        final AtomicReference<UnknownKNIMEVersionLoadPolicy> result =
                new AtomicReference<>(UnknownKNIMEVersionLoadPolicy.Abort);
        m_display.syncExec(new Runnable() {
            @Override
            public void run() {
                MessageDialogWithToggle dialog = new MessageDialogWithToggle(m_display.getActiveShell(),
                    "Loading Workflow created by a Nightly Build",
                    null, e.toString(), MessageDialog.WARNING, labels, 0,
                    "Always load workflows created by a Nightly Build", false);
                switch (dialog.open()) {
                    case IDialogConstants.CANCEL_ID:   // Cancel button
                    case SWT.DEFAULT:                  // cancel by 'esc'
                        result.set(UnknownKNIMEVersionLoadPolicy.Abort);
                        break;
                    default:
                        result.set(UnknownKNIMEVersionLoadPolicy.Try);
                        if (dialog.getToggleState()) {
                            store.setValue(PreferenceConstants.P_CONFIRM_LOAD_NIGHTLY_BUILD_WORKFLOW, false);
                            KNIMEUIPlugin.getDefault().savePluginPreferences();
                        }
                }
            }
        });
        return result.get();
    }
}
