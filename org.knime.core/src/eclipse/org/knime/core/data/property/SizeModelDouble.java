/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.property.SizeHandler.SizeModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;

/**
 * A <code>SizeModel</code> computing sizes of objects (rows) based on the 
 * <code>double</code> value of <code>DataCell</code>.
 * 
 * @author M. Berthold, University of Konstanz
 */
public class SizeModelDouble implements SizeModel {
    /**
     * Supported mapping methods.
     * 
     */
    public enum Mapping {
        
        /** Linear mapping: (v - min / max - min). */
        LINEAR {
            /**
             * 
             * {@inheritDoc}
             */
            @Override
            double getMappedSize(final double d, final double min, 
                    final double max, final double factor) {
                return (((d - min) / (max - min)) * (factor - 1)) + 1;
            }
        },
        /** Square root mapping: (sqrt(v) - sqrt(min) / sqrt(max) - sqrt(min)).
        */
        SQUARE_ROOT {
            /**
             * 
             * {@inheritDoc}
             */
            @Override
            double getMappedSize(final double d, final double min, 
                    final double max, final double factor) {
                return (((Math.sqrt(d) - Math.sqrt(min)) 
                        / (Math.sqrt(max) - Math.sqrt(min)))
                        * (factor - 1)) + 1;
            }
        },
        /** Logarithmic mapping: (ln(v) - ln(min) / ln(max) - ln(min)). */
        LOGARITHMIC {
            /**
             * 
             * {@inheritDoc}
             */
            @Override
            double getMappedSize(final double d, final double min, 
                    final double max, final double factor) {
                return (((Math.log(d) - Math.log(min)) 
                        / (Math.log(max) - Math.log(min))) 
                        * (factor - 1)) + 1;
            }
        },
        /** Exponential mapping: (pow(v) - pow(min) / pow(max) - pow(min)).*/
        EXPONENTIAL {
            /**
             * 
             * {@inheritDoc}
             */
            @Override
            double getMappedSize(final double d, final double min, 
                    final double max, final double factor) {
                return (((Math.pow(d, 2) - Math.pow(min, 2))
                        / (Math.pow(max, 2) - Math.pow(min, 2)))
                        * (factor - 1)) + 1;
            }
        };
        
        /**
         * Returns the mapped size according to the referring mapping method.
         * @param d the domain value to be mapped
         * @param min the minimum domain value
         * @param max the maximum domain value
         * @param factor the scaling factor
         * @return the mapped value
         */
        abstract double getMappedSize(final double d, final double min, 
                final double max, final double factor);

        private static final List<String> VALUES;
        static {
            VALUES = new ArrayList<String>();
            VALUES.add(LINEAR.name());
            VALUES.add(SQUARE_ROOT.name());
            VALUES.add(LOGARITHMIC.name());
            VALUES.add(EXPONENTIAL.name());
        }
        /**
         * 
         * @return all fields as an unmodifiable list of strings
         */
        public static List<String> getStringValues() {
            return Collections.unmodifiableList(VALUES);
        }
    }
    
    
    /** Minimum range value of domain. */
    private final double m_min;
    
    /** Maximum range value of domain. */
    private final double m_max;

    private final double m_factor;
    
    private final Mapping m_mapping;
    
    
    /**
     * Create new SizeHandler based on double values and a given interval.
     * 
     * @param min minimum of domain
     * @param max maximum of domain
     * @throws IllegalArgumentException If min &lt; max
     */
    public SizeModelDouble(final double min, final double max) {
        if (min < max) {
            throw new IllegalArgumentException(
                    "min must not be smaller than max: " + min + " < " + max);
        }
        m_min = min;
        m_max = max;
        m_factor = 2;
        m_mapping = Mapping.LINEAR;
    }
    
    /**
     * Creates a new SizeHandler based on an interval defined by min and max 
     * and a magnification factor which defines the range onto the interval is 
     * mapped. Uses linear mapping.
     *   
     * @param min minimum of the domain
     * @param max maximum of the domain
     * @param factor scaling factor for the mapping
     */
    public SizeModelDouble(final double min, final double max, 
            final double factor) {
        assert min < max;
        m_min = min;
        m_max = max;
        m_factor = factor;
        m_mapping = Mapping.LINEAR;
    }
    
