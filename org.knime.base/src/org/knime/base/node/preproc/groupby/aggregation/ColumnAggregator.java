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
 *    08.08.2008 (Tobias Koetter): created
 */

package org.knime.base.node.preproc.groupby.aggregation;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.config.Config;

import java.util.LinkedList;
import java.util.List;


/**
 * Class that defines the {@link AggregationMethod} for the aggregation column.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class ColumnAggregator {
    private static final String CNFG_AGGR_COL_SECTION = "aggregationColumn";
    private static final String CNFG_COL_NAMES = "columnNames";
    private static final String CNFG_COL_TYPES = "columnTypes";
    private static final String CNFG_AGGR_METHODS = "aggregationMethod";

    private final DataColumnSpec m_origColSpec;
    private final AggregationMethod m_method;
    private AggregationOperator m_operator;

    /**Constructor for class ColumnAggregator.
     * @param origColSpec the {@link DataColumnSpec} of the original column
     * @param method the {@link AggregationMethod} to use for the given column
     *
     */
    public ColumnAggregator(final DataColumnSpec origColSpec,
            final AggregationMethod method) {
        if (origColSpec == null) {
            throw new NullPointerException("colSpec must not be null");
        }
        if (method == null) {
            throw new NullPointerException("method must not be null");
        }
        if (!method.isCompatible(origColSpec)) {
            throw new IllegalArgumentException("Aggregation method '"
                    + method.getLabel() + "' not valid for column '"
                    + origColSpec.getName() + "'");
        }
        m_origColSpec = origColSpec;
        m_method = method;
    }

    /**
     * @return the colName of the original column
     */
    public String getColName() {
        return m_origColSpec.getName();
    }

    /**
     * @return the {@link DataColumnSpec} of the original column
     */
    public DataColumnSpec getColSpec() {
        return m_origColSpec;
    }

    /**
     * @return the {@link DataType} of the original column
     */
    public DataType getDataType() {
        return m_origColSpec.getType();
    }

    /**
     * @return the {@link AggregationMethod} to use
     */
    public AggregationMethod getMethod() {
        return m_method;
    }

    /**
     * @param maxUniqueValues the maximum number of unique values
     * @return the operator for this column
     */
    public AggregationOperator getOperator(final int maxUniqueValues) {
        if (m_operator == null) {
            m_operator = m_method.getOperator(m_origColSpec, maxUniqueValues);
        }
        return m_operator;
    }



    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getColName() + "->" + getMethod();
    }

    /**
     * Creates a {@link List} with all {@link ColumnAggregator}s that were
     * stored in the settings.
     *
     * @param settings the settings object to read from
     * @return {@link List} with the {@link ColumnAggregator}s
     * @throws InvalidSettingsException if the settings are invalid
     */
    public static List<ColumnAggregator> loadColumnAggregators(
            final NodeSettingsRO settings)
            throws InvalidSettingsException {
        final Config cnfg = settings.getConfig(CNFG_AGGR_COL_SECTION);
        final String[] colNames = cnfg.getStringArray(CNFG_COL_NAMES);
        final DataType[] colTypes = cnfg.getDataTypeArray(CNFG_COL_TYPES);
        final String[] aggrMethods = cnfg.getStringArray(CNFG_AGGR_METHODS);
        final List<ColumnAggregator> colAggrList =
            new LinkedList<ColumnAggregator>();
        if (aggrMethods.length != colNames.length) {
            throw new InvalidSettingsException("Column name array and "
                    + "aggregation method array should be of equal size");
        }
        for (int i = 0, length = aggrMethods.length; i < length; i++) {
            final AggregationMethod method =
                AggregationMethod.getMethod4Label(aggrMethods[i]);
            final DataColumnSpec spec = new DataColumnSpecCreator(
                    colNames[i], colTypes[i]).createSpec();
            colAggrList.add(new ColumnAggregator(spec, method));
        }
        return colAggrList;
    }

    /**
     * @param settings the settings object to write to
     * @param cols the {@link ColumnAggregator} objects to save
     */
    public static void saveColumnAggregators(final NodeSettingsWO settings,
            final List<ColumnAggregator> cols) {
        if (settings == null) {
            throw new NullPointerException("settings must not be null");
        }
        if (cols == null) {
            throw new NullPointerException("cols must not be null");
        }
        final String[] colNames = new String[cols.size()];
        final String[] aggrMethods = new String[cols.size()];
        final DataType[] types = new DataType[cols.size()];
        for (int i = 0, length = cols.size(); i < length; i++) {
            final ColumnAggregator aggrCol = cols.get(i);
            colNames[i] = aggrCol.getColName();
            aggrMethods[i] = aggrCol.getMethod().getLabel();
            types[i] = aggrCol.getDataType();
        }
        final Config cnfg = settings.addConfig(CNFG_AGGR_COL_SECTION);
        cnfg.addStringArray(CNFG_COL_NAMES, colNames);
        cnfg.addDataTypeArray(CNFG_COL_TYPES, types);
        cnfg.addStringArray(CNFG_AGGR_METHODS, aggrMethods);
    }
}
