/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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

package org.knime.base.node.preproc.groupby.aggregation;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import org.knime.base.node.preproc.groupby.aggregation.general.ConcatenateOperator;
import org.knime.base.node.preproc.groupby.aggregation.general.CountOperator;
import org.knime.base.node.preproc.groupby.aggregation.general.DuplicateOperatorException;
import org.knime.base.node.preproc.groupby.aggregation.general.FirstOperator;
import org.knime.base.node.preproc.groupby.aggregation.general.FirstValueOperator;
import org.knime.base.node.preproc.groupby.aggregation.general.LastOperator;
import org.knime.base.node.preproc.groupby.aggregation.general.LastValueOperator;
import org.knime.base.node.preproc.groupby.aggregation.general.ListCellOperator;
import org.knime.base.node.preproc.groupby.aggregation.general.MaxOperator;
import org.knime.base.node.preproc.groupby.aggregation.general.MinOperator;
import org.knime.base.node.preproc.groupby.aggregation.general.ModeOperator;
import org.knime.base.node.preproc.groupby.aggregation.general.SetCellOperator;
import org.knime.base.node.preproc.groupby.aggregation.general.UniqueConcatenateOperator;
import org.knime.base.node.preproc.groupby.aggregation.general.UniqueConcatenateWithCountOperator;
import org.knime.base.node.preproc.groupby.aggregation.general.UniqueCountOperator;
import org.knime.base.node.preproc.groupby.aggregation.numerical.MeanOperator;
import org.knime.base.node.preproc.groupby.aggregation.numerical.ProductOperator;
import org.knime.base.node.preproc.groupby.aggregation.numerical.StdDeviationOperator;
import org.knime.base.node.preproc.groupby.aggregation.numerical.SumOperator;
import org.knime.base.node.preproc.groupby.aggregation.numerical.VarianceOperator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Class which lists all available aggregation methods including
 * helper methods.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public final class AggregationMethods {

    private static AggregationMethods instance;

    private final Map<String, AggregationOperator> m_operators =
        new HashMap<String, AggregationOperator>();

    private final AggregationMethod m_defNotNumericalMeth;
    private final AggregationMethod m_defNumericalMeth;
    private final AggregationMethod m_rowOrderMethod;

    private AggregationMethods() {
        //add all default methods
        try {
            //The numerical methods
                /**Mean.*/
            final AggregationOperator meanOperator = new MeanOperator(0);
            addOperator(meanOperator);
            m_defNumericalMeth = getMethod(meanOperator.getLabel());
                /**Sum.*/
            addOperator(new SumOperator(0));
              /**Product.*/
            addOperator(
                    new ProductOperator(0));
              /**Variance.*/
            addOperator(
                    new VarianceOperator(0));
              /**Standard deviation.*/
            addOperator(
                    new StdDeviationOperator(0));

          //The none numerical methods
            /**Takes the first cell per group.*/
            final AggregationOperator firstOperator = new FirstOperator(0);
            addOperator(firstOperator);
            m_defNotNumericalMeth = getMethod(firstOperator.getLabel());
            /**Takes the first value per group.*/
            final AggregationOperator firstValOperator =
                new FirstValueOperator(0);
            addOperator(firstValOperator);
            m_rowOrderMethod = getMethod(firstValOperator.getLabel());
            /**Takes the last cell per group.*/
            addOperator(new LastOperator(0));
              /**Minimum.*/
            addOperator(new MinOperator(null, 0));
              /**Maximum.*/
            addOperator(new MaxOperator(null, 0));
              /**Takes the last value per group.*/
            addOperator(new LastValueOperator(0));
              /**Takes the value which occurs most.*/
            addOperator(new ModeOperator(0));
              /**Concatenates all cell values.*/
            addOperator(new ConcatenateOperator(0));
              /**Concatenates all distinct cell values.*/
            addOperator(new UniqueConcatenateOperator(0));
              /**Concatenates all distinct cell values and counts the members.*/
            addOperator(new UniqueConcatenateWithCountOperator(0));
              /**Counts the number of unique group members.*/
            addOperator(new UniqueCountOperator(0));
              /**Counts the number of group members.*/
            addOperator(new CountOperator(0));
              /** List collection.*/
            addOperator(new ListCellOperator(0));
              /** Set collection.*/
            addOperator(new SetCellOperator(0));
        } catch (final DuplicateOperatorException e) {
            throw new IllegalStateException(
                    "Exception while initializing class: "
                    + getClass().getName() + " Exception: " + e.getMessage());
        }
    }

    /**
     * Returns the only instance of this class.
     * @return the only instance
     */
    private static AggregationMethods getInstance() {
        if (instance == null) {
            instance = new AggregationMethods();
        }
        return instance;
    }

    private void addOperator(final AggregationOperator operator)
        throws DuplicateOperatorException {
        if (operator == null) {
            throw new NullPointerException("operator must not be null");
        }
        final String label = operator.getLabel();
        final AggregationOperator existingOp = getOperator(label);
        if (existingOp != null) {
            throw new DuplicateOperatorException(
                    "Operator with label: " + label + " already registered",
                    existingOp);
        }
        m_operators.put(label, operator);
    }

    /**
     * This method allows the registration of new {@link AggregationOperator}s.
     * Check first if an {@link AggregationOperator} with the same name
     * is already registered using the {{@link #operatorExists(String)} method.
     *
     * @param operator the {@link AggregationOperator} to register
     * @throws DuplicateOperatorException if an operator with the same name
     * already exists
     */
    public static void registerOperator(final AggregationOperator operator)
    throws DuplicateOperatorException {
        getInstance().addOperator(operator);
    }

    /**
     * @param name the unique name to check
     * @return <code>true</code> if an operator with the given name is already
     * registered
     */
    public static boolean operatorExists(final String name) {
        return getInstance().getOperator(name) != null;
    }

    /**
     * @param name the unique name of the operator
     * @return the operator or <code>null</code> if none exists with
     * the given name
     */
    private AggregationOperator getOperator(final String name) {
        return m_operators.get(name);
    }

    /**
     * @return {@link Collection} with all registered
     * {@link AggregationOperator}s
     */
    private Collection<AggregationOperator> getOperators() {
        return m_operators.values();
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
     * @param type the {@link DataType} to check
     * @return all {@link AggregationOperator}s that are compatible with
     * the given {@link DataType}
     */
    public static List<AggregationMethod> getCompatibleMethods(
            final DataType type) {
        if (type == null) {
            throw new IllegalArgumentException("type must not be empty");
        }
        final List<AggregationMethod> compatibleMethods =
            new ArrayList<AggregationMethod>();
        final Collection<AggregationOperator> operators =
            getInstance().getOperators();
        for (final AggregationOperator operator : operators) {
            if (operator.isCompatible(type)) {
                compatibleMethods.add(operator);
            }
        }
        Collections.sort(compatibleMethods);
        return compatibleMethods;
    }

    /**
     * @param type the {@link DataType} to check
     * @return the labels of all aggregation methods that are compatible
     * with the given {@link DataType}
     */
    public static List<String> getCompatibleMethodLabels(final DataType type) {
        if (type == null) {
            throw new IllegalArgumentException("type must not be empty");
        }
        final List<AggregationMethod> compatibleMethods =
            getCompatibleMethods(type);
        final List<String> methods =
            new ArrayList<String>(compatibleMethods.size());
        for (final AggregationMethod method : compatibleMethods) {
            methods.add(method.getLabel());
        }
        return methods;
    }

    /**
     * @param spec the {@link DataColumnSpec} to get the default method for
     * @return the default {@link AggregationMethod} for the given column spec
     */
    public static AggregationMethod getDefaultMethod(
            final DataColumnSpec spec) {
        final List<AggregationMethod> methods =
            getCompatibleMethods(spec.getType());
        if (methods.size() > 0) {
            return methods.get(0);
        }
        return new FirstOperator(0);
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
        return getInstance().getMethod(label);
    }

    /**
     * @param label the label of the {@link AggregationMethod} to get
     * @return the <code>AggregationMethod</code> with the given label
     * @throws IllegalArgumentException if no <code>AggregationMethod</code>
     * exists for the given label
     */
    private AggregationMethod getMethod(final String label)
    throws IllegalArgumentException {
        if (label == null) {
            throw new NullPointerException("Label must not be null");
        }
        final AggregationOperator operator = getOperator(label);
        if (operator == null) {
            throw new IllegalArgumentException("No method found for label: "
                + label);
        }
        return operator;
    }

    /**
     * @return all available methods ordered by the supported type and the
     * operator name
     */
    public static List<AggregationMethod> getAvailableMethods() {
        final List<AggregationMethod> methods =
            new ArrayList<AggregationMethod>(getInstance().getOperators());
        Collections.sort(methods);
        return methods;
    }

    /**
     * @return the default not numerical method
     */
    public static AggregationMethod getDefaultNotNumericalMethod() {
        return getInstance().m_defNotNumericalMeth;
    }

    /**
     * @return the default not numerical method
     */
    public static AggregationMethod getDefaultNumericalMethod() {
        return getInstance().m_defNumericalMeth;
    }

    /**
     * @return the method used to order the rows of the output
     * table if the row order should be retained
     */
    public static AggregationMethod getRowOrderMethod() {
        return getInstance().m_rowOrderMethod;
    }
}
