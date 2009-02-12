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
 * ---------------------------------------------------------------------
 * 
 * History
 *   29.03.2007 (thiel): created
 */
package org.knime.base.node.mine.sota.logic;

import java.util.Hashtable;

/**
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public class CellClassCounter {
    
    private Hashtable<String, Integer> m_classFrequency;
    
    private String m_mostFrequentClass;
    private int m_freqOfMostFrequentClass;
    
    /**
     * Creates new instance of <code>CellClassCounter</code>.
     */
    public CellClassCounter() {
        m_classFrequency = new Hashtable<String, Integer>();
        m_mostFrequentClass = null;
        m_freqOfMostFrequentClass = 0;
    }

    /**
     * Adds the given class to the class frequency counter.
     * 
     * @param cellClass The class to add.
     */
    public void addClass(final String cellClass) {
        if (cellClass == null) {
            return;
        }
        
        Integer freq = m_classFrequency.get(cellClass);
        if (freq == null) {
            freq = 1;
        } else {
            freq++;
        }
        m_classFrequency.put(cellClass, freq);
        
        if (m_mostFrequentClass == null) {
            m_mostFrequentClass = cellClass;
            m_freqOfMostFrequentClass = 1;
        } else {
            if (freq > m_freqOfMostFrequentClass) {
                m_freqOfMostFrequentClass = freq;
                m_mostFrequentClass = cellClass;
            }
        }
    }
    
    /**
     * @return the class which appears most frequently.
     */
    public String getMostFrequentClass() {        
        return m_mostFrequentClass;
    }
}
