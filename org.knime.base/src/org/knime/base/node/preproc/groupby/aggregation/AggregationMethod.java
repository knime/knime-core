/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 *    28.06.2007 (Tobias Koetter): created
 */

package org.knime.base.node.preproc.groupby.aggregation;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import java.util.ArrayList;
import java.util.List;


/**
 * Enumeration which lists all available aggregation methods including
 * helper methods.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public enum AggregationMethod {

//The numerical methods
    /**Minimum.*/
    MIN(NumericOperators.getInstance().new MinOperator(null, 0)),
    /**Maximum.*/
    MAX(NumericOperators.getInstance().new MaxOperator(null, 0)),
    /**Average.*/
    MEAN(NumericOperators.getInstance().new MeanOperator(0)),
    /**Sum.*/
    SUM(NumericOperators.getInstance().new SumOperator(0)),
    /**Variance.*/
    VARIANCE(NumericOperators.getInstance().new VarianceOperator(0)),
    /**Standard deviation.*/
    STD_DEVIATION(NumericOperators.getInstance().new StdDeviationOperator(0)),

//The none numerical methods
    /**Takes the first cell per group.*/
    FIRST(Operators.getInstance().new FirstOperator(0)),
    /**Takes the first value per group.*/
    FIRST_VALUE(Operators.getInstance().new FirstValueOperator(0)),
    /**Takes the last cell per group.*/
    LAST(Operators.getInstance().new LastOperator(0)),
    /**Takes the last value per group.*/
    LAST_VALUE(Operators.getInstance().new LastValueOperator(0)),
    /**Takes the value which occurs most.*/
    MODE(Operators.getInstance().new ModeOperator(0)),
    /**Concatenates all cell values.*/
    CONCATENATE(Operators.getInstance().new ConcatenateOperator(0)),
    /**Concatenates all distinct cell values.*/
    UNIQUE_CONCATENATE(
            Operators.getInstance().new UniqueConcatenateOperator(0)),
    /**Concatenates all distinct cell values and counts the members.*/
    UNIQUE_CONCATENATE_WITH_COUNT(
            Operators.getInstance().new UniqueConcatenateWithCountOperator(0)),
    /**Counts the number of unique group members.*/
    UNIQUE_COUNT(Operators.getInstance().new UniqueCountOperator(0)),
    /**Counts the number of group members.*/
    COUNT(Operators.getInstance().new CountOperator(0)),
    /** List collection.*/
    LIST(Operators.getInstance().new ListCellOperator(0)),
    /** Set collection.*/
    SET(Operators.getInstance().new SetCellOperator(0));

    private final AggregationOperator m_operator;

    /**Constructor for class AggregationMethod.
     *
     * @param operator the {@link AggregationOperator} implementation to use
     */
    private AggregationMethod(final AggregationOperator operator) {
        m_operator = operator;
    }


    /**
     * @return the label
     */
    public String getLabel() {
        return m_operator.getLabel();
    }

    /**
     * @return the short label which is used in the column name
     */
    public String getShortLabel() {
        return m_operator.getShortLabel();
    }


    /**
     * @return <code>true</code> if only numerical columns are accepted
     */
    public boolean isNumerical() {
        return m_operator.isNumerical();
    }

    /**
     * @param origColSpec the {@link DataColumnSpec} of the original column
     * @param maxUniqueValues the maximum number of unique values
     * @return the operator of this method
     */
    public AggregationOperator getOperator(final DataColumnSpec origColSpec,
            final int maxUniqueValues) {
        return m_operator.createInstance(origColSpec, maxUniqueValues);
    }

    /**
     * @param colName the name of the new column
     * @param origSpec the original {@link DataColumnSpec}
     * should be kept
     * @return the new {@link DataColumnSpecCreator} for the aggregated column
     */
    public DataColumnSpec createColumnSpec(final String colName,
            final DataColumnSpec origSpec) {
       return m_operator.createColumnSpec(colName, origSpec);
    }

    /**
     * @return <code>true</code> if this method checks the maximum unique
     * values limit.
     */
    public boolean isUsesLimit() {
        return m_operator.isUsesLimit();
    }

    /**
     * @param colSpec the {@link DataColumnSpec} to test for compatibility to
     * this {@link AggregationMethod}
     * @return <code>true</code> if this method could be used to aggregation
     * the column with the given specification
     */
    public boolean isCompatible(final DataColumnSpec colSpec) {
        return !isNumerical()
            || colSpec.getType().isCompatible(DoubleValue.class);
    }

    /**
     * @param colSpec the {@link DataColumnSpec} to check
     * @param numericColMethod the {@link AggregationMethod} for
     * numerical columns
     * @param nominalColMethod the {@link AggregationMethod} for none
     * numerical columns
     * @return the {@link AggregationMethod} to use
     */
    public static AggregationMethod getAggregationMethod(
            final DataColumnSpec colSpec,
            final AggregationMethod numericColMethod,
            final AggregationMethod nominalColMethod) {
        if (colSpec.getType().isCompatible(DoubleValue.class)) {
            return numericColMethod;
        }
        return nominalColMethod;
    }

    /**
     * @param colSpec the {@link DataColumnSpec} to check
     * @return all aggregation methods that are compatible with the given
     * {@link DataColumnSpec}
     */
    public static List<AggregationMethod> getCompatibleMethods(
            final DataColumnSpec colSpec) {
        return getMethods(colSpec.getType().isCompatible(DoubleValue.class));
    }


    /**
     * @param spec the {@link DataColumnSpec} to get the default method for
     * @return the default {@link AggregationMethod} for the given column spec
     */
    public static AggregationMethod getDefaultMethod(
            final DataColumnSpec spec) {
        if (spec.getType().isCompatible(DoubleValue.class)) {
            return getDefaultNumericMethod();
        }
        return getDefaultNominalMethod();
    }

    /**
     * @return the default method for numerical columns
     */
    public static AggregationMethod getDefaultNumericMethod() {
        return MIN;
    }

    /**
     * @return the default method for none numerical columns
     */
    public static AggregationMethod getDefaultNominalMethod() {
        return FIRST;
    }

    /**
     * @param model the {@link SettingsModelString} with the label of the
     * <code>AggregationMethod</code>
     * @return the <code>AggregationMethod</code> for the given label
     */
    public static AggregationMethod getMethod4SettingsModel(
            final SettingsModelString model) {
        if (model == null) {
            throw new NullPointerException("model must not be null");
        }
        return getMethod4Label(model.getStringValue());
    }

    /**
     * @param label the label to get the <code>AggregationMethod</code> for.
     * @return the <code>AggregationMethod</code> with the given label
     * @throws IllegalArgumentException if no <code>AggregationMethod</code>
     * exists for the given label
     */
    public static AggregationMethod getMethod4Label(final String label)
    throws IllegalArgumentException {
        if (label == null) {
            throw new NullPointerException("Label must not be null");
        }
        final AggregationMethod[] methods = values();
        for (final AggregationMethod method : methods) {
            if (method.getLabel().equals(label)) {
                return method;
            }
        }
        throw new IllegalArgumentException("No method found for label: "
                + label);
    }

    private static List<AggregationMethod> getMethods(final boolean numeric) {
        final AggregationMethod[] methods = values();
        final List<AggregationMethod> labels =
            new ArrayList<AggregationMethod>(methods.length);
        for (int i = 0, length = methods.length; i < length; i++) {
            if (numeric || methods[i].isNumerical() == numeric) {
                labels.add(methods[i]);
            }
        }
        return labels;
    }

    /**
     * @return a <code>List</code> with the labels of all numerical methods
     */
    public static List<String> getNumericalMethodLabels() {
        final AggregationMethod[] methods = values();
        final List<String> labels = new ArrayList<String>(methods.length);
        for (int i = 0, length = methods.length; i < length; i++) {
            labels.add(methods[i].getLabel());
        }
        return labels;
    }

    /**
     * @return a <code>List</code> with the labels of all none numerical methods
     */
    public static List<String> getNoneNumericalMethodLabels() {
        return getLabels(false);
    }

    private static List<String> getLabels(final boolean numeric) {
        final AggregationMethod[] methods = values();
        final List<String> labels = new ArrayList<String>(methods.length);
        for (int i = 0, length = methods.length; i < length; i++) {
            if (methods[i].isNumerical() == numeric) {
                labels.add(methods[i].getLabel());
            }
        }
        return labels;
    }
}
