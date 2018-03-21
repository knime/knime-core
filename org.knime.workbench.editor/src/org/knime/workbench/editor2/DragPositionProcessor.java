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
 *   Mar 19, 2018 (loki): created
 */
package org.knime.workbench.editor2;

import java.util.ArrayList;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.knime.workbench.editor2.editparts.ConnectionContainerEditPart;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.editor2.figures.ProgressPolylineConnection;

/**
 * The genesis of this class is AP-5238; we want to re-produce some of the functionality found in
 * WorkflowEditorDropTargetListener without participating in that class hierarchy - so we are sidechaining the
 * applicable functionality into its own processor class.
 *
 * TODO tracking node and edge counts is currently pointless
 *
 * @author loki der quaeler
 */
class DragPositionProcessor {

    /**
     * Implementors of this interface will have the method invoked on the SWT thread; it will not be invoked for
     * successive hits on the same target, if that target has not been vetoed by any vetoers.
     */
    public interface TargetVetoer {

        boolean shouldVetoTarget(NodeContainerEditPart potentialNode, ConnectionContainerEditPart potentialEdge);

    }


    /**
     * The color of dragged over edges.
     */
    private static final Color RED = new Color(null, 255, 0, 0);


    private final EditPartViewer m_parentViewer;

    private final ArrayList<TargetVetoer> m_vetoers;

    private NodeContainerEditPart m_markedNode;

    private ConnectionContainerEditPart m_markedEdge;

    private int m_nodeCount;

    private int m_edgeCount;

    private NodeContainerEditPart m_node;
    private NodeContainerEditPart m_avoidMarkingNode;

    private ConnectionContainerEditPart m_edge;
    private ConnectionContainerEditPart m_avoidMarkingEdge;

    private Color m_edgeColor;

    private int m_edgeWidth;

    DragPositionProcessor(final EditPartViewer parent) {
        m_parentViewer = parent;

        m_vetoers = new ArrayList<>();
    }

    //
    // Package available methods
    //

    /**
     * This invokes <code>processDragEventAtPoint(p, true)</code>
     *
     * @param p This is expected to be in untranslated (untranslated from the source SWT event) coordinates.
     */
    void processDragEventAtPoint(final Point p) {
        processDragEventAtPoint(p, true);
    }

    /**
     * Consumers of this processor should call this method (when appropriate) as their drag event processor; after
     * execution returns from this block, the package-access getters of this class can be consulted concerning the
     * results of the processing.
     *
     * @param p This is expected to be in untranslated (untranslated from the source SWT event) coordinates.
     * @param shouldAffectMark If this is true, the UI is updated concerning hit detected objects, as well as keeping
     *            markedNode or markedEdge populated appropriately.
     */
    void processDragEventAtPoint(final Point p, final boolean shouldAffectMark) {
        final org.eclipse.draw2d.geometry.Point dropLocation = getDropLocation(p);
        final EditPart ep = m_parentViewer.findObjectAt(dropLocation);
        final NodeContainerEditPart priorNodeSelection = m_node;
        final ConnectionContainerEditPart priorEdgeSelection = m_edge;

        m_node = null;
        m_edge = null;
        m_nodeCount = 0;
        m_edgeCount = 0;

        if (ep instanceof NodeContainerEditPart) {
            final NodeContainerEditPart hit = (NodeContainerEditPart)ep;

            if ((!hit.equals(m_avoidMarkingNode)) && (hit.equals(priorNodeSelection) || (!consultVetoers(hit, null)))) {
                m_node = hit;
                m_nodeCount++;
            }
        } else if (ep instanceof ConnectionContainerEditPart) {
            final ConnectionContainerEditPart hit = (ConnectionContainerEditPart)ep;

            if ((!hit.equals(m_avoidMarkingEdge)) && (hit.equals(priorEdgeSelection) || (!consultVetoers(null, hit)))) {
                m_edge = hit;
                m_edgeCount++;
            }
        }

        if (shouldAffectMark) {
            unmarkSelection();
        }

        if ((m_node != null) && (m_nodeCount >= m_edgeCount)) {
            if (shouldAffectMark) {
                m_markedNode = m_node;
                m_markedNode.markForReplacement();
            }

            // workaround for eclipse bug 393868 (https://bugs.eclipse.org/bugs/show_bug.cgi?id=393868)
            WindowsDNDHelper.hideDragImage();
        } else if (m_edge != null) {
            m_edgeColor = m_edge.getFigure().getForegroundColor();
            m_edgeWidth = ((ProgressPolylineConnection)m_edge.getFigure()).getLineWidth();
            if (shouldAffectMark) {
                m_markedEdge = m_edge;
                ((ProgressPolylineConnection)m_markedEdge.getFigure()).setLineWidth(m_edgeWidth + 3);
                m_markedEdge.getFigure().setForegroundColor(RED);
            }

            // workaround for eclipse bug 393868 (https://bugs.eclipse.org/bugs/show_bug.cgi?id=393868)
            WindowsDNDHelper.hideDragImage();
        }
    }

