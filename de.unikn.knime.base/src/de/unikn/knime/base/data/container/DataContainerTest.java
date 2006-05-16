/*  
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 */
package de.unikn.knime.base.data.container;

import java.util.Collections;
import java.util.Comparator;
import java.util.Random;
import java.util.Set;
import java.util.Vector;

import junit.framework.TestCase;
import de.unikn.knime.core.data.DataCell;
import de.unikn.knime.core.data.DataRow;
import de.unikn.knime.core.data.DataTable;
import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.data.DataType;
import de.unikn.knime.core.data.RowIterator;
import de.unikn.knime.core.data.def.DoubleCell;
import de.unikn.knime.core.data.def.IntCell;
import de.unikn.knime.core.data.def.DefaultRow;
import de.unikn.knime.core.data.def.StringCell;
import de.unikn.knime.core.data.util.ObjectToDataCellConverter;

/**
 * Test case for class <code>DataContainer</code>.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class DataContainerTest extends TestCase {

    private static final DataTableSpec EMPTY_SPEC = new DataTableSpec(
            new DataCell[] {}, new DataType[] {});

    /**
     * Main method. Ignores argument.
     * 
     * @param args Ignored.
     */
    public static void main(final String[] args) {
        junit.textui.TestRunner.run(DataContainerTest.class);
    }

    /**
     * method being tested: isOpen().
     */
    public final void testIsOpen() {
        DataContainer c = new DataContainer();
        assertFalse(c.isOpen());
        c.open(EMPTY_SPEC);
        assertTrue(c.isOpen());
        c.close();
        assertFalse(c.isOpen());
    }

    /**
     * method being tested: open().
     */
    public final void testOpen() {
        DataContainer c = new DataContainer();
        assertFalse(c.isOpen());
        c.open(EMPTY_SPEC);
        c.addRowToTable(new DefaultRow(new StringCell(
                "no one is going to read me"), new DataCell[] {}));
        assertTrue(c.isOpen());
        // "reopen" it: drops the data, continues from scratch. Hopefully.
        c.open(EMPTY_SPEC);
        c.close();
        DataTable table = c.getTable();
        for (RowIterator it = table.iterator(); it.hasNext();) {
            fail("There shouldn't be content");
        }
    }

    /**
     * method being tested: isClosed().
     */
    public final void testIsClosed() {
        DataContainer c = new DataContainer();
        assertFalse(c.isClosed());
        c.open(EMPTY_SPEC);
        assertFalse(c.isClosed());
        c.close();
        assertTrue(c.isClosed());
    }

    /**
     * method being tested: close().
     */
    public final void testClose() {
        DataContainer c = new DataContainer();
        try {
            c.close();
            fail();
        } catch (IllegalStateException e) {
            System.out.println(e.getMessage());
        }
        c.open(EMPTY_SPEC);
        c.close();
        // hm, does it work again?
        c.close(); // should ignore it
    }

    /**
     * method being tested: getTable().
     */
    public final void testGetTable() {
        DataContainer c = new DataContainer();
        try {
            c.getTable();
            fail();
        } catch (IllegalStateException e) {
            System.out.println(e.getMessage());
        }
        c.open(EMPTY_SPEC);
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
    public final void testAddRowToTable() {
        DataContainer c = new DataContainer();
        DataCell r1Key = new StringCell("row 1");
        DataCell r1Cell1 = new StringCell("Row 1, Cell 1");
        DataCell r1Cell2 = new IntCell(12);
        DataRow r1 = new DefaultRow(r1Key, new DataCell[] {r1Cell1, r1Cell2});
        DataCell r2Key = new StringCell("row 2");
        DataCell r2Cell1 = new StringCell("Row 2, Cell 1");
        DataCell r2Cell2 = new IntCell(22);
        DataRow r2 = new DefaultRow(r2Key, new DataCell[] {r2Cell1, r2Cell2});
        DataCell r3Key = new StringCell("row 3");
        DataCell r3Cell1 = new StringCell("Row 3, Cell 1");
        DataCell r3Cell2 = new IntCell(32);
        DataRow r3 = new DefaultRow(r3Key, new DataCell[] {r3Cell1, r3Cell2});

        // add row to non open table
        try {
            c.addRowToTable(r1);
            fail();
        } catch (IllegalStateException e) {
            System.out.println(e.getMessage());
        }
        DataCell[] colNames = new DataCell[]{
                new StringCell("Column 1"),
                new StringCell("Column 2")
        };
        DataType[] colTypes = new DataType[] {
                StringCell.TYPE,
                IntCell.TYPE
        };
        DataTableSpec spec1 = new DataTableSpec(colNames, colTypes);
        c.open(spec1);
        c.addRowToTable(r1);
        c.addRowToTable(r2);

        // add row 1 twice
        try {
            c.addRowToTable(r1);
            // ... eh eh, you don't do this
            fail();
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }
        c.addRowToTable(r3);
        
        // add incompatible types
        DataCell r4Key = new StringCell("row 4");
        DataCell r4Cell1 = new StringCell("Row 4, Cell 1");
        DataCell r4Cell2 = new DoubleCell(42.0); // not allowed
        DataRow r4 = new DefaultRow(r4Key, new DataCell[] {r4Cell1, r4Cell2});
        try {
            c.addRowToTable(r4);
            fail();
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }
        
        // add wrong sized row
        DataCell r5Key = new StringCell("row 5");
        DataCell r5Cell1 = new StringCell("Row 5, Cell 1");
        DataCell r5Cell2 = new IntCell(52); 
        DataCell r5Cell3 = new DoubleCell(53.0);
        DataRow r5 = new DefaultRow(
                r5Key, new DataCell[] {r5Cell1, r5Cell2, r5Cell3});
        try {
            c.addRowToTable(r5);
            fail();
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }

        // add null
        try {
            c.addRowToTable((DataRow)null);
            fail();
        } catch (NullPointerException e) {
            System.out.println(e.getMessage());
        }
        
        // addRow should preserve the order, we try here randomely generated
        // IntCells as key (the container puts it in a linked has map)
        DataCell[] values = new DataCell[0];
        Vector<DataCell> order = new Vector<DataCell>(500); 
        for (int i = 0; i < 500; i++) {
            // fill it - this should be easy to preserve (as the int value
            // is also the hash code) 
            order.add(new IntCell(i));
        }
        // shuffle it - that should screw it up
        Collections.shuffle(order);
        // empty the container
        c.open(EMPTY_SPEC);
        for (DataCell key : order) {
            c.addRowToTable(new DefaultRow(key, values));
        }
        c.close();
        DataTable table = c.getTable();
        int pos = 0;
        for (RowIterator it = table.iterator(); it.hasNext(); pos++) {
            DataRow cur = it.next();
            assertEquals(cur.getKey().getId(), order.get(pos));
        }
        assertEquals(pos, order.size());
    } // testAddRowToTable()
    
    /**
     * Try a big file :-).
     * 
     */
    public void testBigFile() {
        // with these setting (1000, 5000) it will write an 18MB cache file
        // (the latest data this value was checked: 22. November 2005...)
        final int colCount = 1000;
        final int rowCount = 5000;
        DataCell[] names = new DataCell[colCount];
        DataType[] types = new DataType[colCount];
        for (int c = 0; c < colCount; c++) {
            names[c] = new StringCell("Column " + c);
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
        DataContainer container = new DataContainer();
        container.open(spec);
        final ObjectToDataCellConverter conv = new ObjectToDataCellConverter();
        final long seed = System.currentTimeMillis();
        Random rand = new Random(seed);
        for (int i = 0; i < rowCount; i++) {
            DataCell key = new StringCell("Row " + i);
            DataCell[] cells = new DataCell[colCount];
            for (int c = 0; c < colCount; c++) {
                DataCell cell = null;
                switch (c % 3) {
                case 0: 
                    cell = conv.createDataCell(rand.nextDouble() - 0.5); 
                    break;
                case 1:
                    String s;
                    if (rand.nextDouble() < 0.1) {
                        s = new String(
                                createRandomChars(rand.nextInt(50), rand));
                    } else {
                        s = "Row" + i + "; Column:" + c;
                    }
                    cell = conv.createDataCell(s);
                    break;
                case 2: 
                    // use full range of int
                    int r = (int)rand.nextLong();
                    cell = conv.createDataCell(r); 
                    break;
                default: throw new InternalError();
                }
                cells[c] = cell;
            }
            DataRow row = new DefaultRow(key, cells);
            container.addRowToTable(row);
            row = null;
            cells = null;
            key = null;
        }
        container.close();
        final DataTable table = container.getTable();
        Runnable runnable = new Runnable() {
            public void run() {
                int i = 0;
                Random rand1 = new Random(seed);
                for (RowIterator it = table.iterator(); 
                    it.hasNext(); i++) {
                    DataCell key = new StringCell("Row " + i);
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
                                        rand1.nextInt(50), rand1));
                            } else {
                                s = "Row" + i + "; Column:" + c;
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
                    DataRow row1 = new DefaultRow(key, cells);
                    DataRow row2 = it.next();
                    assertEquals(row1, row2);
                }
                assertEquals(i, rowCount);
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
    } // testBigFile()
    
    /** Test if the domain is retained. */
    public void testTableDomain() {
        DataContainer c = new DataContainer();
        DataCell r1Key = new StringCell("row 1");
        DataCell r1Cell1 = new StringCell("Row 1, Cell 1");
        DataCell r1Cell2 = new IntCell(12);
        DataRow r1 = new DefaultRow(r1Key, new DataCell[] {r1Cell1, r1Cell2});
        DataCell r2Key = new StringCell("row 2");
        DataCell r2Cell1 = new StringCell("Row 2, Cell 1");
        DataCell r2Cell2 = new IntCell(22);
        DataRow r2 = new DefaultRow(r2Key, new DataCell[] {r2Cell1, r2Cell2});
        DataCell r3Key = new StringCell("row 3");
        DataCell r3Cell1 = new StringCell("Row 3, Cell 1");
        DataCell r3Cell2 = new IntCell(32);
        DataRow r3 = new DefaultRow(r3Key, new DataCell[] {r3Cell1, r3Cell2});

        DataCell[] colNames = new DataCell[]{
                new StringCell("Column 1"),
                new StringCell("Column 2")
        };
        DataType[] colTypes = new DataType[] {
                StringCell.TYPE,
                IntCell.TYPE
        };
        DataTableSpec spec1 = new DataTableSpec(colNames, colTypes);
        c.open(spec1);
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
    
}
