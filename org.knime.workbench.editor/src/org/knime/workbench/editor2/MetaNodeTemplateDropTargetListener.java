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
 *   04.02.2008 (Fabian Dill): created
 */
package org.knime.workbench.editor2;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.Request;
import org.eclipse.gef.dnd.AbstractTransferDropTargetListener;
import org.eclipse.gef.requests.CreateRequest;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.WorkflowPersistorVersion1xx;
import org.knime.workbench.explorer.filesystem.ExplorerFileStore;
import org.knime.workbench.explorer.view.ContentObject;

/**
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich Switzerland
 * @author Peter Ohl, KNIME.com, Zurich Switzerland
 */
public class MetaNodeTemplateDropTargetListener
    extends AbstractTransferDropTargetListener {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            MetaNodeTemplateDropTargetListener.class);
    private final MetaNodeTemplateFactory m_factory;

    /**
     *
     */
    public MetaNodeTemplateDropTargetListener(final EditPartViewer v) {
        super(v, LocalSelectionTransfer.getTransfer());
        m_factory = new MetaNodeTemplateFactory();
    }

    /** {@inheritDoc} */
    @Override
    protected void updateTargetRequest() {
        ((CreateRequest)getTargetRequest()).setLocation(getDropLocation());
    }

    /** {@inheritDoc} */
    @Override
    protected Request createTargetRequest() {
        CreateRequest request = new CreateRequest();
        request.setFactory(m_factory);
        return request;
    }

    /** {@inheritDoc} */
    @Override
    protected void handleDrop() {
        ContentObject obj = getDragResources(getCurrentEvent());
        Object o = obj.getObject();
        if (!(o instanceof ExplorerFileStore)) {
            return;
        }
        ExplorerFileStore store = (ExplorerFileStore)o;
        IFileStore workflowKNIMEFile = store.getChild(
                WorkflowPersistorVersion1xx.WORKFLOW_FILE);
        if (workflowKNIMEFile.fetchInfo().exists()) {
            m_factory.setSourceFileStore(workflowKNIMEFile);
            super.handleDrop();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled(final DropTargetEvent event) {
        boolean isMetaNodeTemplate = getDragResources(event) != null;
        if (isMetaNodeTemplate) {
            event.feedback = DND.FEEDBACK_SELECT;
            event.operations = DND.DROP_COPY;
            event.detail = DND.DROP_COPY;
        }
        return isMetaNodeTemplate;
    }

//    /**
//     *
//     * @param event drop target event containing the position
//     *  (relative to whole display)
//     * @return point converted to the editor coordinates
//     */
//    protected Point getDropLocation(final DropTargetEvent event) {
//        event.x = event.display.getCursorLocation().x;
//        event.y = event.display.getCursorLocation().y;
//        EditPartViewer viewer = getViewer();
//        Point p = new Point(viewer.getControl()
//                    .toControl(event.x, event.y).x,
//                    viewer.getControl()
//                    .toControl(event.x, event.y).y);
//        LOGGER.debug("to control: " + p);
//        // subtract this amount in order to have the node more or less centered
//        // at the cursor location
//        // more or less because the nodes are still of different width depending
//        // on their name
//        p.x -= 40;
//        p.y -= 40;
//        return p;
//    }

//    /**
//     * {@inheritDoc}
//     */
//    @Override
//    public void drop(final DropTargetEvent event) {
//        LOGGER.debug("drop: " + event);
//        IResource[] metaNodeTemplates = getDragResources(event);
//        if (metaNodeTemplates != null) {
//            // Set the factory on the current request
//            NodeFromNodeTemplateCreationFactory factory
//                = new NodeFromNodeTemplateCreationFactory(template);
//            WorkflowRootEditPart root = (WorkflowRootEditPart)m_viewer
//                .getRootEditPart().getContents();
//            m_viewer.getEditDomain().getCommandStack().execute(
//                    new CreateNodeCommand(root.getWorkflowManager(),
//                            (NodeFactory<? extends NodeModel>)
//                                factory.getNewObject(),
//                            getDropLocation(event)));
//            NodeUsageRegistry.addNode(template);
//            // bugfix: 1500
//            m_viewer.getControl().setFocus();
//        }
//    }


    private ContentObject getDragResources(final DropTargetEvent event) {
        LocalSelectionTransfer transfer = (LocalSelectionTransfer)getTransfer();
        ISelection selection = transfer.getSelection();
        if (selection instanceof IStructuredSelection) {
            IStructuredSelection ss = (IStructuredSelection)selection;
            Object firstElement = ss.getFirstElement();
            if (firstElement instanceof ContentObject) {
                return (ContentObject)firstElement;
            }
        }
        return null;
    }

}
