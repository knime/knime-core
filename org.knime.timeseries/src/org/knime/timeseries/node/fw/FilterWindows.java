/*
 * ------------------------------------------------------------------ *
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
 * --------------------------------------------------------------------- *
 * 
 * History
 *   Jan 17, 2007 (rs): created
 */
package org.knime.timeseries.node.fw;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DoubleCell;

/**
 * Implements Moving Average on a time series. 
 *
 * @author Rosaria Silipo
 * 
 */
public class FilterWindows {
    
    /** Default length of MA window. */
    public static final int DEFAULT_WINLENGTH = 21;
    
    /** enum constants for weight functions. */
    enum WEIGHT_FUNCTIONS { 
        /** no weight function. */
        NONE, 
        /** gaussian weight function. */
        GAUSSIAN, 
        /** Hamming weight function. */
        HAMMING }
    
    /** interface to enumerate weight functions. */
    public interface WeightFunctions {
    
        /** no weight function. */
        public static final String NONE = "none";
        /** Gaussian weight function. */
        public static final String GAUSSIAN = "Gaussian";
        /** Hamming weight function. */
        public static final String HAMMING = "Hamming";
       
    }
    
    private int m_winLength = -1;
    private double [] m_originalValues;
    private double [] m_weights;
    
    private int m_indexOldestValue = 0;
    private int m_indexNewestValue = 0;
    private double m_avg = 0.0;
    private int m_initialValues = 0;
    private boolean m_enoughValues = false;
    
    /**
     * Constructor.
     * Builds MA array with specified number of items.
     * @param winLength MA window length
     * @param weights weight function to overlap to MA window
     */
    public FilterWindows(final int winLength, final String weights) {
        
        m_winLength = winLength;
        
        m_originalValues = new double [m_winLength];
        m_weights = new double [m_winLength];
        
        defineWeights(weights);
    }
        
    /**
     * Constructor.
     * Builds MA array with specified number of items.
     * Window Length is 21 items (default)
     * @param weights weight function to overlap to MA window
     */
     public FilterWindows(final String weights) {
                       
        m_originalValues = new double [m_winLength];
        m_weights = new double [m_winLength];
            
        defineWeights(weights);
     }
       
     /**
      * Constructor.
      * Builds MA array with specified number of items.
      * @param winLength MA window length
      * No weight function
      */
     public FilterWindows(final int winLength) {
         
         m_winLength = winLength;
         
         m_originalValues = new double [m_winLength];
         m_weights = new double [m_winLength];
         
         defineWeights("none");
         
     }
         
     /**
      * Constructor.
      * Builds MA array with specified number of items.
      * Window Length is 21 items (default)
      * No weight function
      */
      public FilterWindows() {
            
          m_originalValues = new double [m_winLength];
          m_weights = new double [m_winLength];
            
          defineWeights("none");
       }
      
      /** Implements moving average algorithm 
       * using the weights defined by the weight function.
       * 
       * @param newValue new value from time series
       * @return m_avg current moving average value
       */
      public DataCell maValue(final double newValue) {
              
           if (!m_enoughValues) {
              
              m_avg += newValue * m_weights[m_indexNewestValue];
              m_originalValues[m_indexNewestValue] = newValue;
              m_indexNewestValue++; 
              
              m_initialValues++;
              m_enoughValues = (m_initialValues == m_winLength);
              
              return DataType.getMissingCell();
              
          } else {
              m_avg = m_avg  
              - (m_originalValues[m_indexOldestValue] 
                                  * m_weights[m_indexOldestValue])
              + (newValue * m_weights[m_indexOldestValue]);
              
              m_indexNewestValue = m_indexOldestValue;
              m_originalValues[m_indexNewestValue] = newValue;
              m_indexOldestValue++;
              if (m_indexOldestValue >= m_winLength) {
                  m_indexOldestValue = 0;
              }
              
              DataCell dc = new DoubleCell(m_avg); 
              return dc;    
          }
      }
      
      private void defineWeights(final String weights) {
          
          if (weights.equals(WeightFunctions.NONE)) {      
              for (int i = 0; i < m_winLength; i++) {
                 m_weights[i] = 1.0 / (double)m_winLength;
              }
          } else if (weights.equals(WeightFunctions.GAUSSIAN)) { 
              // Gaussian built on central value in MA window
              // and sigma as 1/4th of window length
              double mu = (m_winLength - 1) / 2.0;
              double sigma = (m_winLength - 1) / 4.0;
              double a = 1.0 / (Math.sqrt(2 * Math.PI) * sigma);
           
              for (int i = 0; i < m_winLength; i++) {
                  m_weights[i] = 
                      a * Math.exp(-((i - mu) * (i - mu)) / (2 * sigma * sigma));
              }
          } else if (weights.equals(WeightFunctions.HAMMING)) {
              for (int i = 0; i < m_winLength; i++) {
                  m_weights[i] = 0.54 
                         - 0.46 * Math.cos(2 * Math.PI * i / (m_winLength - 1));
              }
          } else {
              for (int i = 0; i < m_winLength; i++) {
                  m_weights[i] = 1.0 / (double)m_winLength;
              }
          }
      }
    }
