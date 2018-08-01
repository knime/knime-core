/*
 * ------------------------------------------------------------------------
 *
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
 *   Nov 10, 2018 (loki): created
 */
package org.knime.workbench.editor2;

import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.SelectionManager;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.widgets.Display;
import org.knime.workbench.editor2.actions.ToggleEditorModeAction;
import org.knime.workbench.editor2.editparts.ConnectionContainerEditPart;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;

/**
 * Per AP-8593, if we're presently in Annotation Edit mode, the user clicking anywhere which is not an annotation should
 * exit that mode.
 *
 * @author loki der quaeler
 */
public class AnnotationModeExitEnabler implements MouseListener {
    /**
     * @see #annotationDragTrackerShouldVeto(Point)
     */
    private static final long UNREASONABLY_QUICK_INTER_EVENT_TIME = 175;

    // Perfectly fine that this is shared across all instances as the amount of time it would take to switch workflow
    // editors falls very far outside our "unreasonably quick" boundary.
    private static final AtomicLong LAST_MODE_EXIT = new AtomicLong(-1);
    private static Point LAST_MODE_EXIT_POINT;

    /**
     * This is a not-so-pretty way to get around SWT not providing a way to consume MouseEvents (KeyEvents have a flag
     * (<code>doIt</code>) which can be set to achieve this.) Since we cannot consume the MouseEvent, it is possible for
     * the exit-mode mouse event consumed by this listener to there after end up in
     * <code>AnnotationEditPart.getDragTracker(Request)</code> and then return a drag tracker which does its own
     * affectation of the <code>SelectionManager</code> - which produces an undesired selection on the canvas.
     *
     * @param p the mouse location at which the click occurred
     * @return true if the drag tracker request on an annotation edit part should return null, or if an existing drag
     *         tracker should avoid handling its mouse up event
     */
    public static boolean annotationDragTrackerShouldVeto(final Point p) {
        if (p.equals(LAST_MODE_EXIT_POINT)) {
            return ((System.currentTimeMillis() - LAST_MODE_EXIT.get()) < UNREASONABLY_QUICK_INTER_EVENT_TIME);
        }

        return false;
    }

    private static void modeExitIsOccurring(final Point p) {
        LAST_MODE_EXIT_POINT = new Point(p);
        LAST_MODE_EXIT.set(System.currentTimeMillis());
    }


    private final DragPositionProcessor m_dragPositionProcessor;

    private final WorkflowEditor m_workflowEditor;

    /**
     * @param editor The editor containing the graphical viewer from which we'll track mouse events.
     */
    public AnnotationModeExitEnabler(final WorkflowEditor editor) {
        if (!editor.getWorkflowManager().isPresent()) {
            //doesn't work with other than the ordinary workflow manager, yet
            m_dragPositionProcessor = null;
            m_workflowEditor = null;
            return;
        }
        final GraphicalViewer viewer = editor.getGraphicalViewer();

        m_workflowEditor = editor;

        m_dragPositionProcessor = new DragPositionProcessor(viewer);

        final FigureCanvas fc = getFigureCanvas();
        fc.addMouseListener(this);
    }

    /**
     * This should be called as part of the parent disposal cycle.
     */
    public void dispose() {
        if (m_workflowEditor != null) {
            final FigureCanvas fc = getFigureCanvas();

            if (fc != null) {
                fc.removeMouseListener(this);
            }
        }
    }

    private FigureCanvas getFigureCanvas() {
        if (m_workflowEditor != null) {
            return (FigureCanvas)m_workflowEditor.getGraphicalViewer().getControl();
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseDoubleClick(final MouseEvent me) { }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseDown(final MouseEvent me) {
        if (WorkflowEditorMode.ANNOTATION_EDIT.equals(m_workflowEditor.getEditorMode())) {
            final SelectionManager sm = m_workflowEditor.getGraphicalViewer().getSelectionManager();

            m_dragPositionProcessor.processDragEventAtPoint(me.display.getCursorLocation(), false,
                (StructuredSelection)sm.getSelection());

            if (m_dragPositionProcessor.getAnnotation() == null) {
                final ToggleEditorModeAction action = new ToggleEditorModeAction(m_workflowEditor, false);
                final NodeContainerEditPart node = m_dragPositionProcessor.getNode();
                final ConnectionContainerEditPart connection = m_dragPositionProcessor.getEdge();

                modeExitIsOccurring(m_dragPositionProcessor.getLastPosition());

                action.runInSWT();

                // The ol' give-it-a-pause-to-catch-up strategy
                Display.getDefault().asyncExec(() -> {
                    sm.deselectAll();

                    if (node != null) {
                        sm.appendSelection(node);
                    } else if (connection != null) {
                        sm.appendSelection(connection);
                    }
                });
            }

            m_dragPositionProcessor.unmarkSelection();
            m_dragPositionProcessor.clearMarkingAvoidance();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseUp(final MouseEvent me) { }
}
