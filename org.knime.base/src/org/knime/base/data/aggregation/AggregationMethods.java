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

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.data.collection.ListDataValue;
import org.knime.core.data.collection.SetDataValue;
import org.knime.core.data.date.DateAndTimeValue;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

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
import org.knime.base.data.aggregation.general.SetCellOperator;
import org.knime.base.data.aggregation.general.SortedListCellOperator;
import org.knime.base.data.aggregation.general.UniqueConcatenateOperator;
import org.knime.base.data.aggregation.general.UniqueConcatenateWithCountOperator;
import org.knime.base.data.aggregation.general.UniqueCountOperator;
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

    private static AggregationMethods instance = new AggregationMethods();

    private final Map<String, AggregationOperator> m_operators =
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
        final GlobalSettings globalSettings = new GlobalSettings(0);
        final OperatorColumnSettings inclMissing =
            new OperatorColumnSettings(true, null);
        final OperatorColumnSettings exclMissing =
            new OperatorColumnSettings(false, null);
        //add all default methods
        try {
//The collection methods
            /**And.*/
            addOperator(new AndElementOperator(globalSettings, inclMissing));
            /**And count.*/
            addOperator(
                    new AndElementCountOperator(globalSettings, inclMissing));
            /**Or.*/
            addOperator(new OrElementOperator(globalSettings, inclMissing));
            /**Or count.*/
            addOperator(
                    new OrElementCountOperator(globalSettings, inclMissing));
            /**XOR.*/
            addOperator(new XORElementOperator(globalSettings, inclMissing));
            /**XOR count.*/
            addOperator(
                    new XORElementCountOperator(globalSettings, inclMissing));
            /**Element counter.*/
            addOperator(new ElementCountOperator(globalSettings, inclMissing));
//The date methods
            /**Date mean operator.*/
            addOperator(new DateMeanOperator(globalSettings, exclMissing));
            /**Median date operator.*/
            addOperator(new MedianDateOperator(globalSettings, exclMissing));
            /**Day range operator.*/
            addOperator(new DayRangeOperator(globalSettings, exclMissing));
            /**Milliseconds range operator.*/
            addOperator(new MillisRangeOperator(globalSettings, exclMissing));

//The numerical methods
                /**Mean.*/
            final AggregationOperator meanOperator =
                new MeanOperator(globalSettings, exclMissing);
            addOperator(meanOperator);
            m_defNumericalMeth = getMethod(meanOperator.getLabel());
                /**Median.*/
            addOperator(new MedianOperator(globalSettings, exclMissing));
                /**Sum.*/
            addOperator(new SumOperator(globalSettings, exclMissing));
              /**Product.*/
            addOperator(new ProductOperator(globalSettings, exclMissing));
              /**Range.*/
            addOperator(new RangeOperator(globalSettings, exclMissing));
              /**Variance.*/
            addOperator(new VarianceOperator(globalSettings, exclMissing));
              /**Standard deviation.*/
            addOperator(new StdDeviationOperator(globalSettings, exclMissing));

//The general methods that work with all DataCells
            /**Takes the first cell per group.*/
            final AggregationOperator firstOperator =
                new FirstOperator(globalSettings, inclMissing);
            addOperator(firstOperator);
            m_defNotNumericalMeth = getMethod(firstOperator.getLabel());
            m_rowOrderMethod = new FirstOperator(globalSettings, inclMissing);
            /**Takes the last cell per group.*/
            addOperator(new LastOperator(globalSettings, inclMissing));
              /**Minimum.*/
            addOperator(new MinOperator(globalSettings, inclMissing));
              /**Maximum.*/
            addOperator(new MaxOperator(globalSettings, inclMissing));
              /**Takes the value which occurs most.*/
            addOperator(new ModeOperator(globalSettings, inclMissing));
              /**Concatenates all cell values.*/
            addOperator(new ConcatenateOperator(globalSettings, inclMissing));
              /**Concatenates all distinct cell values.*/
            addOperator(
                    new UniqueConcatenateOperator(globalSettings, exclMissing));
              /**Concatenates all distinct cell values and counts the members.*/
            addOperator(new UniqueConcatenateWithCountOperator(
                    globalSettings, exclMissing));
              /**Counts the number of unique group members.*/
            addOperator(new UniqueCountOperator(globalSettings, inclMissing));
              /**Counts the number of group members.*/
            addOperator(new CountOperator(globalSettings, inclMissing));
            /**Counts the number of missing values per group.*/
            addOperator(new MissingValueCountOperator(globalSettings,
                    inclMissing));
              /** List collection.*/
            addOperator(new ListCellOperator(globalSettings, inclMissing));
            /** Sorted list collection.*/
            addOperator(
                    new SortedListCellOperator(globalSettings, inclMissing));
              /** Set collection.*/
            addOperator(new SetCellOperator(globalSettings, inclMissing));
        } catch (final DuplicateOperatorException e) {
            throw new IllegalStateException(
                    "Exception while initializing class: "
                    + getClass().getName() + " Exception: " + e.getMessage());
        }
        //register all extension point implementations
        registerExtensionPoints();
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
                            + "'.)");
                    if (decl != null) {
                        LOGGER.error("Extension " + decl + " ignored.", t);
                    }
                }
            }
        } catch (final Exception e) {
            LOGGER.error("Exception while registering "
                    + "aggregation operator extensions");
        }
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
        instance().addOperator(operator);
    }

    /**
     * @param name the unique name to check
     * @return <code>true</code> if an operator with the given name is already
     * registered
     */
    public static boolean operatorExists(final String name) {
        return instance().getOperator(name) != null;
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
     * the given {@link DataType} or an empty list if none is compatible
     */
    public static List<AggregationMethod> getCompatibleMethods(
            final DataType type) {
        final List<AggregationMethod> compatibleMethods =
            new ArrayList<AggregationMethod>();
        if (type == null) {
            return compatibleMethods;
        }
        final Collection<AggregationOperator> operators =
            instance().getOperators();
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
     * @param type the {@link DataType} to check
     * @return the labels of all aggregation methods that are compatible
     * with the given {@link DataType} group by the supported data type
     */
    public static Map<String, List<String>> getCompatibleMethodGroupLabels(
            final DataType type) {
        final List<AggregationMethod> methods = getCompatibleMethods(type);
        final Map<Class<? extends DataValue>, List<AggregationMethod>>
            methodGroups = groupMethodsByType(methods);
        final Map<String, List<String>> methodGroupLabels =
            new HashMap<String, List<String>>(methodGroups.size());
        for (final Entry<Class<? extends DataValue>, List<AggregationMethod>>
                entry : methodGroups.entrySet()) {
            final Class<? extends DataValue> supportedType = entry.getKey();
            final List<AggregationMethod> supportedMethods = entry.getValue();
            final LinkedList<String> labels = new LinkedList<String>();
            for (final AggregationMethod supportedMethod : supportedMethods) {
                labels.add(supportedMethod.getLabel());
            }
            methodGroupLabels.put(
                    getUserTypeLabel(supportedType) +  " Methods", labels);
        }
        return methodGroupLabels;
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
                : instance().m_operators.values()) {
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
        return new FirstOperator(new GlobalSettings(0),
                new OperatorColumnSettings(false, null));
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
        return instance().getMethod(label);
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
        AggregationOperator operator = getOperator(label);
        if (operator == null) {
            operator = oldOperators(label);
            if (operator == null) {
                throw new IllegalArgumentException("No method found for label: "
                        + label);
            }
        }
        return operator;
    }

    /**
     * Compatibility method that returns old methods which are no longer
     * available in the front end.
     *
     * @param label the unique label of the operator
     * @return the operator for the given label or <code>null</code> if none
     * exists
     */
    private AggregationOperator oldOperators(final String label) {
        final GlobalSettings globalSettings = new GlobalSettings(0);
        if (label.equals("Unique element count")) {
            return new OrElementCountOperator(new OperatorData(label,
                    true, false, CollectionDataValue.class, false),
                    globalSettings, new OperatorColumnSettings(true, null));
        }
        if (label.equals("First value")) {
            return new FirstOperator(new OperatorData(label,
                    false, true, DataValue.class, false),
                    globalSettings, new OperatorColumnSettings(false, null));
        }
        if (label.equals("Last value")) {
            return new LastOperator(new OperatorData(label,
                    false, true, DataValue.class, false),
                    globalSettings, new OperatorColumnSettings(false, null));
        }
        if (label.equals("Value count")) {
            return new CountOperator(new OperatorData(label,
                    false, true, DataValue.class, false),
                    globalSettings, new OperatorColumnSettings(false, null));
        }
        return null;
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
