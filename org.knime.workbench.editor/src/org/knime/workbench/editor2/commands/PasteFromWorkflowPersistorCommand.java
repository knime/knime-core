/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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

import static org.knime.core.ui.wrapper.Wrapper.unwrap;
import static org.knime.core.ui.wrapper.Wrapper.wraps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.gef.EditPartViewer;
import org.eclipse.swt.widgets.Display;
import org.knime.core.node.workflow.ConnectionID;
import org.knime.core.node.workflow.ConnectionUIInformation;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.node.workflow.WorkflowAnnotation;
import org.knime.core.node.workflow.WorkflowAnnotationID;
import org.knime.core.node.workflow.WorkflowCopyContent;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.core.ui.node.workflow.ConnectionContainerUI;
import org.knime.core.ui.node.workflow.NodeContainerUI;
import org.knime.core.ui.node.workflow.WorkflowCopyUI;
import org.knime.core.ui.node.workflow.WorkflowCopyWithOffsetUI;
import org.knime.core.ui.node.workflow.WorkflowManagerUI;
import org.knime.core.ui.node.workflow.async.OperationNotAllowedException;
import org.knime.workbench.editor2.ClipboardObject;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.WorkflowEditorMode;
import org.knime.workbench.editor2.actions.ToggleEditorModeAction;
import org.knime.workbench.editor2.editparts.WorkflowRootEditPart;
import org.knime.workbench.ui.async.AsyncUtil;

