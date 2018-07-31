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
 */
package org.knime.core.ui.node.workflow;

import java.util.ArrayList;
import java.util.List;

import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.node.workflow.WorkflowAnnotation;
import org.knime.core.node.workflow.WorkflowCopyContent;
import org.knime.core.ui.UI;

/**
 * Represents copied parts of a workflow including their offset.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 * @noreference This interface is not intended to be referenced by clients.
 */
public interface WorkflowCopyWithOffsetUI extends WorkflowCopyUI, UI {

    /**
     * @return the (minimal) x position of the workflow copy
     */
    int getX();

    /**
     * @return the (minimal) y position of the workflow copy
     */
    int getY();

    /**
     * @param shift amount to shift the workflow copy in x direction
     */
    void setXShift(final int shift);

    /**
     * @param shift amount to shift the workflow copy in y direction
     */
    void setYShift(final int shift);

    /**
     * @return the shift in x direction
     */
    public int getXShift();

    /**
     * @return the shift in y direction
     */
    public int getYShift();

    /**
     * Calculates the offset (i.e. minimal x,y-position) of a {@link WorkflowCopyContent}-object. The actual objects
     * (nodes, annotations) to get the positions are provided by the given workflow manager.
     *
     * @param wcc the copy object representing the objects to get the offset for
     * @param wfm workflow manager to get the actual object positions
     * @return 2-dim array containing the offset (x,y)
     */
    static int[] calcOffset(final WorkflowCopyContent wcc, final WorkflowManagerUI wfm) {
        NodeID[] nodes = wcc.getNodeIDs();
        List<int[]> insertedElementBounds = new ArrayList<int[]>();
        for (NodeID i : nodes) {
            NodeContainerUI nc = wfm.getNodeContainer(i);
            NodeUIInformation ui = nc.getUIInformation();
            int[] bounds = ui.getBounds();
            insertedElementBounds.add(bounds);
        }

        WorkflowAnnotation[] annos = wfm.getWorkflowAnnotations(wcc.getAnnotationIDs());
        for (WorkflowAnnotation a : annos) {
            int[] bounds =
                new int[] {a.getX(), a.getY(), a.getWidth(), a.getHeight()};
            insertedElementBounds.add(bounds);
        }
        int smallestX = Integer.MAX_VALUE;
        int smallestY = Integer.MAX_VALUE;
        for (int[] bounds : insertedElementBounds) {
            int currentX = bounds[0];
            int currentY = bounds[1];
            if (currentX < smallestX) {
                smallestX = currentX;
            }
            if (currentY < smallestY) {
                smallestY = currentY;
            }
        }
        return new int[]{smallestX, smallestY};
    }
}
