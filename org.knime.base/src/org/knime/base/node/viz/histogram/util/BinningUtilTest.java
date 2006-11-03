/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *    11.10.2006 (Tobias Koetter): created
 */
package org.knime.base.node.viz.histogram.util;

import junit.framework.TestCase;

/**
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public class BinningUtilTest extends TestCase {

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
        expected = 1;
        isInteger = true;
        interval = BinningUtil.createBinInterval(maxVal, minVal, 
                noOfBars, isInteger);
        assertEquals(interval, expected);
        
        isInteger = false;
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
        double start = BinningUtil.createBinStart(minVal, interval);
        assertEquals(start, expected);
        
        minVal = 1;
        interval = 1;
        expected = 0;
        start = BinningUtil.createBinStart(minVal, interval);
        assertEquals(start, expected);
        
        minVal = 2;
        interval = 1;
        expected = 2;
        start = BinningUtil.createBinStart(minVal, interval);
        assertEquals(start, expected);
    }

    /**
     * Test method for {@link 
     * org.knime.base.node.viz.histogram.util.BinningUtil
     * #myRoundedBorders(double, double, int)}.
     */
    public void testMyRoundedBorders() {
        double value = 0;
        double increment = 0;
        int noOfDigits = 2;
        double roundedVal = 
            BinningUtil.myRoundedBorders(value, increment, noOfDigits);
        assertEquals(roundedVal, value);
    }

}
