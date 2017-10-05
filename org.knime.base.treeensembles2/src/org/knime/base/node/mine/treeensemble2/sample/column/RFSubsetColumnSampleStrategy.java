/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 *
 * History
 *   Jan 4, 2012 (wiswedel): created
 */
package org.knime.base.node.mine.treeensemble2.sample.column;

import java.util.Arrays;

import org.apache.commons.math.random.JDKRandomGenerator;
import org.apache.commons.math.random.RandomData;
import org.apache.commons.math.random.RandomDataImpl;
import org.knime.base.node.mine.treeensemble2.data.TreeData;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeSignature;

/**
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public class RFSubsetColumnSampleStrategy extends ColumnSampleStrategy {

    private final long m_seed;

    private final TreeData m_data;

    private final int m_subsetSize;

    /**
     *  */
    public RFSubsetColumnSampleStrategy(final TreeData data, final RandomData random, final int subsetSize) {
        m_seed = random.nextLong(Long.MIN_VALUE, Long.MAX_VALUE);
        m_data = data;
        int totalColCount = data.getColumns().length;
        if (subsetSize <= 0 || subsetSize > totalColCount) {
            throw new IllegalArgumentException(String.format("column subset size out of bounds (0, %d]: %d",
                totalColCount, subsetSize));
        }
        m_subsetSize = subsetSize;
    }

    /** {@inheritDoc} */
    @Override
    public ColumnSample getColumnSampleForTreeNode(final TreeNodeSignature treeNodeSignature) {
        byte[] signature = treeNodeSignature.getSignaturePath();
        JDKRandomGenerator generator = new JDKRandomGenerator();
        generator.setSeed(m_seed);
        int[] newSeed = new int[signature.length];
        for (int i = 0; i < signature.length; i++) {
            for (int p = 0; p <= signature[i]; p++) {
                newSeed[i] = generator.nextInt();
            }
        }
        generator.setSeed(newSeed);
        int totalColCount = m_data.getColumns().length;
        RandomData rd = new RandomDataImpl(generator);
        int[] includes = rd.nextPermutation(totalColCount, m_subsetSize);
        Arrays.sort(includes);
        return new SubsetColumnSample(m_data, includes);
    }

}
