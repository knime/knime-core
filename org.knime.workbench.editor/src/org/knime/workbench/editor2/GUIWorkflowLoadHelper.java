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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 */
package org.knime.workbench.editor2;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.knime.core.node.workflow.Credentials;
import org.knime.core.node.workflow.WorkflowLoadHelper.DefaultWorkflowLoadHelper;
import org.knime.workbench.ui.masterkey.CredentialVariablesDialog;

/**
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
final class GUIWorkflowLoadHelper extends DefaultWorkflowLoadHelper {

    private final Display m_display;
    private final String m_workflowName;
    private final boolean m_isTemplateFlow;

    /**
     * @param display Display host.
     * @param workflowName Name of the workflow (dialog title)
     * @param isTemplateFlow Whether flow is a
     *        {@link #isTemplateFlow() template flow}.
     */
    GUIWorkflowLoadHelper(final Display display, final String workflowName,
            final boolean isTemplateFlow) {
        m_display = display;
        m_workflowName = workflowName;
        m_isTemplateFlow = isTemplateFlow;
    }

    /** {@inheritDoc} */
    @Override
    public List<Credentials> loadCredentials(
            final List<Credentials> credentials) {
        final List<Credentials> newCredentials =
            new ArrayList<Credentials>();
        // run sync'ly in UI thread
        m_display.syncExec(new Runnable() {
            @Override
            public void run() {
                CredentialVariablesDialog dialog =
                    new CredentialVariablesDialog(m_display.getActiveShell(),
                            credentials, m_workflowName);
                if (dialog.open() == Dialog.OK) {
                    newCredentials.addAll(
                            dialog.getCredentials());
                } else {
                    newCredentials.addAll(credentials);
                }
            }
        });
        return newCredentials;
    }

    /** {@inheritDoc} */
    @Override
    public UnknownKNIMEVersionLoadPolicy getUnknownKNIMEVersionLoadPolicy(
            final String workflowVersionString) {
        final String message =
            "You are trying to load a workflow with an unknown "
            + "version: " + workflowVersionString + "\n\n"
            + "How do you want to proceed?";
        String tryAnyway = "&Try Anyway";
        String cancel = "&Cancel";
        final String[] labels = new String[] {tryAnyway, cancel};
        final AtomicReference<UnknownKNIMEVersionLoadPolicy> result =
            new AtomicReference<UnknownKNIMEVersionLoadPolicy>(
                    UnknownKNIMEVersionLoadPolicy.Abort);
        m_display.syncExec(new Runnable() {
            @Override
            public void run() {
                MessageDialog dialog = new MessageDialog(
                        m_display.getActiveShell(), "Unknown workflow version",
                        null, message, MessageDialog.WARNING, labels, 0);
                if (dialog.open() == 0) {
                    result.set(UnknownKNIMEVersionLoadPolicy.Try);
                } else {
                    result.set(UnknownKNIMEVersionLoadPolicy.Abort);
                }
            }
        });
        return result.get();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isTemplateFlow() {
        return m_isTemplateFlow;
    }

}
