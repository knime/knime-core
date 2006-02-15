/*
 * @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * --------------------------------------------------------------------- *
 *   This source code, its documentation and all appendant files         *
 *   are protected by copyright law. All rights reserved.                *
 *                                                                       *
 *   Copyright, 2003 - 2006                                              *
 *   Universitaet Konstanz, Germany.                                     *
 *   Lehrstuhl fuer Angewandte Informatik                                *
 *   Prof. Dr. Michael R. Berthold                                       *
 *                                                                       *
 *   You may not modify, publish, transmit, transfer or sell, reproduce, *
 *   create derivative works from, distribute, perform, display, or in   *
 *   any way exploit any of the content, in whole or in part, except as  *
 *   otherwise expressly permitted in writing by the copyright owner.    *
 * --------------------------------------------------------------------- *
 */
package de.unikn.knime.core.node.property.hilite;

import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;
import de.unikn.knime.core.data.DataCell;
import de.unikn.knime.core.data.DoubleType;
import de.unikn.knime.core.data.IntType;
import de.unikn.knime.core.data.StringType;
  
/**
 * JUnit test for the <code>DefaultHiLiteHandler</code>.
 * 
 * @see DefaultHiLiteHandler
 *  
 * @author Thomas Gabriel, University of Konstanz
 */


public final class DefaultHiLiteHandlerTest extends TestCase {
          
    /**
     * Internal <code>HiLiteLister</code> which throws exceptions inside the 
     * <code>hiLite(KeyEvent)</code> and <code>unHiLite(KeyEvent)</code> 
     * methods depending on the current hilit items. 
     */
    final class MyTestHiLiteListener implements HiLiteListener {
        private final HiLiteHandler m_hdl;
        /*
         * Accessed by outer class to set the currently hilit items.
         */
        private final HashSet<DataCell> m_set = new HashSet<DataCell>();
        /**
         * Create a new test listener.
         * @param hdl Internal hilite handler reference to check hilite status.
         */
        public MyTestHiLiteListener(final HiLiteHandler hdl) {
            m_hdl = hdl;
        }
        /**
         * @see HiLiteListener#hiLite(KeyEvent)
         */
        public void hiLite(final KeyEvent event) {
            if (event == null || event.keys().isEmpty()) {
                throw new IllegalArgumentException(
                    "The event cannot be null or empty.");
            }
            final Set<DataCell> eset = event.keys();
            boolean fail = !eset.equals(m_set);
            for (DataCell c : eset) {
                if (!m_hdl.isHiLit(c)) {
                    fail = false;
                    break;
                }
            }
            String msg = m_set.toString();
            m_set.clear();
            assertFalse(eset.isEmpty());
            if (fail) {
                throw new IllegalStateException(
                    "The KeyEvent contains not the same keys to hilite: "
                        + msg + " <> " + eset);
            }

        }
        /**
         * @see HiLiteListener#unHiLite(KeyEvent)
         */
        public void unHiLite(final KeyEvent event) {
            if (event == null || event.keys().isEmpty()) {
                throw new IllegalArgumentException(
                    "The event cannot be null or empty.");
            }
            Set<DataCell> eset = event.keys();
            boolean fail = !eset.equals(m_set);
            for (DataCell c : eset) {
                if (m_hdl.isHiLit(c)) {
                    fail = false;
                    break;
                }
            }
            m_set.clear();
            assertFalse(eset.isEmpty());
            if (fail) {
                throw new IllegalStateException(
                    "The KeyEvent contains not the same keys to unhilite: "
                        + m_set + " <> " + eset);
            }
        }
        /**
         * Clears and adds the given cell to the set.
         * @param cell The data cell to add.
         */
        final void set(final DataCell cell) {
            m_set.clear();     
            m_set.add(cell);
        }
        /**
         * Clears and adds the all given data cells to the set.
         * @param set The set to get the cells from.
         */
        final void set(final Set<DataCell> set) {
            m_set.clear();     
            m_set.addAll(set);
        }
        /**
         * @see HiLiteListener#resetHiLite()
         */
        public void resetHiLite() {
            // TODO Auto-generated method stub
        }
    }   // MyTestHiLiteHandler
    /*
     * Init three different data cells. 
     */
    private DataCell m_c1 = StringType.STRING_TYPE.getMissingCell();
    private DataCell m_c2 = DoubleType.DOUBLE_TYPE.getMissingCell();
    private DataCell m_c3 = IntType.INT_TYPE.getMissingCell();
    /*
     * Init data cell set with m_c1 and m_c2.
     */
    private HashSet<DataCell> m_s1;
    /*
     * Init data cell set with m_c2 and m_c3.
     */
    private HashSet<DataCell> m_s2;

