/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright by KNIME.com, Zurich, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.com
 * email: contact@knime.com
 * ---------------------------------------------------------------------
 *
 * History
 *   Created on 01.08.2014 by koetter
 */
package org.knime.core.node.port.database.aggregation.function;

import org.knime.core.data.DoubleValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.port.database.aggregation.DBAggregationFunction;
import org.knime.core.node.port.database.aggregation.DBAggregationFunctionFactory;
import org.knime.core.node.port.database.aggregation.SimpleDBAggregationFunction;

/**
 *
 * @author Tobias Koetter, KNIME.com, Zurich, Switzerland
 * @since 2.11
 */
public final class StdDevSampDBAggregationFunction extends SimpleDBAggregationFunction {
    private static final String ID = "STDDEV_SAMP";

    /**Factory for the parent class.*/
    public static final class Factory implements DBAggregationFunctionFactory {
        private static final StdDevSampDBAggregationFunction INSTANCE = new StdDevSampDBAggregationFunction();

        /**
         * {@inheritDoc}
         */
        @Override
        public String getId() {
            return ID;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DBAggregationFunction createInstance() {
            return INSTANCE;
        }
    }

    private StdDevSampDBAggregationFunction() {
        super(ID, "The function computes the sample standard deviation, respectively, of the input values."
                + "The function evaluates all input rows matched by the query and is scaled by 1/(N-1)",
                DoubleCell.TYPE, DoubleValue.class);
    }
}
