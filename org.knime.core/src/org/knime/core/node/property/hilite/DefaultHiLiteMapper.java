/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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
 *   21.03.2005 (gabriel): created
 * 2006-06-08 (tm): reviewed   
 */
package org.knime.core.node.property.hilite;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;


/**
 * A default mapper for hilite translation which holds a map from 
 * {@link DataCell} to a set of {@link DataCell}s as value.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class DefaultHiLiteMapper implements HiLiteMapper {    
    /*
     * Keep the mapping.
     */
    private final Map<DataCell, Set<DataCell>> m_map;
    
    /**
     * Creates a new default hilite mapper.
     * 
     * @param map keeps the <code>DataCell</code> to set of
     *      <code>DataCell</code>s mapping
     * @throws NullPointerException if <code>map</code> is <code>null</code>
     */
    public DefaultHiLiteMapper(final Map<DataCell, Set<DataCell>> map) {
        if (map == null) {
            throw new NullPointerException("Map must not be null.");
        }
        m_map = map;
    }

    /**
     * @see HiLiteMapper#getKeys(org.knime.core.data.DataCell)
     */
    public Set<DataCell> getKeys(final DataCell key) {
        return m_map.get(key);
    }

    /**
     * @see java.lang.Iterable#iterator()
     */
    public Set<DataCell> keySet() {
        return Collections.unmodifiableSet(m_map.keySet());
    }
    
    
    private static final String CFG_MAPPED_KEYS = "CFG_MAPPED_KEYS";

    /**
     * Saves the settings in this mapper to a config object. Note that it writes
     * directly to the passed root node of the config tree. It's good practice
     * to open an local config object on which this method is invoked.
     * @param config The config to write to.
     */
    public void save(final ConfigWO config) {
        for (DataCell key : keySet()) {
            Set<DataCell> mappedKey = getKeys(key);
            ConfigWO keySettings = config.addConfig(key.toString());
            keySettings.addDataCell(key.toString(), key);
            keySettings.addDataCellArray(CFG_MAPPED_KEYS, 
                    mappedKey.toArray(new DataCell[mappedKey.size()]));
        }
    }
    
    /** Restores the mapper from the config object that has been written using
     * the save method.
     * @param config To read from
     * @return A new mapper based on the settings.
     * @throws InvalidSettingsException If that fails.
     */
    public static DefaultHiLiteMapper load(final ConfigRO config) 
        throws InvalidSettingsException {
        // load hilite mapping
        LinkedHashMap<DataCell, Set<DataCell>> mapping 
            = new LinkedHashMap<DataCell, Set<DataCell>>();
        for (String key : config.keySet()) {
            ConfigRO keySettings = config.getConfig(key);
            DataCell cellKey = keySettings.getDataCell(key);
            DataCell[] mappedKeys = keySettings.getDataCellArray(
                    CFG_MAPPED_KEYS);
            mapping.put(cellKey, new LinkedHashSet<DataCell>(
                    Arrays.asList(mappedKeys)));
        }
        return new DefaultHiLiteMapper(mapping);
    }
}
