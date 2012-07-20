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

import org.knime.base.data.aggregation.booleancell.FalseCountOperator;
import org.knime.base.data.aggregation.booleancell.TrueCountOperator;
import org.knime.base.data.aggregation.collection.AndElementCountOperator;
import org.knime.base.data.aggregation.collection.AndElementOperator;
import org.knime.base.data.aggregation.collection.ElementCountOperator;
import org.knime.base.data.aggregation.collection.OrElementCountOperator;
import org.knime.base.data.aggregation.collection.OrElementOperator;
import org.knime.base.data.aggregation.collection.XORElementCountOperator;
import org.knime.base.data.aggregation.collection.XORElementOperator;
import org.knime.base.data.aggregation.date.DateMeanOperator;
import org.knime.base.data.aggregation.date.DayRangeOperator;
import org.knime.base.data.aggregation.date.MedianDateOperator;
import org.knime.base.data.aggregation.date.MillisRangeOperator;
import org.knime.base.data.aggregation.general.ConcatenateOperator;
import org.knime.base.data.aggregation.general.CountOperator;
import org.knime.base.data.aggregation.general.FirstOperator;
import org.knime.base.data.aggregation.general.LastOperator;
import org.knime.base.data.aggregation.general.ListCellOperator;
import org.knime.base.data.aggregation.general.MaxOperator;
import org.knime.base.data.aggregation.general.MinOperator;
import org.knime.base.data.aggregation.general.MissingValueCountOperator;
import org.knime.base.data.aggregation.general.ModeOperator;
import org.knime.base.data.aggregation.general.PercentOperator;
import org.knime.base.data.aggregation.general.SetCellOperator;
import org.knime.base.data.aggregation.general.SortedListCellOperator;
import org.knime.base.data.aggregation.general.UniqueConcatenateOperator;
import org.knime.base.data.aggregation.general.UniqueConcatenateWithCountOperator;
import org.knime.base.data.aggregation.general.UniqueCountOperator;
import org.knime.base.data.aggregation.numerical.GeometricMeanOperator;
import org.knime.base.data.aggregation.numerical.GeometricStdDeviationOperator;
import org.knime.base.data.aggregation.numerical.MeanOperator;
import org.knime.base.data.aggregation.numerical.MedianOperator;
import org.knime.base.data.aggregation.numerical.ProductOperator;
import org.knime.base.data.aggregation.numerical.RangeOperator;
import org.knime.base.data.aggregation.numerical.StdDeviationOperator;
import org.knime.base.data.aggregation.numerical.SumOperator;
import org.knime.base.data.aggregation.numerical.VarianceOperator;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;


