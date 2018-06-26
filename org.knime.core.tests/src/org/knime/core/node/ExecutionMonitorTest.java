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
