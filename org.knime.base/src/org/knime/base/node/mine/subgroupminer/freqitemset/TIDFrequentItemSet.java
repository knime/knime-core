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
 *   29.05.2006 (Fabian Dill): created
 */
package org.knime.base.node.mine.subgroupminer.freqitemset;

import java.util.List;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class TIDFrequentItemSet extends FrequentItemSet {
    private List<Integer> m_tids;

    /**
     * 
     * @param id the id of this itemset
     * @param itemIds the item ids
     * @param support the support (abs)
     * @param tids the transaction ids
     */
    public TIDFrequentItemSet(final String id, 
            final List<Integer> itemIds, final double support,
            final List<Integer> tids) {
        super(id, itemIds, support);
        m_tids = tids;
    }

    /**
     * Returns the transaction ids.
     * 
     * @return the transaction ids
     */
    public List<Integer> getTIDs() {
        return m_tids;
    }

    /**
     * Sets the transaction ids of this itemset.
     * 
     * @param tids the transaction ids of this itemset
     */
    public void setTIDs(final List<Integer> tids) {
        m_tids = tids;
    }
}
