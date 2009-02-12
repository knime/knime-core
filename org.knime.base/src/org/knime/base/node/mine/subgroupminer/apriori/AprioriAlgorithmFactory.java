/* 
 * -------------------------------------------------------------------
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
 *   13.12.2005 (dill): created
 */
package org.knime.base.node.mine.subgroupminer.apriori;

import java.util.ArrayList;
import java.util.List;

/**
 * To hide the different implementations of the apriori algorithm to the
 * NodeModel, the NodeDialog simply displays the registered
 * AlgorithmDataStructure's and the NodeModel passes it to this factory.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public final class AprioriAlgorithmFactory {
    /**
     * Register here possible implementations of the apriori algorithm to be
     * provided by the subgroup miner node (SubgroupMinerModel).
     * 
     * @author Fabian Dill, University of Konstanz
     */
    public enum AlgorithmDataStructure {
        /** A prefix tree where the nodes are realized as arrays. * */
        ARRAY,
        /* LIST */
        /** The TIDList stores the ids of the transactions. * */
        TIDList;

        /**
         * Returns the values of this enum as a list of strings.
         * 
         * @return the values of this enum as a list of strings
         */
        public static List<String> asStringList() {
            Enum[] values = values();
            List<String> list = new ArrayList<String>();
            for (int i = 0; i < values.length; i++) {
                list.add(values[i].name());
            }
            return list;
        }
    }

    private AprioriAlgorithmFactory() {
        // just to prohibit instantiation
    }

    /**
     * Returns an instance of the AprioriAlgorithm interface according to the
     * passed type.
     * 
     * @param type the desired algorithm implementation
     * @param bitSetLength the bitset length of the transactions, i.e. the
     *            number of items
     *            @param dbsize number of transactions
     * @return an instance of the AprioriAlgorithm
     */
    public static AprioriAlgorithm getAprioriAlgorithm(
            final AlgorithmDataStructure type, final int bitSetLength, 
            final int dbsize) {
        if (type.equals(AlgorithmDataStructure.ARRAY)) {
            return new ArrayApriori(bitSetLength, dbsize);
            /*
             * }else if(type.equals(SubgroupMinerConfig.DataStruture.LIST)){
             * return new ListApriori();
             */
        } else if (type.equals(AlgorithmDataStructure.TIDList)) {
            return new TIDApriori();
        } else {
            throw new RuntimeException("Type not supported: " + type);
        }
    }
}
