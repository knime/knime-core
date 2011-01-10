/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2011
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
 * -------------------------------------------------------------------
 * 
 * History
 *   Feb 26, 2006 (wiswedel): created
 */
package org.knime.core.node;

import junit.framework.TestCase;

/** Test class to the execution monitor that, so far, only tests the 
 * functionality of sub progresses.
 */
public class ExecutionMonitorTest extends TestCase {

    /**
     * Test method for 'ExecutionMonitor.createSubProgress(double)'.
     */
    public final void testCreateSubProgress1() {
        DefaultNodeProgressMonitor dad = new DefaultNodeProgressMonitor();
        ExecutionMonitor dadEx = new ExecutionMonitor(dad);
        dadEx.setProgress(0.25);
        ExecutionMonitor sub1 = dadEx.createSubProgress(0.5);
        sub1.setProgress(0.5);
        double n = dad.getProgress();
        assertTrue(n > 0.49 && n < 0.51); // should be 0.5
        ExecutionMonitor subsub1 = sub1.createSubProgress(0.5);
        subsub1.setProgress(1);
        n = dad.getProgress();
        assertTrue(n > 0.74 && n < 0.76); // should be 0.75
    }

    /**
     * With different threads accessing the same progress.
     * Test method for 'ExecutionMonitor.createSubProgress(double)'.
     */
    public final void testCreateSubProgress2() {
        DefaultNodeProgressMonitor dad = new DefaultNodeProgressMonitor();
        final ExecutionMonitor dadEx = new ExecutionMonitor(dad);
        Thread[] ts = new Thread[9];
        for (int i = 0; i < ts.length; i++) {
            ts[i] = new Thread(new Runnable() {
                public void run() {
                    ExecutionMonitor sub = dadEx.createSubProgress(0.1);
                    double p = 0.0;
                    while (p < 1) {
                        double n = Math.random() * 0.1;
                        sub.setProgress(Math.min(p + n, 1.0));
                        p += n;
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException ie) {
                            fail();
                        }
                    }
                }
            });
        }
        for (int i = 0; i < ts.length; i++) {
            ts[i].start();
        }
        for (int i = 0; i < ts.length; i++) {
            try {
                ts[i].join();
            } catch (InterruptedException ie) {
                fail();
            }
        }
        // progress should be more or less 0.9
        double n = dad.getProgress();
        assertTrue("progress=" + n, n < 0.91 && n > 0.89);
    }
    
}
