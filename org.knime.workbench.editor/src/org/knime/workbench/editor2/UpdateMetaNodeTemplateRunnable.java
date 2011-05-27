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
 *   13.04.2011 (wiswedel): created
 */
package org.knime.workbench.editor2;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.widgets.Display;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResultEntry.LoadResultEntryType;
import org.knime.core.node.workflow.WorkflowPersistor.MetaNodeLinkUpdateResult;


/**
 * Runnable used to update a single meta node template link.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public class UpdateMetaNodeTemplateRunnable extends PersistWorkflowRunnable {

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(UpdateMetaNodeTemplateRunnable.class);

    /** The host WFM. */
    private WorkflowManager m_parentWFM;
    /** The IDs of the meta node links to be updated. */
    private NodeID[] m_ids;
    /** The IDs of the meta nodes after update (very very likely correspond
     * to m_ids except in case of errors. */
    private List<NodeID> m_newIDs;
    /** The undo persistors of the previously deleted meta nodes. */
    private List<WorkflowPersistor> m_undoPersistors;

    /**
     * @param wfm target workflow (where to insert)
     * @param ids The ID of the meta node to update
     */
    public UpdateMetaNodeTemplateRunnable(final WorkflowManager wfm,
            final NodeID[] ids) {
        m_parentWFM = wfm;
        m_ids = ids;
    }

    /** {@inheritDoc} */
    @Override
    public void run(final IProgressMonitor pm) throws InterruptedException {
        m_newIDs = new ArrayList<NodeID>();
        m_undoPersistors = new ArrayList<WorkflowPersistor>();
        // create progress monitor
        ProgressHandler progressHandler =
            new ProgressHandler(pm, 101, "Updating meta node links...");
        final CheckCancelNodeProgressMonitor progressMonitor
        = new CheckCancelNodeProgressMonitor(pm);
        progressMonitor.addProgressListener(progressHandler);
        final Display d = Display.getDefault();
        ExecutionMonitor exec = new ExecutionMonitor(progressMonitor);
        IStatus[] stats = new IStatus[m_ids.length];
        for (int i = 0; i < m_ids.length; i++) {
            NodeID id = m_ids[i];
            NodeContainer nc = m_parentWFM.getNodeContainer(id);
            ExecutionMonitor subExec =
                exec.createSubProgress(1.0 / m_ids.length);
            String progMsg = "Meta Node Link \"" + nc.getNameWithID() + "\"";
            exec.setMessage(progMsg);
            GUIWorkflowLoadHelper loadHelper =
                new GUIWorkflowLoadHelper(d, progMsg, true);
            MetaNodeLinkUpdateResult updateMetaNodeLinkResult;
            try {
                updateMetaNodeLinkResult =
                    m_parentWFM.updateMetaNodeLink(id, subExec, loadHelper);
            } catch (CanceledExecutionException e) {
                String message = "Meta node update canceled";
                LOGGER.warn(message, e);
                throw new InterruptedException(message);
            }
            WorkflowPersistor p = updateMetaNodeLinkResult.getUndoPersistor();
            if (p != null) { // no error
                m_newIDs.add(updateMetaNodeLinkResult.getMetaNode().getID());
                m_undoPersistors.add(p);
            }
            // meta nodes don't have data
            // data load errors are unexpected but OK
            IStatus status = createStatus(updateMetaNodeLinkResult, true);
            subExec.setProgress(1.0);
            switch (status.getSeverity()) {
            case IStatus.OK:
                break;
            case IStatus.WARNING:
                logPreseveLineBreaks("Warnings during load: "
                        + updateMetaNodeLinkResult.getFilteredError(
                                "", LoadResultEntryType.Warning), false);
                break;
            default:
                logPreseveLineBreaks("Errors during load: "
                        + updateMetaNodeLinkResult.getFilteredError(
                                "", LoadResultEntryType.Warning), true);
            }
            stats[i] = status;
        }
        pm.done();
        final IStatus status =
            createMultiStatus("Update meta node links", stats);
        final String message;
        switch (status.getSeverity()) {
        case IStatus.OK:
            message = "No problems during meta node link update.";
            break;
        case IStatus.WARNING:
            message = "Warnings during meta node link update";
            break;
        default:
            message = "Errors during meta node link update";
        }

        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                // will not open if status is OK.
                ErrorDialog.openError(
                        Display.getDefault().getActiveShell(),
                        "Update Meta Node Links", message, status);
            }
        });
    }

    /** @return the newIDs */
    public List<NodeID> getNewIDs() {
        return m_newIDs;
    }

    /** @return the undoPersistors */
    public List<WorkflowPersistor> getUndoPersistors() {
        return m_undoPersistors;
    }

    /** Set fields to null so that they can get GC'ed. */
    public void discard() {
        m_parentWFM = null;
        m_ids = null;
        m_newIDs = null;
        m_undoPersistors = null;
    }

}

