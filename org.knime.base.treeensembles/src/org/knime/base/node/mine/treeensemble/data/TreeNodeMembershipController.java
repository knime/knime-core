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
 *   21.10.2015 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble.data;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Adrian Nembach, KNIME Konstanz
 */
public class TreeNodeMembershipController {

    private Map<String,Integer> m_colName2InternIndex;
    private TreeColumnMembershipController[] m_columnController;


    /**
     * @param treeData
     * @param sampleWeights
     */
    public TreeNodeMembershipController(final TreeData treeData, final double[] sampleWeights) {
        m_colName2InternIndex = new HashMap<String,Integer>();
        TreeAttributeColumnData[] attrCols = treeData.getColumns();
        m_columnController = new TreeColumnMembershipController[attrCols.length];
        for (int i = 0; i < attrCols.length; i++) {
            TreeAttributeColumnData col = attrCols[i];
            String colName = col.getMetaData().getAttributeName();
            m_columnController[i] = new TreeColumnMembershipController(col, sampleWeights);
            m_colName2InternIndex.put(colName, i);
        }
    }

    private TreeNodeMembershipController(final TreeColumnMembershipController[] childController, final Map<String, Integer> colName2InternIndex) {
        m_colName2InternIndex = colName2InternIndex;
        m_columnController = childController;
    }

    /**
     * @param originalIndices
     */
    public void updateColumnMemberships(final int[] originalIndices) {
        for (TreeColumnMembershipController controller : m_columnController) {
            controller.updateMemberships(originalIndices);
        }
    }

    /**
     * @param childWeights
     * @return TreeNodeMembershipController containing indices corresponding to childWeights greater than 0.
     */
    public TreeNodeMembershipController createChildTreeNodeMembershipController(final double[] childWeights) {
        TreeColumnMembershipController[] childController = new TreeColumnMembershipController[m_columnController.length];
        for (int i = 0; i < m_columnController.length; i++) {
            childController[i] = m_columnController[i].createChildTreeColumnMembershipController(childWeights);
        }

        return new TreeNodeMembershipController(childController, m_colName2InternIndex);
    }

    /**
     * @param column
     * @return the TreeColumnMembershipController for <b>column</b>
     */
    public TreeColumnMembershipController getControllerForColumn(final TreeAttributeColumnData column) {
        String colName = column.getMetaData().getAttributeName();
        Integer index = m_colName2InternIndex.get(colName);
        if (index == null) {
            throw new IllegalArgumentException("No controller for column \"" + colName + "\" registered.");
        }
        return m_columnController[index];
    }
}