    void addVetoer(final TargetVetoer tv) {
        synchronized (m_vetoers) {
            m_vetoers.add(tv);
        }
    }

    /**
     * We implement this because we do the checks for marking in event processing to compare against the previous
     * targetting selection before doing the potentially more heavy weight veto checking.
     *
     * This optimization results in the situation where a mouse down which sets the selection followed by a small mouse
     * movement will result in the same selection being carried but the vetoers not being invoked, so a vetoer which
     * seeks to disallow targeting on the mouse down object won't have a chance to veto.
     *
     * Call, <code>clearMarkingAvoidance()</code> to clear this state.
     *
     * This is not thread-safe, though will imaginably always be called on the SWT as part of the event processing.
     */
    void rememberCurrentTargetsToAvoidMarking() {
        m_avoidMarkingNode = m_node;
        m_avoidMarkingEdge = m_edge;
    }

    void clearMarkingAvoidance() {
        m_avoidMarkingNode = null;
        m_avoidMarkingEdge = null;
    }

    /**
     * Unmark node and edge; this nulls out the state concerning *marked* node and edge, but not the node or edge
     * objects last detected during the last invocation of processDragEventAtPoint(...)
     */
    void unmarkSelection() {
        if (m_markedNode != null) {
            m_markedNode.unmarkForReplacement();
            m_markedNode = null;

            // workaround for eclipse bug 393868 (https://bugs.eclipse.org/bugs/show_bug.cgi?id=393868)
            WindowsDNDHelper.showDragImage();
        }

        if (m_markedEdge != null) {
            m_markedEdge.getFigure().setForegroundColor(m_edgeColor);
            ((ProgressPolylineConnection)m_markedEdge.getFigure()).setLineWidth(m_edgeWidth);
            m_markedEdge = null;

            // workaround for eclipse bug 393868 (https://bugs.eclipse.org/bugs/show_bug.cgi?id=393868)
            WindowsDNDHelper.showDragImage();
        }
    }

    /**
     * @return <code>true</code> if either an edge or node has been targeted in the last invocation of
     *         <code>processDragEventAtPoint(...)</code>
     */
    boolean hasATarget() {
        return (m_node != null) || (m_edge != null);
    }

    /**
     * @return the nodeCount
     */
    int getNodeCount() {
        return m_nodeCount;
    }

    /**
     * @return the edgeCount
     */
    int getEdgeCount() {
        return m_edgeCount;
    }

    /**
     * @return the node
     */
    NodeContainerEditPart getNode() {
        return m_node;
    }

    /**
     * @return the edge
     */
    ConnectionContainerEditPart getEdge() {
        return m_edge;
    }


    //
    // Only private functionality follows
    //

    /**
     * Converts the event mouse location to editor relative coordinates.
     *
     * @param the position (relative to whole display)
     * @return point converted to the editor coordinates
     */
    private org.eclipse.draw2d.geometry.Point getDropLocation(final Point p) {
        Point swtPoint = m_parentViewer.getControl().toControl(p.x, p.y);

        return new org.eclipse.draw2d.geometry.Point(swtPoint.x, swtPoint.y);
    }

    /**
     * @return <code>true</code> if the targetting has been vetoed, false otherwise
     */
    private boolean consultVetoers(final NodeContainerEditPart potentialNode,
        final ConnectionContainerEditPart potentialEdge) {
        synchronized (m_vetoers) {
            for (TargetVetoer tv : m_vetoers) {
                if (tv.shouldVetoTarget(potentialNode, potentialEdge)) {
                    return true;
                }
            }
        }

        return false;
    }
}
