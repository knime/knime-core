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

import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.data.collection.ListDataValue;
import org.knime.core.data.collection.SetDataValue;
import org.knime.core.data.date.DateAndTimeValue;
import org.knime.core.data.vector.bitvector.BitVectorValue;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

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
import org.knime.base.data.aggregation.numerical.MeanOperator;
import org.knime.base.data.aggregation.numerical.MedianOperator;
import org.knime.base.data.aggregation.numerical.ProductOperator;
import org.knime.base.data.aggregation.numerical.RangeOperator;
import org.knime.base.data.aggregation.numerical.StdDeviationOperator;
import org.knime.base.data.aggregation.numerical.SumOperator;
import org.knime.base.data.aggregation.numerical.VarianceOperator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(AggregationMethods.class);

    /**The id of the AggregationMethod extension point.*/
    public static final String EXT_POINT_ID =
            "org.knime.base.AggregationOperator";

    /**The attribute of the aggregation method extension point.*/
    public static final String EXT_POINT_ATTR_DF = "AggregationOperator";
    /**Default global settings object used in operator templates.*/
    private static final GlobalSettings GLOBAL_SETTINGS = new GlobalSettings();
    /**Default include missing values {@link OperatorColumnSettings} object
     * used in operator templates.*/
    private static final OperatorColumnSettings INCL_MISSING =
            new OperatorColumnSettings(true, null);
    /**Default exclude missing values {@link OperatorColumnSettings} object
     * used in operator templates.*/
    private static final OperatorColumnSettings EXCL_MISSING =
            new OperatorColumnSettings(false, null);
    /**Singleton instance.*/
    private static AggregationMethods instance = new AggregationMethods();

    /**Map with all valid operators that are available to the user.*/
    private final Map<String, AggregationOperator> m_operators =
        new HashMap<String, AggregationOperator>();
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
            addOperator(new AndElementOperator(GLOBAL_SETTINGS, INCL_MISSING));
            /**And count.*/
            addOperator(
                    new AndElementCountOperator(GLOBAL_SETTINGS, INCL_MISSING));
            /**Or.*/
            addOperator(new OrElementOperator(GLOBAL_SETTINGS, INCL_MISSING));
            /**Or count.*/
            addOperator(
                    new OrElementCountOperator(GLOBAL_SETTINGS, INCL_MISSING));
            /**XOR.*/
            addOperator(new XORElementOperator(GLOBAL_SETTINGS, INCL_MISSING));
            /**XOR count.*/
            addOperator(
                    new XORElementCountOperator(GLOBAL_SETTINGS, INCL_MISSING));
            /**Element counter.*/
            addOperator(
                    new ElementCountOperator(GLOBAL_SETTINGS, INCL_MISSING));
//The date methods
            /**Date mean operator.*/
            addOperator(new DateMeanOperator(GLOBAL_SETTINGS, EXCL_MISSING));
            /**Median date operator.*/
            addOperator(new MedianDateOperator(GLOBAL_SETTINGS, EXCL_MISSING));
            /**Day range operator.*/
            addOperator(new DayRangeOperator(GLOBAL_SETTINGS, EXCL_MISSING));
            /**Milliseconds range operator.*/
            addOperator(new MillisRangeOperator(GLOBAL_SETTINGS, EXCL_MISSING));

//The numerical methods
            /**Mean.*/
            final AggregationOperator meanOperator =
                new MeanOperator(GLOBAL_SETTINGS, EXCL_MISSING);
            addOperator(meanOperator);
            m_defNumericalMeth = getOperator(meanOperator.getId());
            /**Geometric Mean.*/
            addOperator(new GeometricMeanOperator(GLOBAL_SETTINGS,
                    EXCL_MISSING));
            /**Median.*/
            addOperator(new MedianOperator(GLOBAL_SETTINGS, EXCL_MISSING));
            /**Sum.*/
            addOperator(new SumOperator(GLOBAL_SETTINGS, EXCL_MISSING));
            /**Product.*/
            addOperator(new ProductOperator(GLOBAL_SETTINGS, EXCL_MISSING));
            /**Range.*/
            addOperator(new RangeOperator(GLOBAL_SETTINGS, EXCL_MISSING));
            /**Variance.*/
            addOperator(new VarianceOperator(GLOBAL_SETTINGS, EXCL_MISSING));
            /**Standard deviation.*/
            addOperator(
                    new StdDeviationOperator(GLOBAL_SETTINGS, EXCL_MISSING));

//The boolean methods
            /**True count operator.*/
            addOperator(new TrueCountOperator(GLOBAL_SETTINGS, EXCL_MISSING));
            /**False count operator.*/
            addOperator(new FalseCountOperator(GLOBAL_SETTINGS, EXCL_MISSING));

