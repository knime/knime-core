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
 *   02.06.2006 (gabriel): created
 */
package de.unikn.knime.core.data.property;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import de.unikn.knime.core.data.DataCell;
import de.unikn.knime.core.data.property.ColorHandler.ColorModel;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.config.Config;

/**
 * Color model which maps a set of <code>DataCell</code> objects to 
 * <code>Color</code>.
 */
public final class ColorModelNominal implements ColorModel {
    
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
        m_map = map;
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
     * Saves the <code>DataCell</code> to <code>Color</code> mapping to the 
     * given <code>Config</code>. The color is split into red, green, blue, and
     * alpha component which are stored as int array.
     * @param config Save settings to.
     * @see de.unikn.knime.core.data.property.ColorHandler.ColorModel
     *      #save(de.unikn.knime.core.node.config.Config)
     * @throws NullPointerException If the <i>config</i> is <code>null</code>. 
     */
    public void save(final Config config) {
        DataCell[] keys = m_map.keySet().toArray(new DataCell[0]);
        config.addDataCellArray("keys", keys);
        for (int i = 0; i < keys.length; i++) {
            Color color = m_map.get(keys[i]).getColor();
            config.addIntArray(keys[i].toString(), 
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
    public static ColorModelNominal load(final Config config) 
            throws InvalidSettingsException {
        Map<DataCell, ColorAttr> map = new HashMap<DataCell, ColorAttr>();
        DataCell[] keys = config.getDataCellArray("keys");
        for (int i = 0; i < keys.length; i++) {
            int[] value = config.getIntArray(keys[i].toString());
            Color color = new Color(value[0], value[1], value[2], value[3]);
            map.put(keys[i], ColorAttr.getInstance(color));
        }
        return new ColorModelNominal(map);
    }
}   
