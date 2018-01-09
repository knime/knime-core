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
 * ------------------------------------------------------------------------
 *
 * History
 *   Dec 28, 2011 (wiswedel): created
 */
package org.knime.base.node.mine.treeensemble2.data;

import java.math.BigInteger;
import java.util.HashSet;

import org.apache.commons.math.random.RandomDataImpl;
import org.junit.Assert;
import org.junit.Test;
import org.knime.base.node.mine.treeensemble2.data.TreeNominalColumnData.FullBinarySplitEnumeration;
import org.knime.base.node.mine.treeensemble2.data.TreeNominalColumnData.RandomBinarySplitEnumeration;

/**
 * Tests random binary split generation.
 * @author Bernd Wiswedel, KNIME.com, Zurich. Switzerland
 */
public class RandomBinarySplitEnumerationTest {

    /** Tests random enumeration on all possible masks. All valid values must be returned, no duplicates
     * expected. */
    @Test
    public void testFullEnumeration() {
        final int valueCount = 16;
        final int validCount = (int)Math.pow(2, valueCount - 1) - 1;

        /* Reference enumeration */
        final FullBinarySplitEnumeration fullEnum = new FullBinarySplitEnumeration(valueCount);
        final HashSet<BigInteger> allAvailableBS = new HashSet<>();
        do {
            allAvailableBS.add(fullEnum.getValueMask());
        } while(fullEnum.next());

        /* random enum to test */
        RandomBinarySplitEnumeration randomEnum = new RandomBinarySplitEnumeration(
            valueCount, validCount, new RandomDataImpl());
        int i = 0;
        do {
            BigInteger valueMask = randomEnum.getValueMask();
            if (!allAvailableBS.remove(valueMask)) {
                Assert.fail("value mask appeared twice: " + valueMask + " (this is iteration " + i + ".)");
            }
            i++;
        } while (randomEnum.next());

        if (!allAvailableBS.isEmpty()) {
            Assert.fail("Some value masks not returned (" + allAvailableBS.size() + " left)");
        }
    }

}
