/* This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 */
package org.knime.base.node.mine.cluster.kmeans;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class LinearNorm {
    
    
    private class Interval {
        private final double m_original;
        private final double m_norm;
        
        /**
         * 
         * @param orig original value
         * @param norm normed value
         */
        public Interval(final double orig, final double norm) {
            m_original = orig;
            m_norm = norm;
        }
        
        /**
         * 
         * @return the original value
         */
        public double getOriginalValue() {
            return m_original;
        }
        /**
         * 
         * @return the normalized value
         */
        public double getNormValue() {
            return m_norm;
        }
        
        @Override
        public String toString() {
            return "orig=" + m_original + " norm=" + m_norm;
        }
    }

    private final String m_fieldName;
    
    private final List<Interval>m_intervals;
    
    /**
     * 
     * @param fieldName the name of the field
     */
    public LinearNorm(final String fieldName) {
        m_fieldName = fieldName;
        m_intervals = new ArrayList<Interval>();
    }

    /**
     * Represents a LinearNorm PMML element. 
     * Adds an pair of values: original value and normed value.
     *  
     * @param origValue the original value
     * @param normValue the mapped norm value
     */
    public void addInterval(final double origValue, final double normValue) {
        m_intervals.add(new Interval(origValue, normValue));
    }
    
    /**
     * 
     * @return the name of the field
     */
    public String getName() {
        return m_fieldName;
    }
    
    public double unnormalize(final double value) {
        for (int i = 0; i < m_intervals.size() - 1; i++) {
            Interval lower = m_intervals.get(i);
            Interval upper = m_intervals.get(i + 1);
            if (lower.m_norm <= value && value <= upper.m_norm) {
                double y = lower.m_original + ((value - lower.m_norm)
                        *((upper.m_original - lower.m_original)
                                /(upper.m_norm - lower.m_norm)));
                return y;
                /*
                return value * (upper.m_original - lower.m_original
                    + lower.m_original);
                    */
            }
        }
        throw new IllegalArgumentException(
                "Value " + value 
                + "is out of reported linear normalization!");
    }
    
}
