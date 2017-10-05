/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *   20.10.2015 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.data;

import java.util.Arrays;

import org.knime.base.node.mine.treeensemble2.node.learner.TreeEnsembleLearnerConfiguration;

/**
 *
 * @author Adrian Nembach, KNIME.com
 */
public class TreeByteNumericColumnData extends TreeNumericColumnData {

    private byte[] m_sortedData;

    //    private int[] m_originalIndexInColumnList;

    /**
     * @param metaData
     * @param configuration
     * @param sortedData
     * @param originalIndexInColumnList
     */
    protected TreeByteNumericColumnData(final TreeNumericColumnMetaData metaData,
        final TreeEnsembleLearnerConfiguration configuration, final byte[] sortedData,
        final int[] originalIndexInColumnList) {
        super(metaData, configuration, originalIndexInColumnList);
        m_sortedData = sortedData;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getSorted(final int index) {
        if (index < 0) {
            throw new IndexOutOfBoundsException("An index below 0 is not allowed.");
        } else if (index >= m_sortedData.length) {
            throw new IndexOutOfBoundsException("The index is too large.");
        }

        int value = m_sortedData[index] & 0x0FF;
        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getFirstIndexWithValue(final double value) {
        if (value < 0 || value > 255) {
            throw new IllegalArgumentException("value must be in the byte range.");
        }
        final byte byteValue = (byte)Math.ceil(value);
        return Arrays.binarySearch(m_sortedData, byteValue);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getLengthNonMissing() {
        return m_sortedData.length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsMissingValues() {
        return false;
    }

}
