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
 * If you have any quesions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *   02.06.2006 (gabriel): created
 */
package de.unikn.knime.core.data.property;

import java.awt.Color;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import de.unikn.knime.core.data.DataCell;
import de.unikn.knime.core.data.property.ColorHandler.ColorModel;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.config.ConfigRO;
import de.unikn.knime.core.node.config.ConfigWO;

/**
 * Color model which maps a set of <code>DataCell</code> objects to 
 * <code>Color</code>.
 */
public final class ColorModelNominal implements ColorModel, Iterable<DataCell> {
    
    /** Maps DataCell values to ColorAttr. */
    private final Map<DataCell, ColorAttr> m_map;
    
    /**
     * Creates new ColorHandler based on a mapping.
     * @param map Mapping form DataCell values to ColorAttr objects.
     * @throws IllegalArgumentException If the map is null.
     */
    public ColorModelNominal(final Map<DataCell, ColorAttr> map) {
        if (map == null)  {
            throw new IllegalArgumentException("Mapping must not be null.");
        }
        m_map = Collections.unmodifiableMap(map);
    }

    /**
     * Returns a ColorAttr for the given DataCell value, or 
     * <code>ColorAttr.DEFAULT</code> if not set.
     * @param dc A DataCell value to get color for.
     * @return A ColorAttr for a DataCell value.
     */
    public ColorAttr getColorAttr(final DataCell dc) {
        Object o = m_map.get(dc);
        if (o == null) {
            return ColorAttr.DEFAULT;
        }
        return (ColorAttr) o;
    }
    
    /**
     * Returns an iterator over the keys.
     * @see java.lang.Iterable#iterator()
     * @return - returns an iterator over the keys.
     */
    public Iterator<DataCell> iterator() {
        return m_map.keySet().iterator();
    }
    
    private static final String CFG_KEYS = "keys";
    
    /**
     * Saves the <code>DataCell</code> to <code>Color</code> mapping to the 
     * given <code>Config</code>. The color is split into red, green, blue, and
     * alpha component which are stored as int array.
     * @param config Save settings to.
     * @see de.unikn.knime.core.data.property.ColorHandler.ColorModel
     *      #save(ConfigWO)
     * @throws NullPointerException If the <i>config</i> is <code>null</code>. 
     */
    public void save(final ConfigWO config) {
        ConfigWO keyConfig = config.addConfig(CFG_KEYS);
        for (Map.Entry<DataCell, ColorAttr> e : m_map.entrySet()) {
            DataCell key = e.getKey();
            keyConfig.addDataCell(key.toString(), key);
            Color color = e.getValue().getColor();
            config.addIntArray(key.toString(), 
                    color.getRed(), color.getGreen(), 
                    color.getBlue(), color.getAlpha());
        }
    }
    
    /**
     * Read color settings from given <code>Config</code> and returns a new
     * <code>ColorModelNominal</code> object.
     * @param config Reads color model from.
     * @return A new <code>ColorModelNominal</code> object.
     * @throws InvalidSettingsException If the color model settings could not
     *         be read.
     * @throws NullPointerException If the <i>config</i> is <code>null</code>.
     */
    public static ColorModelNominal load(final ConfigRO config) 
            throws InvalidSettingsException {
        Map<DataCell, ColorAttr> map = new HashMap<DataCell, ColorAttr>();
        ConfigRO keyConfig = config.getConfig(CFG_KEYS);
        for (String key : keyConfig.keySet()) {
            int[] value = config.getIntArray(key.toString());
            Color color = new Color(value[0], value[1], value[2], value[3]);
            DataCell cell = keyConfig.getDataCell(key);
            map.put(cell, ColorAttr.getInstance(color));
        }
        return new ColorModelNominal(map);
    }

    /**
     * @return A String for this <code>ColorModel</code> as list of 
     * <code>DataCell</code> to <code>Color</code> mapping.
     */
    public String printColorMapping() {
        StringBuffer buf = new StringBuffer();
        for (DataCell cell : m_map.keySet()) {
            Color color = m_map.get(cell).getColor();
            if (buf.length() > 0) {
                buf.append(",");
            }
            buf.append(cell.toString() + "->" + color.toString());
        }
        return "[" + buf.toString() + "]";
    }
      
    /**
     * @return <i>Nominal ColorModel</i>
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Nominal ColorModel";
    }
    
}   
