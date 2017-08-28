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
 */
package org.knime.workbench.editor2.editparts;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.DragTracker;
import org.eclipse.gef.Request;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Display;
import org.knime.core.def.node.workflow.IAnnotation;
import org.knime.core.def.node.workflow.INodeAnnotation;
import org.knime.core.def.node.workflow.INodeContainer;
import org.knime.core.def.node.workflow.NodeUIInformation;
import org.knime.core.def.node.workflow.NodeUIInformationEvent;
import org.knime.workbench.editor2.WorkflowSelectionDragEditPartsTracker;
import org.knime.workbench.editor2.figures.NodeAnnotationFigure;
import org.knime.workbench.editor2.figures.NodeContainerFigure;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.knime.workbench.ui.preferences.PreferenceConstants;

/**
 *
 * @author Peter Ohl, KNIME.com AG, Zurich, Switzerland
 */
public class NodeAnnotationEditPart extends AnnotationEditPart {

    /**
     * font actually used to compute the bounds of the annotation figure.
     */
    private Font m_lastDefaultFont = null;

    /**
     * {@inheritDoc}
     */
    @Override
    public void nodeUIInformationChanged(final NodeUIInformationEvent evt) {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                WorkflowRootEditPart parent = (WorkflowRootEditPart)getParent();
                INodeAnnotation anno = (INodeAnnotation)getModel();
                NodeAnnotationFigure annoFig = (NodeAnnotationFigure)getFigure();
                annoFig.newContent(anno);
                // node annotation ignores its x/y ui info and hooks itself to its node
                INodeContainer node = anno.getNodeContainer();
                if (node == null) {
                    // may happen if the node is disposed before this runnable is executed
                    return;
                }
                NodeUIInformation nodeUI = node.getUIInformation();
                int x = anno.getX();
                int y = anno.getY();
                int w = anno.getWidth();
                int h = anno.getHeight();
                boolean update = false; // update only if anno has no ui info
                if (w <= 0 || h <= 0) {
                    /* this code can be removed (but not for a bug fix release) as this
                     * method is called at least twice during activation and the 2nd
                     * invocation has valid bounds. To reproduce: create flow w/ v2.4,
                     * load in v2.5+ and set a break point
                     *
                     * TODO: remove if block + don't save bounds in node annotation
                     * (always computed automatically)
                     */
                    update = true;
                    // if the annotation has no width, make it as wide as the node
                    if (nodeUI != null && nodeUI.getBounds()[2] > 0) {
                        // pre2.5 flows used the node width as label width
                        w = nodeUI.getBounds()[2];
                    }
                    // make it at least wide enough to hold "Node 9999xxxxxxxxx"
                    w = Math.max(w, getNodeAnnotationMinWidth());
                    h = getNodeAnnotationMinHeight();
                } else {
                    // recalculate the dimension according to the default font (which
                    // could change through the pref page or on different OS)
                    Font currDefFont = AnnotationEditPart.getNodeAnnotationDefaultFont();
                    if (!currDefFont.equals(m_lastDefaultFont)) {
                        m_lastDefaultFont = currDefFont;
                        Dimension textBounds = annoFig.getPreferredSize();
                        h = Math.max(textBounds.height, getNodeAnnotationMinHeight());
                        w = Math.max(textBounds.width, getNodeAnnotationMinWidth());
                    }
                }
                if (nodeUI != null) {
                    NodeContainerEditPart nodePart =
                        (NodeContainerEditPart)getViewer().getEditPartRegistry().get(node);
                    Point offset;
                    int nodeHeight;
                    int symbFigWidth;
                    if (nodePart != null) {
                        NodeContainerFigure fig = (NodeContainerFigure)nodePart.getFigure();
                        offset = fig.getOffsetToRefPoint(nodeUI);
                        nodeHeight = fig.getPreferredSize().height;
                        symbFigWidth = fig.getSymbolFigure().getPreferredSize().width;
                    } else {
                        offset = new Point(65, 35);
                        nodeHeight = NodeContainerFigure.HEIGHT;
                        symbFigWidth = 32;
                    }
                    int[] nodeBounds = nodeUI.getBounds();
                    int mid = nodeBounds[0] + (symbFigWidth / 2);
                    x = mid - (w / 2);
                    y = nodeBounds[1] + nodeHeight + 1 - offset.y;
                    update = true;
                }
                if (update) {
                    anno.setDimensionNoNotify(x, y, w, h);
                }
                parent.setLayoutConstraint(NodeAnnotationEditPart.this, annoFig, new Rectangle(x, y, w, h));
                refreshVisuals();
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected IFigure createFigure() {
        IAnnotation anno = getModel();
        NodeAnnotationFigure f = new NodeAnnotationFigure(anno);
        return f;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DragTracker getDragTracker(final Request request) {
        return new WorkflowSelectionDragEditPartsTracker(this);
    }

    /**
     * @return the minimum width of a node annotation.
     */
    public static int getNodeAnnotationMinWidth() {
        // make it at least wide enough to hold "Node 9999xxxxxxxxx"
        String prefix =
                KNIMEUIPlugin
                        .getDefault()
                        .getPreferenceStore()
                        .getString(PreferenceConstants.P_DEFAULT_NODE_LABEL);
        if (prefix == null || prefix.isEmpty()) {
            prefix = "Node";
        }
        int minTextW =
                AnnotationEditPart.nodeAnnotationDefaultLineWidth(prefix
                        + " 9999xxxxxxxxx");
        // but not less than the node default width
        return Math.max(minTextW, NodeContainerFigure.WIDTH);
    }

    public static int getNodeAnnotationMinHeight() {
        return AnnotationEditPart.nodeAnnotationDefaultOneLineHeight();
    }
}
