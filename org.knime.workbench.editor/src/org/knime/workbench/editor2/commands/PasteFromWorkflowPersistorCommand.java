/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   Jul 8, 2009 (wiswedel): created
 */
package org.knime.workbench.editor2.commands;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.commands.Command;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.ConnectionUIInformation;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.workbench.editor2.ClipboardObject;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.editparts.WorkflowRootEditPart;

/**
 * Pasts the current clipboard object (containing workflow persistor) into
 * the current workflow.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class PasteFromWorkflowPersistorCommand extends Command {
    
    private final ClipboardObject m_clipboardObject;
    private final WorkflowEditor m_editor;
    private ShiftCalculator m_shiftCalculator;
    
    private NodeID[] m_pastedIDs;

    /**
     * @param editor The workflow to paste into
     * @param clipboardObject  The current clipboard object.
     * @param shiftCalculator The shift calculation routine, used to include
     * some offset during paste or to provide target coordinates.
     * 
     */
    public PasteFromWorkflowPersistorCommand(final WorkflowEditor editor,
            final ClipboardObject clipboardObject,
            final ShiftCalculator shiftCalculator) {
        m_editor = editor;
        m_clipboardObject = clipboardObject;
        m_shiftCalculator = shiftCalculator;
    }
    
    /** {@inheritDoc} */
    @Override
    public boolean canExecute() {
        return m_editor != null && m_clipboardObject != null
        && !m_clipboardObject.getCopyPersistor().getNodeLoaderMap().isEmpty();
    }
    
    /** {@inheritDoc} */
    @Override
    public void execute() {
        WorkflowManager manager = m_editor.getWorkflowManager();
        WorkflowPersistor copyPersistor = m_clipboardObject.getCopyPersistor();
        m_pastedIDs = manager.paste(copyPersistor);
        Set<NodeID> newIDs = new HashSet<NodeID>(); // fast lookup below
        int[] moveDist = m_shiftCalculator.calculateShift(
                m_pastedIDs, manager, m_clipboardObject);
        // for redo-operations we need the exact same shift.
        m_shiftCalculator = new FixedShiftCalculator(moveDist);
        for (NodeID id : m_pastedIDs) {
            newIDs.add(id);
            NodeContainer nc = manager.getNodeContainer(id);
            NodeUIInformation oldUI = (NodeUIInformation)nc.getUIInformation();
            NodeUIInformation newUI = 
                oldUI.createNewWithOffsetPosition(moveDist);
            nc.setUIInformation(newUI);
        }
        for (ConnectionContainer conn : manager.getConnectionContainers()) {
            if (newIDs.contains(conn.getDest()) 
                    && newIDs.contains(conn.getSource())) {
                // get bend points and move them
                ConnectionUIInformation oldUI = 
                    (ConnectionUIInformation)conn.getUIInfo();
                if (oldUI != null) {
                    ConnectionUIInformation newUI = 
                        oldUI.createNewWithOffsetPosition(moveDist);
                    conn.setUIInfo(newUI);
                }
            }
        }
        
        EditPartViewer partViewer = m_editor.getViewer();
        partViewer.deselectAll();
        // select the new ones....
        if (partViewer.getRootEditPart().getContents() != null 
                && partViewer.getRootEditPart().getContents() 
                instanceof WorkflowRootEditPart) {
            ((WorkflowRootEditPart)partViewer.getRootEditPart().getContents())
                .setFutureSelection(m_pastedIDs);
        }
    }
    
    /** {@inheritDoc} */
    @Override
    public boolean canUndo() {
        WorkflowManager manager = m_editor.getWorkflowManager();
        if (m_pastedIDs == null || m_pastedIDs.length == 0) {
            return false;
        }
        for (NodeID id : m_pastedIDs) {
            if (!manager.canRemoveNode(id)) {
                return false;
            }
        }
        return true;
    }
    
    /** {@inheritDoc} */
    @Override
    public void undo() {
        WorkflowManager manager = m_editor.getWorkflowManager();
        for (NodeID id : m_pastedIDs) {
            manager.removeNode(id); // will skip unknown ids
        }
    }
    
    /**
     * @return the pastedIDs
     */
    public NodeID[] getPastedIDs() {
        return m_pastedIDs;
    }
    
    /**
     * Encapsulates the operation to calculate the offset when pasting nodes.
     * This offset is fixed when inserting using Ctrl-V but may be different
     * when used from within the context menu.
     */
    public abstract static class ShiftCalculator {
        
        /** Calculates the shift (offset) when inserting nodes.
         * @param ids The nodes that were inserted.
         * @param manager The manager where they were inserted.
         * @param clipObject The clipboard object (for retrieval counter)
         * @return The shift (array of length 2).
         */
        public abstract int[] calculateShift(final NodeID[] ids, 
                final WorkflowManager manager, 
                final ClipboardObject clipObject);
    }
    
    /** A fixed shift calculator that returns the shift provided in the 
     * constructor. It's used in redo operations to replay the exact same
     * behavior.
     */
    private final class FixedShiftCalculator extends ShiftCalculator {

        private final int[] m_moveDist;
        
        /** @param moveDist The shift to return. */
        public FixedShiftCalculator(final int[] moveDist) {
            m_moveDist = moveDist;
        }
        
        /** {@inheritDoc} */
        @Override
        public int[] calculateShift(
                final NodeID[] ids, final WorkflowManager manager,
                final ClipboardObject clipObject) {
            return m_moveDist;
        }
    }

}