/**
 * Pasts the current clipboard object (containing workflow persistor) into
 * the current workflow.
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class PasteFromWorkflowPersistorCommand
    extends AbstractKNIMECommand {

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
        super(editor.getWorkflowManagerUI());
        m_editor = editor;
        m_clipboardObject = clipboardObject;
        m_shiftCalculator = shiftCalculator;
    }

    /** {@inheritDoc} */
    @Override
    public boolean canExecute() {
        if (!super.canExecute()) {
            return false;
        }
        if (m_editor == null || m_clipboardObject == null) {
            return false;
        }
        WorkflowCopyUI wfCopy = m_clipboardObject.getWorkflowCopy();
        if (wraps(wfCopy, WorkflowPersistor.class)) {
            if (!wraps(getHostWFMUI(), WorkflowManager.class)) {
                //cross copies from WorkflowManager to WorkflowManagerUI not possible, yet
                return false;
            }
            //persistor for local workflows
            WorkflowPersistor copyPersistor = unwrap(wfCopy, WorkflowPersistor.class);
            if (!copyPersistor.getNodeLoaderMap().isEmpty()) {
                return true;
            }
            if (!copyPersistor.getWorkflowAnnotations().isEmpty()) {
                return true;
            }
        } else {
            if (wraps(getHostWFMUI(), WorkflowManager.class)) {
                //cross copies from WorkflowManager to WorkflowManagerUI not possible, yet
                return false;
            }
            return true;
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void execute() {
        final WorkflowManagerUI manager = m_editor.getWorkflowManagerUI();
        final WorkflowCopyUI wfCopy = m_clipboardObject.getWorkflowCopy();

        m_pastedContent = AsyncUtil.wfmAsyncSwitch(wfm -> {
            //in case of a sync workflow managewr:
            //paste the content, calculate the shift and then move the pasted objects accordingly
            final WorkflowCopyContent pastedContent = wfm.paste(wfCopy);

            final NodeID[] pastedNodes = pastedContent.getNodeIDs();
            // As of AP-8593, it will not be possible to have nodes and annotations on the clipboard concurrently
            final WorkflowAnnotation[] pastedAnnos = wfm.getWorkflowAnnotations(pastedContent.getAnnotationIDs());
            final boolean pasteIsForNodes = ((pastedNodes != null) && (pastedNodes.length > 0));
            final boolean needToggle =
                pasteIsForNodes ? (!WorkflowEditorMode.NODE_EDIT.equals(m_editor.getEditorMode()))
                    : (!WorkflowEditorMode.ANNOTATION_EDIT.equals(m_editor.getEditorMode()));

            if (needToggle) {
                final ToggleEditorModeAction toggleAction = new ToggleEditorModeAction(m_editor);

                toggleAction.runInSWT();
            }

            final Set<NodeID> newIDs = new HashSet<>(); // fast lookup below
            final List<int[]> insertedElementBounds = new ArrayList<>();
            for (final NodeID i : pastedNodes) {
                final NodeContainerUI nc = manager.getNodeContainer(i);
                final NodeUIInformation ui = nc.getUIInformation();
                final int[] bounds = ui.getBounds();
                insertedElementBounds.add(bounds);
            }
            for (final WorkflowAnnotation a : pastedAnnos) {
                final int[] bounds = new int[]{a.getX(), a.getY(), a.getWidth(), a.getHeight()};
                insertedElementBounds.add(bounds);
            }
            final int[] moveDist = m_shiftCalculator.calculateShift(insertedElementBounds, manager, m_clipboardObject);
            // for redo-operations we need the exact same shift.
            m_shiftCalculator = new FixedShiftCalculator(moveDist);
            for (final NodeID id : pastedNodes) {
                newIDs.add(id);
                final NodeContainerUI nc = manager.getNodeContainer(id);
                final NodeUIInformation oldUI = nc.getUIInformation();
                final NodeUIInformation newUI = NodeUIInformation.builder(oldUI).translate(moveDist).build();
                nc.setUIInformation(newUI);
            }
            for (final ConnectionContainerUI conn : manager.getConnectionContainers()) {
                if (newIDs.contains(conn.getDest()) && newIDs.contains(conn.getSource())) {
                    // get bend points and move them
                    final ConnectionUIInformation oldUI = conn.getUIInfo();
                    if (oldUI != null) {
                        final ConnectionUIInformation newUI =
                            ConnectionUIInformation.builder(oldUI).translate(moveDist).build();
                        conn.setUIInfo(newUI);
                    }
                }
            }
            for (final WorkflowAnnotation a : pastedAnnos) {
                a.shiftPosition(moveDist[0], moveDist[1]);
            }
            setFutureSelection(pastedContent.getNodeIDs(),
                Arrays.asList(manager.getWorkflowAnnotations(pastedContent.getAnnotationIDs())));
            return pastedContent;
        }, wfm -> {
            //in case of async workflow managers:
            //get the offset, calculate the shift and then paste the objects considering this shift
            assert wfCopy instanceof WorkflowCopyWithOffsetUI;
            final WorkflowCopyWithOffsetUI wfCopyOffset = (WorkflowCopyWithOffsetUI)wfCopy;

            //calc shift
            final int[] shift =
                m_shiftCalculator.calculateShift(wfCopyOffset.getX(), wfCopyOffset.getY(), m_clipboardObject);
            // for redo-operations we need the exact same shift.
            m_shiftCalculator = new FixedShiftCalculator(shift);
            wfCopyOffset.setXShift(shift[0]);
            wfCopyOffset.setYShift(shift[1]);

            Display.getDefault().syncExec(() -> {
                setFutureSelection(WorkflowRootEditPart.ALL_NEW_NODES, WorkflowRootEditPart.ALL_NEW_ANNOTATIONS);
            });
            return wfm.pasteAsync(wfCopyOffset);
        }, manager, "Pasting workflow parts ...");
    }

    private void setFutureSelection(final NodeID[] nodeIds, final Collection<WorkflowAnnotation> was) {
        EditPartViewer partViewer = m_editor.getViewer();
        partViewer.deselectAll();
        // select the new ones....
        if (partViewer.getRootEditPart().getContents() != null
                && partViewer.getRootEditPart().getContents()
                instanceof WorkflowRootEditPart) {
            WorkflowRootEditPart rootEditPart =
                    (WorkflowRootEditPart)partViewer.getRootEditPart()
                            .getContents();
            rootEditPart.setFutureSelection(nodeIds);
            rootEditPart.setFutureAnnotationSelection(was);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean canUndo() {
        if (m_pastedContent == null) {
            //can happen, if workflow copy is not available on the server anymore
            //will almost never happen
            return false;
        }
        WorkflowManagerUI manager = m_editor.getWorkflowManagerUI();
        NodeID[] pastedNodes = m_pastedContent.getNodeIDs();
        WorkflowAnnotationID[] pastedAnnos = m_pastedContent.getAnnotationIDs();
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
        WorkflowManagerUI manager = m_editor.getWorkflowManagerUI();
        NodeID[] nodeIDs = m_pastedContent.getNodeIDs();
        ConnectionID[] connIDs = new ConnectionID[0];
        WorkflowAnnotationID[] annoIDs = m_pastedContent.getAnnotationIDs();
        try {
            AsyncUtil.wfmAsyncSwitchRethrow(wfm -> {
                wfm.remove(nodeIDs, connIDs, annoIDs);
                return null;
            }, wfm -> wfm.removeAsync(nodeIDs, connIDs, annoIDs), manager, "Removing workflow parts ...");
        } catch (OperationNotAllowedException e) {
            openDialog("Problem undoing paste command", "Pasted parts cannot removed anymore: " + e.getMessage());
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
                final WorkflowManagerUI manager,
                final ClipboardObject clipObject);

        /**
         * Calculates the shift based on a given position (x,y).
         *
         * @param offsetX the actual offset in X direction
         * @param offsetY the actual offset in Y direction
         * @param clipObject The clipboard object (for retrieval count)
         * @return The shift (array of length 2).
         */
        public abstract int[] calculateShift(int offsetX, int offsetY, ClipboardObject clipObject);
    }

    /** A fixed shift calculator that returns the shift provided in the
     * constructor. It's used in redo operations to replay the exact same
     * behavior.
     */
    private final class FixedShiftCalculator extends ShiftCalculator {

        private final int[] m_moveDist;

        /** @param moveDist The shift to return. */
        public FixedShiftCalculator(final int[] moveDist) {
            m_moveDist = moveDist.clone();
        }

        /** {@inheritDoc} */
        @Override
        public int[] calculateShift(
                final Iterable<int[]> bounds, final WorkflowManagerUI manager,
                final ClipboardObject clipObject) {
            return m_moveDist;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int[] calculateShift(final int offsetX, final int offsetY, final ClipboardObject clipObject) {
            return m_moveDist;
        }
    }
}
