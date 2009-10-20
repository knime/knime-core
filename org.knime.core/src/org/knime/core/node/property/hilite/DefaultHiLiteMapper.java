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
