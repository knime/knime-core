/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 *   20.02.2006 (sieb): created
 */
package org.knime.workbench.editor2.actions;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowAnnotation;
import org.knime.core.node.workflow.WorkflowCopyContent;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.workbench.editor2.ClipboardObject;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.editparts.AnnotationEditPart;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;

/**
 * Implements the clipboard copy action to copy nodes and connections into the
 * clipboard.
 *
 * @author Christoph Sieb, University of Konstanz
 */
public class CopyAction extends AbstractClipboardAction {

    private NodeContainerEditPart[] m_nodeParts;
    private AnnotationEditPart[] m_annotationParts;

    /**
     * Constructs a new clipboard copy action.
     *
     * @param editor the workflow editor this action is intended for
     */
    public CopyAction(final WorkflowEditor editor) {
        super(editor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return ActionFactory.COPY.getId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        ISharedImages sharedImages =
            PlatformUI.getWorkbench().getSharedImages();
        return sharedImages.getImageDescriptor(ISharedImages.IMG_TOOL_COPY);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getText() {
        return "Copy";
    }

    /**
     * At least one node must be selected.
     *
     * {@inheritDoc}
     */
    @Override
    protected boolean calculateEnabled() {
        NodeContainerEditPart[] parts =
            getSelectedParts(NodeContainerEditPart.class);
        AnnotationEditPart[] anno =
            getSelectedParts(AnnotationEditPart.class);
        return parts.length > 0 || anno.length > 0;
    }

    /** {@inheritDoc} */
    @Override
    public void runInSWT() {
        m_nodeParts = getSelectedParts(NodeContainerEditPart.class);
        m_annotationParts = getSelectedParts(AnnotationEditPart.class);

        NodeID[] ids = new NodeID[m_nodeParts.length];
        for (int i = 0; i < m_nodeParts.length; i++) {
            NodeContainerEditPart nodeEP = m_nodeParts[i];
            ids[i] = nodeEP.getNodeContainer().getID();
        }
        WorkflowAnnotation[] annotations =
            new WorkflowAnnotation[m_annotationParts.length];
        for (int i = 0; i < m_annotationParts.length; i++) {
            annotations[i] = m_annotationParts[i].getModel();
        }

        WorkflowCopyContent content = new WorkflowCopyContent();
        content.setNodeIDs(ids);
        content.setAnnotation(annotations);
        WorkflowPersistor copyPersistor = getManager().copy(false, content);

        // ClipboardWorkflowManager.put(getManager(), ids);

        // the information about the nodes is stored in the config XML format
        // also used to store workflow information in the kflow files
        // getEditor().getClipboard().setContents(
        // new Object[]{getNodeSettings(nodeParts,
        // connectionParts)},
        // new Transfer[]{ResourceTransfer.getInstance()});
        getEditor().setClipboardContent(new ClipboardObject(copyPersistor));

        // update the actions
        getEditor().updateActions();

        // Give focus to the editor again. Otherwise the actions (selection)
        // is not updated correctly.
        getWorkbenchPart().getSite().getPage().activate(getWorkbenchPart());
    }

    /** @return the annotationParts */
    public AnnotationEditPart[] getAnnotationParts() {
        return m_annotationParts;
    }

    /** @return the nodeParts */
    public NodeContainerEditPart[] getNodeParts() {
        return m_nodeParts;
    }

    /** {@inheritDoc} */
    @Override
    public void runOnNodes(final NodeContainerEditPart[] nodeParts) {
        throw new IllegalStateException(
                "Not to be called as runInSWT is overwritten.");
    }

}
