/*
 * ------------------------------------------------------------------ *
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
 * --------------------------------------------------------------------- *
 * 
 * History
 *   Jan 17, 2007 (rs): created
 */
package org.knime.timeseries.node.MA;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DoubleCell;

/**
 * Implements Moving Average on a time series. 
 *
 * @author Rosaria Silipo
 * 
 */
public class MovingAverage {
    
    /** Default length of MA window. */
    public static final int DEFAULT_WINLENGTH = 21;
    
    /** enum constants for weight functions. */
    enum WEIGHT_FUNCTIONS { 
        /** no weight function. */
        SIMPLE, 
        /** weight function for exponential moving average. */
        EXPONENTIAL }
    
    /** interface to enumerate weight functions. */
    public interface WeightFunctions {
    
        /** no weight function. */
        public static final String SIMPLE = "simple";
        /** weight functionfor exponential moving average. */
        public static final String EXPONENTIAL = "exponential";
       
    }
    
    private int m_winLength = -1;
    private double [] m_originalValues;
    private double [] m_weights;
    private double m_expWeight = 0.0;
    
    private String m_weightFunction = null;
    
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
    public MovingAverage(final int winLength, final String weights) {
        
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
     public MovingAverage(final String weights) {
                       
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
     public MovingAverage(final int winLength) {
         
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
      public MovingAverage() {
            
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
          
          double previousAvg = m_avg;      
          boolean previousEnoughValues = m_enoughValues;
          DataCell dc = simpleMA(newValue);
          
          if (m_weightFunction.equals(WeightFunctions.EXPONENTIAL)) {
              if (previousEnoughValues) {
                  
                 dc = new DoubleCell(
                      newValue * m_expWeight + previousAvg * (1 - m_expWeight));
                 
              } else {
                  
                  return DataType.getMissingCell();
              }
          } 
          return dc;
      }
      
      private DataCell simpleMA(final double newValue) {
           if (!m_enoughValues) {
              
              m_avg += newValue * m_weights[m_indexNewestValue];
              m_originalValues[m_indexNewestValue] = newValue;
              m_indexNewestValue++; 
              
              m_initialValues++;
              m_enoughValues = (m_initialValues == m_winLength);
              
              if (!m_enoughValues) {
                 return DataType.getMissingCell();
              } else {
                  DoubleCell dc = new DoubleCell(m_avg); 
                  return dc;    
              }
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
              
              DoubleCell dc = new DoubleCell(m_avg); 
              return dc;    
          }
      }
      
      private void defineWeights(final String weights) {
    
         m_weightFunction = weights;

         for (int i = 0; i < m_winLength; i++) {
              m_weights[i] = 1.0 / (double)m_winLength;
         }
         
         if (m_weightFunction.equals(WeightFunctions.EXPONENTIAL)) {
             m_expWeight = 2.0 / (m_winLength + 1);
         } 
         
      }
    }
