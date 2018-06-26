/* 
 * 
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
