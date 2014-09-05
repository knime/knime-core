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


import org.knime.core.data.DataType;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.port.database.StatementManipulator;
import org.knime.core.node.port.database.aggregation.DBAggregationFunction;
import org.knime.core.node.port.database.aggregation.function.concatenate.AbstractConcatDBAggregationFunction;
import org.knime.core.node.port.database.aggregation.function.concatenate.ConcatDBAggregationFuntionSettings;

/**
 *
 * @author Tobias Koetter, KNIME.com, Zurich, Switzerland
 * @since 2.11
 */
public final class GroupConcatDBAggregationFunction extends AbstractConcatDBAggregationFunction {

    private final String m_name;
    private final String m_function;

    /**
     * Constructor.
     */
    public GroupConcatDBAggregationFunction() {
        this("GROUP_CONCAT");
    }
    /**
     * @param id the id, name and function at the same time
     */
    public GroupConcatDBAggregationFunction(final String id) {
        this(id, id);
    }

    /**
     * @param name the name to display
     * @param function the function to use in the sql fragment
     */
    public GroupConcatDBAggregationFunction(final String name, final String function) {
        super(new ConcatDBAggregationFuntionSettings(","));
        m_name = name;
        m_function = function;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getColumnName() {
        return getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return m_name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataType getType(final DataType originalType) {
        return StringCell.TYPE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "An aggregate function that returns a single string representing the argument value concatenated "
                + "together for each row of the result set. The value separator can be specified in the function "
                + "settings dialog.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCompatible(final DataType type) {
        return type.isCompatible(StringValue.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSQLFragment(final StatementManipulator manipulator, final String tableName, final String colName) {
        return m_function + "(" + tableName + "." + manipulator.quoteIdentifier(colName)
                + ", " + quoteSeparator(getSettings().getSeparator()) + ")";
    }

    /**
     * @param separator the value separator
     * @return the quoted separator
     */
    protected String quoteSeparator(final String separator) {
        return "'" + separator + "'";
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public DBAggregationFunction createInstance() {
        //here we have to pass the original id to have a valid clone
        return new GroupConcatDBAggregationFunction(getId());
    }
}
