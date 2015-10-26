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
 *   Created on 27.08.2014 by koetter
 */
package org.knime.core.node.port.database.aggregation.function;

import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.port.database.aggregation.DBAggregationFunction;
import org.knime.core.node.port.database.aggregation.DBAggregationFunctionFactory;
import org.knime.core.node.port.database.aggregation.function.column.AbstractColumnDBAggregationFunction;

/**
 * Aggregation function.
 * @author Tobias Koetter, KNIME.com, Zurich, Switzerland
 * @since 2.11
 */
public class RegrAvgXDBAggregationFunction extends AbstractColumnDBAggregationFunction {

    private static final String ID = "REGR_AVGX";
    /**Factory for parent class.*/
    public static final class Factory implements DBAggregationFunctionFactory {
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
            return new RegrAvgXDBAggregationFunction();
        }
    }

    /**
     * Constructor.
     */
    public RegrAvgXDBAggregationFunction() {
        super("X column: ", null, DoubleValue.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataType getType(final DataType originalType) {
        return DoubleCell.TYPE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLabel() {
        return getId();
    }

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
    public boolean isCompatible(final DataType type) {
        return type.isCompatible(DoubleValue.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "The function regr_avgx(Y, X) returns the average of the independent variable (sum(X)/N).";
    }
}
