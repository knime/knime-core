/*
 * --------------------------------------------------------------------- *
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
 * --------------------------------------------------------------------- *
 *
 * 2006-06-08 (tm): reviewed
 */
package org.knime.core.node.property.hilite;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.knime.core.data.RowKey;

/**
 * JUnit test for the <code>HiLiteHandler</code>.
 *
 * @see HiLiteHandler
 *
 * @author Thomas Gabriel, University of Konstanz
 */
public final class DefaultHiLiteHandlerTest  {
    private final RowKey m_c1 = new RowKey("m_c1");
    private final RowKey m_c2 = new RowKey("m_c2");
    private final RowKey m_c3 = new RowKey("m_c3");

    /** Set with m_c1 and m_c2. */
    private Set<RowKey> m_s12;
    /** Set with m_c2 and m_c3. */
    private Set<RowKey> m_s23;

    private HiLiteHandler m_hdl;
    private HiLiteListener m_l1;
    private HiLiteListener m_l2;


    private class MyHiLiteListener implements HiLiteListener {
        @Override
        public void hiLite(final KeyEvent event) {
        }
        @Override
        public void unHiLite(final KeyEvent event) {
        }
        @Override
        public void unHiLiteAll(final KeyEvent event) {
        }
    }

    /**
     * Init internal members.
     * @throws Exception If setup failed.
     */
    @Before
    public void setUp() throws Exception {
        m_hdl = new HiLiteHandler();
        m_l1 = new MyHiLiteListener();
        m_l2 = new MyHiLiteListener();

        m_s12 = new HashSet<RowKey>();
        m_s12.add(m_c1);
        assertTrue(m_s12.contains(m_c1));
        m_s12.add(m_c2);
        assertTrue(m_s12.contains(m_c2));
        assertTrue(m_s12.size() == 2);
        m_s23 = new HashSet<RowKey>();
        m_s23.add(m_c2);
        assertTrue(m_s23.contains(m_c2));
        m_s23.add(m_c3);
        assertTrue(m_s23.contains(m_c3));
        assertTrue(m_s23.size() == 2);
    }

    /**
     * Test adding and removing hilite listener.
     */
    @Test
    public void test1() {
        m_hdl.addHiLiteListener(m_l1);
        m_hdl.addHiLiteListener(m_l1);

        m_hdl.addHiLiteListener(m_l2);
        m_hdl.addHiLiteListener(m_l2);

        m_hdl.removeHiLiteListener(m_l1);
        m_hdl.removeHiLiteListener(m_l1);

        m_hdl.removeHiLiteListener(m_l2);
        m_hdl.removeHiLiteListener(m_l2);

        m_hdl.addHiLiteListener(null);
        m_hdl.removeHiLiteListener(null);

        m_hdl.addHiLiteListener(m_l1);
        m_hdl.addHiLiteListener(m_l2);
        m_hdl.removeAllHiLiteListeners();
    }

