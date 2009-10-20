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
