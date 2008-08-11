/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
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
 * ---------------------------------------------------------------------
 *
 * History
 *   11.08.2008 (thor): created
 */
package org.knime.base.util.math;

import java.util.concurrent.atomic.AtomicInteger;

import org.knime.base.util.math.Combinations.Callback;

import junit.framework.TestCase;

/**
 * Testcase for the Combinations class from base.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class CombinationsTest extends TestCase {
    /**
     * Checks if rank and unrank work together correctly.
     */
    public void testRank() {
        Combinations c = new Combinations(20, 5);
        for (int i = 0; i < c.getNumberOfCombinations(); i++) {
            assertEquals(i, c.rank(c.unrank(i)));
        }
    }

    /**
     * Checks if all combinations are enumerated.
     */
    public void testEnumerate() {
        Combinations c = new Combinations(20, 5);
        final AtomicInteger count = new AtomicInteger();
        Callback cb = new Callback() {
            @Override
            public boolean visit(int[] selected, int n, int k, long index,
                    long max) {
                count.incrementAndGet();
                return true;
            }
        };
        c.enumerate(cb);
        assertEquals(c.getNumberOfCombinations(), count.intValue());
    }
}
