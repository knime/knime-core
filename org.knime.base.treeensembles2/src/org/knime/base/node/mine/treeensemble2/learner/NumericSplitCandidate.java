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
 *
 * History
 *   Jan 8, 2012 (wiswedel): created
 */
package org.knime.base.node.mine.treeensemble2.learner;

import java.util.BitSet;

import org.knime.base.node.mine.treeensemble2.data.TreeNumericColumnData;
import org.knime.base.node.mine.treeensemble2.data.TreeNumericColumnMetaData;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeNumericCondition;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeNumericCondition.NumericOperator;

/**
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public class NumericSplitCandidate extends SplitCandidate {

    public static final byte NO_MISSINGS = 0;

    public static final byte MISSINGS_GO_LEFT = 1;

    public static final byte MISSINGS_GO_RIGHT = 2;

    private final double m_splitValue;

    private final byte m_missingsStrategy;

    /**
     * @param treeNumericColumnData
     */
    public NumericSplitCandidate(final TreeNumericColumnData treeNumericColumnData, final double splitValue,
        final double gainValue, final BitSet missedRows, final byte missingsStrategy) {
        super(treeNumericColumnData, gainValue, missedRows);
        m_splitValue = splitValue;
        m_missingsStrategy = missingsStrategy;
    }

    /** {@inheritDoc} */
    @Override
    public TreeNumericColumnData getColumnData() {
        return (TreeNumericColumnData)super.getColumnData();
    }

    double getSplitValue() {
        return m_splitValue;
    }

    /** {@inheritDoc} */
    @Override
    public TreeNodeNumericCondition[] getChildConditions() {
        TreeNumericColumnMetaData meta = getColumnData().getMetaData();
        switch (m_missingsStrategy) {
            case (MISSINGS_GO_LEFT) :
                return new TreeNodeNumericCondition[]{
                    new TreeNodeNumericCondition(meta, m_splitValue, NumericOperator.LessThanOrEqual, true),
                    new TreeNodeNumericCondition(meta, m_splitValue, NumericOperator.LargerThan, false)};
            case (MISSINGS_GO_RIGHT) :
                return new TreeNodeNumericCondition[]{
                    new TreeNodeNumericCondition(meta, m_splitValue, NumericOperator.LessThanOrEqual, false),
                    new TreeNodeNumericCondition(meta, m_splitValue, NumericOperator.LargerThan, true)};
            // same as no specified direction
            default :
                return new TreeNodeNumericCondition[]{
                    new TreeNodeNumericCondition(meta, m_splitValue, NumericOperator.LessThanOrEqual, false),
                    new TreeNodeNumericCondition(meta, m_splitValue, NumericOperator.LargerThan, false)};
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean canColumnBeSplitFurther() {
        return true;
    }

}
