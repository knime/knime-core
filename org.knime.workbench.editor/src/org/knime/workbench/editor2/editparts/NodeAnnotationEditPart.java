/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (c) KNIME.com AG, Zurich, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.com
 * email: contact@knime.com
 * ---------------------------------------------------------------------
 *
 * Created: Oct 20, 2011
 * Author: Peter Ohl
 */
package org.knime.workbench.editor2.editparts;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.knime.core.node.workflow.NodeAnnotation;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.node.workflow.NodeUIInformationEvent;
import org.knime.workbench.editor2.figures.AnnotationFigure3;
import org.knime.workbench.editor2.figures.NodeContainerFigure;

/**
 *
 * @author Peter Ohl, KNIME.com AG, Zurich, Switzerland
 */
public class NodeAnnotationEditPart extends AnnotationEditPart {

    /**
     *
     */
    public NodeAnnotationEditPart() {
        // TODO Auto-generated constructor stub
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void nodeUIInformationChanged(final NodeUIInformationEvent evt) {
        WorkflowRootEditPart parent = (WorkflowRootEditPart)getParent();
        NodeAnnotation anno = (NodeAnnotation)getModel();
        AnnotationFigure3 annoFig = (AnnotationFigure3)getFigure();
        annoFig.newContent(anno);
        // node annotation ignores its x/y ui info and hooks itself to its node
        NodeContainer node = anno.getNodeContainer();
        NodeUIInformation nodeUI = node.getUIInformation();
        int x = anno.getX();
        int y = anno.getY();
        int w = anno.getWidth();
        int h = anno.getHeight();
        boolean update = false; // update only if anno has no ui info
        if (w <= 0 || h <= 0) {
            update = true;
            // if the annotation has no width, make it as wide as the node
            if (nodeUI != null && nodeUI.getBounds()[2] > 0) {
                w = nodeUI.getBounds()[2];
            } else {
                w = NodeContainerFigure.WIDTH;
            }
            h = NodeAnnotationEditPart.defaultOneLineHeight();
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
        parent.setLayoutConstraint(this, annoFig, new Rectangle(x, y, w, h));
        refreshVisuals();
    }

}