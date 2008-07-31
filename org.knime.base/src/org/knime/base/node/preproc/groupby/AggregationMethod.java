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

package org.knime.base.node.preproc.groupby;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
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
    MIN("Minimum", true, "Min({1})", null, false, true,
            NumericOperators.getInstance().new MinOperator(0)),
    /**Maximum.*/
    MAX("Maximum", true, "Max({1})", null, false, true,
            NumericOperators.getInstance().new MaxOperator(0)),
    /**Average.*/
    MEAN("Mean", true, "Mean({1})", DoubleCell.TYPE, false, false,
            NumericOperators.getInstance().new MeanOperator(0)),
    /**Sum.*/
    SUM("Sum", true, "Sum({1})", DoubleCell.TYPE, false, false,
            NumericOperators.getInstance().new SumOperator(0)),
    /**Variance.*/
    VARIANCE("Variance", true, "Variance({1})", DoubleCell.TYPE, false, false,
            NumericOperators.getInstance().new VarianceOperator(0)),
    /**Standard deviation.*/
    STD_DEVIATION("Standard deviation", true, "Standard deviation({1})",
            DoubleCell.TYPE, false, false,
            NumericOperators.getInstance().new StdDeviationOperator(0)),

//The none numerical methods
    /**Takes the first cell per group.*/
    FIRST("First", false, "First({1})", null, false, true,
            Operators.getInstance().new FirstOperator(0)),
    /**Takes the first value per group.*/
    FIRST_VALUE("First value", false, "First value({1})", null, false, true,
            Operators.getInstance().new FirstValueOperator(0)),
    /**Takes the last cell per group.*/
    LAST("Last", false, "Last({1})", null, false, true,
            Operators.getInstance().new LastOperator(0)),
    /**Takes the last value per group.*/
    LAST_VALUE("Last value", false, "Last value({1})", null, false, true,
            Operators.getInstance().new LastValueOperator(0)),
    /**Takes the value which occurs most.*/
    MODE("Mode", false, "Mode({1})", null, true, true,
            Operators.getInstance().new ModeOperator(0)),
    /**Takes the value which occurs most.*/
    CONCATENATE("Concatenate", false, "Concatenate({1})", StringCell.TYPE,
            false, false, Operators.getInstance().new ConcatenateOperator(0)),
    /**Takes the value which occurs most.*/
    UNIQUE_CONCATENATE("Unique concatenate", false, "Unique concatenate({1})",
            StringCell.TYPE, true, false,
            Operators.getInstance().new UniqueConcatenateOperator(0)),
    /**Counts the number of unique group members.*/
    UNIQUE_COUNT("Unique count", false, "Unique count({1})",
            IntCell.TYPE, true, false,
            Operators.getInstance().new UniqueCountOperator(0)),
    /**Counts the number of group members.*/
    COUNT("Count", false, "Count({1})", IntCell.TYPE, false, false,
            Operators.getInstance().new CountOperator(0));

    /**The column name place holder.*/
    private static final String PLACE_HOLDER = "{1}";

    /**The String to use by concatenation operators.*/
    public static final String CONCATENATOR = ", ";

    private final AggregationOperator m_operator;
    private final String m_label;
    private final boolean m_numerical;
    private final String m_columnNamePattern;
    private final DataType m_dataType;
    private final boolean m_usesLimit;
    private final boolean m_keepColSpec;

    /**Constructor for class AggregationMethod.
     * @param label user readable label
     * @param numerical <code>true</code> if the operator is only suitable
     * for numerical columns
     * @param columnNamePattern the pattern for the result column name
     * @param type the {@link DataType} of the result column or
     * <code>null</code> if the type stays the same
     * @param usesLimit <code>true</code> if the method checks the number of
     * unique values limit.
     * @param keepColSpec <code>true</code> if the original column specification
     * should be kept if possible
     * @param operator the {@link AggregationOperator} implementation to use
     */
    private AggregationMethod(final String label, final boolean numerical,
            final String columnNamePattern, final DataType type,
            final boolean usesLimit, final boolean keepColSpec,
            final AggregationOperator operator) {
        m_label = label;
        m_numerical = numerical;
        m_columnNamePattern = columnNamePattern;
        m_dataType = type;
        m_usesLimit = usesLimit;
        m_keepColSpec = keepColSpec;
        m_operator = operator;
    }


    /**
     * @return the label
     */
    public String getLabel() {
        return m_label;
    }


    /**
     * @return <code>true</code> if only numerical columns are accepted
     */
    public boolean isNumerical() {
        return m_numerical;
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
    public String getColumnName(final String origColumnName) {
        if (m_columnNamePattern == null || m_columnNamePattern.length() < 1) {
            return origColumnName;
        }
        return m_columnNamePattern.replace(PLACE_HOLDER, origColumnName);
    }

    /**
     * @param origSpec the original {@link DataColumnSpec}
     * @param keepColName <code>true</code> if the original column name
     * should be kept
     * @return the new {@link DataColumnSpec} for the aggregated column
     */
    public DataColumnSpec createColumnSpec(final DataColumnSpec origSpec,
            final boolean keepColName) {
        if (origSpec == null) {
            throw new NullPointerException(
                    "Original column spec must not be null");
        }
        final DataColumnSpecCreator specCreator;
        if (m_keepColSpec && (m_dataType == null
                || origSpec.getType().equals(m_dataType))) {
             specCreator = new DataColumnSpecCreator(origSpec);
        } else {
            final DataType type;
            if (m_dataType == null) {
                type = origSpec.getType();
            } else {
                type = m_dataType;
            }
            specCreator = new DataColumnSpecCreator(origSpec.getName(), type);
        }

        if (!keepColName) {
            specCreator.setName(getColumnName(origSpec.getName()));
        }
        return specCreator.createSpec();
    }

    /**
     * @return <code>true</code> if this method checks the maximum unique
     * values limit.
     */
    public boolean isUsesLimit() {
        return m_usesLimit;
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
