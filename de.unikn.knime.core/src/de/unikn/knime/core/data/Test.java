/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
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
 * History
 *   09.07.2005 (bernd): created
 */
package de.unikn.knime.core.data;

import java.util.Arrays;

import junit.framework.TestCase;
import de.unikn.knime.core.data.def.DefaultDoubleCell;
import de.unikn.knime.core.data.def.DefaultFuzzyIntervalCell;
import de.unikn.knime.core.data.def.DefaultFuzzyNumberCell;

/**
 * 
 * @author bernd, University of Konstanz
 */
public class Test extends TestCase {

    /**
     * Tests plain doubles.
     */
    public void testPlainDouble() {
        // make a bunch of int and double cells
        DataCell[] cells = new DataCell[10];
        for (int i = 0; i < cells.length; i++) {
            cells[i] = new DefaultDoubleCell((double)i);
        }
        DataType superType = cells[0].getType();
        for (int i = 1; i < cells.length; i++) {
            superType = 
                DataType.getCommonSuperType(superType, cells[i].getType());
        }
        assertTrue(superType.isCompatible(DoubleValue.class));
        // that would be desirable:
        // assertTrue(superType.canReturn(DblValue.class));
        for (int i = 0; i < cells.length; i++) {
            double d = ((DoubleValue)cells[i]).getDoubleValue();
            assertEquals(d, (double)i, 0.0);
        }
    }

    /**
     * Test fuzzy cells.
     */
    public void testFuzzy() {
        // make a bunch of int and double cells
        DataCell[] cells = new DataCell[10];
        for (int i = 0; i < cells.length; i++) {
            if (Math.random() < 0.5) {
                cells[i] = new DefaultFuzzyNumberCell((double)(i - 1),
                        (double)i, (double)(i + 1));
            } else {
                cells[i] = new DefaultFuzzyIntervalCell((double)(i - 1),
                        (double)i, i + 0.5, (double)(i + 1));
            }
        }
        DataType superType = cells[0].getType();
        for (DataCell c : cells) {
            superType = DataType.getCommonSuperType(superType, c.getType());
        }
        assertTrue(superType.isCompatible(FuzzyIntervalValue.class));
        assertTrue(superType == FuzzyIntervalType.FUZZY_INTERVAL_TYPE);
        // that would be desirable:
        // assertTrue(superType.canReturn(DblValue.class));
        for (int i = 0; i < cells.length; i++) {
            double d = ((FuzzyIntervalValue)cells[i]).getMinSupport();
            assertEquals(d, i - 1, 0.0);
        }
    }

    /**
     * Test sorting.
     */
    public void testSort() {
        // make a bunch of int and double cells
        DataCell[] cells = new DataCell[10];
        for (int i = 0; i < cells.length; i++) {
            if (Math.random() < 0.5) {
                cells[i] = new DefaultFuzzyNumberCell((double)(i - 1),
                        (double)i, (double)(i + 1));
            } else {
                cells[i] = new DefaultFuzzyIntervalCell((double)(i - 1),
                        (double)i, i + 0.5, (double)(i + 1));
            }
        }
        DataType superType = cells[0].getType();
        for (DataCell c : cells) {
            superType = DataType.getCommonSuperType(superType, c.getType());
        }
        assertTrue(superType.isCompatible(FuzzyIntervalValue.class));
        Arrays.sort(cells, superType.getComparator());
    }

}
