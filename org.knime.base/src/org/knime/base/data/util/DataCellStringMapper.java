/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 *   06.08.2005 (mb): created
 */
package org.knime.base.data.util;

import java.util.HashMap;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;


/**
 * Allow a mapping of arbitrary {@link org.knime.core.data.DataCell}s to
 * unique, well-behaved strings, for example for usage with external executables
 * that may frown upon parsing arbitrary strings. Keeps two maps for each
 * direction ({@link DataCell} <-> {@link String}) and creates new, unique
 * Strings for unknown DataCells.
 * 
 * @author Michael Berthold, University of Konstanz
 * @author Nicolas Cebron, University of Konstanz
 */
public final class DataCellStringMapper {

    private static final String CFG_ORIGSTRINGTOSTRING = "origstringToString";

    private static final String CFG_STRINGTOCELL = "stringToCell";

    private static final String CFG_UNIQUEINDEX = "unique_index";
    
    private final HashMap<DataCell, String> m_cellToString;

    private final  HashMap<String, String> m_origstringToString;

    private final HashMap<String, DataCell> m_stringToCell;

    private final HashMap<String, String> m_stringToOrigstring;

    private int m_uniqueIndex = 0;

    /**
     * Public constructor.
     */
    public DataCellStringMapper() {
        m_cellToString = new HashMap<DataCell, String>();
        m_origstringToString = new HashMap<String, String>();
        m_stringToCell = new HashMap<String, DataCell>();
        m_stringToOrigstring = new HashMap<String, String>();
    }
    
    private DataCellStringMapper(final HashMap<DataCell, String> cellToString,
            final HashMap<String, String> origstringToString,
            final HashMap<String, DataCell> stringToCell,
            final HashMap<String, String> stringToOrigstring,
            final int uniqueIndex) {
        m_cellToString = cellToString;
        m_origstringToString = origstringToString;
        m_stringToCell = stringToCell;
        m_stringToOrigstring = stringToOrigstring;
        m_uniqueIndex = uniqueIndex;
    }
        
    
    /**
     * @param cell {@link DataCell} to be replaced
     * @return unique string representation
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
     * @param origString the original 'ugly' string
     * @return unique string representation
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
     * @return {@link DataCell} represented by the string
     */
    public DataCell stringToDataCell(final String str) {
        DataCell c = m_stringToCell.get(str);
        assert (c != null); // String must exist!
        return c;
    }

    /**
     * @param str string representation
     * @return string original string
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
                    || (('0' <= c) && (c <= '9')) || (c == '-') || (c == '_')
                    || (c == '?')) {
                newNameBuffer.append(c);
            }
        }
        return newNameBuffer.toString();
    }
    
    /**
     * Saves the {@link DataCellStringMapper}> to the given {@link ConfigWO}.
     * 
     * @param config Save settings to.
     */
    public void save(final ConfigWO config) {
        config.addInt(CFG_UNIQUEINDEX, m_uniqueIndex);
        ConfigWO origstringToString = config.addConfig(CFG_ORIGSTRINGTOSTRING);
        for (Map.Entry<String, String> e : m_origstringToString.entrySet()) {
            String key = e.getKey();
            String value = e.getValue();
            origstringToString.addString(key, value);
        }
        ConfigWO stringToCell = config.addConfig(CFG_STRINGTOCELL);
        for (Map.Entry<String, DataCell> e : m_stringToCell.entrySet()) {
            String key = e.getKey();
            DataCell v = e.getValue();
            stringToCell.addDataCell(key, v);
        }
    }

    /**
     * Reads a {@link DataCellStringMapper} from given {@link ConfigRO}.
     * 
     * @param config to read the mapper from
     * @return A new {@link DataCellStringMapper} object.
     * @throws InvalidSettingsException If the settings could not be read.
     */
    public static DataCellStringMapper load(final ConfigRO config)
            throws InvalidSettingsException {
        HashMap<String, DataCell> tmpstringToCell =
                new HashMap<String, DataCell>();
        HashMap<DataCell, String> tmpcellToString =
                new HashMap<DataCell, String>();
        ConfigRO keyConfig = config.getConfig(CFG_STRINGTOCELL);
        for (String key : keyConfig.keySet()) {
            DataCell cell = keyConfig.getDataCell(key);
            tmpstringToCell.put(key, cell);
            tmpcellToString.put(cell, key);
        }
        HashMap<String, String> tmporigstringToString =
                new HashMap<String, String>();
        HashMap<String, String> tmpstringToOrigstring =
                new HashMap<String, String>();
        keyConfig = config.getConfig(CFG_ORIGSTRINGTOSTRING);
        for (String key : keyConfig.keySet()) {
            String value = keyConfig.getString(key);
            tmporigstringToString.put(key, value);
            tmpstringToOrigstring.put(value, key);
        }
        int tmpuniqueIndex = config.getInt(CFG_UNIQUEINDEX);

        return new DataCellStringMapper(tmpcellToString, tmporigstringToString,
                tmpstringToCell, tmpstringToOrigstring, tmpuniqueIndex);
    }
}
