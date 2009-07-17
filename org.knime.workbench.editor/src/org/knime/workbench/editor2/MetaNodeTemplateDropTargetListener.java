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
package org.knime.workbench.editor2;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.util.TransferDropTargetListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.repository.RepositoryFactory;
import org.knime.workbench.repository.model.MetaNodeTemplate;
import org.knime.workbench.ui.metanodes.MetaNodeTemplateRepositoryItem;
import org.knime.workbench.ui.metanodes.MetaNodeTemplateRepositoryManager;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class MetaNodeTemplateDropTargetListener 
    implements TransferDropTargetListener {

//    private static final NodeLogger LOGGER = NodeLogger.getLogger(
//            MetaNodeTemplateDropTargetListener.class);
    
    // TODO: later on use the viewer to execute a command
    // in this way, the creation of meta node templates can be encapsulated 
    // into an Action/Command
    private EditPartViewer m_viewer;
    private WorkflowEditor m_editor;
    
    public MetaNodeTemplateDropTargetListener(WorkflowEditor editor, 
            EditPartViewer viewer) {
        m_viewer = viewer;
        m_editor = editor;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void dragEnter(DropTargetEvent event) {
        event.feedback = DND.FEEDBACK_SELECT;
        event.operations = DND.DROP_COPY;
        event.detail = DND.DROP_COPY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dragLeave(DropTargetEvent event) {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dragOperationChanged(DropTargetEvent event) {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dragOver(DropTargetEvent event) {
        event.feedback = DND.FEEDBACK_SELECT;
        event.operations = DND.DROP_COPY;
        event.detail = DND.DROP_COPY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void drop(final DropTargetEvent event) {
        LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();
        
        // check instanceof  
        NodeID id = null;
        WorkflowManager sourceManager = null;
        Object selection = ((IStructuredSelection)transfer.getSelection())
            .getFirstElement(); 
        if (selection instanceof MetaNodeTemplateRepositoryItem) {
            id = ((MetaNodeTemplateRepositoryItem)selection).getNodeID();
            sourceManager = MetaNodeTemplateRepositoryManager.getInstance()
                .getWorkflowManager(); 
        } else if (selection instanceof MetaNodeTemplate) {
            id = ((MetaNodeTemplate)selection).getManager().getID();
            sourceManager = RepositoryFactory.META_NODE_ROOT;
        }
        if (id == null || sourceManager == null) {
            return;
        }
        NodeID[] copied = m_editor.getWorkflowManager().copyFromAndPasteHere(
                sourceManager, new NodeID[] {id});
        // create UI info
        NodeContainer newNode = m_editor.getWorkflowManager().getNodeContainer(
                copied[0]);
        NodeUIInformation uiInfo = 
            (NodeUIInformation)newNode.getUIInformation();
        event.x = event.display.getCursorLocation().x;
        event.y = event.display.getCursorLocation().y;
        org.eclipse.swt.graphics.Point toControl = 
            m_viewer.getControl().toControl(event.x, event.y);
        Point p = new Point(toControl.x, toControl.y);
        if (uiInfo == null) {
            uiInfo = new NodeUIInformation(p.x, p.y, -1, -1, false);
        } else {
            uiInfo.setNodeLocation(p.x, p.y, -1, -1);
        }
        newNode.setUIInformation(uiInfo);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dropAccept(DropTargetEvent event) {
    }

    @Override
    public Transfer getTransfer() {
        return LocalSelectionTransfer.getTransfer();
    }

    @Override
    public boolean isEnabled(DropTargetEvent event) {
        // TODO Auto-generated method stub
        return true;
    }

}
