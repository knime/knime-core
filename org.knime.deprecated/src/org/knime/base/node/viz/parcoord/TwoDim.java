/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
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
 * -------------------------------------------------------------------
 * 
 * History
 *   Mar 16, 2005 (sp): created
 */
package org.knime.base.node.viz.parcoord;

import java.io.Serializable;

/**
 * 
 * @author Simona Pintilie, University of Konstanz
 */
public class TwoDim implements Serializable {

    /**
     * the minimum value.
     */
    private double m_min;
    /**
     * the maximum value.
     */
    private double m_max;
    /**
     * the string value.
     */
    private int m_stringReference;

    /**
     *@param min the min value
     *@param max the max value
     *@param s the reference to the string value or -1
     */
    public TwoDim(final double min, final double max, final int s) {
        m_min = min; 
        m_max = max;
        m_stringReference = s;
    }
    /**
     * 
     */
    public TwoDim() {
        this(0, 0, -1);
    }
    /**
     * 
     * @return min a double value
     */
    public double getMin() {
        return m_min;
    }
    /**
     * 
     * @return max a double value
     */
    public double getMax() {
        return m_max;
    }
    /**
     * 
     * @return reference an int value
     */
    public int getStringReference() {
        return m_stringReference;
    }
    /**
     * 
     * @param max a double value
     */
    public void setMax(final double max) {
        m_max = max;
    }
    /**
     * 
     * @param min a double value
     */
    public void setMin(final double min) {
        m_min = min;
    }
    /**
     * 
     * @param s an int value
     */
    public void setStringReference(final int s) {
        m_stringReference = s;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
       return "[" + m_min + ", " + m_max + "]";
    } 

}