    /**
     * Init internal members.
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        m_s1 = new HashSet<DataCell>();
        m_s1.add(m_c1);
        assertTrue(m_s1.contains(m_c1));
        m_s1.add(m_c2);
        assertTrue(m_s1.contains(m_c2));
        m_s2 = new HashSet<DataCell>();
        m_s2.add(m_c2);
        assertTrue(m_s2.contains(m_c2));
        m_s2.add(m_c3);
        assertTrue(m_s2.contains(m_c3));
    }
    
    /**
     * Destroy internal members.
     * @see junit.framework.TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
        m_s1 = null;
        m_s2 = null;
        m_c1 = null;
        m_c2 = null;
        m_c3 = null;
    }
    
    /**
     * Test adding and removing hilite listener.
     */
    public void test1() {
        HiLiteHandler hdl = new DefaultHiLiteHandler();
        HiLiteListener l1 = new MyTestHiLiteListener(hdl);
        HiLiteListener l2 = new MyTestHiLiteListener(hdl);
        hdl.addHiLiteListener(l1);
        hdl.addHiLiteListener(l2);
        hdl.removeHiLiteListener(l1);
        hdl.removeHiLiteListener(l2);
    }

    /**
     * Test exceptions for adding and removing hilite listeners.
     */
    public void test2() {
        HiLiteHandler hdl = new DefaultHiLiteHandler();
        HiLiteListener l1 = new MyTestHiLiteListener(hdl);
        HiLiteListener l2 = new MyTestHiLiteListener(hdl);
        hdl.addHiLiteListener(l1);
        hdl.addHiLiteListener(l1);
        hdl.removeHiLiteListener(l2);
        hdl.removeHiLiteListener(l1);
        hdl.removeHiLiteListener(l1);
        hdl.addHiLiteListener(null);
        hdl.removeHiLiteListener(null);
    }
    
    /**
     * Test exceptions during hilite and unhilite. 
     */
    public void test3() {
        HiLiteHandler hdl = new DefaultHiLiteHandler();
        HiLiteListener l1 = new MyTestHiLiteListener(hdl);
        HiLiteListener l2 = new MyTestHiLiteListener(hdl);
        hdl.addHiLiteListener(l1);
        hdl.addHiLiteListener(l2);
        
        try {
            hdl.hiLite((DataCell) null);
            fail("Exception expected: null DataCell to hilite.");
        } catch (Exception e) {
            assertTrue(true);
        }
        
        try {
            hdl.hiLite((Set<DataCell>) null);
            fail("Exception expected: null DataCellSet to hilite.");
        } catch (Exception e) {
            assertTrue(true);
        }
        
        try {
            hdl.unHiLite((DataCell) null);
            fail("Exception expected: null DataCell to unhilite.");
        } catch (Exception e) {
            assertTrue(true);
        }
        
        try {
            hdl.unHiLite((Set<DataCell>) null);
            fail("Exception expected: null DataCellSet to unhilite.");
        } catch (Exception e) {
            assertTrue(true);
        }
        
    }
    
    /**
     * Test hiliting and unhiliting for data cells. 
     */
    public void test4() {
        // init and register handler/listeners
        HiLiteHandler hdl = new DefaultHiLiteHandler();
        MyTestHiLiteListener l1 = new MyTestHiLiteListener(hdl);
        MyTestHiLiteListener l2 = new MyTestHiLiteListener(hdl);
        hdl.addHiLiteListener(l1);
        hdl.addHiLiteListener(l2);
        // ensure c1 and c2 are not hilit
        assertFalse(hdl.isHiLit(m_c1));
        assertFalse(hdl.isHiLit(m_c2));
        
        // hilite c1
        l1.set(m_c1);
        l2.set(m_c1);
        hdl.hiLite(m_c1);
        assertTrue(hdl.isHiLit(m_c1));
        hdl.hiLite(m_c1);
        assertTrue(hdl.isHiLit(m_c1));
        
        // hilite c2
        l1.set(m_c2);
        l2.set(m_c2);
        hdl.hiLite(m_c2);
        assertTrue(hdl.isHiLit(m_c2));
        hdl.hiLite(m_c2);
        assertTrue(hdl.isHiLit(m_c2));

        // unhilite c1
        l1.set(m_c1);
        l2.set(m_c1);
        hdl.unHiLite(m_c1);
        assertFalse(hdl.isHiLit(m_c1));
        hdl.unHiLite(m_c1);
        assertFalse(hdl.isHiLit(m_c1));

        // unhilite c2
        l1.set(m_c2);
        l2.set(m_c2);
        hdl.resetHiLite();
        assertFalse(hdl.isHiLit(m_c2));
        hdl.unHiLite(m_c2);
        assertFalse(hdl.isHiLit(m_c2));
        
    }
    
