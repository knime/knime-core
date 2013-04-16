/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 *   02.06.2006 (gabriel): created
 */
package org.knime.core.data.property;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.property.ShapeFactory.Shape;
import org.knime.core.data.property.ShapeHandler.ShapeModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;


/**
 * Nominal <code>ShapeModel</code> which maps a set of <code>DataCell</code> 
 * element to <code>Shape</code>.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public final class ShapeModelNominal implements ShapeModel, Iterable<DataCell> {
    
    /** Maps DataCell values to Shape. */
    private final Map<DataCell, Shape> m_map;
    
    /**
     * Creates new nominal <code>ShapeModel</code> based on a mapping.
     * @param map Mapping form <code>DataCell</code> values to 
     *            <code>Shape</code> objects.
     * @throws IllegalArgumentException If the map is null.
     */
    public ShapeModelNominal(final Map<DataCell, Shape> map) {
        if (map == null)  {
            throw new IllegalArgumentException("Mapping must not be null.");
        }
        m_map = Collections.unmodifiableMap(map);
    }

    /**
     * Returns a Shape for the given DataCell value, or 
     * <code>ShapeFactory.DEFAULT</code> if not set.
     * @param dc A DataCell value to get shape for.
     * @return A Shape for a DataCell value.
     */
    public Shape getShape(final DataCell dc) {
        Shape shape = m_map.get(dc);
        if (shape == null) {
            return ShapeFactory.getShape(ShapeFactory.DEFAULT);
        }
        return shape;
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
     * Saves the <code>DataCell</code> to <code>Shape</code> mapping to the 
     * given <code>Config</code>.
     * @param config Save settings to.
     * @throws NullPointerException If the <i>config</i> is <code>null</code>. 
     */
    public void save(final ConfigWO config) {
        ConfigWO keyConfig = config.addConfig(CFG_KEYS);
        for (Map.Entry<DataCell, Shape> e : m_map.entrySet()) {
            DataCell key = e.getKey();
            keyConfig.addDataCell(key.toString(), key);
            Shape shape = e.getValue();
            config.addString(key.toString(), shape.toString());
        }
    }
    
    /**
     * Reads Shape settings from given <code>Config</code> and returns a new
     * <code>ShapeModelNominal</code> object.
     * @param config Reads shape model from.
     * @return A new <code>ShapeModelNominal</code> object.
     * @throws InvalidSettingsException If the <code>ShapeModel</code> settings
     *         could not be read.
     * @throws NullPointerException If the <i>config</i> is <code>null</code>.
     */
    public static ShapeModelNominal load(final ConfigRO config) 
            throws InvalidSettingsException {
        Map<DataCell, Shape> map = new HashMap<DataCell, Shape>();
        ConfigRO keyConfig = config.getConfig(CFG_KEYS);
        for (String key : keyConfig.keySet()) {
            String shape = config.getString(key.toString());
            DataCell cell = keyConfig.getDataCell(key);
            map.put(cell, ShapeFactory.getShape(shape));
        }
        return new ShapeModelNominal(map);
    }

    /**
     * @return A String for this <code>ShapeModel</code> as list of 
     * <code>DataCell</code> to <code>Shape</code> mapping.
     */
    public String printShapeMapping() {
        StringBuilder buf = new StringBuilder();
        for (DataCell cell : m_map.keySet()) {
            Shape shape = m_map.get(cell);
            if (buf.length() > 0) {
                buf.append(",");
            }
            buf.append(cell.toString() + "->" + shape.toString());
        }
        return "[" + buf.toString() + "]";
    }
      
    /**
     * @return <i>Nominal ShapeModel</i>
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Nominal ShapeModel";
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || !(obj instanceof ShapeModelNominal)) {
            return false;
        }
        ShapeModelNominal model = (ShapeModelNominal) obj;
        return m_map.equals(model.m_map);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return m_map.hashCode();
    }
}   
