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
 *
 * History
 *   07.05.2011 (mb): created
 */
package org.knime.workbench.editor2.commands;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.gef.EditPartViewer;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.knime.core.api.node.workflow.IWorkflowAnnotation;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.action.ExpandSubnodeResult;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.editparts.WorkflowRootEditPart;

/**
 * Command that expands the selected sub node.
 *
 * @author Patrick Winter, KNIME.com AG, Zurich, Switzerland
 */
public class ExpandSubNodeCommand extends AbstractKNIMECommand {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(ExpandSubNodeCommand.class);

    private final NodeID m_id;

    private ExpandSubnodeResult m_expandResult;

    private final WorkflowEditor m_editor;

    /**
     * @param wfm the workflow manager holding the new metanode
     * @param id of node to be expanded.
     * @param editor on which this command is executed
     */
    public ExpandSubNodeCommand(final WorkflowManager wfm, final NodeID id, final WorkflowEditor editor) {
        super(wfm);
        m_id = id;
        m_editor = editor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canExecute() {
        if (!super.canExecute()) {
            return false;
        }
        return getHostWFM().canExpandSubNode(m_id) == null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        try {
            WorkflowManager hostWFM = getHostWFM();

            // close editor of subnode and children
            NodeID wfmID = ((SubNodeContainer)hostWFM.getNodeContainer(m_id)).getWorkflowManager().getID();
            for (IEditorPart child : m_editor.getSubEditors(m_id)) {
                child.getEditorSite().getPage().closeEditor(child, false);
            }

            m_expandResult = hostWFM.expandSubWorkflow(m_id);
            List<IWorkflowAnnotation> annotations =
                Arrays.stream(m_expandResult.getExpandedCopyContent().getAnnotationIDs())
                    .map(id -> hostWFM.getWorkflowAnnotation(id)).collect(Collectors.toList());
            NodeID[] nodeIDs = m_expandResult.getExpandedCopyContent().getNodeIDs();

            EditPartViewer partViewer = m_editor.getViewer();
            partViewer.deselectAll();
            // select the new ones....
            if (partViewer.getRootEditPart().getContents() != null
                && partViewer.getRootEditPart().getContents() instanceof WorkflowRootEditPart) {
                WorkflowRootEditPart rootEditPart = (WorkflowRootEditPart)partViewer.getRootEditPart().getContents();
                rootEditPart.setFutureSelection(nodeIDs);
                rootEditPart.setFutureAnnotationSelection(annotations);
            }

        } catch (Exception e) {
            String error = "Expanding Wrapped Metanode failed: " + e.getMessage();
            LOGGER.error(error, e);
            MessageDialog.openError(Display.getCurrent().getActiveShell(), "Expand failed", error);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canUndo() {
        return m_expandResult != null && m_expandResult.canUndo();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void undo() {
        m_expandResult.undo();
        m_expandResult = null;
    }

}
