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
 *   06.02.2006 (tg): created
 */
package de.unikn.knime.core.data.property;

import java.util.Arrays;

import de.unikn.knime.core.data.DataCell;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.config.Config;
import de.unikn.knime.core.node.config.ConfigRO;
import de.unikn.knime.core.node.config.ConfigWO;

/**
 * Final <code>ColorHandler</code> implementation which forwards color
 * requests to its internal <code>ColorModel</code>, both are able to
 * save and load their color settings to an <code>Config</code> object.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public final class ColorHandler implements PropertyHandler {
    
    /**
     * Knows about the color settings.
     */
    private final ColorModel m_model;
    
    /**
     * Create new color handler with the given <code>ColorModel</code>.
     * @param model The color model which has the color settings.
     * @throws IllegalArgumentException If the model is <code>null</code>.
     */
    public ColorHandler(final ColorModel model) {
        if (model == null) {
            throw new IllegalArgumentException("ColorModel must not be null.");
        }
        m_model = model;
    }
    
    /**
     * Returns a <code>ColorAttr</code> object as specified by the content
     * of the given <code>DataCell</code> requested from the underlying 
     * <code>ColorModel</code>. If no <code>ColorAttr</code>
     * is assigned to <i>dc</i>, this method shall return a default
     * color, but never <code>null</code>.
     * 
     * @param dc Value to be used to generate color.
     * @return A <code>ColorAttr</code> object to be used.
     * @see ColorAttr#DEFAULT
     */
    public ColorAttr getColorAttr(final DataCell dc) {
        ColorAttr color = m_model.getColorAttr(dc);
        if (color == null) {
            return ColorAttr.DEFAULT;
        }
        return color;
    }
    
    private static final String CFG_COLOR_MODEL_CLASS = "color_model_class";
    private static final String CFG_COLOR_MODEL = "color_model";
    
    /**
     * Saves the <code>ColorModel</code> to the given <code>Config</code> by
     * adding a the <code>ColorModel</code> class as String and calling
     * <code>save()</code> within the model.
     * @param config Color settings save to.
     * @throws NullPointerException If the <i>config</i> is <code>null</code>.
     */
    public void save(final Config config) {
        assert config.keySet().isEmpty() : "Subconfig must be empty: " 
            +  Arrays.toString(config.keySet().toArray());
        config.addString(CFG_COLOR_MODEL_CLASS, m_model.getClass().getName());
        m_model.save(config.addConfig(CFG_COLOR_MODEL));
    }
    
    /**
     * Reads the color model settings from the given <code>Config</code>, inits 
     * the model, and returns a new <code>ColorHandler</code>.
     * @param config Read color settings from.
     * @return A new <code>ColorHandler</code> object.
     * @throws InvalidSettingsException If either the class or color model
     *         could not be read.
     * @throws NullPointerException If the <i>config</i> is <code>null</code>. 
     */
    public static ColorHandler load(final ConfigRO config) 
            throws InvalidSettingsException {
        String modelClass = config.getString(CFG_COLOR_MODEL_CLASS);
        if (modelClass.equals(ColorModelNominal.class.getName())) {
            ConfigRO subConfig = config.getConfig(CFG_COLOR_MODEL);
            return new ColorHandler(ColorModelNominal.load(subConfig));
        } else if (modelClass.equals(ColorModelRange.class.getName())) {
            ConfigRO subConfig = config.getConfig(CFG_COLOR_MODEL);
            return new ColorHandler(ColorModelRange.load(subConfig));
        } else {
            throw new InvalidSettingsException("Unknown ColorModel class: "
                    + modelClass);
        }
    }
    
    /**
     * Returns a string summary of the underlying {@link ColorModel}.
     * 
     * @return a string summary
     */
    @Override
    public String toString() {
        return m_model.toString();
    }
    
 
    /**
     * Interface for allowing requests for color settings. Only package visible.
     */
    interface ColorModel {
        /**
         * Returns the <code>ColorAttr</code> for given attribut value.
         * @param dc The <code>DataCell</code> to get the color for.
         * @return A <code>ColorAttr</code> object, but not <code>null</code>.
         */
        ColorAttr getColorAttr(DataCell dc);
        /**
         * Save color model settings to the given <code>Config</code>.
         * @param config Save settings to.
         */
        void save(ConfigWO config);
    }

}
