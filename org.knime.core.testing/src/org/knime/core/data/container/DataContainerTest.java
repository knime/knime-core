/*  
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2013
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
 */
package org.knime.core.data.container;

import java.util.Collections;
import java.util.Comparator;
import java.util.Random;
import java.util.Set;
import java.util.Vector;

import junit.framework.TestCase;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.util.ObjectToDataCellConverter;
import org.knime.core.util.DuplicateKeyException;

/**
 * Test case for class <code>DataContainer</code>.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
@SuppressWarnings("deprecation")
public class DataContainerTest extends TestCase {

    private static final DataTableSpec EMPTY_SPEC = new DataTableSpec(
            new String[] {}, new DataType[] {});
    private static final DataTableSpec SPEC_STR_INT_DBL = new DataTableSpec(new String[] {"String", "Int", "Double"}, 
            new DataType[] {StringCell.TYPE, IntCell.TYPE, DoubleCell.TYPE});

    /**
     * Main method. Ignores argument.
     * 
     * @param args Ignored.
     */
    public static void main(final String[] args) {
        junit.textui.TestRunner.run(DataContainerTest.class);
    }

    /**
     * method being tested: open().
     */
    public final void testOpen() {
        DataContainer c = new DataContainer(EMPTY_SPEC);
        c.addRowToTable(new DefaultRow(
                "no one is going to read me", new DataCell[] {}));
        assertTrue(c.isOpen());
    }

    /**
     * method being tested: isClosed().
     */
    public final void testIsClosed() {
        DataContainer c = new DataContainer(EMPTY_SPEC);
        assertFalse(c.isClosed());
        c.close();
        assertTrue(c.isClosed());
        for (DataRow row : c.getTable()) {
            fail("No data should be in the table: " + row);
        }
    }

    /**
     * method being tested: close().
     */
    public final void testClose() {
        DataContainer c = new DataContainer(EMPTY_SPEC);
        c.close();
        // hm, does it work again?
        c.close(); // should ignore it
    }
    
    public final void testMemoryAlertAfterClose() throws Exception {
        DataContainer container = new DataContainer(SPEC_STR_INT_DBL, 
                true, Integer.MAX_VALUE, false);
        for (RowIterator it = generateRows(100000); it.hasNext();) {
            container.addRowToTable(it.next());
        }
        container.close();
        container.getBufferedTable().getBuffer().writeAllRowsFromListToFile(false);
        RowIterator tableIterator = container.getTable().iterator();
        for (RowIterator it = generateRows(100000); it.hasNext();) {
            assertEquals(it.next(), tableIterator.next());
        }
    }
    
    public final void testMemoryAlertAfterCloseWhileReading() throws Exception {
        DataContainer container = new DataContainer(SPEC_STR_INT_DBL, 
                true, Integer.MAX_VALUE, false);
        int count = 100000;
        for (RowIterator it = generateRows(count); it.hasNext();) {
            container.addRowToTable(it.next());
        }
        container.close();
        RowIterator tableIterator = container.getTable().iterator();
        RowIterator it = generateRows(count);
        int i;
        for (i = 0; i < count / 2; i++) {
            assertEquals(it.next(), tableIterator.next());
        }
        container.getBufferedTable().getBuffer().writeAllRowsFromListToFile(false);
        
        for (; i < count; i++) {
            assertEquals(it.next(), tableIterator.next());
        }
    }
    
    public void testMemoryAlertWhileWrite() throws Exception {
        DataContainer cont = new DataContainer(SPEC_STR_INT_DBL, true, 1000000);
        int nrRows = 10;
        RowIterator it = generateRows(nrRows);
        int i = 0;
        for (; i < nrRows / 2; i++) {
            cont.addRowToTable(it.next());
        }
        cont.getBuffer().writeAllRowsFromListToFile(true);
        for (; i < nrRows; i++) {
            cont.addRowToTable(it.next());
        }
        cont.close();
        RowIterator tableIT = cont.getTable().iterator();
        for (RowIterator r = generateRows(nrRows); r.hasNext();) {
            DataRow expected = r.next();
            DataRow actual = tableIT.next();
            assertEquals(expected, actual);
        }
    }

    private static RowIterator generateRows(final int count) {
        return new RowIterator() {
            
            private int m_index = 0;
            
            @Override
            public DataRow next() {
                DefaultRow r = new DefaultRow(RowKey.createRowKey(m_index), 
                        new StringCell("String " + m_index), new IntCell(m_index), new DoubleCell(m_index));
                m_index++;
                return r;
            }
            
            @Override
            public boolean hasNext() {
                return m_index < count;
            }
        };
    }

    /**
     * method being tested: getTable().
     */
    public final void testGetTable() {
        DataContainer c = new DataContainer(EMPTY_SPEC);
        try {
            c.getTable();
            fail();
        } catch (IllegalStateException e) {
            System.out.println(e.getMessage());
        }
        c.close();
        c.getTable();
    }

    /**
     * method being tested: addRowToTable().
     */
    public final void testDuplicateKey() {
        String[] colNames = new String[]{"Column 1", "Column 2"};
        DataType[] colTypes = new DataType[] {
                StringCell.TYPE,
                IntCell.TYPE
        };
        DataTableSpec spec1 = new DataTableSpec(colNames, colTypes);
        DataContainer c = new DataContainer(spec1);
        RowKey r1Key = new RowKey("row 1");
        DataCell r1Cell1 = new StringCell("Row 1, Cell 1");
        DataCell r1Cell2 = new IntCell(12);
        DataRow r1 = new DefaultRow(r1Key, new DataCell[] {r1Cell1, r1Cell2});
        RowKey r2Key = new RowKey("row 2");
        DataCell r2Cell1 = new StringCell("Row 2, Cell 1");
        DataCell r2Cell2 = new IntCell(22);
        DataRow r2 = new DefaultRow(r2Key, new DataCell[] {r2Cell1, r2Cell2});
        c.addRowToTable(r1);
        c.addRowToTable(r2);
        
        // add row 1 twice
        try {
            c.addRowToTable(r1);
            c.close();
            // ... eh eh, you don't do this
            fail();
        } catch (DuplicateKeyException e) {
            System.out.println(e.getMessage());
        }
    }
        
    /**
     * method being tested: addRowToTable().
     */
    public final void testIncompatibleTypes() {
        String[] colNames = new String[]{"Column 1", "Column 2"};
        DataType[] colTypes = new DataType[] {
                StringCell.TYPE,
                IntCell.TYPE
        };
        DataTableSpec spec1 = new DataTableSpec(colNames, colTypes);
        DataContainer c = new DataContainer(spec1);
        RowKey r1Key = new RowKey("row 1");
        DataCell r1Cell1 = new StringCell("Row 1, Cell 1");
        DataCell r1Cell2 = new IntCell(12);
        DataRow r1 = new DefaultRow(r1Key, new DataCell[] {r1Cell1, r1Cell2});
        RowKey r2Key = new RowKey("row 2");
        DataCell r2Cell1 = new StringCell("Row 2, Cell 1");
        DataCell r2Cell2 = new IntCell(22);
        DataRow r2 = new DefaultRow(r2Key, new DataCell[] {r2Cell1, r2Cell2});
        RowKey r3Key = new RowKey("row 3");
        DataCell r3Cell1 = new StringCell("Row 3, Cell 1");
        DataCell r3Cell2 = new IntCell(32);
        DataRow r3 = new DefaultRow(r3Key, new DataCell[] {r3Cell1, r3Cell2});
        c.addRowToTable(r1);
        c.addRowToTable(r2);
        c.addRowToTable(r3);
        
        // add incompatible types
        RowKey r4Key = new RowKey("row 4");
        DataCell r4Cell1 = new StringCell("Row 4, Cell 1");
        DataCell r4Cell2 = new DoubleCell(42.0); // not allowed
        DataRow r4 = new DefaultRow(r4Key, new DataCell[] {r4Cell1, r4Cell2});
        try {
            c.addRowToTable(r4);
            c.close();
            fail();
        } catch (DataContainerException e) {
            if (!(e.getCause() instanceof IllegalArgumentException)) {
                throw e;
            } else {
                System.out.println(e.getCause().getMessage());
            }
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }
        
    }
    
    /**
     * method being tested: addRowToTable().
     */
    public final void testWrongCellCountRow() {
        String[] colNames = new String[]{"Column 1", "Column 2"};
        DataType[] colTypes = new DataType[] {
                StringCell.TYPE,
                IntCell.TYPE
        };
        DataTableSpec spec1 = new DataTableSpec(colNames, colTypes);
        DataContainer c = new DataContainer(spec1);
        RowKey r1Key = new RowKey("row 1");
        DataCell r1Cell1 = new StringCell("Row 1, Cell 1");
        DataCell r1Cell2 = new IntCell(12);
        DataRow r1 = new DefaultRow(r1Key, new DataCell[] {r1Cell1, r1Cell2});
        RowKey r2Key = new RowKey("row 2");
        DataCell r2Cell1 = new StringCell("Row 2, Cell 1");
        DataCell r2Cell2 = new IntCell(22);
        DataRow r2 = new DefaultRow(r2Key, new DataCell[] {r2Cell1, r2Cell2});
        RowKey r3Key = new RowKey("row 3");
        DataCell r3Cell1 = new StringCell("Row 3, Cell 1");
        DataCell r3Cell2 = new IntCell(32);
        DataRow r3 = new DefaultRow(r3Key, new DataCell[] {r3Cell1, r3Cell2});
        c.addRowToTable(r1);
        c.addRowToTable(r2);
        c.addRowToTable(r3);

        // add wrong sized row
        RowKey r5Key = new RowKey("row 5");
        DataCell r5Cell1 = new StringCell("Row 5, Cell 1");
        DataCell r5Cell2 = new IntCell(52); 
        DataCell r5Cell3 = new DoubleCell(53.0);
        DataRow r5 = new DefaultRow(
                r5Key, new DataCell[] {r5Cell1, r5Cell2, r5Cell3});
        try {
            c.addRowToTable(r5);
            c.close();
            fail();
        } catch (DataContainerException e) {
            if (!(e.getCause() instanceof IllegalArgumentException)) {
                throw e;
            } else {
                System.out.println(e.getCause().getMessage());
            }
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }
    }
    
    /**
     * method being tested: addRowToTable().
     */
    public final void testAddNullRow() {
        DataContainer c = new DataContainer(EMPTY_SPEC);
        c.addRowToTable(new DefaultRow(new RowKey("Row1"), new DataCell[0]));
        // add null
        try {
            c.addRowToTable((DataRow)null);
            c.close();
            fail();
        } catch (NullPointerException e) {
            System.out.println(e.getMessage());
        }
    }
    
    /**
     * method being tested: addRowToTable().
     */
    public final void testRowOrder() {
        // addRow should preserve the order, we try here randomly generated
        // IntCells as key (the container puts it in a linked has map)
        DataCell[] values = new DataCell[0];
        Vector<RowKey> order = new Vector<RowKey>(500); 
        for (int i = 0; i < 500; i++) {
            // fill it - this should be easy to preserve (as the int value
            // is also the hash code) 
            order.add(new RowKey(Integer.toString(i)));
        }
        // shuffle it - that should screw it up
        Collections.shuffle(order);
        DataContainer c = new DataContainer(EMPTY_SPEC);
        for (RowKey key : order) {
            c.addRowToTable(new DefaultRow(key, values));
        }
        c.close();
        DataTable table = c.getTable();
        int pos = 0;
        for (RowIterator it = table.iterator(); it.hasNext(); pos++) {
            DataRow cur = it.next();
            assertEquals(cur.getKey().getString(), order.get(pos).getString());
        }
        assertEquals(pos, order.size());
    } // testAddRowToTable()
    
    /**
     * Try a big file :-).
     * 
     */
    public void testBigFile() {
        // with these setting (50, 1000) it will write an 250MB cache file
        // (the latest data this value was checked: 31. August 2006...)
        final int colCount = 50;
        final int rowCount = 100;
        String[] names = new String[colCount];
        DataType[] types = new DataType[colCount];
        for (int c = 0; c < colCount; c++) {
            names[c] = "Column " + c;
            switch (c % 3) {
                case 0: types[c] = DoubleCell.TYPE; break;
                case 1: types[c] = StringCell.TYPE; break;
                case 2: types[c] = IntCell.TYPE; break;
                default: throw new InternalError();
            }
        }
        DataTableSpec spec = new DataTableSpec(names, types);
        names = null;
        types = null;
        DataContainer container = new DataContainer(spec);
        final ObjectToDataCellConverter conv = new ObjectToDataCellConverter();
        final long seed = System.currentTimeMillis();
        Random rand = new Random(seed);
        for (int i = 0; i < rowCount; i++) {
            DataRow row = createRandomRow(i, colCount, rand, conv);
            container.addRowToTable(row);
        }
        container.close();
        final Throwable[] throwables = new Throwable[1];
        final DataTable table = container.getTable();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    int i = 0;
                    Random rand1 = new Random(seed);
                    for (RowIterator it = table.iterator(); 
                        it.hasNext(); i++) {
                        DataRow row1 = 
                            createRandomRow(i, colCount, rand1, conv);
                        DataRow row2 = it.next();
                        assertEquals(row1, row2);
                    }
                    assertEquals(i, rowCount);
                } catch (Throwable t) {
                    throwables[0] = t;
                }
            }
        }; // Runnable 
        // make two threads read the buffer (file) concurrently.
        Thread t1 = new Thread(runnable);
        Thread t2 = new Thread(runnable);
        t1.start();
        t2.start();
        try {
            // seems that the event dispatch thread must not release the 
            // reference to the table, otherwise it is (I guess!!) garbage 
            // collected: You comment these lines and see the error message. 
            t1.join();
            t2.join();
        } catch (InterruptedException ie) {
            ie.printStackTrace();
            fail();
        }
        if (throwables[0] != null) {
            throw new RuntimeException(throwables[0]);
        }
    } // testBigFile()

    /** Restoring into main memory. 
     * @see ContainerTable#restoreIntoMemory()*/
    public void testRestoreIntoMemory() {
        // with these setting (50, 100) it will write an 250MB cache file
        // (the latest data this value was checked: 31. August 2006...)
        final int colCount = 50;
        final int rowCount = 100;
        String[] names = new String[colCount];
        DataType[] types = new DataType[colCount];
        for (int c = 0; c < colCount; c++) {
            names[c] = "Column " + c;
            switch (c % 3) {
                case 0: types[c] = DoubleCell.TYPE; break;
                case 1: types[c] = StringCell.TYPE; break;
                case 2: types[c] = IntCell.TYPE; break;
                default: throw new InternalError();
            }
        }
        DataTableSpec spec = new DataTableSpec(names, types);
        names = null;
        types = null;
        DataContainer container = new DataContainer(spec, true, 0);
        final ObjectToDataCellConverter conv = new ObjectToDataCellConverter();
        final long seed = System.currentTimeMillis();
        Random rand = new Random(seed);
        for (int i = 0; i < rowCount; i++) {
            DataRow row = createRandomRow(i, colCount, rand, conv);
            container.addRowToTable(row);
            row = null;
        }
        container.close();
        assertTrue(container.getBufferedTable().getBuffer().usesOutFile());
        final Throwable[] throwables = new Throwable[1];
        final ContainerTable table = container.getBufferedTable();
        table.restoreIntoMemory();
        // different iterators restore the content, each of which one row
        RowIterator[] its = new RowIterator[10];
        for (int i = 0; i < its.length; i++) {
            its[i] = table.iterator();
            for (int count = 0; count < i + 1; count++) {
                its[i].next();
            }
        }
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    int i = 0;
                    Random rand1 = new Random(seed);
                    for (RowIterator it = table.iterator(); 
                        it.hasNext(); i++) {
                        DataRow row1 = 
                            createRandomRow(i, colCount, rand1, conv);
                        DataRow row2 = it.next();
                        assertEquals(row1, row2);
                    }
                    assertEquals(i, rowCount);
                } catch (Throwable t) {
                    throwables[0] = t;
                }
            }
        }; // Runnable 
        // make two threads read the buffer (file) concurrently.
        Thread t1 = new Thread(runnable);
        Thread t2 = new Thread(runnable);
        t1.start();
        t2.start();
        try {
            // seems that the event dispatch thread must not release the 
            // reference to the table, otherwise it is (I guess!!) garbage 
            // collected: You comment these lines and see the error message. 
            t1.join();
            t2.join();
        } catch (InterruptedException ie) {
            ie.printStackTrace();
            fail();
        }
        if (throwables[0] != null) {
            throw new RuntimeException(throwables[0]);
        }
    } // testBigFile()
    
    /** Test if the domain is retained. */
    public void testTableDomain() {
        RowKey r1Key = new RowKey("row 1");
        DataCell r1Cell1 = new StringCell("Row 1, Cell 1");
        DataCell r1Cell2 = new IntCell(12);
        DataRow r1 = new DefaultRow(r1Key, new DataCell[] {r1Cell1, r1Cell2});
        RowKey r2Key = new RowKey("row 2");
        DataCell r2Cell1 = new StringCell("Row 2, Cell 1");
        DataCell r2Cell2 = new IntCell(22);
        DataRow r2 = new DefaultRow(r2Key, new DataCell[] {r2Cell1, r2Cell2});
        RowKey r3Key = new RowKey("row 3");
        DataCell r3Cell1 = new StringCell("Row 3, Cell 1");
        DataCell r3Cell2 = new IntCell(32);
        DataRow r3 = new DefaultRow(r3Key, new DataCell[] {r3Cell1, r3Cell2});

        String[] colNames = new String[]{"Column 1", "Column 2"};
        DataType[] colTypes = new DataType[] {
                StringCell.TYPE,
                IntCell.TYPE
        };
        DataTableSpec spec1 = new DataTableSpec(colNames, colTypes);
        DataContainer c = new DataContainer(spec1);
        // add in different order
        c.addRowToTable(r2);
        c.addRowToTable(r1);
        c.addRowToTable(r3);
        c.close();
        DataTable table = c.getTable();
        DataTableSpec tableSpec = table.getDataTableSpec();
        
        // check possible values
        Set<DataCell> possibleValues = 
            tableSpec.getColumnSpec(0).getDomain().getValues();
        assertEquals(possibleValues.size(), 3);
        assertTrue(possibleValues.contains(r1Cell1));
        assertTrue(possibleValues.contains(r2Cell1));
        assertTrue(possibleValues.contains(r3Cell1));
        // no possible values for integer column
        possibleValues = tableSpec.getColumnSpec(1).getDomain().getValues();
        assertNull(possibleValues);

        // check min max
        DataCell min = tableSpec.getColumnSpec(0).getDomain().getLowerBound();
        DataCell max = tableSpec.getColumnSpec(0).getDomain().getLowerBound();
        assertNull(min);
        assertNull(max);

        min = tableSpec.getColumnSpec(1).getDomain().getLowerBound();
        max = tableSpec.getColumnSpec(1).getDomain().getUpperBound();
        Comparator<DataCell> comparator = 
            tableSpec.getColumnSpec(1).getType().getComparator(); 
        assertTrue(comparator.compare(min, max) < 0);
        assertEquals(min, r1Cell2);
        assertEquals(max, r3Cell2);
    }
    
    private static char[] createRandomChars(
            final int length, final Random rand) {
        char[] result = new char[length];
        for (int i = 0; i < result.length; i++) {
            result[i] = (char)rand.nextInt(Character.MAX_VALUE); 
        }
        return result;
    }
    
    private static DataRow createRandomRow(final int index, final int colCount,
            final Random rand1, final ObjectToDataCellConverter conv) {
        RowKey key = new RowKey("Row " + index);
        DataCell[] cells = new DataCell[colCount];
        for (int c = 0; c < colCount; c++) {
            DataCell cell = null;
            switch (c % 3) {
            case 0: 
                cell = conv.createDataCell(
                        rand1.nextDouble() - 0.5); 
                break;
            case 1:
                String s;
                if (rand1.nextDouble() < 0.1) {
                    s = new String(createRandomChars(
                            rand1.nextInt(1000000), rand1));
                } else {
                    s = "Row" + index + "; Column:" + c;
                }
                cell = conv.createDataCell(s);
                break;
            case 2: 
                // use full range of int
                int r = (int)rand1.nextLong();
                cell = conv.createDataCell(r); 
                break;
            default: throw new InternalError();
            }
            cells[c] = cell;
        }
        return new DefaultRow(key, cells);
    }
    
}
