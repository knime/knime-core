/* 
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
 *   06.02.2006 (tg): created
 */
package de.unikn.knime.core.data.property;

import de.unikn.knime.core.data.DataCell;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.config.Config;

/**
 * Interface for a color handler generating colors based on - usually
 * user controlled - function on the value of a <code>DataCell</code>. The 
 * internal <code>ColorModel</code> allows to set colors and supports save and
 * load.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public final class ColorHandler implements PropertyHandler {
    
    /**
     * Knows about the color settings.
     */
    private final ColorModel m_model;
    
    /**
     * Create new color handler with the given model.
     * @param model The color model which keep the color settings.
     * @throws IllegalArgumentException If the model is <code>null</code>.
     */
    public ColorHandler(final ColorModel model) {
        if (model == null) {
            throw new IllegalArgumentException("ColorModel must not be null.");
        }
        m_model = model;
    }
    
    /**
     * Return a <code>ColorAttr</code> as specified by the content
     * of the given <code>DataCell</code>. If no <code>ColorAttr</code>
     * is assigned to <i>dc</i>, this method shall return a default
     * color, but never <code>null</code>.
     * 
     * @param dc Value to be used to generate color.
     * @return A <code>ColorAttr</code> object to be used.
     * @see ColorAttr#DEFAULT
     */
    public ColorAttr getColorAttr(final DataCell dc) {
        return m_model.getColorAttr(dc);
    }
    
    public void save(final Config config) {
        config.addString("color_model_class", m_model.getClass().getName());
        m_model.save(config);
    }
    
    public static ColorHandler load(final Config config) 
            throws InvalidSettingsException {
        String modelClass = config.getString("color_model_class");
        if (modelClass.equals(ColorModelNominal.class.getName())) {
            return new ColorHandler(ColorModelNominal.load(config));
        } else if (modelClass.equals(ColorModelRange.class.getName())) {
            return new ColorHandler(ColorModelRange.load(config));
        } else {
            throw new InvalidSettingsException("Unknown ColorModel: "
                    + modelClass);
        }
    }
    
    interface ColorModel {
        ColorAttr getColorAttr(DataCell dc);
        void save(Config config);
    }

}
