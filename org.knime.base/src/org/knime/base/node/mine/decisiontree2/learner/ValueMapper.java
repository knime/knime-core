/*
 * ------------------------------------------------------------------
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   31.07.2007 (sieb): created
 */
package org.knime.base.node.mine.decisiontree2.learner;

import java.util.HashMap;

/**
 * A mapper mapping the objects of the defined class <code>S</code> to integer
 * indices and vice versa.
 * 
 * @author Christoph Sieb, University of Konstanz
 * @param <S> the type to map to an integer
 */
public class ValueMapper<S> {

    /**
     * Maps the class values to an integer for faster counting of frequencies.
     */
    private HashMap<S, Integer> m_valueToIndexMap;

    /**
     * Maps the integer index to the class values.
     */
    private HashMap<Integer, S> m_indexToValueMap;

    /**
     * The indices are used in a consecutive manner. I.e. the first integer
     * mapping is 0 the second 1 and so on.
     */
    private int m_lastUsedIntegerIndex;

    /**
     * Constructs an empty mapper. The indices are used in a consecutive manner.
     * I.e. the first integer mapping is 0 the second 1 and so on.
     */
    public ValueMapper() {
        m_valueToIndexMap = new HashMap<S, Integer>();
        m_indexToValueMap = new HashMap<Integer, S>();
        m_lastUsedIntegerIndex = -1;
    }

    /**
     * Returns the mapped object for the corresponding index.
     * 
     * @param index the index for which to return the corresponding mapped
     *            object
     * @return the mapped object for the corresponding index, <code>null</code>
     *         if there exists no mapping for the given index
     */
    public S getMappedObject(final int index) {
        return m_indexToValueMap.get(index);
    }

    /**
     * Returns all mapped objects in the order they were inserted.
     * 
     * @return the mapped objects
     */
    public S[] getMappedObjectsInMappingOrder() {
        @SuppressWarnings("unchecked")
        S[] objects = (S[])new Object[getNumMappings()];
        for (int i = 0; i < objects.length; i++) {
            objects[i] = m_indexToValueMap.get(i);
        }

        return objects;
    }

    /**
     * Returns the integer index for the corresponding mapped object.
     * 
     * @param mappedObject the mapped object for which to return the
     *            corresponding integer index
     * @return the integer index for the corresponding mapped object,
     *         <code>-1</code> if there exists no mapping for the given mapped
     *         object
     */
    public int getIndex(final S mappedObject) {
        Integer index = m_valueToIndexMap.get(mappedObject);
        if (index == null) {
            return -1;
        }
        return index.intValue();
    }

    /**
     * Returns the integer index for the corresponding mapped object. If there
     * exists no mapping for the given mapped object a new mapping is created
     * and the new integer index is returned.
     * 
     * @param mappedObject the mapped object for which to return the
     *            corresponding integer index
     * @return the integer index for the corresponding mapped object, a new
     *         integer index if the mapped object mapping did not existed. The
     *         new mapping index is the next available integer according to the
     *         previous mapping
     */
    public int getIndexMayBeAdded(final S mappedObject) {
        Integer index = m_valueToIndexMap.get(mappedObject);
        if (index == null) {
            m_lastUsedIntegerIndex++;
            m_indexToValueMap.put(m_lastUsedIntegerIndex, mappedObject);
            m_valueToIndexMap.put(mappedObject, m_lastUsedIntegerIndex);
            return m_lastUsedIntegerIndex;
        }
        return index.intValue();
    }

    /**
     * Returns the number of mappings.
     * 
     * @return the number of mappings
     */
    public int getNumMappings() {
        return m_indexToValueMap.size();
    }
}