/**
 * Singleton that lists all available aggregation methods including
 * helper methods to retrieve meaningful methods.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public final class AggregationMethods {

    private static final class DataValueClassComparator implements
        Comparator<Class<? extends DataValue>> {

        /**The only instance of this comparator.*/
        static final AggregationMethods.DataValueClassComparator COMPARATOR =
            new DataValueClassComparator();


        /**
         * {@inheritDoc}
         */
        @Override
        public int compare(final Class<? extends DataValue> o1,
                final Class<? extends DataValue> o2) {
            if (o1 == null) {
                if (o2 == null) {
                    return 0;
                }
                return -1;
            }
            if (o2 == null) {
                return 1;
            }
            final String name1 = getUserTypeLabel(o1);
            final String name2 = getUserTypeLabel(o2);
            return name1.compareTo(name2);
        }

    }

    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(AggregationMethods.class);

    /**The id of the AggregationMethod extension point.*/
    public static final String EXT_POINT_ID =
            "org.knime.base.AggregationOperator";

    /**The attribute of the aggregation method extension point.*/
    public static final String EXT_POINT_ATTR_DF = "AggregationOperator";

    /**Singleton instance.*/
    private static AggregationMethods instance = new AggregationMethods();

    /**Map with all valid operators that are available to the user.*/
    private final Map<String, AggregationOperator> m_operators =
        new LinkedHashMap<String, AggregationOperator>();
    /**Map with previously used but now deprecated operators. These
     * operators are not shown to the user.*/
    private final Map<String, AggregationOperator> m_deprecatedOperators =
        new HashMap<String, AggregationOperator>();

    private final AggregationMethod m_defNotNumericalMeth;
    private final AggregationMethod m_defNumericalMeth;
    private final AggregationMethod m_rowOrderMethod;

    /**
     * Returns the only instance of this class.
     * @return the only instance
     */
    private static AggregationMethods instance() {
        return instance;
    }

    private AggregationMethods() {
        //add all default methods
        try {
//The collection methods
            /**And.*/
            addOperator(new AndElementOperator(GlobalSettings.DEFAULT,
                    OperatorColumnSettings.DEFAULT_INCL_MISSING));
            /**And count.*/
            addOperator(
                    new AndElementCountOperator(GlobalSettings.DEFAULT,
                            OperatorColumnSettings.DEFAULT_INCL_MISSING));
            /**Or.*/
            addOperator(new OrElementOperator(GlobalSettings.DEFAULT,
                    OperatorColumnSettings.DEFAULT_INCL_MISSING));
            /**Or count.*/
            addOperator(
                    new OrElementCountOperator(GlobalSettings.DEFAULT,
                            OperatorColumnSettings.DEFAULT_INCL_MISSING));
            /**XOR.*/
            addOperator(new XORElementOperator(GlobalSettings.DEFAULT,
                    OperatorColumnSettings.DEFAULT_INCL_MISSING));
            /**XOR count.*/
            addOperator(
                    new XORElementCountOperator(GlobalSettings.DEFAULT,
                            OperatorColumnSettings.DEFAULT_INCL_MISSING));
            /**Element counter.*/
            addOperator(
                    new ElementCountOperator(GlobalSettings.DEFAULT,
                            OperatorColumnSettings.DEFAULT_INCL_MISSING));
//The date methods
            /**Date mean operator.*/
            addOperator(new DateMeanOperator(GlobalSettings.DEFAULT,
                    OperatorColumnSettings.DEFAULT_EXCL_MISSING));
            /**Median date operator.*/
            addOperator(new MedianDateOperator(GlobalSettings.DEFAULT,
                    OperatorColumnSettings.DEFAULT_EXCL_MISSING));
            /**Day range operator.*/
            addOperator(new DayRangeOperator(GlobalSettings.DEFAULT,
                    OperatorColumnSettings.DEFAULT_EXCL_MISSING));
            /**Milliseconds range operator.*/
            addOperator(new MillisRangeOperator(GlobalSettings.DEFAULT,
                    OperatorColumnSettings.DEFAULT_EXCL_MISSING));

//The numerical methods
            /**Mean.*/
            final AggregationOperator meanOperator =
                new MeanOperator(GlobalSettings.DEFAULT,
                        OperatorColumnSettings.DEFAULT_EXCL_MISSING);
            addOperator(meanOperator);
            m_defNumericalMeth = getOperator(meanOperator.getId());
            /**Standard deviation.*/
            addOperator(
                    new StdDeviationOperator(GlobalSettings.DEFAULT,
                            OperatorColumnSettings.DEFAULT_EXCL_MISSING));
            /**Variance.*/
            addOperator(new VarianceOperator(GlobalSettings.DEFAULT,
                    OperatorColumnSettings.DEFAULT_EXCL_MISSING));
            /**Median.*/
            addOperator(new MedianOperator(GlobalSettings.DEFAULT,
                    OperatorColumnSettings.DEFAULT_EXCL_MISSING));
            /**Sum.*/
            addOperator(new SumOperator(GlobalSettings.DEFAULT,
                    OperatorColumnSettings.DEFAULT_EXCL_MISSING));
            /**Product.*/
            addOperator(new ProductOperator(GlobalSettings.DEFAULT,
                    OperatorColumnSettings.DEFAULT_EXCL_MISSING));
            /**Range.*/
            addOperator(new RangeOperator(GlobalSettings.DEFAULT,
                    OperatorColumnSettings.DEFAULT_EXCL_MISSING));
            /**Geometric Mean.*/
            addOperator(new GeometricMeanOperator(GlobalSettings.DEFAULT,
                    OperatorColumnSettings.DEFAULT_EXCL_MISSING));
            /**Geometric deviation.*/
            addOperator(
                    new GeometricStdDeviationOperator(GlobalSettings.DEFAULT,
                            OperatorColumnSettings.DEFAULT_EXCL_MISSING));

//The boolean methods
            /**True count operator.*/
            addOperator(new TrueCountOperator(GlobalSettings.DEFAULT,
                    OperatorColumnSettings.DEFAULT_EXCL_MISSING));
            /**False count operator.*/
            addOperator(new FalseCountOperator(GlobalSettings.DEFAULT,
                    OperatorColumnSettings.DEFAULT_EXCL_MISSING));

//The general methods that work with all DataCells
            /**Takes the first cell per group.*/
            final AggregationOperator firstOperator =
                new FirstOperator(GlobalSettings.DEFAULT,
                        OperatorColumnSettings.DEFAULT_INCL_MISSING);
            addOperator(firstOperator);
            m_defNotNumericalMeth = getOperator(firstOperator.getId());
            m_rowOrderMethod = new FirstOperator(GlobalSettings.DEFAULT,
                    OperatorColumnSettings.DEFAULT_INCL_MISSING);
            /**Takes the last cell per group.*/
            addOperator(new LastOperator(GlobalSettings.DEFAULT,
                    OperatorColumnSettings.DEFAULT_INCL_MISSING));
            /**Minimum.*/
            addOperator(new MinOperator(GlobalSettings.DEFAULT,
                    OperatorColumnSettings.DEFAULT_INCL_MISSING));
            /**Maximum.*/
            addOperator(new MaxOperator(GlobalSettings.DEFAULT,
                    OperatorColumnSettings.DEFAULT_INCL_MISSING));
            /**Takes the value which occurs most.*/
            addOperator(new ModeOperator(GlobalSettings.DEFAULT,
                    OperatorColumnSettings.DEFAULT_INCL_MISSING));
            /**Concatenates all cell values.*/
            addOperator(new ConcatenateOperator(GlobalSettings.DEFAULT,
                    OperatorColumnSettings.DEFAULT_INCL_MISSING));
            /**Concatenates all distinct cell values.*/
            addOperator(new UniqueConcatenateOperator(GlobalSettings.DEFAULT,
                    OperatorColumnSettings.DEFAULT_EXCL_MISSING));
            /**Concatenates all distinct cell values and counts the members.*/
            addOperator(new UniqueConcatenateWithCountOperator(
                    GlobalSettings.DEFAULT,
                    OperatorColumnSettings.DEFAULT_EXCL_MISSING));
            /**Counts the number of unique group members.*/
            addOperator(new UniqueCountOperator(GlobalSettings.DEFAULT,
                    OperatorColumnSettings.DEFAULT_INCL_MISSING));
            /**Counts the number of group members.*/
            addOperator(new CountOperator(GlobalSettings.DEFAULT,
                    OperatorColumnSettings.DEFAULT_INCL_MISSING));
            /**Returns the percentage of the group.*/
            addOperator(new PercentOperator(GlobalSettings.DEFAULT,
                    OperatorColumnSettings.DEFAULT_INCL_MISSING));
            /**Counts the number of missing values per group.*/
            addOperator(new MissingValueCountOperator(GlobalSettings.DEFAULT,
                    OperatorColumnSettings.DEFAULT_INCL_MISSING));
            /** Set collection.*/
            addOperator(new SetCellOperator(GlobalSettings.DEFAULT,
                    OperatorColumnSettings.DEFAULT_INCL_MISSING));
            /** List collection.*/
            addOperator(new ListCellOperator(GlobalSettings.DEFAULT,
                    OperatorColumnSettings.DEFAULT_INCL_MISSING));
            /** Sorted list collection.*/
            addOperator(
                    new SortedListCellOperator(GlobalSettings.DEFAULT,
                            OperatorColumnSettings.DEFAULT_INCL_MISSING));

            //add old and deprecated operators to be backward compatible
            registerDeprecatedOperators();
        } catch (final DuplicateOperatorException e) {
            throw new IllegalStateException(
                    "Exception while initializing class: "
                    + getClass().getName() + " Exception: " + e.getMessage());
        }
        //register all extension point implementations
        registerExtensionPoints();
    }

    /**
     * This method registers previous methods which are deprecated in order to
     * be backward compatible. The methods are stored separate from the
     * methods to use.
     *
     * @throws DuplicateOperatorException if one of the methods already exists
     */
    @SuppressWarnings("deprecation")
    private void registerDeprecatedOperators()
        throws DuplicateOperatorException {
        addDeprecatedOperator(new OrElementCountOperator(new OperatorData(
                "Unique element count", true, false, CollectionDataValue.class,
                false), GlobalSettings.DEFAULT,
                OperatorColumnSettings.DEFAULT_INCL_MISSING));
        addDeprecatedOperator(new FirstOperator(new OperatorData("First value",
                    false, true, DataValue.class, false),
                    GlobalSettings.DEFAULT,
                    OperatorColumnSettings.DEFAULT_EXCL_MISSING));
        addDeprecatedOperator(new LastOperator(new OperatorData("Last value",
                    false, true, DataValue.class, false),
                    GlobalSettings.DEFAULT,
                    OperatorColumnSettings.DEFAULT_EXCL_MISSING));
        addDeprecatedOperator(new CountOperator(new OperatorData("Value count",
                    false, true, DataValue.class, false),
                    GlobalSettings.DEFAULT,
                    OperatorColumnSettings.DEFAULT_EXCL_MISSING));

        //methods changed in KNIME version 2.4
        /** Concatenates all cell values. */
        addDeprecatedOperator(new org.knime.base.data.aggregation.deprecated
                .ConcatenateOperator(GlobalSettings.DEFAULT,
                        OperatorColumnSettings.DEFAULT_INCL_MISSING));
        /** Concatenates all distinct cell values. */
        addDeprecatedOperator(new org.knime.base.data.aggregation.deprecated
                .UniqueConcatenateOperator(GlobalSettings.DEFAULT,
                        OperatorColumnSettings.DEFAULT_EXCL_MISSING));
        /** Concatenates all distinct cell values and counts the members. */
        addDeprecatedOperator(new org.knime.base.data.aggregation.deprecated
                .UniqueConcatenateWithCountOperator(GlobalSettings.DEFAULT,
                        OperatorColumnSettings.DEFAULT_EXCL_MISSING));

        //methods changed in KNIME version 2.5.2
        addDeprecatedOperator(new org.knime.base.data.aggregation.deprecated
                .SumOperator(GlobalSettings.DEFAULT,
                        OperatorColumnSettings.DEFAULT_EXCL_MISSING));
    }

    /**
     * Registers the given operator as a deprecated operator. The operator is
     * accessible via the
     * @param operator the deprecated operator to register
     * @throws DuplicateOperatorException if the method already exists
     */
    private void addDeprecatedOperator(final AggregationOperator operator)
        throws DuplicateOperatorException {
        if (operator == null) {
            throw new NullPointerException("operator must not be null");
        }
        final String id = operator.getId();
        final AggregationOperator existingOp = getOperator(id);
        if (existingOp != null) {
            throw new DuplicateOperatorException(
                    "Operator with id: " + id + " already registered",
                    existingOp);
        }
        m_deprecatedOperators.put(id, operator);
    }

    /**
     * Registers all extension point implementations.
     */
    private void registerExtensionPoints() {
        try {
            final IExtensionRegistry registry = Platform.getExtensionRegistry();
            final IExtensionPoint point =
                registry.getExtensionPoint(EXT_POINT_ID);
            if (point == null) {
                LOGGER.error("Invalid extension point: " + EXT_POINT_ID);
                throw new IllegalStateException("ACTIVATION ERROR: "
                        + " --> Invalid extension point: " + EXT_POINT_ID);
            }
            for (final IConfigurationElement elem
                    : point.getConfigurationElements()) {
                final String operator = elem.getAttribute(EXT_POINT_ATTR_DF);
                final String decl =
                    elem.getDeclaringExtension().getUniqueIdentifier();

                if (operator == null || operator.isEmpty()) {
                    LOGGER.error("The extension '" + decl
                            + "' doesn't provide the required attribute '"
                            + EXT_POINT_ATTR_DF + "'");
                    LOGGER.error("Extension " + decl + " ignored.");
                    continue;
                }

                try {
                    final AggregationOperator aggrOperator =
                            (AggregationOperator)elem.createExecutableExtension(
                                    EXT_POINT_ATTR_DF);
                    addOperator(aggrOperator);
                } catch (final Throwable t) {
                    LOGGER.error("Problems during initialization of "
                            + "aggregation operator (with id '" + operator
                            + "'.)", t);
                    if (decl != null) {
                        LOGGER.error("Extension " + decl + " ignored.", t);
                    }
                }
            }
        } catch (final Exception e) {
            LOGGER.error("Exception while registering "
                    + "aggregation operator extensions", e);
        }
    }

    private void addOperator(final AggregationOperator operator)
        throws DuplicateOperatorException {
        if (operator == null) {
            throw new NullPointerException("operator must not be null");
        }
        final String id = operator.getId();
        final AggregationOperator existingOp = getOperator(id);
        if (existingOp != null) {
            throw new DuplicateOperatorException(
                    "Operator with id: " + id + " already registered",
                    existingOp);
        }
        m_operators.put(id, operator);
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
        instance().addOperator(operator);
    }

    /**
     * @param id the unique id to check
     * @return <code>true</code> if an operator with the given name is already
     * registered
     */
    public static boolean operatorExists(final String id) {
            return instance().getOperator(id) != null;
    }

    /**
     * @return an unmodifiable {@link Collection} with all registered
     * {@link AggregationOperator}s
     */
    private Collection<AggregationOperator> getOperators() {
        return Collections.unmodifiableCollection(m_operators.values());
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
     * the given {@link DataType} or an empty list if none is compatible
     */
    public static List<AggregationMethod> getCompatibleMethods(
            final DataType type) {
        final List<AggregationMethod> compatibleMethods =
            new ArrayList<AggregationMethod>();
        if (type == null) {
            return compatibleMethods;
        }
        for (final AggregationOperator operator : instance().getOperators()) {
            if (operator.isCompatible(type)) {
                compatibleMethods.add(operator);
            }
        }
        return compatibleMethods;
    }

    /**
     * @param type the {@link DataType} to check
     * @return the aggregation methods that are compatible
     * with the given {@link DataType} grouped by the supported data type
     */
    public static Map<Class<? extends DataValue>, List<AggregationMethod>>
        getCompatibleMethodGroups(final DataType type) {
        final List<AggregationMethod> methods = getCompatibleMethods(type);
        final Map<Class<? extends DataValue>, List<AggregationMethod>>
            methodGroups = groupMethodsByType(methods);
        return methodGroups;
    }

    /**
     * @param type the {@link DataType} to check
     * @return the aggregation methods that are compatible
     * with the given {@link DataType} grouped by the supported data type
     * @since 2.6
     */
    public static List<Entry<String, List<AggregationMethod>>>
        getCompatibleMethodGroupList(final DataType type) {
        final Map<Class<? extends DataValue>, List<AggregationMethod>>
        methodGroups = AggregationMethods.getCompatibleMethodGroups(type);
        final Set<Entry<Class<? extends DataValue>, List<AggregationMethod>>>
            methodSet = methodGroups.entrySet();
        final List<String> labels = new ArrayList<String>(methodSet.size());
        final Map<String, List<AggregationMethod>> labelSet =
            new HashMap<String, List<AggregationMethod>>(methodSet.size());
        for (final Entry<Class<? extends DataValue>, List<AggregationMethod>>
            entry : methodSet) {
            final String label =
                getUserTypeLabel(entry.getKey());
            labels.add(label);
            labelSet.put(label, entry.getValue());
        }
        Collections.sort(labels);
        final List<Entry<String, List<AggregationMethod>>> list =
            new ArrayList<Entry<String, List<AggregationMethod>>>(
                    methodSet.size());
        for (final String label : labels) {
            final List<AggregationMethod> methods = labelSet.get(label);
            final Entry<String, List<AggregationMethod>> entry =
                new Map.Entry<String, List<AggregationMethod>>() {
                    @Override
                    public String getKey() {
                        return label;
                    }
                    @Override
                    public List<AggregationMethod> getValue() {
                        return methods;
                    }
                    @Override
                    public List<AggregationMethod> setValue(
                            final List<AggregationMethod> value) {
                        return methods;
                    }
            };
            list.add(entry);
        }
        return list;
    }

    /**
     * Returns a set with all data types that are supported by at least one
     * {@link AggregationOperator}.
     * @return all data types that are supported by at least one
     * {@link AggregationOperator}
     */
    public static Collection<Class<? extends DataValue>> getSupportedTypes() {
        final Set<Class<? extends DataValue>> supportedTypes =
            new HashSet<Class<? extends DataValue>>();
        for (final AggregationOperator operator : instance.getOperators()) {
            supportedTypes.add(operator.getSupportedType());
        }
        return supportedTypes;
    }

    /**
     * Creates a more user friendly string for the given type. The types are
     * returned starting with a capital letter such as Numerical, Date, etc.
     *
     * @param type the type to get the user readable name starting with
     * a capital letter such as General, Numerical or Date.
     * @return the user friendlier name starting with a capital letter
     */
    public static String getUserTypeLabel(
            final Class<? extends DataValue> type) {
        if (type == DataValue.class) {
            return "General";
        }
        if (type == DoubleValue.class) {
             return "Numerical";
        }
        final String typeName = type.getName();
        final int idx = typeName.lastIndexOf('.');
        String valueName = typeName;
        if (idx >= 0) {
            //remove all package names
            valueName = typeName.substring(idx + 1);
        }
        //remove DataValue and Value from the name
        valueName = valueName.replace("DataValue", "");
        valueName = valueName.replace("Value", "");
        //Split the name at capital letters
        final StringBuilder buf = new StringBuilder();
        for (int i = 0; i < valueName.length(); i++) {
            if (i > 0 && Character.isUpperCase(valueName.charAt(i))) {
                buf.append(" ");
            }
            buf.append(valueName.charAt(i));
        }
        return buf.toString();
    }

    /**
     * @param methods the methods to group
     * @return a {@link Map} with the given aggregation methods grouped
     * by their supported data type.
     */
    public static Map<Class<? extends DataValue>, List<AggregationMethod>>
        groupMethodsByType(final List<AggregationMethod> methods) {
        final TreeMap<Class<? extends DataValue>, List<AggregationMethod>>
            methodGroups =
            new TreeMap<Class<? extends DataValue>, List<AggregationMethod>>(
                    DataValueClassComparator.COMPARATOR);
        for (final AggregationMethod method : methods) {
            final Class<? extends DataValue> type = method.getSupportedType();
            List<AggregationMethod> list = methodGroups.get(type);
            if (list == null) {
                list = new LinkedList<AggregationMethod>();
                methodGroups.put(type, list);
            }
            list.add(method);
        }
        return methodGroups;
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
        return new FirstOperator(GlobalSettings.DEFAULT,
                new OperatorColumnSettings(false, null));
    }

    /**
     * @param dataValueClass the {@link DataValue} class to get the default
     * method for
     * @return the default {@link AggregationMethod} for the given
     * {@link DataValue} class
     * @since 2.6
     */
    public static AggregationMethod getDefaultMethod(
            final Class<? extends DataValue> dataValueClass) {
        final Map<Class<? extends DataValue>, List<AggregationMethod>> methods =
                groupMethodsByType(getAvailableMethods());
        final List<AggregationMethod> compatibleMethods =
            methods.get(dataValueClass);
        if (compatibleMethods.size() > 0) {
            return compatibleMethods.get(0);
        }
        return new FirstOperator(GlobalSettings.DEFAULT,
                new OperatorColumnSettings(false, null));
    }

    /**
     * @param model the {@link SettingsModelString} with the id of the
     * <code>AggregationMethod</code>
     * @return the <code>AggregationMethod</code> for the given id
     */
    public static AggregationMethod getMethod4SettingsModel(
            final SettingsModelString model) {
        if (model == null) {
            throw new NullPointerException("model must not be null");
        }
        return getMethod4Id(model.getStringValue());
    }

    /**
     * @param id the id to get the <code>AggregationMethod</code> for.
     * @return the <code>AggregationMethod</code> with the given id
     * @throws IllegalArgumentException if no <code>AggregationMethod</code>
     * exists for the given id
     */
    public static AggregationMethod getMethod4Id(final String id) {
        final AggregationOperator operator = instance().getOperator(id);
        if (operator == null) {
            throw new IllegalArgumentException("No method found for id: "
                    + id);
        }
        return operator;
    }

    /**
     * @param id the id of the {@link AggregationOperator} to get
     * @return the <code>AggregationOperator</code> with the given id or
     * <code>null</code> if none exists with the id
     */
    private AggregationOperator getOperator(final String id) {
        if (id == null) {
            throw new NullPointerException("id must not be null");
        }
        AggregationOperator operator = m_operators.get(id);
        if (operator == null) {
            operator = m_deprecatedOperators.get(id);
        }
        return operator;
    }

    /**
     * Creates a {@link JScrollPane} that lists all available aggregation
     * methods and a short description of each method.
     *
     * @return a {@link JScrollPane} that can be added to any dialog to display
     * all available aggregation methods and their description.
     */
    public static JScrollPane createDescriptionPane() {
        final StringBuilder buf = getHTMLDescription();
        final JEditorPane editorPane = new JEditorPane("text/html",
                buf.toString());
        editorPane.setEditable(false);
        final JScrollPane scrollPane = new JScrollPane(editorPane,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        return scrollPane;
    }

    /**
     * @return the HTML String that lists all available aggregation methods
     * and their description as a definition list.
     */
    public static StringBuilder getHTMLDescription() {
        final Map<Class<? extends DataValue>, List<AggregationMethod>>
            methodGroups = groupMethodsByType(getAvailableMethods());
        final StringBuilder buf = new StringBuilder();
        final Set<Entry<Class<? extends DataValue>, List<AggregationMethod>>>
            groups = methodGroups.entrySet();
        boolean first = true;
        for (final Entry<Class<? extends DataValue>, List<AggregationMethod>>
            group : groups) {
            if (first) {
                first = false;
            } else {
                //close the previous definition list
                buf.append("</dl>");
                buf.append("\n");
            }
            final List<AggregationMethod> methods = group.getValue();
            buf.append("<h2 style='text-align:center'>");
            buf.append(getUserTypeLabel(group.getKey()));
            buf.append(" Methods");
            buf.append("</h2>");
            buf.append("\n");
            buf.append("<dl>");
            buf.append("\n");
            for (final AggregationMethod method : methods) {
                buf.append("<dt><b>");
                buf.append(method.getLabel());
                buf.append("</b></dt>");
                buf.append("\n");
                buf.append("<dd>");
                buf.append(method.getDescription());
                buf.append("</dd>");
                buf.append("\n");
            }
        }
        //close the last definition list
        buf.append("</dl>");
        return buf;
    }

    /**
     * @return all available methods ordered by the supported type and the
     * operator name
     */
    public static List<AggregationMethod> getAvailableMethods() {
        final List<AggregationMethod> methods =
            new ArrayList<AggregationMethod>(instance().getOperators());
        Collections.sort(methods);
        return methods;
    }

    /**
     * @return the default not numerical method
     */
    public static AggregationMethod getDefaultNotNumericalMethod() {
        return instance().m_defNotNumericalMeth;
    }

    /**
     * @return the default numerical method
     */
    public static AggregationMethod getDefaultNumericalMethod() {
        return instance().m_defNumericalMeth;
    }

    /**
     * @return the method used to order the rows of the output
     * table if the row order should be retained
     */
    public static AggregationMethod getRowOrderMethod() {
        return instance().m_rowOrderMethod;
    }
}
