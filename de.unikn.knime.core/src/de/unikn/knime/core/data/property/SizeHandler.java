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
 *   01.02.2006 (mb): created
 */
package de.unikn.knime.core.data.property;

import java.util.Arrays;

import de.unikn.knime.core.data.DataCell;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.config.Config;

/**
 * Final <code>SizeHandler</code> implementation which keeps a 
 * <code>SizeModel</code> to request size settings from.
 * 
 * @author M. Berthold, University of Konstanz
 */
public final class SizeHandler implements PropertyHandler {
    
    /**
     * The size model.
     */
    private final SizeModel m_model;
    
    /**
     * Creates a new <code>SizeHandler</code> based on the the given
     * <code>SizeModel</code>.
     * @param model The model for sizes.
     * @throws IllegalArgumentException If the <i>model</i> is 
     *         <code>null</code>.
     */
    public SizeHandler(final SizeModel model) {
        if (model == null) {
            throw new IllegalArgumentException("SizeModel must not be null.");
        }
        m_model = model;
    }
    
    /** The default size in case no SizeHandler exists. */
    public static final double DEFAULT_SIZE = 0.0;
    
    /**
     * Return size (in [0,1], that is percent, as specified by the content
     * of the given <code>DataCell</code>.
     * 
     * @param dc Value to be used to compute size.
     * @return percentage value to base actual size on. -1 if value is illegal.
     */
    public double getSize(final DataCell dc) {
        return m_model.getSize(dc);
    }
    
    private static final String CFG_SIZE_MODEL_CLASS = "size_model_class";
    private static final String CFG_SIZE_MODEL        = "size_model";
    
    /**
     * Save the <code>SizeModel</code> class and settings to the given 
     * <code>Config</code>. 
     * @param config To write size settings into.
     * @throws NullPointerException If the <i>config</i> is <code>null</code>.
     */
    public void save(final Config config) {
        assert config.keySet().isEmpty() : "Subconfig must be empty: " 
            +  Arrays.toString(config.keySet().toArray());
        config.addString(CFG_SIZE_MODEL_CLASS, m_model.getClass().getName());
        m_model.save(config.addConfig(CFG_SIZE_MODEL));
    }
    
    /**
     * Reads size settings from the given <code>Config</code> and returns a new
     * <code>SizeHandler</code>.
     * @param config Read size settings from.
     * @return A new <code>SizeHandler</code> object.
     * @throws InvalidSettingsException The the size model settings could not
     *         be read.
     * @throws NullPointerException If the <i>config</i> is <code>null</code>.
     */
    public static SizeHandler load(final Config config) 
            throws InvalidSettingsException {
        String modelClass = config.getString(CFG_SIZE_MODEL_CLASS);
        if (modelClass.equals(SizeModelDouble.class.getName())) {
            Config subConfig = config.getConfig(CFG_SIZE_MODEL);
            return new SizeHandler(SizeModelDouble.load(subConfig));
        } else {
            throw new InvalidSettingsException("Unknown SizeModel class: "
                   + modelClass);
        }
    }
    
    /**
     * @return String summary of the underlying <code>SizeModel</code>.
     * @see SizeModel#toString()
     */
    @Override
    public String toString() {
        return m_model.toString();
    }
    
    /**
     * Internal <code>SizeModel</code> used to request sizes by 
     * <code>DataCell</code> attribut value.
     */
    interface SizeModel {
        /**
         * Returns a <code>double</code> value for a given <code>DataCell</code>
         * within 0 and 1, or -1 if no color setting available.
         * @param dc The attribut value to get size for.
         * @return A <code>double</code> between 0 and 1.
         */
        double getSize(DataCell dc);
        /**
         * Save size settings to.
         * @param config This object.
         */
        void save(Config config);
    }
    
}
