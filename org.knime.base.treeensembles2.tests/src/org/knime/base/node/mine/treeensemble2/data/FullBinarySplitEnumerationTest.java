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

import junit.framework.Assert;

import org.junit.Test;
import org.knime.base.node.mine.treeensemble2.data.TreeNominalColumnData.BinarySplitEnumeration;
import org.knime.base.node.mine.treeensemble2.data.TreeNominalColumnData.FullBinarySplitEnumeration;

/** Tests enumeration of full binary split enumeration.
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class FullBinarySplitEnumerationTest {

    @Test(timeout=2000L)
    public void testBinarySplitEnumerationCountTuples() {
        byte maxNrUniqueValues = 10;
        for (byte nrUniqueValues = 2; nrUniqueValues < maxNrUniqueValues; nrUniqueValues++) {
            BinarySplitEnumeration instance = new FullBinarySplitEnumeration(nrUniqueValues);
            final int expectedTupleCount = (int)Math.pow(2, nrUniqueValues - 1) - 1;
            int count = 0;
            do {
                count++;
            } while (instance.next());
            Assert.assertEquals("For test count = " + nrUniqueValues, expectedTupleCount, count);
        }
    }

    @Test(timeout=2000L)
    public void testEnumerationForCountEquals4() {
        FullBinarySplitEnumeration instance = new FullBinarySplitEnumeration((byte)4);
        Assert.assertEquals(toBigInteger("1000"), instance.getValueMask());
        Assert.assertTrue(instance.next());
        Assert.assertEquals(toBigInteger("1001"), instance.getValueMask());
        Assert.assertTrue(instance.next());
        Assert.assertEquals(toBigInteger("1010"), instance.getValueMask());
        Assert.assertTrue(instance.next());
        Assert.assertEquals(toBigInteger("1011"), instance.getValueMask());
        Assert.assertTrue(instance.next());
        Assert.assertEquals(toBigInteger("1100"), instance.getValueMask());
        Assert.assertTrue(instance.next());
        Assert.assertEquals(toBigInteger("1101"), instance.getValueMask());
        Assert.assertTrue(instance.next());
        Assert.assertEquals(toBigInteger("1110"), instance.getValueMask());

        Assert.assertFalse(instance.next());
    }

    @Test(timeout=2000L)
    public void testEnumerationForCountEquals5() {
        FullBinarySplitEnumeration instance = new FullBinarySplitEnumeration((byte)5);
        Assert.assertEquals(toBigInteger("10000"), instance.getValueMask());
        Assert.assertTrue(instance.next());
        Assert.assertEquals(toBigInteger("10001"), instance.getValueMask());
        Assert.assertTrue(instance.next());
        Assert.assertEquals(toBigInteger("10010"), instance.getValueMask());
        Assert.assertTrue(instance.next());
        Assert.assertEquals(toBigInteger("10011"), instance.getValueMask());
        Assert.assertTrue(instance.next());
        Assert.assertEquals(toBigInteger("10100"), instance.getValueMask());
        Assert.assertTrue(instance.next());
        Assert.assertEquals(toBigInteger("10101"), instance.getValueMask());
        Assert.assertTrue(instance.next());
        Assert.assertEquals(toBigInteger("10110"), instance.getValueMask());
        Assert.assertTrue(instance.next());
        Assert.assertEquals(toBigInteger("10111"), instance.getValueMask());
        Assert.assertTrue(instance.next());
        Assert.assertEquals(toBigInteger("11000"), instance.getValueMask());
        Assert.assertTrue(instance.next());
        Assert.assertEquals(toBigInteger("11001"), instance.getValueMask());
        Assert.assertTrue(instance.next());
        Assert.assertEquals(toBigInteger("11010"), instance.getValueMask());
        Assert.assertTrue(instance.next());
        Assert.assertEquals(toBigInteger("11011"), instance.getValueMask());
        Assert.assertTrue(instance.next());
        Assert.assertEquals(toBigInteger("11100"), instance.getValueMask());
        Assert.assertTrue(instance.next());
        Assert.assertEquals(toBigInteger("11101"), instance.getValueMask());
        Assert.assertTrue(instance.next());
        Assert.assertEquals(toBigInteger("11110"), instance.getValueMask());

        Assert.assertFalse(instance.next());
    }

    private static final BigInteger toBigInteger(final String mask) {
        return BigInteger.valueOf(Long.parseLong(mask, 2));
    }

}
