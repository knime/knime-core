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
 *
 * History
 *   Dec 27, 2011 (wiswedel): created
 */
package org.knime.base.node.mine.treeensemble.data;

import org.knime.base.node.mine.treeensemble.model.TreeNodeCondition;
import org.knime.base.node.mine.treeensemble.node.learner.TreeEnsembleLearnerConfiguration;

/**
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class TreeOrdinaryNumericColumnData extends TreeNumericColumnData {

    private final double[] m_sortedData;

//    private final int[] m_originalIndexInColumnList;

    TreeOrdinaryNumericColumnData(final TreeNumericColumnMetaData metaData,
        final TreeEnsembleLearnerConfiguration configuration, final double[] sortedData,
        final int[] orginalIndexInColumnList) {
        super(metaData, configuration, orginalIndexInColumnList);
        m_sortedData = sortedData;
//        m_originalIndexInColumnList = orginalIndexInColumnList;
    }

    /** {@inheritDoc} */
    @Override
    public TreeNumericColumnMetaData getMetaData() {
        return super.getMetaData();
    }



    /**
     * {@inheritDoc}
     */
    @Override
    public TreeNodeMembershipController getChildNodeMembershipController(final TreeNodeCondition childCondition,
        final TreeNodeMembershipController parentController) {

        return parentController;
//        final TreeNodeNumericCondition numCondition = (TreeNodeNumericCondition)childCondition;
//        final NumericOperator numOperator = numCondition.getNumericOperator();
//        final double splitValue = numCondition.getSplitValue();
//        TreeColumnMembershipController columnController = parentController.getControllerForColumn(this);
//        int length = columnController.getLength();
//        ArrayList<Integer> childOriginalIndices = new ArrayList<Integer>();
//
//        for (int i = 0; i < length; i++) {
//            int index = columnController.getSortedIndex(i);
//            final double value = m_sortedData[index];
//            final int originalColIndex = m_originalIndexInColumnList[index];
//            boolean matches;
//            switch (numOperator) {
//                case LessThanOrEqual:
//                    matches = value <= splitValue;
//                    break;
//                case LargerThan:
//                    matches = value > splitValue;
//                    break;
//                default:
//                    throw new IllegalStateException("Unknown operator " + numOperator);
//            }
//            if (matches) {
//                childOriginalIndices.add(originalColIndex);
//            }
//        }
//
//        return parentController.createChildTreeNodeMembershipController(
//            childOriginalIndices.toArray(new Integer[childOriginalIndices.size()]));
    }



    /**
     * {@inheritDoc}
     */
    @Override
    public double getSorted(final int index) {
        if (index < 0) {
            throw new IndexOutOfBoundsException("A negative index is not allowed.");
        } else if (index >= m_sortedData.length) {
            throw new IndexOutOfBoundsException("The index is too large.");
        }
        return m_sortedData[index];
    }



}