    /**
     * Creates a new SizeHandler based on an interval defined by min and max 
     * and a magnification factor which defines the range onto the interval is 
     * mapped. Uses the provided mapping method.
     *   
     * @param min minimum of the domain
     * @param max maximum of the domain
     * @param factor scaling factor for the mapping
     * @param mapping the mapping method to use (linear, square root, 
     *      logarithmic)
     */
    public SizeModelDouble(final double min, final double max, 
            final double factor, final Mapping mapping) {
        assert min < max;
        m_min = min;
        m_max = max;
        m_factor = factor;
        m_mapping = mapping;
    }    
    
    
    /**
     * Compute size based on actual value of this cell and the range
     * which was defined during construction.
     * 
     * @param dc value to be used for size computation.
     * @return size in percent or -1 if cell type invalid or out of range
     * 
     * @deprecated use {@link #getSizeFactor(DataCell)} instead.
     * @see SizeHandler#getSize(DataCell)
     */
    @Deprecated
    public double getSize(final DataCell dc) {
        if (dc.isMissing()) {
            return SizeHandler.DEFAULT_SIZE;
        }
        if (dc.getType().isCompatible(DoubleValue.class)) {
            double d = ((DoubleValue)dc).getDoubleValue();
            if ((d < m_min) || (d > m_max)) {
                return SizeHandler.DEFAULT_SIZE;    // out of range
            }
            return (d - m_min) / (m_max - m_min);
        }
        return SizeHandler.DEFAULT_SIZE;       // incomptible type
    }
    
    /**
     * Computes the size based on the actual value of the provided cell, the 
     * interval, the scaling factor and the mapping method. Factor will be 
     * larger or equal to one and with no maximum value. Indicates the scaling 
     * factor. The largest value should be displayed <code>n</code> times 
     * larger.
     * 
     * 
     * {@inheritDoc}
     */
    public double getSizeFactor(final DataCell dc) {
        if (dc.isMissing()) {
            return SizeHandler.DEFAULT_SIZE_FACTOR;
        }
        if (dc.getType().isCompatible(DoubleValue.class)) {
            double d = ((DoubleValue)dc).getDoubleValue();
            if ((d < m_min) || (d > m_max)) {
                return SizeHandler.DEFAULT_SIZE_FACTOR; // out of range
            }
            return m_mapping.getMappedSize(d, m_min, m_max, m_factor);
        }
        return SizeHandler.DEFAULT_SIZE_FACTOR; // incompatible type
    }

    
    /** @return minimum double value. */
    public double getMinValue() {
        return m_min;
    }

    /** @return maximum double value. */
    public double getMaxValue() {
        return m_max;
    }
    
    /**
     * 
     * @return the scaling factor
     */
    public double getFactor() {
        return m_factor;
    }
    
    /**
     * 
     * @return the mapping method
     */
    public Mapping getMappingMethod() {
        return m_mapping;
    }

    private static final String CFG_MIN = "min";
    private static final String CFG_MAX = "max";
    private static final String CFG_FACTOR = "factor";
    private static final String CFG_MAPPING = "mapping";
    
    /**
     * Saves min and max ranges to the given <code>Config</code>.
     * @param config To write bounds into.
     * @see org.knime.core.data.property.SizeHandler.SizeModel
     *      #save(ConfigWO)
     * @throws NullPointerException If the <i>config</i> is <code>null</code>.
     */
    public void save(final ConfigWO config) {
        config.addDouble(CFG_MIN, m_min);
        config.addDouble(CFG_MAX, m_max);
        config.addDouble(CFG_FACTOR, m_factor);
        config.addString(CFG_MAPPING, m_mapping.name());
    }
    
    /**
     * Reads the size settings and return a new <code>SizeModelDouble</code>.
     * @param config Read min and max bound from.
     * @return A new size model.
     * @throws InvalidSettingsException If the bounds could not be read.
     * @throws NullPointerException If the <i>config</i> is <code>null</code>.
     */
    public static SizeModelDouble load(final ConfigRO config) 
            throws InvalidSettingsException {
        double min = config.getDouble(CFG_MIN);
        double max = config.getDouble(CFG_MAX);
        double factor = config.getDouble(CFG_FACTOR, 2);
        String mapping = config.getString(CFG_MAPPING, Mapping.LINEAR.name());
        return new SizeModelDouble(min, max, factor, Mapping.valueOf(mapping));
    }
    
    /**
     * @return String representation containing SizeModel type and min/max
     *         boundaries.
     */
    @Override
    public String toString() {
        NumberFormat nf = NumberFormat.getInstance();
        return "DoubleRange SizeModel (factor=" + nf.format(m_factor) 
            + ", method=" + m_mapping.name() + ")";
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || !(obj instanceof SizeModelDouble)) {
            return false;
        }
        SizeModelDouble model = (SizeModelDouble) obj;
        return m_mapping.equals(model.m_mapping)
            && Double.compare(m_factor, model.m_factor) == 0
            && Double.compare(m_min, model.m_min) == 0
            && Double.compare(m_max, model.m_max) == 0;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        // copied from java.lang.Double
        long bits = Double.doubleToLongBits(m_factor);
        return (int)(bits ^ (bits >>> 32));
    }

}