    /**
     * Tests exceptions for hiliting and unhiliting data cells.
     */
    public void test5() {
        // init and register handler/listeners
        HiLiteHandler hdl = new DefaultHiLiteHandler();
        MyTestHiLiteListener l = new MyTestHiLiteListener(hdl);
        hdl.addHiLiteListener(l);
        // ensure c1 and c2 are not hilit
        assertFalse(hdl.isHiLit(m_c1));
        assertFalse(hdl.isHiLit(m_c2));
        
        // hilite c1
        try {
            hdl.hiLite(m_c1);
            fail("Exception expected: has not yet been unhilit.");
        } catch (Exception e) {
            assertTrue(true);
        }
        assertTrue(hdl.isHiLit(m_c1));
        hdl.hiLite(m_c1);
        assertTrue(hdl.isHiLit(m_c1));
        
        // hilite c2
        try {
            hdl.hiLite(m_c2);
            fail("Exception expected: has not yet been unhilit.");
        } catch (Exception e) {
            assertTrue(true);
        }
        assertTrue(hdl.isHiLit(m_c2));
        hdl.hiLite(m_c2);
        assertTrue(hdl.isHiLit(m_c2));
        
        // unhilite c1
        try {
            hdl.unHiLite(m_c1);
            fail("Exception expected: has not yet been unhilit.");
        } catch (Exception e) {
            assertTrue(true);
        }
        assertFalse(hdl.isHiLit(m_c1));
        hdl.unHiLite(m_c1);
        assertFalse(hdl.isHiLit(m_c1));
        
        // unhilite c2
        try {
            hdl.resetHiLite();
            fail("Exception expected: has not yet been unhilit.");
        } catch (Exception e) {
            assertTrue(true);
        }
        assertFalse(hdl.isHiLit(m_c2));
        hdl.unHiLite(m_c2);
        assertFalse(hdl.isHiLit(m_c2));
        
    }
    
    /**
     * Tests hiliting and unhiliting for data cell sets.
     */
    public void test6() {
        // init and register handler/listeners
        HiLiteHandler hdl = new DefaultHiLiteHandler();
        MyTestHiLiteListener l1 = new MyTestHiLiteListener(hdl);
        MyTestHiLiteListener l2 = new MyTestHiLiteListener(hdl);
        hdl.addHiLiteListener(l1);
        hdl.addHiLiteListener(l2);
        // ensure c1, c2, and c3 are not hilit
        assertFalse(hdl.isHiLit(m_c1));
        assertFalse(hdl.isHiLit(m_c2));
        assertFalse(hdl.isHiLit(m_c3));
        
        // hilite s1
        l1.set(m_s1);
        l2.set(m_s1);
        hdl.hiLite(m_s1);
        assertTrue(hdl.isHiLit(m_c1));
        assertTrue(hdl.isHiLit(m_c2));
        hdl.hiLite(m_s1);
        assertTrue(hdl.isHiLit(m_c1));
        assertTrue(hdl.isHiLit(m_c2));
        
        // hilite c2
        assertTrue(hdl.isHiLit(m_c2));
        hdl.hiLite(m_c2);
        assertTrue(hdl.isHiLit(m_c2));
        
        // hilite s2
        l1.set(m_c3);
        l2.set(m_c3);
        hdl.hiLite(m_s2);
        assertTrue(hdl.isHiLit(m_c1));
        assertTrue(hdl.isHiLit(m_c2));
        assertTrue(hdl.isHiLit(m_c3));
        hdl.hiLite(m_s2);
        assertTrue(hdl.isHiLit(m_c1));
        assertTrue(hdl.isHiLit(m_c2));
        assertTrue(hdl.isHiLit(m_c3));
        
        // unhilite s1
        l1.set(m_s1);
        l2.set(m_s1);
        hdl.unHiLite(m_s1);
        assertFalse(hdl.isHiLit(m_c1));
        assertFalse(hdl.isHiLit(m_c2));
        assertTrue(hdl.isHiLit(m_c3));
        hdl.unHiLite(m_c1);
        assertFalse(hdl.isHiLit(m_c1));
        assertFalse(hdl.isHiLit(m_c2));
        assertTrue(hdl.isHiLit(m_c3));
        
        // unhilite c3
        l1.set(m_c3);
        l2.set(m_c3);
        hdl.resetHiLite();
        assertFalse(hdl.isHiLit(m_c2));
        hdl.unHiLite(m_c3);
        assertFalse(hdl.isHiLit(m_c2));
        
        // unhilite s2
        l1.set(m_c3);
        l2.set(m_c3);
        hdl.unHiLite(m_s2);
        assertFalse(hdl.isHiLit(m_c3));
        hdl.unHiLite(m_s2);
        assertFalse(hdl.isHiLit(m_c3));
        
    }
    
