/* 
 * -------------------------------------------------------------------
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
import org.knime.core.data.RowKey;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;


/**
 * A default mapper for hilite translation which holds a map from 
 * {@link RowKey} to a set of {@link RowKey}s as value.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class DefaultHiLiteMapper implements HiLiteMapper {
    
    /** Keep the mapping. */
    private final Map<RowKey, Set<RowKey>> m_map;

    /**
     * Creates a new default hilite mapper.
     * 
     * @param map keeps the <code>RowKey</code> to set of
     *      <code>RowKey</code>s mapping
     */
    public DefaultHiLiteMapper(final Map<RowKey, Set<RowKey>> map) {
        if (map == null) {
            m_map = Collections.emptyMap();
        } else {
            m_map = map;
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public Set<RowKey> getKeys(final RowKey key) {
        return m_map.get(key);
    }

    /**
     * {@inheritDoc}
     */
    public Set<RowKey> keySet() {
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
        for (RowKey key : keySet()) {
            Set<RowKey> mappedKeys = getKeys(key);
            ConfigWO keySettings = config.addConfig(key.toString());
            keySettings.addString(key.getString(), key.getString());
            keySettings.addRowKeyArray(CFG_MAPPED_KEYS, 
                    mappedKeys.toArray(new RowKey[mappedKeys.size()]));
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
        LinkedHashMap<RowKey, Set<RowKey>> mapping 
            = new LinkedHashMap<RowKey, Set<RowKey>>();
        for (String key : config.keySet()) {
            ConfigRO keySettings = config.getConfig(key);
            String cellKey;
            try {
                // load keys before 2.0
                cellKey = keySettings.getDataCell(key).toString();
            } catch (InvalidSettingsException ise) {
                cellKey = keySettings.getString(key);
            }
            Set<RowKey> keySet;
            try {
                // load mapping before 2.0
                DataCell[] mappedKeys = keySettings.getDataCellArray(
                    CFG_MAPPED_KEYS);
                keySet = new LinkedHashSet<RowKey>();
                for (DataCell dc : mappedKeys) {
                    keySet.add(new RowKey(dc.toString()));
                }
            } catch (InvalidSettingsException ise) {
                RowKey[] mappedKeys =
                        keySettings.getRowKeyArray(CFG_MAPPED_KEYS);
                keySet = new LinkedHashSet<RowKey>(Arrays.asList(mappedKeys));
            }
            mapping.put(new RowKey(cellKey), keySet);
        }
        return new DefaultHiLiteMapper(mapping);
    }
}
