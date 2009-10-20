/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
