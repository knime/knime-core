/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
