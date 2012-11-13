/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.config.Config;

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
     * @param cnfg the {@link Config} to write to
     * @param namedAggregationOperators list of the
     * {@link NamedAggregationOperator}s
     * @since 2.7
     */
    public static void saveMethods(final NodeSettingsWO cnfg,
            final List<NamedAggregationOperator> namedAggregationOperators) {
        if (cnfg == null) {
            throw new NullPointerException("config must not be null");
        }
        if (namedAggregationOperators == null) {
            throw new NullPointerException("methods must not be null");
        }
        final String[] colNames = new String[namedAggregationOperators.size()];
        final String[] aggrMethods =
            new String[namedAggregationOperators.size()];
        final boolean[] inclMissingVals =
            new boolean[namedAggregationOperators.size()];
        for (int i = 0, length = namedAggregationOperators.size();
            i < length; i++) {
            final NamedAggregationOperator aggrMethod =
                namedAggregationOperators.get(i);
            colNames[i] = aggrMethod.getName();
            aggrMethods[i] = aggrMethod.getMethodTemplate().getId();
            inclMissingVals[i] = aggrMethod.inclMissingCells();
            if (aggrMethod.hasOptionalSettings()) {
                try {
                    NodeSettingsWO operatorSettings = cnfg.addNodeSettings(
                       createSettingsKey(i, aggrMethod.getName(), aggrMethod));
                    aggrMethod.saveSettingsTo(operatorSettings);
                } catch (Exception e) {
                    LOGGER.error(
                    "Exception while saving settings for aggreation operator '"
                    + aggrMethod.getId() + "', reason: " + e.getMessage());
                }
            }
        }

        cnfg.addStringArray(CNFG_RESULT_COL_NAMES, colNames);
        cnfg.addStringArray(CNFG_AGGR_METHODS, aggrMethods);
        cnfg.addBooleanArray(CNFG_INCL_MISSING_VALS, inclMissingVals);
    }

    /**
     * @param cnfg the {@link Config} to read from
     * @return a list of all {@link NamedAggregationOperator}s
     * @throws InvalidSettingsException if the settings are invalid
     * @since 2.7
     */
    public static List<NamedAggregationOperator> loadMethods(
         final NodeSettingsRO cnfg)
    throws InvalidSettingsException {
        final String[] resultColNames =
            cnfg.getStringArray(CNFG_RESULT_COL_NAMES);
        final String[] aggrMethods =
            cnfg.getStringArray(CNFG_AGGR_METHODS);
        final boolean[] inclMissingVals =
            cnfg.getBooleanArray(CNFG_INCL_MISSING_VALS);
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
            if (method.hasOptionalSettings()) {
                try {
                    NodeSettingsRO operatorSettings = cnfg.getNodeSettings(
                               createSettingsKey(i, resultColNames[i], method));
                    method.loadValidatedSettings(operatorSettings);
                } catch (Exception e) {
                    LOGGER.error(
                    "Exception while loading settings for aggreation operator '"
                    + method.getId() + "', reason: " + e.getMessage());
                }
            }
            colAggrList.add(new NamedAggregationOperator(resultColName,
                    method, inclMissingVal));
        }
        return colAggrList;
    }

    /**
     * @param idx the index of the aggregation method (not necessary continuous)
     * @param columnName the name of the result column
     * @param method the {@link AggregationMethod} to use
     * @return the unique settings key
     */
    private static String createSettingsKey(final int idx,
            final String columnName, final AggregationMethod method) {
        return idx + "_" + columnName + "_" + method.getId();
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
