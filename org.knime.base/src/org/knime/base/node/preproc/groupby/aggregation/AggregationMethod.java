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
    MIN(NumericOperators.getInstance().new MinOperator(0)),
    /**Maximum.*/
    MAX(NumericOperators.getInstance().new MaxOperator(0)),
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
    /**Takes the value which occurs most.*/
    CONCATENATE(Operators.getInstance().new ConcatenateOperator(0)),
    /**Takes the value which occurs most.*/
    UNIQUE_CONCATENATE(
            Operators.getInstance().new UniqueConcatenateOperator(0)),
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
     * @return <code>true</code> if only numerical columns are accepted
     */
    public boolean isNumerical() {
        return m_operator.isNumerical();
    }

    /**
     * @param maxUniqueValues the maximum number of unique values
     * @return the operator of this method
     */
    public AggregationOperator getOperator(final int maxUniqueValues) {
        return m_operator.createInstance(maxUniqueValues);
    }

    /**
     * @param origColumnName the original column name
     * @return the new name of the aggregation column
     */
    public String createColumnName(final String origColumnName) {
        return m_operator.createColumnName(origColumnName);
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
            if (methods[i].isNumerical() == numeric) {
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
