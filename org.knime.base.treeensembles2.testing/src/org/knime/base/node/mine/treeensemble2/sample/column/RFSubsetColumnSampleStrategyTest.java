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
 * History
 *   23.05.2016 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.sample.column;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;
import org.knime.base.node.mine.treeensemble2.learner.TreeNodeSignatureFactory;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeSignature;

/**
 *
 * @author Adrian Nembach, KNIME.com
 */
public class RFSubsetColumnSampleStrategyTest extends AbstractColumnSampleTest {

    /**
     * Tests the method {@link RFSubsetColumnSampleStrategy#getColumnSampleForTreeNode(org.knime.base.node.mine.treeensemble2.model.TreeNodeSignature)}
     *
     * @throws Exception
     */
    @Test
    public void testGetColumnSampleForTreeNode() throws Exception {
        final RFSubsetColumnSampleStrategy strategy = new RFSubsetColumnSampleStrategy(createTreeData(), RD, 5);
        final TreeNodeSignatureFactory sigFac = createSignatureFactory();
        TreeNodeSignature rootSig = sigFac.getRootSignature();
        ColumnSample sample = strategy.getColumnSampleForTreeNode(rootSig);
        assertEquals("Wrong number of columns in sample.", 5, sample.getNumCols());
        int[] colIndices0 = sample.getColumnIndices();
        sample = strategy.getColumnSampleForTreeNode(sigFac.getChildSignatureFor(rootSig, (byte)0));
        assertEquals("Wrong number of columns in sample.", 5, sample.getNumCols());
        int[] colIndices1 = sample.getColumnIndices();
        sample = strategy.getColumnSampleForTreeNode(sigFac.getChildSignatureFor(rootSig, (byte)1));
        assertEquals("Wrong number of columns in sample.", 5, sample.getNumCols());
        int[] colIndices2 = sample.getColumnIndices();
        assertEquals("sample sizes differ.", colIndices0.length, colIndices1.length);
        assertEquals("sample sizes differ.", colIndices0.length, colIndices2.length);
        assertEquals("sample sizes differ.", colIndices1.length, colIndices2.length);
        boolean match = true;
        for (int i = 0; i < colIndices0.length; i++) {
            match = match && colIndices0[i] == colIndices1[i] && colIndices0[i] == colIndices2[i];
            if (!match) {
                break;
            }
        }
        assertFalse("It is very unlikely that we get 3 times the same column sample.", match);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSubsetSizeSmallerZero() throws Exception {
        new RFSubsetColumnSampleStrategy(createTreeData(), RD, -1);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSubsetSizeGreaterNumColumns() throws Exception {
        new RFSubsetColumnSampleStrategy(createTreeData(), RD, TREE_DATA_SIZE + 1);
    }
}
