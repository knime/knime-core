/*
 * ------------------------------------------------------------------
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
 *   29.09.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter.box;

import org.knime.base.node.viz.plotter.LabelPaintUtil;




/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class Box {
    
    private final int m_min;
    
    private final int m_lowerQuartile;
    
    private final int m_median;
    
    private final int m_upperQuartile;
    
    private final int m_max;
    
    private final int m_x;
    
    private final double[] m_domainValues;
    
    private static final int SENSITIVITY = 5;

    /** Constant for the rounding of the values shown in tooltip. */
    public static final double ROUNDING_FACTOR = 100;
    
    private int m_lowerWhisker;
    
    private int m_upperWhisker;
    
    private String m_columnName;
    
    /**
     * The graphical representation of a box in a box plot. 
     * @param x the central x position
     * @param min the min value.
     * @param lowerQuartile the lower quartile
     * @param median the median
     * @param upperQuartile the upper quartile
     * @param max the maximum value
     * @param domainValues the original values (not mapped).
     */
    public Box(final int x, final int min, final int lowerQuartile, 
            final int median,
            final int upperQuartile, final int max, 
            final double[] domainValues) {
        m_x = x;
        m_min = min;
        m_lowerQuartile = lowerQuartile;
        m_median = median;
        m_upperQuartile = upperQuartile;
        m_max = max;
        m_domainValues = domainValues;
    }
    
    /**
     * 
     * @return the central x pos
     */
    public int getX() {
        return m_x;
    }
    
    /**
     * 
     * @return mapped minimum value
     */
    public int getMin() {
        return m_min;
    }

    
    /**
     * 
     * @return mapped lower quartile
     */
    public int getLowerQuartile() {
        return m_lowerQuartile;
    }
    
    
    /**
     * 
     * @return mapped median
     */
    public int getMedian() {
        return m_median;
    }

    /**
     * 
     * @return mapped upper quartile
     */
    public int getUpperQuartile() {
        return m_upperQuartile;
    }
    

    /**
     * 
     * @return mapped maximum
     */
    public int getMax() {
        return m_max;
    }
    
    /**
     * 
     * @return domain values (not mapped)
     */
    public double[] getDomainValues() {
        return m_domainValues;
    }
    
    /**
     * 
     * @param columnName name of the referring column.
     */
    public void setColumnName(final String columnName) {
        m_columnName = columnName;
    }
    
    /**
     * 
     * @return name of the referring column.
     */
    public String getColumnName() {
        return m_columnName;
    }
    
    /**
     * 
     * @param lowerWhisker the lower whisker
     */
    public void setLowerWhiskers(final int lowerWhisker) {
        m_lowerWhisker = lowerWhisker;
    }
    
    /**
     * 
     * @param upperWhisker the upper whisker
     */
    public void setUpperWhiskers(final int upperWhisker) {
        m_upperWhisker = upperWhisker;
    }
    
    /**
     * 
     * @return lower whisker
     */
    public  int getLowerWhisker() {
        return m_lowerWhisker;
    }
    
    
    /**
     * 
     * @return upper whisker
     */
    public int getUpperWhisker() {
        return m_upperWhisker;
    }
    
    
    
    /**
     * Returns the domain values as a string for all values near y.
     * @param y the mouse point
     * @return the tooltip with information about the box.
     */
    public String getToolTip(final int y) {
        StringBuffer buffer = new StringBuffer();
        if (y < m_min + SENSITIVITY && y > m_min - SENSITIVITY) {
            buffer.append("min:" 
                    + LabelPaintUtil.getDoubleAsString(
                            m_domainValues[BoxPlotNodeModel.MIN], 
                            ROUNDING_FACTOR) + " ");
        }
        if (y < m_lowerWhisker + SENSITIVITY 
                && y > m_lowerWhisker - SENSITIVITY) {
            buffer.append("smallest:" 
                    + LabelPaintUtil.getDoubleAsString(
                    m_domainValues[BoxPlotNodeModel.LOWER_WHISKER],
                    ROUNDING_FACTOR) + " ");
        }  
        if (y < m_lowerQuartile + SENSITIVITY 
                && y > m_lowerQuartile - SENSITIVITY) {
            buffer.append("q1:" 
                     + LabelPaintUtil.getDoubleAsString(
                    m_domainValues[BoxPlotNodeModel.LOWER_QUARTILE],
                    ROUNDING_FACTOR) + " ");
        }
        if (y < m_median + SENSITIVITY && y > m_median - SENSITIVITY) {
            buffer.append("median:" 
                    + LabelPaintUtil.getDoubleAsString(
                            m_domainValues[BoxPlotNodeModel.MEDIAN],
                            ROUNDING_FACTOR) + " ");
        }
        if (y < m_upperQuartile + SENSITIVITY 
                && y > m_upperQuartile - SENSITIVITY) {
            buffer.append("q3:" 
                    + LabelPaintUtil.getDoubleAsString(
                    m_domainValues[BoxPlotNodeModel.UPPER_QUARTILE],
                    ROUNDING_FACTOR) + " ");
        }
        if (y < m_upperWhisker + SENSITIVITY 
                && y > m_upperWhisker - SENSITIVITY) {
            buffer.append("largest:" 
                    + LabelPaintUtil.getDoubleAsString(
                            m_domainValues[BoxPlotNodeModel.UPPER_WHISKER],
                            ROUNDING_FACTOR) + " ");
        }        
        if (y < m_max + SENSITIVITY && y > m_max - SENSITIVITY) {
            buffer.append("max:" 
                    + LabelPaintUtil.getDoubleAsString(
                            m_domainValues[BoxPlotNodeModel.MAX],
                            ROUNDING_FACTOR) + " ");
        }
        return buffer.toString();
    }

}
