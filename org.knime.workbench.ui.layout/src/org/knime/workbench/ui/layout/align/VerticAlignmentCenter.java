/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
                        o1.getNodeContainer()
                        .getUIInformation();
                NodeUIInformation ui2 =
                        o2.getNodeContainer()
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
                nodes[0].getNodeContainer()
                .getUIInformation();
        if (nui == null) {
            LOGGER.warn("Only nodes with location information can be aligned.");
            return Collections.emptyMap();
        }
        int refX = nui.getBounds()[0];

        for (int i = 1 /* idx 0 is anchor */; i < nodes.length; i++) {
            NodeContainer nc = nodes[i].getNodeContainer();
            NodeUIInformation ui = nc.getUIInformation();
            if (ui.getBounds()[0] != refX) {
                offsets.put(nodes[i], refX - ui.getBounds()[0]);
            }
        }
        return offsets;
    }

}
