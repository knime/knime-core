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
 *   Dec 27, 2011 (wiswedel): created
 */
package org.knime.base.node.mine.treeensemble2.data;

import java.util.Arrays;

import org.knime.base.node.mine.treeensemble2.node.learner.TreeEnsembleLearnerConfiguration;

/**
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public class TreeOrdinaryNumericColumnData extends TreeNumericColumnData {

    private final float[] m_sortedData;
    private final int m_indexFirstMissing;
    private final boolean m_containsMissingValues;

//    private final int[] m_originalIndexInColumnList;

    TreeOrdinaryNumericColumnData(final TreeNumericColumnMetaData metaData,
        final TreeEnsembleLearnerConfiguration configuration, final float[] sortedData,
        final int[] orginalIndexInColumnList, final boolean containsMissingValues, final int indexFirstMissing) {
        super(metaData, configuration, orginalIndexInColumnList);
        m_sortedData = sortedData;
        m_indexFirstMissing = indexFirstMissing;
        m_containsMissingValues = containsMissingValues;
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
    public double getSorted(final int index) {
        if (index < 0) {
            throw new IndexOutOfBoundsException("A negative index is not allowed.");
        } else if (index >= m_sortedData.length) {
            throw new IndexOutOfBoundsException("The index is too large.");
        }
        return m_sortedData[index];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getFirstIndexWithValue(final double value) {
        return Arrays.binarySearch(m_sortedData, (float)value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getLengthNonMissing() {
        return m_indexFirstMissing;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsMissingValues() {
        return m_containsMissingValues;
    }

}
