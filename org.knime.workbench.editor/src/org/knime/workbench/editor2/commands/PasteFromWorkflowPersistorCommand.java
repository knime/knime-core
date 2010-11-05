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
 * ---------------------------------------------------------------------
 *
 * History
 *   Jul 8, 2009 (wiswedel): created
 */
package org.knime.workbench.editor2.commands;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.commands.Command;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.ConnectionUIInformation;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.node.workflow.WorkflowAnnotation;
import org.knime.core.node.workflow.WorkflowCopyContent;
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

    private WorkflowCopyContent m_pastedContent;

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
        if (m_editor == null || m_clipboardObject == null) {
            return false;
        }
        WorkflowPersistor copyPersistor = m_clipboardObject.getCopyPersistor();
        if (!copyPersistor.getNodeLoaderMap().isEmpty()) {
            return true;
        }
        if (!copyPersistor.getWorkflowAnnotations().isEmpty()) {
            return true;
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void execute() {
        WorkflowManager manager = m_editor.getWorkflowManager();
        WorkflowPersistor copyPersistor = m_clipboardObject.getCopyPersistor();
        m_pastedContent = manager.paste(copyPersistor);
        NodeID[] pastedNodes = m_pastedContent.getNodeIDs();
        WorkflowAnnotation[] pastedAnnos = m_pastedContent.getAnnotations();
        Set<NodeID> newIDs = new HashSet<NodeID>(); // fast lookup below
        List<int[]> insertedElementBounds = new ArrayList<int[]>();
        for (NodeID i : pastedNodes) {
            NodeContainer nc = manager.getNodeContainer(i);
            NodeUIInformation ui = (NodeUIInformation)nc.getUIInformation();
            int[] bounds = ui.getBounds();
            insertedElementBounds.add(bounds);
        }
        for (WorkflowAnnotation a : pastedAnnos) {
            int[] bounds =
                new int[] {a.getX(), a.getY(), a.getWidth(), a.getHeight()};
            insertedElementBounds.add(bounds);
        }
        int[] moveDist = m_shiftCalculator.calculateShift(
                insertedElementBounds, manager, m_clipboardObject);
        // for redo-operations we need the exact same shift.
        m_shiftCalculator = new FixedShiftCalculator(moveDist);
        for (NodeID id : pastedNodes) {
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
        for (WorkflowAnnotation a : pastedAnnos) {
            a.shiftPosition(moveDist[0], moveDist[1]);
        }

        EditPartViewer partViewer = m_editor.getViewer();
        partViewer.deselectAll();
        // select the new ones....
        if (partViewer.getRootEditPart().getContents() != null
                && partViewer.getRootEditPart().getContents()
                instanceof WorkflowRootEditPart) {
            ((WorkflowRootEditPart)partViewer.getRootEditPart().getContents())
                .setFutureSelection(pastedNodes);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean canUndo() {
        WorkflowManager manager = m_editor.getWorkflowManager();
        NodeID[] pastedNodes = m_pastedContent.getNodeIDs();
        WorkflowAnnotation[] pastedAnnos = m_pastedContent.getAnnotations();
        if ((pastedNodes == null || pastedNodes.length == 0)
                && (pastedAnnos == null || pastedAnnos.length == 0)) {
            return false;
        }
        for (NodeID id : pastedNodes) {
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
        for (NodeID id : m_pastedContent.getNodeIDs()) {
            manager.removeNode(id);
        }
        for (WorkflowAnnotation anno : m_pastedContent.getAnnotations()) {
            manager.removeAnnotation(anno);
        }
    }

    /**
     * Encapsulates the operation to calculate the offset when pasting nodes.
     * This offset is fixed when inserting using Ctrl-V but may be different
     * when used from within the context menu.
     */
    public abstract static class ShiftCalculator {

        /** Calculates the shift (offset) when inserting nodes/annotations.
         * @param bounds The bounds of the inserted elements
         * @param manager The manager where they were inserted.
         * @param clipObject The clipboard object (for retrieval counter)
         * @return The shift (array of length 2).
         */
        public abstract int[] calculateShift(Iterable<int[]> bounds,
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
                final Iterable<int[]> bounds, final WorkflowManager manager,
                final ClipboardObject clipObject) {
            return m_moveDist;
        }
    }

}
