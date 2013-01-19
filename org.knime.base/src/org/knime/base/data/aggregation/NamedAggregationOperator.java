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

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Combines a method with the name to use for this method.
 *
 * @author Tobias Koetter, University of Konstanz
 * @since 2.6
 */
public class NamedAggregationOperator extends AggregationMethodDecorator {

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(NamedAggregationOperator.class);

    /**Config key for the name the result column should have.*/
    protected static final String CNFG_RESULT_COL_NAMES = "resultColName";

    private String m_name;

    /**Constructor for class NamedAggregationMethod.
     * @param method the {@link AggregationMethod} to use
     */
    public NamedAggregationOperator(final AggregationMethod method) {
        super(method);
        m_name = getMethodTemplate().getColumnLabel();
    }

    /**Constructor for class NamedAggregationMethod.
     * @param name the name to use for the selected method
     * @param method the {@link AggregationMethod} to use
     * @param inclMissingCells <code>true</code> if missing cells should be
     * considered
     */
    public NamedAggregationOperator(final String name,
            final AggregationMethod method, final boolean inclMissingCells) {
        super(method, inclMissingCells);
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name must not be empty");
        }
        m_name = name;
    }

    /**
     * @param settings the {@link NodeSettingsWO} to write to
     * @param operators list of the
     * {@link NamedAggregationOperator}s to save
     * @since 2.7
     */
    public static void saveMethods(final NodeSettingsWO settings,
            final List<NamedAggregationOperator> operators) {
        if (settings == null) {
            throw new NullPointerException("config must not be null");
        }
        if (operators == null) {
            throw new NullPointerException("methods must not be null");
        }
        final String[] colNames = new String[operators.size()];
        final String[] aggrMethods =
            new String[operators.size()];
        final boolean[] inclMissingVals =
            new boolean[operators.size()];
        for (int i = 0, length = operators.size();
            i < length; i++) {
            final NamedAggregationOperator operator =
                operators.get(i);
            colNames[i] = operator.getName();
            aggrMethods[i] = operator.getId();
            inclMissingVals[i] = operator.inclMissingCells();
            if (operator.hasOptionalSettings()) {
                try {
                    NodeSettingsWO operatorSettings = settings.addNodeSettings(
                       createSettingsKey(operator));
                    operator.saveSettingsTo(operatorSettings);
                } catch (Exception e) {
                    LOGGER.error(
                    "Exception while saving settings for aggreation operator '"
                        + operator.getId() + "', reason: " + e.getMessage());
                }
            }
        }

        settings.addStringArray(CNFG_RESULT_COL_NAMES, colNames);
        settings.addStringArray(CNFG_AGGR_METHODS, aggrMethods);
        settings.addBooleanArray(CNFG_INCL_MISSING_VALS, inclMissingVals);
    }

    /**
     * @param settings the {@link NodeSettingsRO} to read from
     * @return a list of all {@link NamedAggregationOperator}s
     * @throws InvalidSettingsException if the settings are invalid
     * @since 2.7
     */
    public static List<NamedAggregationOperator> loadOperators(
         final NodeSettingsRO settings)
    throws InvalidSettingsException {
        return loadOperators(settings, null);
    }

    /**
     * @param settings the {@link NodeSettingsRO} to read from
     * @param spec the {@link DataTableSpec} of the input table
     * @return a list of all {@link NamedAggregationOperator}s
     * @throws InvalidSettingsException if the settings are invalid
     * @since 2.7
     */
    public static List<NamedAggregationOperator> loadOperators(
         final NodeSettingsRO settings, final DataTableSpec spec)
    throws InvalidSettingsException {
        final String[] resultColNames =
            settings.getStringArray(CNFG_RESULT_COL_NAMES);
        final String[] aggrMethods =
            settings.getStringArray(CNFG_AGGR_METHODS);
        final boolean[] inclMissingVals =
            settings.getBooleanArray(CNFG_INCL_MISSING_VALS);
        final List<NamedAggregationOperator> colAggrList =
            new LinkedList<NamedAggregationOperator>();
        if (aggrMethods.length != resultColNames.length) {
            throw new InvalidSettingsException("Name array and "
                + "aggregation method array should be of equal size");
        }
        for (int i = 0, length = aggrMethods.length; i < length; i++) {
            final String resultColName = resultColNames[i];
            final AggregationMethod method =
                AggregationMethods.getMethod4Id(aggrMethods[i]);
            final boolean inclMissingVal = inclMissingVals[i];
            NamedAggregationOperator operator =
                new NamedAggregationOperator(resultColName,
                                         method, inclMissingVal);
            if (operator.hasOptionalSettings()) {
                try {
                    NodeSettingsRO operatorSettings = settings.getNodeSettings(
                               createSettingsKey(operator));
                    if (spec != null) {
                        //this method is called from the dialog
                        operator.loadSettingsFrom(operatorSettings, spec);
                    } else {
                        //this method is called from the node model where we do not
                        //have the DataTableSpec
                        operator.loadValidatedSettings(operatorSettings);
                    }
                } catch (Exception e) {
                    LOGGER.error(
                    "Exception while loading settings for aggreation operator '"
                        + operator.getId() + "', reason: " + e.getMessage());
                }
            }
            colAggrList.add(operator);
        }
        return colAggrList;
    }

    /**
     * Validates the operator specific settings of all {@link NamedAggregationOperator}s
     * that require additional settings.
     *
     * @param settings the settings to validate
     * @param operators the operators to validate
     * @throws InvalidSettingsException if the settings of an operator are not valid
     * @since 2.7
     */
    public static void validateSettings(final NodeSettingsRO settings,
                                        final List<NamedAggregationOperator> operators)
        throws InvalidSettingsException {
        for (NamedAggregationOperator operator : operators) {
            if (operator.hasOptionalSettings()) {
                try {
                    final NodeSettingsRO operatorSettings = settings.getNodeSettings(
                                     createSettingsKey(operator));
                    operator.validateSettings(operatorSettings);
                } catch (InvalidSettingsException e) {
                    throw new InvalidSettingsException(
                       "Invalid operator settings for result column '"
                        + operator.getName()
                        + "', reason: " + e.getMessage());
                }
            }
        }
    }

    /**
     * @param spec the {@link DataTableSpec} of the input table
     * @param operators the {@link NamedAggregationOperator}s to configure
     * @throws InvalidSettingsException if an {@link NamedAggregationOperator} could
     * not be configured with the given input spec
     * @since 2.7
     */
    public static void configure(final DataTableSpec spec, final List<NamedAggregationOperator> operators)
        throws InvalidSettingsException {
        for (NamedAggregationOperator operator : operators) {
            if (operator.hasOptionalSettings()) {
                //check that the input spec is compatible with the aggregator
                try {
                    operator.configure(spec);
                } catch (InvalidSettingsException e) {
                    throw new InvalidSettingsException(
                       "Invalid operator settings for result column '"
                        + operator.getName()
                        + "', reason: " + e.getMessage());
                }
            }
        }
    }

    /**
     * @param operator the {@link NamedAggregationOperator} to use
     * @return the unique settings key
     */
    private static String createSettingsKey(final NamedAggregationOperator operator) {
        //the result column name alone would be enough to generate a unique key
        //we only add the operator id as additional information
        return operator.getId() + "_" + operator.getName();
    }

    /**
     * Specifies the name of the column in the result table.
     * This name is usually set by the user and should be used instead of the
     * name returned by the {@link #getColumnLabel()} method.
     * @return the name the result column should have
     * @see #getColumnLabel()
     */
    public String getName() {
        return m_name;
    }


    /**
     * @param name the name to set
     */
    public void setName(final String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name must not be empty");
        }
        m_name = name;
    }

    /**
     * Resets the name to the default name.
     */
    public void resetName() {
        setName(getMethodTemplate().getColumnLabel());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return m_name + " : " + super.toString();
    }
}
