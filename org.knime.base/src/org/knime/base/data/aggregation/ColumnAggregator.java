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
 */

package org.knime.base.data.aggregation;

import java.util.LinkedList;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.config.Config;


/**
 * Class that defines the {@link AggregationMethod} for a column.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class ColumnAggregator extends AggregationMethodDecorator {

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(ColumnAggregator.class);

    private static final String CNFG_AGGR_COL_SECTION = "aggregationColumn";
    private static final String CNFG_COL_NAMES = "columnNames";
    private static final String CNFG_COL_TYPES = "columnTypes";

    private final DataColumnSpec m_origColSpec;
    private AggregationOperator m_operator;


    /**Constructor for class ColumnAggregator.
     * @param origColSpec the {@link DataColumnSpec} of the original column
     * @param method the {@link AggregationMethod} to use for the given column
     * @since 2.6
     */
    public ColumnAggregator(final DataColumnSpec origColSpec,
            final AggregationMethod method) {
        this(origColSpec, method, method.inclMissingCells());
    }

    /**Constructor for class ColumnAggregator.
     * @param origColSpec the {@link DataColumnSpec} of the original column
     * @param method the {@link AggregationMethod} to use for the given column
     * @param inclMissingCells <code>true</code> if missing cells should be
     * considered during aggregation
     */
    public ColumnAggregator(final DataColumnSpec origColSpec,
            final AggregationMethod method, final boolean inclMissingCells) {
        super(method, inclMissingCells);
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
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ColumnAggregator clone() {
        return new ColumnAggregator(m_origColSpec, getMethodTemplate(),
                inclMissingCells());
    }

    /**
     * @return the colName of the original column
     */
    public String getOriginalColName() {
        return m_origColSpec.getName();
    }

    /**
     * @return the {@link DataColumnSpec} of the original column
     */
    public DataColumnSpec getOriginalColSpec() {
        return m_origColSpec;
    }

    /**
     * @return the {@link DataType} of the original column
     */
    public DataType getOriginalDataType() {
        return m_origColSpec.getType();
    }

    /**
     * @param type the type to check for
     * @return <code>true</code> if the column is compatible to the given type
     */
    public boolean isCompatible(final Class<? extends DataValue> type) {
        final DataType originalDataType = getOriginalDataType();
        final DataType dataType;
        if (DataType.getMissingCell().getType().equals(originalDataType)) {
            //this is the MissingType use the DataCell type instead since
            //the missing type is compatible to all other types which is weird
            dataType = DataType.getType(DataCell.class);
        } else {
            dataType = originalDataType;
        }
        return dataType.isCompatible(type);
    }

    /**
     * Creates only ones an {@link AggregationOperator} that is always
     * returned by this method.
     * In order to remove the created {@link AggregationOperator} call the
     * {@link #reset()} method.
     *
     * @param globalSettings the maximum number of unique values
     * @return the operator for this column
     */
    public AggregationOperator getOperator(
            final GlobalSettings globalSettings) {
        if (m_operator == null) {
            final OperatorColumnSettings opColSettings =
                new OperatorColumnSettings(inclMissingCells(), m_origColSpec);
            m_operator = getMethodTemplate().createOperator(
                    globalSettings, opColSettings);
        }
        return m_operator;
    }

    /**
     * Removes any cached operators.
     */
    public void reset() {
        m_operator = null;
    }

    /**
     * @return the column name that should be used for the operator
     */
    public String getOperatorColName() {
        final String operatorLabel = getMethodTemplate().getColumnLabel();
        if (supportsMissingValueOption() && !inclMissingCells()) {
            //add the star to indicate that missing values are excluded
            //but only if the method supports the changing of this option
            //by the user to be compatible to old methods
            return operatorLabel + "*";
        }
        return operatorLabel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getOriginalColName() + "->" + super.toString();
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
        return loadColumnAggregators(settings, null);
    }

    /**
     * Creates a {@link List} with all {@link ColumnAggregator}s that were
     * stored in the settings.
     *
     * @param settings the settings object to read from
     * @param spec {@link DataTableSpec} of the input table if available
     * @return {@link List} with the {@link ColumnAggregator}s
     * @throws InvalidSettingsException if the settings are invalid
     * @since 2.7
     */
    public static List<ColumnAggregator> loadColumnAggregators(
            final NodeSettingsRO settings, final DataTableSpec spec)
            throws InvalidSettingsException {
        final Config cnfg = settings.getConfig(CNFG_AGGR_COL_SECTION);
        final String[] colNames = cnfg.getStringArray(CNFG_COL_NAMES);
        final DataType[] colTypes = cnfg.getDataTypeArray(CNFG_COL_TYPES);
        final String[] aggrMethods = cnfg.getStringArray(CNFG_AGGR_METHODS);
        boolean[] inclMissingVals = null;
        try {
            inclMissingVals = cnfg.getBooleanArray(CNFG_INCL_MISSING_VALS);
        } catch (final InvalidSettingsException e) {
            // be compatible to version 2.3 and earlier
        }
        final List<ColumnAggregator> colAggrList =
            new LinkedList<ColumnAggregator>();
        if (aggrMethods.length != colNames.length) {
            throw new InvalidSettingsException("Column name array and "
                    + "aggregation method array should be of equal size");
        }
        for (int i = 0, length = aggrMethods.length; i < length; i++) {
            final AggregationMethod method =
                AggregationMethods.getMethod4Id(aggrMethods[i]);
            final boolean inclMissingVal;
            if (inclMissingVals != null) {
                inclMissingVal = inclMissingVals[i];
            } else {
                //get the default behavior of the method
                inclMissingVal = method.inclMissingCells();
            }
            final DataColumnSpec resultSpec = new DataColumnSpecCreator(
                                        colNames[i], colTypes[i]).createSpec();
            final ColumnAggregator aggrCol =
                new ColumnAggregator(resultSpec, method, inclMissingVal);
            if (aggrCol.hasOptionalSettings()) {
                try {
                    NodeSettingsRO operatorSettings = settings.getNodeSettings(
                                   createSettingsKey(aggrCol));
                    if (spec != null) {
                        //this method is called from the dialog
                        aggrCol.loadSettingsFrom(operatorSettings, spec);
                    } else {
                        //this method is called from the node model where we do not
                        //have the DataTableSpec
                        aggrCol.loadValidatedSettings(operatorSettings);
                    }
                } catch (Exception e) {
                    LOGGER.error(
                     "Exception while loading settings for aggreation operator '"
                        + aggrCol.getId() + "', reason: " + e.getMessage());
                }
            }
            colAggrList.add(aggrCol);
        }
        return colAggrList;
    }

    /**
     * @param settings the settings object to write to
     * @param aggregators the {@link ColumnAggregator} objects to save
     */
    public static void saveColumnAggregators(final NodeSettingsWO settings,
            final List<ColumnAggregator> aggregators) {
        if (settings == null) {
            throw new NullPointerException("settings must not be null");
        }
        if (aggregators == null) {
            throw new NullPointerException("cols must not be null");
        }
        final String[] colNames = new String[aggregators.size()];
        final String[] aggrMethods = new String[aggregators.size()];
        final boolean[] inclMissingVals = new boolean[aggregators.size()];
        final DataType[] types = new DataType[aggregators.size()];
        for (int i = 0, length = aggregators.size(); i < length; i++) {
            final ColumnAggregator colAggr = aggregators.get(i);
            colNames[i] = colAggr.getOriginalColName();
            final AggregationMethod method = colAggr.getMethodTemplate();
            aggrMethods[i] = colAggr.getId();
            types[i] = colAggr.getOriginalDataType();
            inclMissingVals[i] = colAggr.inclMissingCells();
            if (colAggr.hasOptionalSettings()) {
                try {
                    final NodeSettingsWO operatorSettings =
                        settings.addNodeSettings(createSettingsKey(colAggr));
                    method.saveSettingsTo(operatorSettings);
                } catch (Exception e) {
                    LOGGER.error(
                        "Exception while saving settings for aggreation operator '"
                        + colAggr.getId() + "', reason: " + e.getMessage());
                }
            }
        }
        final Config cnfg = settings.addConfig(CNFG_AGGR_COL_SECTION);
        cnfg.addStringArray(CNFG_COL_NAMES, colNames);
        cnfg.addDataTypeArray(CNFG_COL_TYPES, types);
        cnfg.addStringArray(CNFG_AGGR_METHODS, aggrMethods);
        cnfg.addBooleanArray(CNFG_INCL_MISSING_VALS, inclMissingVals);
    }

    /**
     * Validates the operator specific settings of all {@link ColumnAggregator}s
     * that require additional settings.
     *
     * @param settings the settings to validate
     * @param aggregators the operators to validate
     * @throws InvalidSettingsException if the settings of an operator are not valid
     * @since 2.7
     */
    public static void validateSettings(final NodeSettingsRO settings,
                                        final List<ColumnAggregator> aggregators)
        throws InvalidSettingsException {
        for (int i = 0, length = aggregators.size(); i < length; i++) {
            final ColumnAggregator colAggr = aggregators.get(i);
            if (colAggr.hasOptionalSettings()) {
                final NodeSettingsRO operatorSettings =
                    settings.getNodeSettings(createSettingsKey(colAggr));
                try {
                    colAggr.validateSettings(operatorSettings);
                } catch (InvalidSettingsException e) {
                    throw new InvalidSettingsException(
                       "Invalid settings for aggreation operator '"
                        + colAggr.getLabel() + "' for column '"
                        + colAggr.getOriginalColName()
                        + "', reason: " + e.getMessage());
                }
            }
        }
    }

    /**
     * @param spec the {@link DataTableSpec} of the input table
     * @param aggregators the {@link ColumnAggregator}s to configure
     * @throws InvalidSettingsException if an {@link ColumnAggregator} could
     * not be configured with the given input spec
     * @since 2.7
     */
    public static void configure(final DataTableSpec spec, final List<ColumnAggregator> aggregators)
        throws InvalidSettingsException {
        for (ColumnAggregator colAggr : aggregators) {
            if (colAggr.hasOptionalSettings()) {
                //check that the input spec is compatible with the aggregator
                try {
                    colAggr.configure(spec);
                } catch (InvalidSettingsException e) {
                    throw new InvalidSettingsException(
                       "Invalid settings for aggreation operator '"
                        + colAggr.getLabel() + "' for column '"
                        + colAggr.getOriginalColName()
                        + "', reason: " + e.getMessage());
                }
            }
        }
    }

    /**
     * @param colAggr the {@link ColumnAggregator} to use
     * @return the unique settings key
     */
    private static String createSettingsKey(final ColumnAggregator colAggr) {
        //the method id and the original column name are unique since
        //both are used to generate the new column name in the result table
        //which must be unique as well
        return colAggr.getId() + "_" + colAggr.getOriginalColName();
    }

    /**
     * Use {@link #setInclMissingCells(boolean)} instead.
     *
     * @param inclMissingCells the inclMissingCells to set
     * see {@link #setInclMissingCells(boolean)}
     */
    @Deprecated
    public void setinclMissingCells(final boolean inclMissingCells) {
        super.setInclMissingCells(inclMissingCells);
    }
}
