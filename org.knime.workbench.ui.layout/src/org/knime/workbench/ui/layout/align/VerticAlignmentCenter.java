/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (c) KNIME.com, Zurich, Switzerland
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
 * Created: Mar 31, 2011
 * Author: ohl
 */
package org.knime.workbench.ui.layout.align;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;

/**
 *
 * @author ohl, University of Konstanz
 */
public class VerticAlignmentCenter {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(VerticAlignmentCenter.class);

    private VerticAlignmentCenter() {
        // nothing in here.
    }

    /**
     * @param wfm the manager holding the nodes to layout
     * @param nodeParts the nodes to align
     * @return a map with offsets for the nodes
     */
    static Map<NodeContainerEditPart, Integer> doLayout(
            final WorkflowManager wfm,
            final NodeContainerEditPart[] nodeParts) {

        if (nodeParts.length == 0) {
            return Collections.emptyMap();
        }

        NodeContainerEditPart[] nodes = nodeParts.clone();
        // sorts by the y position
        Arrays.sort(nodes, new Comparator<NodeContainerEditPart>() {
            @Override
            public int compare(final NodeContainerEditPart o1,
                    final NodeContainerEditPart o2) {
                NodeUIInformation ui1 =
                        (NodeUIInformation)o1.getNodeContainer()
                                .getUIInformation();
                NodeUIInformation ui2 =
                        (NodeUIInformation)o2.getNodeContainer()
                                .getUIInformation();
                if (ui1 == null || ui2 == null) {
                    return 0;
                }
                if (ui1.getBounds()[1] < ui2.getBounds()[1]) {
                    return -1;
                } else {
                    return (ui1.getBounds()[1] > ui2.getBounds()[1]) ? 1 : 0;
                }
            }
        });

        // most left node is the anchor that doesn't change
        HashMap<NodeContainerEditPart, Integer> offsets =
                new HashMap<NodeContainerEditPart, Integer>();
        NodeUIInformation nui =
                (NodeUIInformation)nodes[0].getNodeContainer()
                        .getUIInformation();
        if (nui == null) {
            LOGGER.warn("Only nodes with location information can be aligned.");
            return Collections.emptyMap();
        }
        int refX = nui.getBounds()[0];

        for (int i = 1 /* idx 0 is anchor */; i < nodes.length; i++) {
            NodeContainer nc = nodes[i].getNodeContainer();
            NodeUIInformation ui = (NodeUIInformation)nc.getUIInformation();
            if (ui.getBounds()[0] != refX) {
                offsets.put(nodes[i], refX - ui.getBounds()[0]);
            }
        }
        return offsets;
    }

}
