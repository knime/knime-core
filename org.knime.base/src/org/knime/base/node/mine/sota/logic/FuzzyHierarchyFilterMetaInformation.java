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
 *   Jan 19, 2006 (Kilian Thiel): created
 */
package org.knime.base.node.mine.sota.logic;

import java.util.Hashtable;

/**
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public class FuzzyHierarchyFilterMetaInformation {
    private int m_level;

    private int m_size;

    private Hashtable<Integer, Integer> m_indexMinHash;

    private Hashtable<Integer, Integer> m_indexMaxHash;

    /**
     * Creates new instance of FuzzyHierarchyFilterMetaInformation.
     */
    public FuzzyHierarchyFilterMetaInformation() {
        m_level = 0;
        m_size = 0;
        m_indexMinHash = new Hashtable<Integer, Integer>();
        m_indexMaxHash = new Hashtable<Integer, Integer>();
    }

    /**
     * @return the level
     */
    public int getLevel() {
        return m_level;
    }

    /**
     * @param level the level to set
     */
    public void setLevel(final int level) {
        this.m_level = level;
    }

    /**
     * @return the size
     */
    public int getSize() {
        return m_size;
    }

    /**
     * @param size the size to set
     */
    public void setSize(final int size) {
        this.m_size = size;
    }

    /**
     * Adds given value i to size.
     * 
     * @param i value to add to size
     */
    public void addToSize(final int i) {
        m_size += i;
    }

    /**
     * Returns the min value at given index.
     * 
     * @param index index of min value to return
     * @return the min value at given index
     */
    public int getMinAtIndex(final int index) {
        return m_indexMinHash.get(index);
    }

    /**
     * Returns the max value at given index.
     * 
     * @param index index of max value to return
     * @return the max value at given index
     */
    public int getMaxAtIndex(final int index) {
        return m_indexMaxHash.get(index);
    }

    /**
     * Sets given min value at given index.
     * 
     * @param min value to set
     * @param index index to set value at
     */
    public void setMinValueAtIndex(final int min, final int index) {
        m_indexMinHash.put(index, min);
    }

    /**
     * Sets given max value at given index.
     * 
     * @param max value to set
     * @param index index to set value at
     */
    public void setMaxValueAtIndex(final int max, final int index) {
        m_indexMaxHash.put(index, max);
    }
}