    /**
     * Tests exceptions for hiliting and unhiliting data cell sets.
     */
    public void test7() {
        // init and register handler/listeners
        HiLiteHandler hdl = new DefaultHiLiteHandler();
        MyTestHiLiteListener l1 = new MyTestHiLiteListener(hdl);
        MyTestHiLiteListener l2 = new MyTestHiLiteListener(hdl);
        hdl.addHiLiteListener(l1);
        hdl.addHiLiteListener(l2);
        // ensure c1, c2, and c3 are not hilit
        assertFalse(hdl.isHiLit(m_c1));
        assertFalse(hdl.isHiLit(m_c2));
        assertFalse(hdl.isHiLit(m_c3));
        
        // hilite s1
        try {
            hdl.hiLite(m_s1);
            fail("Exception expected.");
        } catch (Exception e) {
            assertTrue(true);
        }
        assertTrue(hdl.isHiLit(m_c1));
        assertTrue(hdl.isHiLit(m_c2));
        hdl.hiLite(m_s1);
        assertTrue(hdl.isHiLit(m_c1));
        assertTrue(hdl.isHiLit(m_c2));
        
        // hilite c2
        assertTrue(hdl.isHiLit(m_c2));
        hdl.hiLite(m_c2);
        assertTrue(hdl.isHiLit(m_c2));
        
        // hilite s2
        try {
            hdl.hiLite(m_s2);
            fail("Exception expected.");
        } catch (Exception e) {
            assertTrue(true);
        }
        assertTrue(hdl.isHiLit(m_c1));
        assertTrue(hdl.isHiLit(m_c2));
        assertTrue(hdl.isHiLit(m_c3));
        hdl.hiLite(m_s2);
        assertTrue(hdl.isHiLit(m_c1));
        assertTrue(hdl.isHiLit(m_c2));
        assertTrue(hdl.isHiLit(m_c3));
        
        // unhilite s1
        try {
            hdl.unHiLite(m_s1);
            fail("Exception expected.");
        } catch (Exception e) {
            assertTrue(true);
        }
        assertFalse(hdl.isHiLit(m_c1));
        assertFalse(hdl.isHiLit(m_c2));
        assertTrue(hdl.isHiLit(m_c3));
        hdl.unHiLite(m_c1);
        assertFalse(hdl.isHiLit(m_c1));
        assertFalse(hdl.isHiLit(m_c2));
        assertTrue(hdl.isHiLit(m_c3));
        
        // unhilite c3
        try {
            hdl.unHiLite(m_c3);
            fail("Exception expected.");
        } catch (Exception e) {
            assertTrue(true);
        }
        assertFalse(hdl.isHiLit(m_c3));
        hdl.unHiLite(m_c2);
        assertFalse(hdl.isHiLit(m_c3));
        
        // unhilite s2
        assertFalse(hdl.isHiLit(m_c1));
        assertFalse(hdl.isHiLit(m_c2));
        assertFalse(hdl.isHiLit(m_c3));
        hdl.resetHiLite();
        assertFalse(hdl.isHiLit(m_c1));
        assertFalse(hdl.isHiLit(m_c2));
        assertFalse(hdl.isHiLit(m_c3));

        
    }    
    /**
     * System entry point.
     * @param args Parameters from command line: ignored.
     */
    public static void main(final String[] args) {
        junit.textui.TestRunner.run(DefaultHiLiteHandlerTest.class);
    }

}   // DefaultHiLiteHandlerTest
