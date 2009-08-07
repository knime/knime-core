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

import org.eclipse.gef.commands.Command;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.ConnectionUIInformation;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.workbench.editor2.ClipboardObject;

/**
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class PasteFromWorkflowPersistorCommand extends Command {
    
    private final ClipboardObject m_clipboardObject;
    private final WorkflowManager m_manager;
    private ShiftCalculator m_shiftCalculator;
    
    private NodeID[] m_pastedIDs;

    /**
     * @param manager 
     * @param clipboardObject 
     * @param shiftCalculator 
     * 
     */
    public PasteFromWorkflowPersistorCommand(final WorkflowManager  manager,
            final ClipboardObject clipboardObject,
            final ShiftCalculator shiftCalculator) {
        m_manager = manager;
        m_clipboardObject = clipboardObject;
        m_shiftCalculator = shiftCalculator;
    }
    
    /** {@inheritDoc} */
    @Override
    public boolean canExecute() {
        return m_manager != null && m_clipboardObject != null
        && !m_clipboardObject.getCopyPersistor().getNodeLoaderMap().isEmpty();
    }
    
    /** {@inheritDoc} */
    @Override
    public void execute() {
        WorkflowPersistor copyPersistor = m_clipboardObject.getCopyPersistor();
        m_pastedIDs = m_manager.paste(copyPersistor);
        Set<NodeID> newIDs = new HashSet<NodeID>(); // fast lookup below
        int[] moveDist = m_shiftCalculator.calculateShift(
                m_pastedIDs, m_manager, m_clipboardObject);
        // for redo-operations we need the exact same shift.
        m_shiftCalculator = new FixedShiftCalculator(moveDist);
        for (NodeID id : m_pastedIDs) {
            newIDs.add(id);
            NodeContainer nc = m_manager.getNodeContainer(id);
            NodeUIInformation oldUI = (NodeUIInformation)nc.getUIInformation();
            NodeUIInformation newUI = 
                oldUI.createNewWithOffsetPosition(moveDist);
            nc.setUIInformation(newUI);
        }
        for (ConnectionContainer conn : m_manager.getConnectionContainers()) {
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
    }
    
    /** {@inheritDoc} */
    @Override
    public boolean canUndo() {
        if (m_pastedIDs == null || m_pastedIDs.length == 0) {
            return false;
        }
        for (NodeID id : m_pastedIDs) {
            if (!m_manager.canRemoveNode(id)) {
                return false;
            }
        }
        return true;
    }
    
    /** {@inheritDoc} */
    @Override
    public void undo() {
        for (NodeID id : m_pastedIDs) {
            m_manager.removeNode(id); // will skip unknown ids
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