    /**
     * Test exceptions during hilite and unhilite.
     */
    @Test
    public void test3() {
        m_hdl.addHiLiteListener(m_l1);
        m_hdl.addHiLiteListener(m_l2);
        try {
            m_hdl.fireHiLiteEvent((RowKey) null);
            fail("Null keys should not be allowed");
        } catch (IllegalArgumentException e) {
        }
        try {
            m_hdl.fireHiLiteEvent((Set<RowKey>) null);
            fail("Null keys should not be allowed");
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        try {
            m_hdl.fireUnHiLiteEvent((RowKey) null);
            fail("Null keys should not be allowed");
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
        try {
            m_hdl.fireUnHiLiteEvent((Set<RowKey>) null);
            fail("Null keys should not be allowed");
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
        try {
            assertFalse(m_hdl.isHiLit((RowKey) null));
            fail("Null keys should not be allowed");
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
        m_hdl.removeAllHiLiteListeners();
    }

    /**
     * Test hiliting and unhiliting for data cells.
     */
    @Test
    public void test4() {
        m_hdl.addHiLiteListener(m_l1);
        m_hdl.addHiLiteListener(m_l2);
        // ensure c1 and c2 are not hilit
        assertFalse(m_hdl.isHiLit(m_c1));
        assertFalse(m_hdl.isHiLit(m_c2));

        // hilite c1
        m_hdl.fireHiLiteEvent(m_c1);
        assertTrue(m_hdl.isHiLit(m_c1));
        m_hdl.fireHiLiteEvent(m_c1);
        assertTrue(m_hdl.isHiLit(m_c1));

        // hilite c2
        m_hdl.fireHiLiteEvent(m_c2);
        assertTrue(m_hdl.isHiLit(m_c2));
        m_hdl.fireHiLiteEvent(m_c2);
        assertTrue(m_hdl.isHiLit(m_c2));

        // unhilite c1
        m_hdl.fireUnHiLiteEvent(m_c1);
        assertFalse(m_hdl.isHiLit(m_c1));
        m_hdl.fireUnHiLiteEvent(m_c1);
        assertTrue(true);
        assertFalse(m_hdl.isHiLit(m_c1));

        // unhilite c2
        m_hdl.fireClearHiLiteEvent();
        assertFalse(m_hdl.isHiLit(m_c2));
        m_hdl.fireUnHiLiteEvent(m_c2);
        assertFalse(m_hdl.isHiLit(m_c2));

        m_hdl.removeAllHiLiteListeners();
    }

    /**
     * Tests exceptions for hiliting and unhiliting data cells.
     */
    @Test
    public void test5() {
        m_hdl.addHiLiteListener(m_l1);
        m_hdl.addHiLiteListener(m_l2);
        // ensure c1 and c2 are not hilit
        assertFalse(m_hdl.isHiLit(m_c1));
        assertFalse(m_hdl.isHiLit(m_c2));

        // hilite c1
        m_hdl.fireHiLiteEvent(m_c1);
        assertTrue(m_hdl.isHiLit(m_c1));

        // hilite c2
        m_hdl.fireHiLiteEvent(m_c2);
        assertTrue(m_hdl.isHiLit(m_c2));

        // unhilite c1
        m_hdl.fireUnHiLiteEvent(m_c1);
        assertFalse(m_hdl.isHiLit(m_c1));
        m_hdl.fireUnHiLiteEvent(m_c1);

        // unhilite all
        m_hdl.fireClearHiLiteEvent();
        assertFalse(m_hdl.isHiLit(m_c1));
        assertFalse(m_hdl.isHiLit(m_c2));
    }

    /**
     * Tests hiliting and unhiliting for data cell sets.
     */
    @Test
    public void test6() {
        m_hdl.addHiLiteListener(m_l1);
        m_hdl.addHiLiteListener(m_l2);
        // ensure c1, c2, and c3 are not hilit
        assertFalse(m_hdl.isHiLit(m_c1));
        assertFalse(m_hdl.isHiLit(m_c2));
        assertFalse(m_hdl.isHiLit(m_c3));

        // hilite s1
        m_hdl.fireHiLiteEvent(m_s12);
        assertTrue(m_hdl.isHiLit(m_c1));
        assertTrue(m_hdl.isHiLit(m_c2));
        m_hdl.fireHiLiteEvent(m_s12);

        // hilite c3
        assertFalse(m_hdl.isHiLit(m_c3));
        m_hdl.fireHiLiteEvent(m_c3);
        assertTrue(m_hdl.isHiLit(m_c3));

        // hilite s2
        m_hdl.fireHiLiteEvent(m_s23);
        assertTrue(m_hdl.isHiLit(m_c1, m_c2, m_c3));

        // unhilite s1
        m_hdl.fireUnHiLiteEvent(m_s12);
        assertFalse(m_hdl.isHiLit(m_c1));
        assertFalse(m_hdl.isHiLit(m_c2));
        assertTrue(m_hdl.isHiLit(m_c3));
        m_hdl.fireUnHiLiteEvent(m_c1);

        // unhilite c3
        assertTrue(m_hdl.isHiLit(m_c3));
        m_hdl.fireUnHiLiteEvent(m_c3);
        assertFalse(m_hdl.isHiLit(m_c3));

        // unhilite s2
        m_hdl.fireUnHiLiteEvent(m_s23);
    }

    /**
     * Tests exceptions for hiliting and unhiliting data cell sets.
     */
    @Test
    public void test7() {
        m_hdl.addHiLiteListener(m_l1);
        m_hdl.addHiLiteListener(m_l2);
        // ensure c1, c2, and c3 are not hilit
        assertFalse(m_hdl.isHiLit(m_c1, m_c2, m_c3));

        // hilite s1
        m_hdl.fireHiLiteEvent(m_s12);
        assertTrue(m_hdl.isHiLit(m_c1, m_c2));
        assertFalse(m_hdl.isHiLit(m_c3));

        // hilite c2
        m_hdl.fireHiLiteEvent(m_c2);

        // hilite s2
        m_hdl.fireHiLiteEvent(m_s23);
        assertTrue(m_hdl.isHiLit(m_c1, m_c2, m_c3));

        // unhilite s1
        m_hdl.fireUnHiLiteEvent(m_s12);
        assertFalse(m_hdl.isHiLit(m_c1));
        assertFalse(m_hdl.isHiLit(m_c2));
        assertTrue(m_hdl.isHiLit(m_c3));
        m_hdl.fireUnHiLiteEvent(m_c1);

        assertFalse(m_hdl.isHiLit(m_c1));
        assertFalse(m_hdl.isHiLit(m_c2));
        assertTrue(m_hdl.isHiLit(m_c3));

        // unhilite c3
        m_hdl.fireUnHiLiteEvent(m_c3);
        assertFalse(m_hdl.isHiLit(m_c3));
        m_hdl.fireUnHiLiteEvent(m_c2);
        assertFalse(m_hdl.isHiLit(m_c3));

        // unhilite s2
        assertFalse(m_hdl.isHiLit(m_c1));
        assertFalse(m_hdl.isHiLit(m_c2));
        assertFalse(m_hdl.isHiLit(m_c3));
        m_hdl.fireClearHiLiteEvent();
        assertFalse(m_hdl.isHiLit(m_c1));
        assertFalse(m_hdl.isHiLit(m_c2));
        assertFalse(m_hdl.isHiLit(m_c3));
    }
}   // HiLiteHandlerTest
