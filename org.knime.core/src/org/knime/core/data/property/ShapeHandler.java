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
 *   01.02.2006 (mb): created
 */
package org.knime.core.data.property;

import org.knime.core.data.DataCell;
import org.knime.core.data.property.ShapeFactory.Shape;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;


/**
 * Final <code>ShapeHandler</code> implementation which keeps a 
 * <code>ShapeModel</code> to request <code>Shape</code> objects from.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public final class ShapeHandler implements PropertyHandler {
    
    /**
     * The shape model.
     */
    private final ShapeModel m_model;
    
    /**
     * Creates a new <code>ShapeHandler</code> based on the the given
     * <code>ShapeModel</code>.
     * @param model The model for shapes.
     * @throws IllegalArgumentException If the <i>model</i> is 
     *         <code>null</code>.
     */
    public ShapeHandler(final ShapeModel model) {
        if (model == null) {
            throw new IllegalArgumentException("ShapeModel must not be null.");
        }
        m_model = model;
    }
    
    /**
     * Returns a <code>Shape</code> of the given <code>DataCell</code>.
     * 
     * @param dc Value to be used to get a <code>Shape</code> for.
     * @return A <code>Shape</code> object.
     */
    public Shape getShape(final DataCell dc) {
        return m_model.getShape(dc);
    }
    
    private static final String CFG_SHAPE_MODEL_CLASS = "shape_model_class";
    private static final String CFG_SHAPE_MODEL       = "shape_model";
    
    /**
     * Save the <code>ShapeModel</code> class and settings to the given 
     * <code>Config</code>. 
     * @param config To write <code>ShapeModel</code> into.
     * @throws NullPointerException If the <i>config</i> is <code>null</code>.
     */
    public void save(final ConfigWO config) {
        config.addString(CFG_SHAPE_MODEL_CLASS, m_model.getClass().getName());
        m_model.save(config.addConfig(CFG_SHAPE_MODEL));
    }
    
    /**
     * Reads <code>ShapeModel</code> from the given <code>Config</code> and 
     * returns a new <code>ShapeHandler</code>.
     * @param config Read <code>ShapeModel</code> from.
     * @return A new <code>ShapeHandler</code> object.
     * @throws InvalidSettingsException The the <code>ShapeModel</code> could 
     *         not be read.
     * @throws NullPointerException If the <i>config</i> is <code>null</code>.
     */
    public static ShapeHandler load(final ConfigRO config) 
            throws InvalidSettingsException {
        String modelClass = config.getString(CFG_SHAPE_MODEL_CLASS);
        if (modelClass.equals(ShapeModelNominal.class.getName())) {
            ConfigRO subConfig = config.getConfig(CFG_SHAPE_MODEL);
            return new ShapeHandler(ShapeModelNominal.load(subConfig));
        } else {
            throw new InvalidSettingsException("Unknown ShapeModel class: "
                   + modelClass);
        }
    }
    
    /**
     * Returns a string summary of the underlying 
     * {@link org.knime.core.data.property.ShapeHandler.ShapeModel}.
     * 
     * @return a string summary
     */
    @Override
    public String toString() {
        return m_model.toString();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || !(obj instanceof ShapeHandler)) {
            return false;
        }
        return m_model.equals(((ShapeHandler)obj).m_model);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return m_model.hashCode();
    }
    
    /**
     * Internal <code>ShapeModel</code> used to request <code>Shape</code> 
     * objects by <code>DataCell</code> attribute value.
     */
    interface ShapeModel {
        /**
         * Returns a <code>Shape</code> object for a given 
         * <code>DataCell</code>.
         * @param dc The attribute value to get size for.
         * @return A <code>double</code> between 0 and 1.
         */
        Shape getShape(DataCell dc);
        /**
         * Saves this <code>ShapeModel</code> to the given <i>config</i>.
         * @param config This object.
         */
        void save(ConfigWO config);
    }
    
}
