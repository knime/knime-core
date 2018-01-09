/*
 * -------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 *
 * History
 *    11.10.2006 (Tobias Koetter): created
 */
package org.knime.base.node.viz.histogram.util;

import org.knime.core.node.NodeLogger;

import junit.framework.TestCase;

/**
 * This class implements the JUnit tests of the {@link BinningUtil} class.
 * @author Tobias Koetter, University of Konstanz
 */
public class BinningUtilTest extends TestCase {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(BinningUtilTest.class);
    /**
     * Test method for {@link
     * org.knime.base.node.viz.histogram.util.BinningUtil
     * #createBinInterval(double, double, int, boolean)
     * org.knime.core.data.DataColumnSpec)}.
     */
    public void testCreateBinInterval() {
        double maxVal = 0;
        double minVal = 0;
        int noOfBars = 0;
        double expected = 0;
        boolean isInteger = true;
        double interval = BinningUtil.createBinInterval(maxVal, minVal,
                noOfBars, isInteger);
        assertEquals(interval, expected);

        isInteger = false;
        interval = BinningUtil.createBinInterval(maxVal, minVal,
                noOfBars, isInteger);
        assertEquals(interval, expected);

        maxVal = 10;
        minVal = 0;
        noOfBars = 10;
        expected = 2;
        isInteger = true;
        interval = BinningUtil.createBinInterval(maxVal, minVal,
                noOfBars, isInteger);
        assertEquals(interval, expected);

        isInteger = false;
        expected = 1;
        interval = BinningUtil.createBinInterval(maxVal, minVal,
                noOfBars, isInteger);
        assertEquals(interval, expected);


        assertTrue("Interval: " + interval + " >= Expected: " + expected,
                interval >= expected);
    }

    /**
     * Test method for {@link
     * org.knime.base.node.viz.histogram.util.BinningUtil
     * #createBinStart(double, double)}.
     */
    public void testCreateBinStart() {
        double minVal = 0;
        double interval = 0;
        double expected = 0;
        double start = BinningUtil.createBinStart(minVal, interval, false);
        assertEquals(start, expected);

        minVal = 1;
        interval = 1;
        expected = 0;
        start = BinningUtil.createBinStart(minVal, interval, false);
        assertEquals(start, expected);

        minVal = 2;
        interval = 1;
        expected = 2;
        start = BinningUtil.createBinStart(minVal, interval, false);
        assertEquals(start, expected);

        minVal = -0.20000000018;
        interval = 2;
        start = BinningUtil.createBinStart(minVal, interval, false);
        assertTrue(start <= minVal);

        minVal = 0.20000000018;
        interval = 2;
        start = BinningUtil.createBinStart(minVal, interval, false);
        assertTrue(start <= minVal);

        minVal = 200000018;
        interval = 2;
        start = BinningUtil.createBinStart(minVal, interval, false);
        assertTrue(start <= minVal);

        minVal = -200000018;
        interval = 2;
        start = BinningUtil.createBinStart(minVal, interval, false);
        assertTrue(start <= minVal);

        minVal = 200000018;
        interval = 333333;
        start = BinningUtil.createBinStart(minVal, interval, false);
        assertTrue(start <= minVal);

        minVal = -200000018;
        interval = 333333;
        start = BinningUtil.createBinStart(minVal, interval, false);
        assertTrue(start <= minVal);

        minVal = 15;
        interval = 2;
        start = BinningUtil.createBinStart(minVal, interval, false);
        assertTrue(start <= minVal);

        minVal = -15;
        interval = 2;
        start = BinningUtil.createBinStart(minVal, interval, false);
        assertTrue(start <= minVal);
    }

    /**
     * Test method {@link BinningUtil#smallValueRounder(double, int, boolean)}.
     */
    public void testSmallValueRounder() {
        double val = -0.200000018;
        double result = BinningUtil.smallValueRounder(val, 2, false, false);
        assertTrue(result < val);

        val = -0.200000018;
        result = BinningUtil.smallValueRounder(val, 2, false, true);
        assertTrue(result > val);

        val = 0.200000018;
        result = BinningUtil.smallValueRounder(val, 2, false, true);
        assertTrue(result > val);

        val = 0.200000018;
        result = BinningUtil.smallValueRounder(val, 2, false, false);
        assertTrue(result < val);
    }

    /**
     * Test method for {@link
     * org.knime.base.node.viz.histogram.util.BinningUtil
     * #myRoundedBorders(double, double, int)}.
     */
    public void testMyRoundedBorders() {

        final int noOfDigits = 2;
        try {
            BinningUtil.myRoundedBorders(0.0, 0, noOfDigits);
            fail("Zero or negative increment shouldn't be allowed");
        } catch (final IllegalArgumentException e) {
            //thats fine
        }
        try {
            BinningUtil.myRoundedBorders(0.0, -1, noOfDigits);
            fail("Zero or negative increment shouldn't be allowed");
        } catch (final IllegalArgumentException e) {
            //thats fine
        }
        double baseVal = 0;
        double increment = 0.1;
        double value = baseVal + increment;
        double roundedVal =
            BinningUtil.myRoundedBorders(value, increment, noOfDigits);
        assertTrue(roundedVal >= value);

        baseVal = -0.200000018;
        testIncrement(10, baseVal, noOfDigits);

        baseVal = 0.200000018;
        testIncrement(10, baseVal, noOfDigits);

        baseVal = 2000;
        testIncrement(10, baseVal, noOfDigits);

        baseVal = 2000;

        increment = 10;
        value = baseVal + increment;
        roundedVal = BinningUtil.myRoundedBorders(value, increment, noOfDigits);
        assertTrue(roundedVal >= value);
    }

    private void testIncrement(final int noOfIncrements,
            final double baseVal, final int noOfDigits) {
        double increment;
        double value;
        double roundedVal;
        for(int i = 0; i < noOfIncrements; i++) {
            increment = 1 / Math.pow(10, i);
            value = baseVal + increment;
            roundedVal =
                BinningUtil.myRoundedBorders(value, increment, noOfDigits);
            LOGGER.debug("Increment: " + increment);
            if (value < 0) {
                assertTrue(roundedVal <= value);
            } else {
                assertTrue(roundedVal >= value);
            }
        }
    }

    /**
     * Test the
     * {@link BinningUtil#createBinInterval(double, double, int, boolean)}
     * method.
     */
    public void testIntegerInterval() {
        double interval = BinningUtil.createBinInterval(-1, -3, 2, true);
        assertTrue(interval == 2);
        interval = BinningUtil.createBinInterval(1, -1, 2, true);
        assertTrue(interval == 2);
        interval = BinningUtil.createBinInterval(3, 1, 2, true);
        assertTrue(interval == 2);

        interval = BinningUtil.createBinInterval(-1, -3, 3, true);
        assertTrue(interval == 1);
        interval = BinningUtil.createBinInterval(1, -1, 3, true);
        assertTrue(interval == 1);
        interval = BinningUtil.createBinInterval(3, 1, 3, true);
        assertTrue(interval == 1);
    }
}
