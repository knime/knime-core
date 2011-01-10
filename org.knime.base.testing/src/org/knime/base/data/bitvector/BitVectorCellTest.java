/* 
 * 
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
 *   01.12.2004 (berthold): created
 */
package org.knime.base.data.bitvector;

import junit.framework.TestCase;

/**
 * Test correct implementation of BitVectorCell: 1) parsing of hexadecimal
 * representation 2) conversion to String 3) comparison.
 * 
 * @author Michael Berthold, University of Konstanz
 */
public class BitVectorCellTest extends TestCase {

    /**
     * Test correctness of hexadecimal parsing.
     */
    public void testHexParsing() {
        BitVectorCell newCell;
        newCell = new BitVectorCell("1");
        assertEquals(32, newCell.toString().length());
        assertEquals("10000000000000000000000000000000", newCell.toString());
        assertEquals("00000001", newCell.toHexString());
        newCell = new BitVectorCell("9");
        assertEquals(32, newCell.toString().length());
        assertEquals("10010000000000000000000000000000", newCell.toString());
        assertEquals("00000009", newCell.toHexString());
        newCell = new BitVectorCell("A");
        assertEquals(32, newCell.toString().length());
        assertEquals("01010000000000000000000000000000", newCell.toString());
        assertEquals("0000000A", newCell.toHexString());
        newCell = new BitVectorCell("F");
        assertEquals(32, newCell.toString().length());
        assertEquals("11110000000000000000000000000000", newCell.toString());
        assertEquals("0000000F", newCell.toHexString());
        newCell = new BitVectorCell("02");
        assertEquals(32, newCell.toString().length());
        assertEquals("01000000000000000000000000000000", newCell.toString());
        assertEquals("00000002", newCell.toHexString());
        newCell = new BitVectorCell("84");
        assertEquals(32, newCell.toString().length());
        assertEquals("00100001000000000000000000000000", newCell.toString());
        assertEquals("00000084", newCell.toHexString());
        newCell = new BitVectorCell("18F");
        assertEquals(32, newCell.toString().length());
        assertEquals("11110001100000000000000000000000", newCell.toString());
        assertEquals("0000018F", newCell.toHexString());
        newCell = new BitVectorCell("1234567A0000018F");
        assertEquals(64, newCell.toString().length());
        assertEquals("1234567A0000018F", newCell.toHexString());
    }
}
