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
 *   01.02.2006 (mb): created
 */
package org.knime.core.data.property;

import org.knime.core.data.DataCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;


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
    
    
    /** The default size in case no SizeHandler exists. 
     * @deprecated use {@link #DEFAULT_SIZE_FACTOR} instead
     */
    @Deprecated
    public static final double DEFAULT_SIZE = 0.0;
    
    /** The default size factor in case no SizeHandler exist, which is one, that
     * is no scaling is done. 
     */
    public static final double DEFAULT_SIZE_FACTOR = 1.0;
    
    /**
     * Return size (in [0,1], that is percent, as specified by the content
     * of the given <code>DataCell</code>.
     * 
     * @param dc Value to be used to compute size.
     * @return percentage value to base actual size on. -1 if value is illegal.
     * @deprecated use {@link #getSizeFactor(DataCell)} instead
     */
    @Deprecated
    public double getSize(final DataCell dc) {
        return m_model.getSize(dc);
    }
    
    /**
     * Returns the size as a scaling factor (in [1, )).
     * @param dc value to use to compute size for
     * @return a double value > 1
     */
    public double getSizeFactor(final DataCell dc) {
        return m_model.getSizeFactor(dc);
    }
    
    private static final String CFG_SIZE_MODEL_CLASS = "size_model_class";
    private static final String CFG_SIZE_MODEL       = "size_model";
    
    /**
     * Save the <code>SizeModel</code> class and settings to the given 
     * <code>Config</code>. 
     * @param config To write size settings into.
     * @throws NullPointerException If the <i>config</i> is <code>null</code>.
     */
    public void save(final ConfigWO config) {
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
    public static SizeHandler load(final ConfigRO config) 
            throws InvalidSettingsException {
        String modelClass = config.getString(CFG_SIZE_MODEL_CLASS);
        if (modelClass.equals(SizeModelDouble.class.getName())) {
            ConfigRO subConfig = config.getConfig(CFG_SIZE_MODEL);
            return new SizeHandler(SizeModelDouble.load(subConfig));
        } else {
            throw new InvalidSettingsException("Unknown SizeModel class: "
                   + modelClass);
        }
    }
    
    /**
     * Returns a string summary of the underlying 
     * {@link org.knime.core.data.property.SizeHandler.SizeModel}.
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
        if (obj == null || !(obj instanceof SizeHandler)) {
            return false;
        }
        return m_model.equals(((SizeHandler)obj).m_model);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return m_model.hashCode();
    }
    
    /**
     * Internal <code>SizeModel</code> used to request sizes by 
     * <code>DataCell</code> attribute value.
     */
    interface SizeModel {
        /**
         * Returns a <code>double</code> value for a given <code>DataCell</code>
         * within 0 and 1, or -1 if no color setting available.
         * @param dc The attribute value to get size for.
         * @return A <code>double</code> between 0 and 1.
         * @deprecated use {@link #getSizeFactor(DataCell)} instead.
         */
        @Deprecated
        double getSize(DataCell dc);
        
        /**
         * 
         * @param dc the attribute value to get the size factor for 
         * @return a double indicating the maginfication relative to the 
         *  normal size used
         */
        double getSizeFactor(DataCell dc);
        /**
         * Save size settings to.
         * @param config This object.
         */
        void save(ConfigWO config);
    }
    
}
