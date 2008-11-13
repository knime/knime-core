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
 *   12.12.2005 (dill): created
 */
package org.knime.base.node.mine.subgroupminer.apriori;

import java.util.List;

import org.knime.base.node.mine.subgroupminer.freqitemset.AssociationRule;
import org.knime.base.node.mine.subgroupminer.freqitemset.FrequentItemSet;
import org.knime.core.data.vector.bitvector.BitVectorValue;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;

/**
 * This is an interface to hide the different implementations of the apriori
 * algorithm to the Nodes. If a class implements this interface and the user
 * should be able to select this type of algorithm, you should also change the
 * {@link AprioriAlgorithmFactory}.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public interface AprioriAlgorithm {
    /**
     * This is the method to start with when mining for frequent itemsets.
     * 
     * @param transactions a list of BitSets representing the bitvectors, thus,
     *            corresponding to the whole database
     * @param minSupport the minimum support as an absolute value
     * @param maxDepth the maximal length of an itemset
     * @param type the desired type of the frequent itemsets
     * @param exec the execution monitor
     * @throws CanceledExecutionException if the execution was cancelled
     */
    public void findFrequentItemSets(List<BitVectorValue> transactions, 
            double minSupport, int maxDepth, FrequentItemSet.Type type, 
            ExecutionMonitor exec)
            throws CanceledExecutionException;

    /**
     * Returns the found frequent itemsets according to their type, which can
     * either be FREE, CLOSED or MAXIMAL.
     * 
     * @param type the desired type, either free, closed or maximal
     * @return a list of the found frequent itemsets of the referring type
     */
    public List<FrequentItemSet> getFrequentItemSets(FrequentItemSet.Type type);

    /**
     * Returns the association rules generated from the found frequent itemsets
     * with the passed minimal confidence.
     * 
     * @param confidence the desired minimal confidence of the rules
     * @return a list of associaiton rules with the minimum confidence
     */
    public List<AssociationRule> getAssociationRules(final double confidence);
}
