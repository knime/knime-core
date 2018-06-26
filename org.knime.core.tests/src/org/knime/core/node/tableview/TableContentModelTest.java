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
package org.knime.core.node.tableview;

import java.util.HashSet;
import java.util.Random;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;

import junit.framework.TestCase;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DefaultRowIterator;
import org.knime.core.data.def.DefaultTable;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.property.hilite.KeyEvent;
import org.knime.core.node.tableview.TableContentModel.TableContentFilter;
import org.knime.core.node.util.ViewUtils;

/**
 * Test class for public methods in a
 * {@link org.knime.core.node.tableview.TableContentModel}.
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
public class TableContentModelTest extends TestCase {

    private static final Object[][] OBJECT_DATA = new Object[][]{
            {new Double(1.0), new Integer(2), "three"},
            {new Double(4.0), new Integer(5), "six"},
            {new Double(7.0), new Integer(8), "nine"},
            {new Double(10.0), new Integer(11), "twelve"},
            {new Double(13.0), new Integer(14), "fifteen"},
            {new Double(16.0), new Integer(17), "eighteen"},
            {new Double(19.0), new Integer(20), "twentyone"},
            {new Double(22.0), new Integer(23), "twentyfour"},
            {new Double(25.0), new Integer(26), "twentyseven"},
    };
    private static final DataTable DATA =
        new DefaultTable(OBJECT_DATA, null, null);


    /**
     * Creates empty TableContentModel, that's it!
     */
    public final void testTableContentModel() {
        final TableContentModel m = new TableContentModel();
        assertEquals("Row Count", m.getRowCount(), 0);
        assertEquals("Col Count", m.getColumnCount(), 0);
        assertTrue("Final row count", m.isRowCountFinal());
        assertFalse("Has Data", m.hasData());
        assertFalse("Has HiLiteHandler", m.hasHiLiteHandler());
    } // testTableContentModel()

    /**
     * Calls second construtor.
     */
    public final void testTableContentModelDataTable() {
        final TableContentModel m1 = new TableContentModel(null);
        assertEquals("Row Count", m1.getRowCount(), 0);
        assertEquals("Col Count", m1.getColumnCount(), 0);
        assertTrue("Final row count", m1.isRowCountFinal());
        assertFalse("Has Data", m1.hasData());
        assertFalse("Has HiLiteHandler", m1.hasHiLiteHandler());
        final TableContentModel m2 = new TableContentModel(DATA);
        assertEquals("Col Count", m2.getColumnCount(), OBJECT_DATA[0].length);
        assertTrue("Has Data", m2.hasData());
        assertFalse("Has HiLiteHandler", m2.hasHiLiteHandler());
    } // testTableContentModelDataTable()

    /**
     * Class under test for void TableContentModel(DataTable, HiLiteHandler).
     */
    public final void testTableContentModelDataTableHiLiteHandler() {
        HiLiteHandler handler = new HiLiteHandler();
        final TableContentModel m2 = new TableContentModel(DATA, handler);
        assertEquals("Col Count", m2.getColumnCount(), OBJECT_DATA[0].length);
        assertTrue("Has Data", m2.hasData());
        assertTrue("Has HiLiteHandler", m2.hasHiLiteHandler());

        final TableContentModel m3 = new TableContentModel(null, handler);
        assertFalse("Has Data", m3.hasData());
        assertTrue("Has HiLiteHandler", m3.hasHiLiteHandler());

    } // testTableContentModelDataTableHiLiteHandler()

    /**
     * Method being tested: setDataTable(DataTable) and hasData().
     */
    public final void testSetDataTable() {
        final TableContentModel m = new TableContentModel();
        assertFalse("Has Data", m.hasData());

        m.setDataTable(DATA);
        assertTrue("Has Data", m.hasData());
        assertSame(DATA, m.getDataTable());
    }

    /**
     * Method being tested: setHiLiteHandler(HiLiteHandler) and
     *                      boolean hasHiLiteHandler().
     */
    public final void testSetHiLiteHandler() {
        final TableContentModel m = new TableContentModel();
        assertFalse("Has Hilitehandler", m.hasHiLiteHandler());
        final HiLiteHandler hiliter = new HiLiteHandler();
        m.setHiLiteHandler(hiliter);
        assertTrue("Has HiLiteHandler", m.hasHiLiteHandler());
        // setting data on and off shouldn't change HiLite handler
        m.setDataTable(DATA);
        assertTrue("Has HiLiteHandler", m.hasHiLiteHandler());
        m.setDataTable(null);
        assertTrue("Has HiLiteHandler", m.hasHiLiteHandler());

        m.setHiLiteHandler(null);
        assertFalse("Has HiLiteHandler", m.hasHiLiteHandler());

    }

    /**
     * Method being tested: int getColumnCount().
     */
    public final void testGetColumnCount() {
        final TableContentModel m = new TableContentModel();
        assertEquals(m.getColumnCount(), 0);
        m.setDataTable(DATA);
        final int colCount = DATA.getDataTableSpec().getNumColumns();
        assertEquals(m.getColumnCount(), colCount);
        m.setDataTable(null);
        assertEquals(m.getColumnCount(), 0);
    }

    /**
     * Method being tested: int getRowCount().
     */
    public final void testGetRowCount() {
        final TableContentModel m = new TableContentModel();
        assertEquals(m.getRowCount(), 0);
        assertTrue(m.isRowCountFinal());
        m.setDataTable(DATA);

        // row count may not be final, check for <=
        assertTrue(m.getRowCount() <= OBJECT_DATA.length);
        m.setDataTable(null);
        assertEquals(m.getRowCount(), 0);
    }

    /**
     * Method being tested: Object getValueAt(int, int).
     */
    public final void testGetValueAt() {
        final TableContentModel m = new TableContentModel();
        try {
            m.getValueAt(1, 0);
            fail("Expected " + IndexOutOfBoundsException.class + " not thrown");
        } catch (IndexOutOfBoundsException e) {
            NodeLogger.getLogger(getClass()).debug("Got expected exception: " + e.getClass().getName(), e);
        }

        // create big data table (so that it has to cache) and
        // try to get the wrong value at the right position (hopefully, we fail)
        final double[][] ddata = new double[20000][50];
        final long seed = System.currentTimeMillis();

        // use random access, so the cache is updated frequently
        Random rand = new Random(seed);
        for (int row = 0; row < ddata.length; row++) {
            double[] curRow = ddata[row];
            for (int col = 0; col < curRow.length; col++) {
                curRow[col] = rand.nextDouble();
            }
        }
        DataTable data = new DefaultTable(ddata, null, null);
        m.setDataTable(data);
        // do random search on table
        for (int i = 0; i < 20000; i++) {
            int row = rand.nextInt(20000);
            int col = rand.nextInt(50);
            Object value = m.getValueAt(row, col);
            assertNotNull(value);
            assertTrue(value instanceof DoubleValue);
            double val = ((DoubleValue)value).getDoubleValue();
            String errorMessage = "getValueAt(" + row + ", " + col
                + ") not equal to what it was set once. Used Random seed "
                + seed + "; You may want to use that for debugging.";
            assertEquals(errorMessage, val, ddata[row][col], 0.0);
        }

        try {
            m.getValueAt(-4, 0);
            fail("Expected " + IndexOutOfBoundsException.class + " not thrown");
        } catch (IndexOutOfBoundsException e) {
            NodeLogger.getLogger(getClass()).debug("Got expected exception: " + e.getClass().getName(), e);
        }

        try {
            m.getValueAt(0, -2);
            fail("Expected " + IndexOutOfBoundsException.class + " not thrown");
        } catch (IndexOutOfBoundsException e) {
            NodeLogger.getLogger(getClass()).debug("Got expected exception: " + e.getClass().getName(), e);
        }

        try {
            m.getValueAt(20000, 0);
            fail("Expected " + IndexOutOfBoundsException.class + " not thrown");
        } catch (IndexOutOfBoundsException e) {
            NodeLogger.getLogger(getClass()).debug("Got expected exception: " + e.getClass().getName(), e);
        }

        try {
            m.getValueAt(0, 500);
            fail("Expected " + IndexOutOfBoundsException.class + " not thrown");
        } catch (IndexOutOfBoundsException e) {
            NodeLogger.getLogger(getClass()).debug("Got expected exception: " + e.getClass().getName(), e);
        }
    }

    /**
     * Method being tested: String getColumnName(int).
     */
    public final void testGetColumnNameint() {
        final TableContentModel m = new TableContentModel();
        try {
            m.getColumnName(0);
            fail("Expected " + IndexOutOfBoundsException.class + " not thrown");
        } catch (IndexOutOfBoundsException e) {
            NodeLogger.getLogger(getClass()).debug("Got expected exception: " + e.getClass().getName(), e);
        }
        final TableContentModel m1 = new TableContentModel(DATA);
        DataTableSpec spec = DATA.getDataTableSpec();
        final int colCount = spec.getNumColumns();
        for (int i = 0; i < colCount; i++) {
            String colName1 = m1.getColumnName(i);
            String colName2 = spec.getColumnSpec(i).getName().toString();
            assertEquals(colName1, colName2);
        }

        try {
            m.getColumnName(-1);
            fail("Expected " + IndexOutOfBoundsException.class + " not thrown");
        } catch (IndexOutOfBoundsException e) {
            NodeLogger.getLogger(getClass()).debug("Got expected exception: " + e.getClass().getName(), e);
        }
        try {
            m.getColumnName(spec.getNumColumns());
            fail("Expected " + IndexOutOfBoundsException.class + " not thrown");
        } catch (IndexOutOfBoundsException e) {
            NodeLogger.getLogger(getClass()).debug("Got expected exception: " + e.getClass().getName(), e);
        }

    }

    /**
     * Method being tested: DataTable getDataTable().
     */
    public final void testGetDataTable() {
        final TableContentModel m = new TableContentModel();
        assertNull(m.getDataTable());
        m.setDataTable(DATA);
        assertEquals(m.getDataTable(), DATA);
        m.setDataTable(null);
        m.setDataTable(null);
        assertNull(m.getDataTable());
    }

    /**
     * Method being tested: boolean isRowCountFinal().
     */
    public final void testIsRowCountFinal() {
        final TableContentModel m = new TableContentModel();
        assertTrue(m.isRowCountFinal());
        m.setDataTable(DATA);
        // no row has been accessed
        assertFalse(m.isRowCountFinal());
        // request last row
        m.getRow(OBJECT_DATA.length - 1);
        assertTrue(m.isRowCountFinal());
        m.clearCache();
        assertTrue(m.isRowCountFinal());
    }

    /**
     * Method being tested: int setCacheSize(int) and
     *                      int getCacheSize().
     */
    public final void testSetCacheSize() {
        final TableContentModel m = new TableContentModel();
        final int currentCacheSize = m.getCacheSize();
        final int currentChunkSize = m.getChunkSize();
        assertTrue(currentChunkSize < currentCacheSize);
        // making it bigger, must be ok at any time
        final int cacheSize1 = m.setCacheSize(2 * currentCacheSize);
        assertEquals(cacheSize1, m.getCacheSize());
        // shorten it may (in this case: MUST) fail!
        final int cacheSize2 = m.setCacheSize(currentChunkSize);
        assertEquals(cacheSize2, m.getCacheSize());
        assertFalse(cacheSize2 == currentChunkSize);
        try {
            m.setCacheSize(-1);
            fail("Expected " + IllegalArgumentException.class + " not thrown");
        } catch (IllegalArgumentException e) {
            NodeLogger.getLogger(getClass()).debug("Got expected exception: " + e.getClass().getName(), e);
        }
    }

    /**
     * Method being tested: int setChunkSize(int) and
     *                      int getChunkSize().
     */
    public final void testSetChunkSize() {
        final TableContentModel m = new TableContentModel();
        final int currentCacheSize = m.getCacheSize();
        final int currentChunkSize = m.getChunkSize();
        assertTrue(currentChunkSize < currentCacheSize);
        // making it smaller, must be ok at any time
        final int chunkSize1 = m.setChunkSize(currentCacheSize / 2);
        assertEquals(chunkSize1, m.getChunkSize());
        // increase chunk size may (in this case: MUST) fail!
        final int chunkSize2 = m.setChunkSize(currentCacheSize);
        assertEquals(chunkSize2, m.getChunkSize());
        assertFalse(chunkSize2 == currentChunkSize);
        try {
            m.setChunkSize(-1);
            fail("Expected " + IllegalArgumentException.class + " not thrown");
        } catch (IllegalArgumentException e) {
            NodeLogger.getLogger(getClass()).debug("Got expected exception: " + e.getClass().getName(), e);
        }
    }

    /**
     * Method being tested: boolean isHiLit(int).
     */
    public final void testIsHiLit() {
        final HiLiteHandler hiliteHdl = new HiLiteHandler();
        final TableContentModel m = new TableContentModel(DATA, hiliteHdl);

        // hilite every other in DATA and check if it is correctly reflected
        // in m
        final HashSet<RowKey> set = new HashSet<RowKey>();
        boolean isEvenNumber = true;
        for (RowIterator it = DATA.iterator(); it.hasNext();) {
            RowKey cell = it.next().getKey();
            if (isEvenNumber) {
                hiliteHdl.fireHiLiteEvent(cell);
                set.add(cell);
            }
            isEvenNumber = !isEvenNumber;
        }
        flushEDTQueue();
        for (int i = 0; i < m.getRowCount(); i++) {
            RowKey key = m.getRow(i).getKey();
            boolean isHiLit = m.isHiLit(i);
            assertEquals(set.contains(key), isHiLit);
        }
        try {
            m.isHiLit(-1);
            fail("Expected " + IndexOutOfBoundsException.class + " not thrown");
        } catch (IndexOutOfBoundsException e) {
            NodeLogger.getLogger(getClass()).debug("Got expected exception: " + e.getClass().getName(), e);
        }
        try {
            m.isHiLit(OBJECT_DATA.length);
            fail("Expected " + IndexOutOfBoundsException.class + " not thrown");
        } catch (IndexOutOfBoundsException e) {
            NodeLogger.getLogger(getClass()).debug("Got expected exception: " + e.getClass().getName(), e);
        }
    }

    /**
     * Method being tested: hiLite(ListSelectionModel) and
     *         (implicitly) hiLite(KeyEvent).
     */
    public final void testHilite() {
        final HiLiteHandler hiliter = new HiLiteHandler();
        final TableContentModel m = new TableContentModel(DATA, hiliter);
        final JTable table = new JTable(m);
        final ListSelectionModel listModel = table.getSelectionModel();

        // make sure, the content model knows about ALL ROWS
        m.getRow(OBJECT_DATA.length - 1);
        assertEquals(table.getRowCount(), OBJECT_DATA.length);

        // select every other row in the JTable and try to propagate that to
        // the hilite handler
        for (int i = 0; i < OBJECT_DATA.length; i += 2) {
            listModel.addSelectionInterval(i, i);
        }
        m.requestHiLite(listModel);
        flushEDTQueue();

        for (int i = 0; i < OBJECT_DATA.length; i++) {
            boolean shouldBeHiLit = (i % 2 == 0);
            boolean isHiLit = m.isHiLit(i);
            assertEquals(shouldBeHiLit, isHiLit);
        }

        // add every third row to the selection.
        for (int i = 0; i < OBJECT_DATA.length; i += 3) {
            listModel.addSelectionInterval(i, i);
        }
        m.requestHiLite(listModel);
        flushEDTQueue();
        for (int i = 0; i < OBJECT_DATA.length; i++) {
            boolean shouldBeHiLit = (i % 2 == 0) || (i % 3 == 0);
            boolean isHiLit = m.isHiLit(i);
            assertEquals(shouldBeHiLit, isHiLit);
        }

        // clearing the selection shouldn't have any effect
        listModel.clearSelection();
        for (int i = 0; i < OBJECT_DATA.length; i++) {
            boolean shouldBeHiLit = (i % 2 == 0) || (i % 3 == 0);
            boolean isHiLit = m.isHiLit(i);
            assertEquals(shouldBeHiLit, isHiLit);
        }

        listModel.setSelectionInterval(0, OBJECT_DATA.length - 1);
        m.requestHiLite(listModel);
        flushEDTQueue();
        for (int i = 0; i < OBJECT_DATA.length; i++) {
            assertTrue(m.isHiLit(i));
        }

        try {
            m.requestHiLite((ListSelectionModel)null);
            fail("Expected " + NullPointerException.class + " not thrown");
        } catch (NullPointerException e) {
            NodeLogger.getLogger(getClass()).debug("Got expected exception: " + e.getClass().getName(), e);
        }

    } // testHilite()

    /**
     * Method being tested: unHiLite(ListSelectionModel)
     *         (implicitly) unHiLite(KeyEvent).
     */
    public final void testUnHilite() {
        final HiLiteHandler hiliter = new HiLiteHandler();
        final TableContentModel m = new TableContentModel(DATA, hiliter);
        final JTable table = new JTable(m);
        final ListSelectionModel listModel = table.getSelectionModel();

        // make sure, the content model knows about ALL ROWS
        m.getRow(OBJECT_DATA.length - 1);
        assertEquals(table.getRowCount(), OBJECT_DATA.length);

        // first: hilite all; unhilite subsequently
        listModel.setSelectionInterval(0, OBJECT_DATA.length - 1);
        m.requestHiLite(listModel);
        flushEDTQueue();
        for (int i = 0; i < OBJECT_DATA.length; i++) {
            assertTrue(m.isHiLit(i));
        }
        listModel.clearSelection();

        // unselect every other row in the JTable and try to propagate that to
        // the hilite handler
        for (int i = 0; i < OBJECT_DATA.length; i += 2) {
            listModel.addSelectionInterval(i, i);
        }
        m.requestUnHiLite(listModel);
        flushEDTQueue();

        for (int i = 0; i < OBJECT_DATA.length; i++) {
            boolean shouldBeUnHiLit = (i % 2 == 0);
            boolean isUnHiLit = !m.isHiLit(i);
            assertEquals(shouldBeUnHiLit, isUnHiLit);
        }

        // remove every third row from the selection.
        for (int i = 0; i < OBJECT_DATA.length; i += 3) {
            listModel.addSelectionInterval(i, i);
        }
        m.requestUnHiLite(listModel);
        flushEDTQueue();

        for (int i = 0; i < OBJECT_DATA.length; i++) {
            boolean shouldBeUnHiLit = (i % 2 == 0) || (i % 3 == 0);
            boolean isUnHiLit = !m.isHiLit(i);
            assertEquals(shouldBeUnHiLit, isUnHiLit);
        }

        // clearing the selection shouldn't have any effect
        listModel.clearSelection();
        for (int i = 0; i < OBJECT_DATA.length; i++) {
            boolean shouldBeUnHiLit = (i % 2 == 0) || (i % 3 == 0);
            boolean isUnHiLit = !m.isHiLit(i);
            assertEquals(shouldBeUnHiLit, isUnHiLit);
        }

        listModel.setSelectionInterval(0, OBJECT_DATA.length - 1);
        m.requestUnHiLite(listModel);
        flushEDTQueue();
        for (int i = 0; i < OBJECT_DATA.length; i++) {
            assertFalse(m.isHiLit(i));
        }
        try {
            m.requestUnHiLite((ListSelectionModel)null);
            fail("Expected " + NullPointerException.class + " not thrown");
        } catch (NullPointerException e) {
            NodeLogger.getLogger(getClass()).debug("Got expected exception: " + e.getClass().getName(), e);
        }

    } // testUnHilite()

    /**
     * Method being tested: resetHiLite().
     */
    public final void testResetHilite() {
        final HiLiteHandler hiliter = new HiLiteHandler();
        final TableContentModel m = new TableContentModel(DATA, hiliter);
        final JTable table = new JTable(m);
        final ListSelectionModel listModel = table.getSelectionModel();

        // make sure, the content model knows about ALL ROWS
        m.getRow(OBJECT_DATA.length - 1);
        assertEquals(table.getRowCount(), OBJECT_DATA.length);

        // first: hilite all;
        listModel.setSelectionInterval(0, OBJECT_DATA.length - 1);
        m.requestHiLite(listModel);
        flushEDTQueue();
        for (int i = 0; i < OBJECT_DATA.length; i++) {
            assertTrue(m.isHiLit(i));
        }

        // reset hilite
        m.requestResetHiLite();
        // hilite happens in EDT thread, this is executed in main - need to wait
        ViewUtils.invokeAndWaitInEDT(new Runnable() {
            @Override
            public void run() {
                // nothing, just run
            }
        });
        for (int i = 0; i < OBJECT_DATA.length; i++) {
            assertFalse(m.isHiLit(i));
        }

        int lucky = (int)(Math.random() * OBJECT_DATA.length);
        listModel.setSelectionInterval(lucky, lucky);
        m.requestHiLite(listModel);
        flushEDTQueue();
        m.setTableContentFilter(TableContentFilter.HiliteOnly);

        // 0 should be ok, it returns the lucky row
        m.getRow(0);
        m.unHiLiteAll(new KeyEvent(this));
        assertEquals(m.getRowCount(), 0);
    }

    /**
     * Method being tested: DataRow getRow(int).
     */
    public final void testGetRow() {
        final TableContentModel m = new TableContentModel(DATA);
        int i = 0;
        for (RowIterator it = DATA.iterator(); it.hasNext(); i++) {
            DataRow row = it.next();
            assertEquals(row, m.getRow(i));
        }
        try {
            m.getRow(-1);
            fail("Expected " + IndexOutOfBoundsException.class + " not thrown");
        } catch (IndexOutOfBoundsException e) {
            NodeLogger.getLogger(getClass()).debug("Got expected exception: " + e.getClass().getName(), e);
        }
        try {
            m.getRow(OBJECT_DATA.length);
            fail("Expected " + IndexOutOfBoundsException.class + " not thrown");
        } catch (IndexOutOfBoundsException e) {
            NodeLogger.getLogger(getClass()).debug("Got expected exception: " + e.getClass().getName(), e);
        }
        // further checking is done at testCachingStrategy() and other
        // test methods
    }

    /**
     * Tests ring buffer chaching strategy of the
     * <code>TableContentModel</code>. This method uses a modified
     * <code>Iterator</code> that throws an <code>Exception</code> when called
     * at an inappropriate time.
     */
    public final void testCachingStrategy() {
        final String[] colnames = new String[]{"C1"};
        final DataType[] colclasses = new DataType[]{DoubleCell.TYPE};
        final DataRow[] data = new DefaultRow[500];
        for (int i = 0; i < data.length; i++) {
            data[i] = new DefaultRow(new RowKey("Row_" + i), new double[]{i});
        }

        // these flags keep track when the iterator of the table may be
        // accessed (will be changed in this method, first flag) and when the
        // iterator is indeed being accessed (set in the iterator below to true,
        // reset here, second flag)
        final boolean[] flags = new boolean[] {true, false};

        // override DataTable to set own iterator
        final DataTable table = new DefaultTable(data, colnames, colclasses) {
            @Override
            public RowIterator iterator() {
                return new RestrictedAccessIterator(getRowsInList(), flags);
            }
        };

        TableContentModel model = new TableContentModel(table);
        final int chunkSize = 25;  // change default chunk and cache size
        final int cacheSize = 2 * chunkSize;
        model.setChunkSize(chunkSize);
        assertEquals(model.getChunkSize(), chunkSize);
        model.setCacheSize(cacheSize);
        assertEquals(model.getCacheSize(), cacheSize);

        assertTrue(flags[1]);  // init of table uses iterator
        flags[1] = false;
        flags[0] = true;       // allow table access

        model.getRow(0);       // get first row, iterator is used
        assertTrue(flags[1]);  // is true when iterator has indeed been used

        // simulate scrolling down - for first "chunksize" rows
        // iterator access
        flags[0] = false;
        flags[1] = false;
        for (int i = 0; i < chunkSize; i++) { // access rows 0 - 24
            model.getRow(i); // will throw exception when iterator is accessed
        }

        assertFalse(flags[0]);
        assertFalse(flags[1]);

        flags[0] = true;         // row "chunksize": update cache!
        model.getRow(chunkSize); // now in cache: Row_1 - Row_50 (release Row_0)
        assertTrue(flags[1]);    // iterator has been used

        flags[0] = false;
        flags[1] = false;

        // cache is full, containing Row_1 - Row_50
        for (int i = 1; i < cacheSize; i++) {
            // must not access Row_50 (will use iterator)
            model.getRow(i);
        }
        assertFalse(flags[0]);
        assertFalse(flags[1]);

        // ok, let's simulate arbitrary jumping in the table and check the
        // cache, let's say 20 different positions, the rows in the cache are:
        // [row+chunksize-cachesize+1 : row+chunksize-1]
        final Random rand = new Random();
        for (int i = 0; i < 200; i++) {
            int row = rand.nextInt(500); // draw some row to access
            flags[0] = true;
            flags[1] = false;
            model.getRow(row);
            if (!flags[1]) {  // row was (by chance) in cache - no check
                continue;     // cache not changed, we may continue
            }
            flags[0] = false; // disallow access
            flags[1] = false;
            int firstRow = Math.max(0, row + chunkSize - cacheSize + 1);
            int lastRow = Math.min(row + chunkSize, 500);
            for (int r = firstRow; r < lastRow; r++) {
                    model.getRow(r);
        }
            assertFalse(flags[0]);
            assertFalse(flags[1]);
        }
    } // testCachingStrategy()

    /**
     * Tests the correctness of the model when only hilited are shown.
     */
    public final void testShowOnlyHiLited() {
        final String[] colnames = new String[]{"C1"};
        final DataType[] colclasses = new DataType[]{DoubleCell.TYPE};
        final DataRow[] data = new DefaultRow[500];
        for (int i = 0; i < data.length; i++) {
            data[i] = new DefaultRow(new RowKey("Row_" + i), new double[]{i});
        }
        // override DataTable to set own iterator
        final DataTable table = new DefaultTable(data, colnames, colclasses);
        TableContentModel model = new TableContentModel(table);
        final int chunkSize = 25;  // change default chunk and cache size
        final int cacheSize = 2 * chunkSize;
        model.setChunkSize(chunkSize);
        assertEquals(model.getChunkSize(), chunkSize);
        model.setCacheSize(cacheSize);
        assertEquals(model.getCacheSize(), cacheSize);
        model.setTableContentFilter(TableContentFilter.HiliteOnly);
        assertEquals(model.getRowCount(), 0);
        assertTrue(model.isRowCountFinal());
        final HiLiteHandler hiliter = new HiLiteHandler();
        model.setHiLiteHandler(hiliter);
        final Random rand = new Random();
        int nrHiLitKeys = 0;
        // ok, let's fire arbitrary events and then check if it is properly
        // reflected in the model
        for (int i = 0; i < 500; i++) {
            if (i % 100 == 0) {
                // clear all, also that should work
                hiliter.fireClearHiLiteEvent();
                flushEDTQueue();
                nrHiLitKeys = 0;
            } else {
                // let at most 20% change
                int count = rand.nextInt(data.length / 5);
                // change randomly drawn keys
                for (int c = 0; c < count; c++) {
                    int index = rand.nextInt(data.length);
                    RowKey keyForIndex = data[index].getKey();
                    boolean isHilit = hiliter.isHiLit(keyForIndex);
                    if (isHilit) {
                        hiliter.fireUnHiLiteEvent(keyForIndex);
                        flushEDTQueue();
                        nrHiLitKeys--;
                    } else {
                        hiliter.fireHiLiteEvent(keyForIndex);
                        flushEDTQueue();
                        nrHiLitKeys++;
                    }
                }
            }
            flushEDTQueue();
            // now the sanity checks
            for (int row = 0; row < model.getRowCount(); row++) {
                RowKey key = model.getRow(row).getKey();
                assertTrue(hiliter.isHiLit(key));
                assertTrue(model.isHiLit(row));
            }
            assertEquals(model.getRowCount(), nrHiLitKeys);
            assertTrue(model.isRowCountFinal());
        }

    }

    /** Runs an empty runnable in the EDT thread to make sure there are no
     * further hilite events queued. */
    private void flushEDTQueue() {
        ViewUtils.invokeAndWaitInEDT(new Runnable() {
            @Override
            public void run() {
                // nothing to do, just run
            }
        });
    }

    /**
     * Iterator that throws exception when <code>next()</code> method is called
     * at an inappropriate time.
     */
    private static class RestrictedAccessIterator extends DefaultRowIterator {

        // flags passed in constructor
        private final boolean[] m_flags;

        /**
         * Constructs new Iterator based on <code>table</code> and the access
         * policy encoded in <code>flags</code>.
         *
         * @param table to iterate over.
         * @param flags two-dimensional array, first flag is set remotely and is
         *            <code>true</code> when <code>next()</code> may be
         *            called. Second flag is set to <code>true</code> by the
         *            <code>next()</code> method when it is called.
         * @see org.knime.core.data.def.DefaultRowIterator
         *      #DefaultRowIterator(DefaultTable)
         */
        public RestrictedAccessIterator(
                final Iterable<DataRow> table, final boolean[] flags) {
            super(table);
            m_flags = flags;
        } // RestrictedAccessIterator(DefaultTable, boolean[])


        /**
         * Pushes iterator forward. If first flag is set to <code>true</code> or
         * throws an exception when it is set to <code>false</code>
         *
         * @return next row in the table
         * @see RowIterator#next()
         * @throws IllegalStateException if iterator is disabled
         */
        @Override
        public DataRow next() {
            if (!m_flags[0]) {
                throw new IllegalStateException(
                        "Iterator should not have been called at the current "
                        + "state, all rows are supposedly cached");
            }
            m_flags[1] = true;
            return super.next();
        } // next()
    } // private class RestrictedAccessIterator
}