//The general methods that work with all DataCells
            /**Takes the first cell per group.*/
            final AggregationOperator firstOperator =
                new FirstOperator(GLOBAL_SETTINGS, INCL_MISSING);
            addOperator(firstOperator);
            m_defNotNumericalMeth = getOperator(firstOperator.getId());
            m_rowOrderMethod = new FirstOperator(GLOBAL_SETTINGS, INCL_MISSING);
            /**Takes the last cell per group.*/
            addOperator(new LastOperator(GLOBAL_SETTINGS, INCL_MISSING));
            /**Minimum.*/
            addOperator(new MinOperator(GLOBAL_SETTINGS, INCL_MISSING));
            /**Maximum.*/
            addOperator(new MaxOperator(GLOBAL_SETTINGS, INCL_MISSING));
            /**Takes the value which occurs most.*/
            addOperator(new ModeOperator(GLOBAL_SETTINGS, INCL_MISSING));
            /**Concatenates all cell values.*/
            addOperator(new ConcatenateOperator(GLOBAL_SETTINGS, INCL_MISSING));
            /**Concatenates all distinct cell values.*/
            addOperator(new UniqueConcatenateOperator(GLOBAL_SETTINGS,
                    EXCL_MISSING));
            /**Concatenates all distinct cell values and counts the members.*/
            addOperator(new UniqueConcatenateWithCountOperator(
                    GLOBAL_SETTINGS, EXCL_MISSING));
            /**Counts the number of unique group members.*/
            addOperator(new UniqueCountOperator(GLOBAL_SETTINGS, INCL_MISSING));
            /**Counts the number of group members.*/
            addOperator(new CountOperator(GLOBAL_SETTINGS, INCL_MISSING));
            /**Returns the percentage of the group.*/
            addOperator(new PercentOperator(GLOBAL_SETTINGS, INCL_MISSING));
            /**Counts the number of missing values per group.*/
            addOperator(new MissingValueCountOperator(GLOBAL_SETTINGS,
                    INCL_MISSING));
            /** List collection.*/
            addOperator(new ListCellOperator(GLOBAL_SETTINGS, INCL_MISSING));
            /** Sorted list collection.*/
            addOperator(
                    new SortedListCellOperator(GLOBAL_SETTINGS, INCL_MISSING));
            /** Set collection.*/
            addOperator(new SetCellOperator(GLOBAL_SETTINGS, INCL_MISSING));

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
                false), GLOBAL_SETTINGS, INCL_MISSING));
        addDeprecatedOperator(new FirstOperator(new OperatorData("First value",
                    false, true, DataValue.class, false),
                    GLOBAL_SETTINGS, EXCL_MISSING));
        addDeprecatedOperator(new LastOperator(new OperatorData("Last value",
                    false, true, DataValue.class, false),
                    GLOBAL_SETTINGS, EXCL_MISSING));
        addDeprecatedOperator(new CountOperator(new OperatorData("Value count",
                    false, true, DataValue.class, false),
                    GLOBAL_SETTINGS, EXCL_MISSING));

        //methods changed in KNIME version 2.4
        /** Concatenates all cell values. */
        addDeprecatedOperator(new org.knime.base.data.aggregation.deprecated
                .ConcatenateOperator(GLOBAL_SETTINGS, INCL_MISSING));
        /** Concatenates all distinct cell values. */
        addDeprecatedOperator(new org.knime.base.data.aggregation.deprecated
                .UniqueConcatenateOperator(GLOBAL_SETTINGS, EXCL_MISSING));
        /** Concatenates all distinct cell values and counts the members. */
        addDeprecatedOperator(new org.knime.base.data.aggregation.deprecated
                .UniqueConcatenateWithCountOperator(GLOBAL_SETTINGS,
                        EXCL_MISSING));
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
        Collections.sort(compatibleMethods);
        return compatibleMethods;
    }

    /**
     * @param type the {@link DataType} to check
     * @return the aggregation methods that are compatible
     * with the given {@link DataType} group by the supported data type
     */
    public static Map<Class<? extends DataValue>, List<AggregationMethod>>
        getCompatibleMethodGroups(final DataType type) {
        final List<AggregationMethod> methods = getCompatibleMethods(type);
        final Map<Class<? extends DataValue>, List<AggregationMethod>>
            methodGroups = groupMethodsByType(methods);
        return methodGroups;
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
        for (final AggregationOperator operator
                : instance.getOperators()) {
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
        if (type == DateAndTimeValue.class) {
            return "Date";
        }
        if (type == CollectionDataValue.class) {
            return "Collection";
        }
        if (type == ListDataValue.class) {
            return "List";
        }
        if (type == SetDataValue.class) {
            return "Set";
        }
        if (type == BooleanValue.class) {
            return "Boolean";
        }
        if (type == BitVectorValue.class) {
            return "Bit vector";
        }
        final String name = type.getName();
        final int i = name.lastIndexOf('.');
        if (i >= 0) {
            return name.substring(i + 1);
        }
        return name;
    }

    /**
     * @param methods the methods to group
     * @return a {@link Map} with the given aggregation methods grouped
     * by their supported data type.
     */
    public static Map<Class<? extends DataValue>, List<AggregationMethod>>
        groupMethodsByType(final List<AggregationMethod> methods) {
        final Map<Class<? extends DataValue>, List<AggregationMethod>>
            methodGroups =
            new HashMap<Class<? extends DataValue>, List<AggregationMethod>>();
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
        return new FirstOperator(new GlobalSettings(),
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
    public static AggregationMethod getMethod4Id(final String id)
    throws IllegalArgumentException {
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
