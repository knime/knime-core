/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
     * @return a list of association rules with the minimum confidence
     */
    public List<AssociationRule> getAssociationRules(final double confidence);
}
