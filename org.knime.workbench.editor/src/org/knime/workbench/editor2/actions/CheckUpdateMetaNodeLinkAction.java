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
 * ---------------------------------------------------------------------
 *
 * History
 *   12.05.2010 (Bernd Wiswedel): created
 */
package org.knime.workbench.editor2.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.MetaNodeTemplateInformation;
import org.knime.core.node.workflow.MetaNodeTemplateInformation.Role;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowLoadHelper;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.KNIMEEditorPlugin;
import org.knime.workbench.editor2.ImageRepository;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.commands.UpdateMetaNodeLinkCommand;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;

/**
 * Action to check for updates of meta node templates.
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public class CheckUpdateMetaNodeLinkAction extends AbstractNodeAction {

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(CheckUpdateMetaNodeLinkAction.class);

    private final boolean m_showInfoMsgIfNoUpdateAvail;

    /** Action ID. */
    public static final String ID = "knime.action.meta_node_check_update_link";

    /** Create new action based on given editor.
     * @param editor The associated editor. */
    public CheckUpdateMetaNodeLinkAction(final WorkflowEditor editor) {
        this(editor, true);
    }

    /** Create new action based on given editor.
     * @param editor The associated editor.
     * @param showInfoMsgIfNoUpdateAvail If to show an info box if no
     * updates are available, true if this is a manually triggered command,
     * false if is run as automatic procedure after load (no user interaction)
     */
    public CheckUpdateMetaNodeLinkAction(final WorkflowEditor editor,
            final boolean showInfoMsgIfNoUpdateAvail) {
        super(editor);
        m_showInfoMsgIfNoUpdateAvail = showInfoMsgIfNoUpdateAvail;
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return ID;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public String getText() {
        return "Update Meta Node Link";
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText() {
        return "Checks whether a newer version of the underlying meta node "
            + "template is available and updates the selected links";
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository.getImageDescriptor(
                "icons/meta/metanode_link_update.png");
    }

    /**
     * @return true, if underlying model instance of
     *         <code>WorkflowManager</code>, otherwise false
     */
    @Override
    protected boolean calculateEnabled() {
        if (getManager().isWriteProtected()) {
            return false;
        }
        NodeContainerEditPart[] nodes =
            getSelectedParts(NodeContainerEditPart.class);
        boolean containsTemplate = false;
        for (NodeContainerEditPart p : nodes) {
            Object model = p.getModel();
            if (model instanceof WorkflowManager) {
                WorkflowManager wm = (WorkflowManager)model;
                if (wm.getTemplateInformation().getRole().equals(Role.Link)) {
                    containsTemplate = true;
                    if (!getManager().canUpdateMetaNodeLink(wm.getID())) {
                        return false;
                    }
                }
            }
        }
        return containsTemplate;
    }

    /** {@inheritDoc} */
    @Override
    public void runOnNodes(final NodeContainerEditPart[] nodes) {
        List<NodeID> candidateList = new ArrayList<NodeID>();
        for (NodeContainerEditPart p : nodes) {
            Object model = p.getModel();
            if (model instanceof WorkflowManager) {
                WorkflowManager wm = (WorkflowManager)model;
                MetaNodeTemplateInformation i = wm.getTemplateInformation();
                if (Role.Link.equals(i.getRole())) {
                    candidateList.add(wm.getID());
                }
            }
        }
        final Shell shell = Display.getCurrent().getActiveShell();
        IWorkbench wb = PlatformUI.getWorkbench();
        IProgressService ps = wb.getProgressService();
        LOGGER.debug("Checking for updates for " + candidateList.size()
                + " meta node link(s)...");
        CheckUpdateRunnableWithProgress runner =
            new CheckUpdateRunnableWithProgress(getManager(), candidateList);
        try {
            ps.busyCursorWhile(runner);
        } catch (InvocationTargetException e) {
            LOGGER.warn("Failed to check for updates: " + e.getMessage(), e);
            return;
        } catch (InterruptedException e) {
            return;
        }
        List<NodeID> updateList = runner.getUpdateList();
        Status status = runner.getStatus();
        if (status.getSeverity() == Status.ERROR
                || status.getSeverity() == Status.WARNING) {
            ErrorDialog.openError(
                    Display.getDefault().getActiveShell(),
                    null, "Errors while checking for "
                    + "updates on meta node links", status);
            if (candidateList.size() == 1) {
                /* As only one meta node is selected and its update failed,
                 * there is nothing else to do. */
                return;
            }
        }

        // find nodes that will be reset as part of the update
        int metaNodesToResetCount = 0;
        for (NodeID id : updateList) {
            WorkflowManager metaNode =
                (WorkflowManager)getManager().getNodeContainer(id);
            // TODO problematic with through-connections
            if (metaNode.containsExecutedNode()) {
                metaNodesToResetCount += 1;
            }
        }

        if (updateList.isEmpty()) {
            if (m_showInfoMsgIfNoUpdateAvail) {
                MessageDialog.openInformation(shell, "Meta Node Update",
                        "No updates available");
            } else {
                LOGGER.info("No updates available ("
                        + candidateList.size() + " meta node link(s))");
            }
        } else {
            boolean isSingle = updateList.size() == 1;
            String title = "Update Meta Node" + (isSingle ? "" : "s");
            StringBuilder messageBuilder = new StringBuilder();
            messageBuilder.append("Update available for ");
            if (isSingle && nodes.length == 1) {
                messageBuilder.append("meta node \"");
                WorkflowManager wm = (WorkflowManager)nodes[0].getModel();
                messageBuilder.append(wm.getNameWithID());
                messageBuilder.append("\".");
            } else if (isSingle) {
                messageBuilder.append("one meta node.");
            } else {
                messageBuilder.append(updateList.size());
                messageBuilder.append(" meta nodes.");
            }
            messageBuilder.append("\n\n");
            if (metaNodesToResetCount > 0) {
                messageBuilder.append("Reset nodes and update now?");
            } else {
                messageBuilder.append("Update now?");
            }
            String message = messageBuilder.toString();
            if (MessageDialog.openQuestion(shell, title, message)) {
                LOGGER.debug("Running update for " + updateList.size()
                        + " meta node(s): " + updateList);
                execute(new UpdateMetaNodeLinkCommand(getManager(),
                        updateList.toArray(new NodeID[updateList.size()])));
            }
        }
    }

    private static final class CheckUpdateRunnableWithProgress
        implements IRunnableWithProgress {

        private final WorkflowManager m_hostWFM;
        private final List<NodeID> m_candidateList;
        private final List<NodeID> m_updateList;
        private Status m_status;

        /**
         * @param hostWFM
         * @param candidateList */
        public CheckUpdateRunnableWithProgress(final WorkflowManager hostWFM,
                final List<NodeID> candidateList) {
            m_hostWFM = hostWFM;
            m_candidateList = candidateList;
            m_updateList = new ArrayList<NodeID>();
        }

        /** {@inheritDoc} */
        @Override
        public void run(final IProgressMonitor monitor)
                throws InvocationTargetException, InterruptedException {
            monitor.beginTask("Checking Link Updates", m_candidateList.size());
            WorkflowLoadHelper lH = new WorkflowLoadHelper(true);
            final String idName = KNIMEEditorPlugin.PLUGIN_ID;
            Status[] stats = new Status[m_candidateList.size()];
            for (int i = 0; i < m_candidateList.size(); i++) {
                NodeID id = m_candidateList.get(i);
                WorkflowManager wm =
                    (WorkflowManager)m_hostWFM.getNodeContainer(id);
                monitor.subTask(wm.getNameWithID());
                Status stat;
                try {
                    String msg;
                    if (m_hostWFM.checkUpdateMetaNodeLink(id, lH)) {
                        m_updateList.add(id);
                        msg = "Update available for " + wm.getNameWithID();
                    } else {
                        msg = "No update available for " + wm.getNameWithID();
                    }
                    stat = new Status(Status.OK, idName, msg);
                } catch (Exception ex) {
                    String msg = "Unable to check for update on meta "
                        + "node \"" + wm.getNameWithID() + "\": "
                        + ex.getMessage();
                    LOGGER.warn(msg, ex);
                    stat = new Status(Status.WARNING , idName, msg, ex);
                }
                if (monitor.isCanceled()) {
                    throw new InterruptedException("Update check canceled");
                }
                stats[i] = stat;
                monitor.worked(1);
            }
            m_status = new MultiStatus(
                    idName, Status.OK, stats, "Meta Node Link Updates", null);
            monitor.done();
        }

        /** @return the updateList */
        public List<NodeID> getUpdateList() {
            return m_updateList;
        }

        /** @return the status */
        public Status getStatus() {
            return m_status;
        }
    }

}
