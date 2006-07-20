/* -------------------------------------------------------------------
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
 *   06.08.2005 (mb): created
 */
package de.unikn.knime.base.data.util;

import java.util.HashMap;

import de.unikn.knime.core.data.DataCell;

/** Allow a mapping of arbitrary DataCells to unique, well-behaved
 * strings, for example for usage with external executables that may
 * frown upon parsing arbitrary strings.
 * Keeps two HashMaps for each direction (DataCell <-> String) and
 * creates new, unique Strings for unknown DataCells.
 * 
 * @author mb, University of Konstanz
 */
public class DataCellStringMapper {

    private HashMap<DataCell, String> m_cellToString = 
        new HashMap<DataCell, String>();

    private HashMap<String, String> m_origstringToString =
        new HashMap<String, String>();

    private HashMap<String, DataCell> m_stringToCell = 
        new HashMap<String, DataCell>();

    private HashMap<String, String> m_stringToOrigstring = 
        new HashMap<String, String>();

    private int m_uniqueIndex = 0;

    /**
     * @param cell DataCell to be replaced
     * @return unique String representation
     */
    public String dataCellToString(final DataCell cell) {
        // check if this cell already has a mapping:
        if (m_cellToString.containsKey(cell)) {
            // yes, return existing string
            String name = m_cellToString.get(cell);
            assert m_stringToCell.containsKey(name);
            return name;
        }
        // no, create new unique string.
        String newName = uniqueString(cell.toString());
        if ((newName.length() == 0) || (m_stringToCell.containsKey(newName))) {
            m_uniqueIndex++;
            newName = "val" + m_uniqueIndex;
            while (m_stringToCell.containsKey(newName)) {
                m_uniqueIndex++;
                newName = "val" + m_uniqueIndex;
            }
        }
        m_cellToString.put(cell, newName);
        m_stringToCell.put(newName, cell);
        return newName;
    }

    /**
     * @param origString the original 'ugly' string.
     * @return unique String representation
     */
    public String origStringToString(final String origString) {
        // check if this cell already has a mapping:
        if (m_origstringToString.containsKey(origString)) {
            // yes, return existing string
            String name = m_origstringToString.get(origString);
            assert m_stringToOrigstring.containsKey(name);
            return name;
        }
        // no, create new unique string.
        String newName = uniqueString(origString);
        if ((newName.length() == 0)
                || (m_stringToOrigstring.containsKey(newName))) {
            m_uniqueIndex++;
            newName = "val" + m_uniqueIndex;
            while (m_stringToOrigstring.containsKey(newName)) {
                m_uniqueIndex++;
                newName = "val" + m_uniqueIndex;
            }
        }
        m_origstringToString.put(origString, newName);
        m_stringToOrigstring.put(newName, origString);
        return newName;
    }

    /**
     * @param str string representation
     * @return DataCell represented by the string
     */
    public DataCell stringToDataCell(final String str) {
        DataCell c = m_stringToCell.get(str);
        assert (c != null); // String must exist!
        return c;
    }

    /**
     * @param str string representation.
     * @return string original string.
     */
    public String stringToOrigString(final String str) {
        String s = m_stringToOrigstring.get(str);
        assert (s != null); // String must exist!
        return s;
    }

    // create a new name, make sure it's unique and return it.
    // Before creating something completely unrecognizable, try
    // to convert existing DataCell content to something string-like:
    private String uniqueString(final String uglyString) {
        StringBuffer newNameBuffer = new StringBuffer("");
        for (int i = 0; i < uglyString.length(); i++) {
            char c = uglyString.charAt(i);
            if ((('a' <= c) && (c <= 'z')) || (('A' <= c) && (c <= 'Z'))
                    || (('0' <= c) && (c <= '9')) || (c == '-') || (c == '_')) {
                newNameBuffer.append(c);
            }
        }
        return newNameBuffer.toString();
    }

}
