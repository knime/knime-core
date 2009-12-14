/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
