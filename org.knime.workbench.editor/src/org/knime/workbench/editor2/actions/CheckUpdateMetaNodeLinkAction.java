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
 * ---------------------------------------------------------------------
 *
 * History
 *   12.05.2010 (Bernd Wiswedel): created
 */
package org.knime.workbench.editor2.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
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
import org.knime.core.def.node.workflow.IWorkflowManager;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.MetaNodeTemplateInformation.Role;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContainerTemplate;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowLoadHelper;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.KNIMEEditorPlugin;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.commands.UpdateMetaNodeLinkCommand;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;

/**
 * Action to check for updates of metanode templates.
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
        return "Update Link\t" + getHotkey("knime.commands.updateMetaNodeLink");
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText() {
        return "Checks whether a newer version of the underlying node "
            + "template is available and updates the selected links";
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository.getIconDescriptor(KNIMEEditorPlugin.PLUGIN_ID, "icons/meta/metanode_link_update.png");
    }

    /**
     * @return true, if underlying model instance of
     *         <code>WorkflowManager</code>, otherwise false
     */
    @Override
    protected boolean internalCalculateEnabled() {
        if (getManager().isWriteProtected()) {
            return false;
        }
        return !getMetaNodesToCheck().isEmpty();
    }

    protected List<NodeID> getMetaNodesToCheck() {
        List<NodeID> list = new ArrayList<NodeID>();
        for (NodeContainerEditPart p : getSelectedParts(NodeContainerEditPart.class)) {
            Object model = p.getModel();
            if (model instanceof NodeContainerTemplate) {
                NodeContainerTemplate tnc = (NodeContainerTemplate)model;
                if (tnc.getTemplateInformation().getRole().equals(Role.Link)) {
                    if (!getManager().canUpdateMetaNodeLink(tnc.getID())) {
                        return Collections.emptyList();
                    }
                    list.add(tnc.getID());
                }
                list.addAll(getNCTemplatesToCheck(tnc));
            }
        }
        return list;
    }

    private List<NodeID> getNCTemplatesToCheck(final NodeContainerTemplate template) {
        List<NodeID> list = new ArrayList<NodeID>();
        for (NodeContainer nc : template.getNodeContainers()) {
            if (nc instanceof NodeContainerTemplate) {
                NodeContainerTemplate tnc = (NodeContainerTemplate)nc;
                if (tnc.getTemplateInformation().getRole().equals(Role.Link)) {
                    if (!getManager().canUpdateMetaNodeLink(tnc.getID())) {
                        return Collections.emptyList();
                    }
                    list.add(tnc.getID());
                }
                list.addAll(getNCTemplatesToCheck(tnc));
            }
        }
        return list;
    }

    /** {@inheritDoc} */
    @Override
    public void runOnNodes(final NodeContainerEditPart[] nodes) {
        throw new IllegalStateException("Not to be called");
    }

    /** {@inheritDoc} */
    @Override
    public void runInSWT() {
        List<NodeID> candidateList = getMetaNodesToCheck();
        final Shell shell = Display.getCurrent().getActiveShell();
        IWorkbench wb = PlatformUI.getWorkbench();
        IProgressService ps = wb.getProgressService();
        LOGGER.debug("Checking for updates for " + candidateList.size()
                + " node link(s)...");
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
        if (status.getSeverity() == IStatus.ERROR
                || status.getSeverity() == IStatus.WARNING) {
            ErrorDialog.openError(Display.getDefault().getActiveShell(),
                    null, "Errors while checking for "
                    + "updates on node links", status);
            if (candidateList.size() == 1) {
                /* As only one node is selected and its update failed,
                 * there is nothing else to do. */
                return;
            }
        }

        // find nodes that will be reset as part of the update
        int nodesToResetCount = 0;
        for (NodeID id : updateList) {
            NodeContainerTemplate templateNode =
                (NodeContainerTemplate)getManager().findNodeContainer(id);
            // TODO problematic with through-connections
            if (templateNode.containsExecutedNode()) {
                nodesToResetCount += 1;
            }
        }

        if (updateList.isEmpty()) {
            if (m_showInfoMsgIfNoUpdateAvail) {
                MessageDialog.openInformation(shell, "Node Update",
                        "No updates available");
            } else {
                LOGGER.info("No updates available ("
                        + candidateList.size() + " node link(s))");
            }
        } else {
            boolean isSingle = updateList.size() == 1;
            String title = "Update Node" + (isSingle ? "" : "s");
            StringBuilder messageBuilder = new StringBuilder();
            messageBuilder.append("Update available for ");
            if (isSingle && candidateList.size() == 1) {
                messageBuilder.append("node \"");
                messageBuilder.append(getManager().findNodeContainer(
                        candidateList.get(0)).getNameWithID());
                messageBuilder.append("\".");
            } else if (isSingle) {
                messageBuilder.append("one node.");
            } else {
                messageBuilder.append(updateList.size());
                messageBuilder.append(" nodes.");
            }
            messageBuilder.append("\n\n");
            if (nodesToResetCount > 0) {
                messageBuilder.append("Reset nodes and update now?");
            } else {
                messageBuilder.append("Update now?");
            }
            String message = messageBuilder.toString();
            if (MessageDialog.openQuestion(shell, title, message)) {
                LOGGER.debug("Running update for " + updateList.size()
                        + " node(s): " + updateList);
                execute(new UpdateMetaNodeLinkCommand(getManager(),
                        updateList.toArray(new NodeID[updateList.size()])));
            }
        }
    }

    private static final class CheckUpdateRunnableWithProgress
        implements IRunnableWithProgress {

        private final IWorkflowManager m_hostWFM;
        private final List<NodeID> m_candidateList;
        private final List<NodeID> m_updateList;
        private Status m_status;

        /**
         * @param hostWFM
         * @param candidateList */
        public CheckUpdateRunnableWithProgress(final IWorkflowManager hostWFM,
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
            WorkflowLoadHelper lH = new WorkflowLoadHelper(true, m_hostWFM.getContext());
            final String idName = KNIMEEditorPlugin.PLUGIN_ID;
            Status[] stats = new Status[m_candidateList.size()];
            int overallStatus = IStatus.OK;
            for (int i = 0; i < m_candidateList.size(); i++) {
                NodeID id = m_candidateList.get(i);
                NodeContainerTemplate tnc = (NodeContainerTemplate)m_hostWFM.findNodeContainer(id);
                WorkflowManager parent = tnc.getParent();
                monitor.subTask(tnc.getNameWithID());
                Status stat;
                try {
                    String msg;
                    if (parent.checkUpdateMetaNodeLink(id, lH)) {
                        m_updateList.add(id);
                        msg = "Update available for " + tnc.getNameWithID();
                    } else {
                        msg = "No update available for " + tnc.getNameWithID();
                    }
                    stat = new Status(IStatus.OK, idName, msg);
                } catch (Exception ex) {
                    Throwable cause = ex;
                    while ((cause.getCause() != null) && (cause.getCause() != cause)) {
                        cause = cause.getCause();
                    }

                    String msg = "Unable to check for update on "
                        + "node \"" + tnc.getNameWithID() + "\": "
                        + cause.getMessage();
                    LOGGER.warn(msg, cause);
                    stat = new Status(IStatus.WARNING , idName, msg, null);
                    overallStatus = IStatus.WARNING;
                }
                if (monitor.isCanceled()) {
                    throw new InterruptedException("Update check canceled");
                }
                stats[i] = stat;
                monitor.worked(1);
            }
            m_status = new MultiStatus(
                    idName, overallStatus, stats, "Some Node Link Updates failed", null);
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
