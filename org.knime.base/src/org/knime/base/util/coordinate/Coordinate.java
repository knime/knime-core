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
 *   02.02.2006 (sieb): created
 *   20.03.2008 (sellien): redesigned
 */
package org.knime.base.util.coordinate;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.StringValue;

/**
 * The abstract class for all coordinate classes. A concrete coordinate depends
 * on whether it is nominal or numeric, etc. All coordinates have an underlying
 * {@link org.knime.core.data.DataColumnSpec}. Ticks have to be created and
 * mapped to their domain values.
 *
 * @author Christoph Sieb, University of Konstanz
 * @author Stephan Sellien, University of Konstanz
 */
public abstract class Coordinate {

    /**
     * The default tick distance in pixel.
     */
    public static final int DEFAULT_ABSOLUTE_TICK_DIST = 35;

    /**
     * The underlying data column spec of this coordinate.
     */
    private DataColumnSpec m_columnSpec;

    private static final Map<Class<? extends DataValue>, CoordinateFactory> 
        MAP = new HashMap<Class<? extends DataValue>, CoordinateFactory>();

    private static final Map<Class<? extends DataValue>, Set<PolicyStrategy>> 
        POLICY_MAP = new HashMap
            <Class<? extends DataValue>, Set<PolicyStrategy>>();

    private static final Map<Class<? extends DataValue>, Set<MappingMethod>> 
        MAPPING_METHODS = new HashMap
            <Class<? extends DataValue>, Set<MappingMethod>>();

    static {
        addCoordinateFactory(DoubleValue.class, new DoubleCoordinateFactory());
        addPolicy(DoubleValue.class, new AscendingNumericTickPolicyStrategy(
                "Ascending"));
        addPolicy(DoubleValue.class, new DescendingNumericTickPolicyStrategy(
                "Descending"));
        addMappingMethod(DoubleValue.class, new LogarithmicMappingMethod());
        addMappingMethod(IntValue.class, new LogarithmicMappingMethod());
        addMappingMethod(DoubleValue.class, new LogarithmicMappingMethod(10));
        addMappingMethod(IntValue.class, new LogarithmicMappingMethod(10));
        addMappingMethod(DoubleValue.class, new LogarithmicMappingMethod(2));
        addMappingMethod(IntValue.class, new LogarithmicMappingMethod(2));

        addPolicy(IntValue.class, new AscendingNumericTickPolicyStrategy(
                "Ascending"));
        addPolicy(IntValue.class, new DescendingNumericTickPolicyStrategy(
                "Descending"));
        addCoordinateFactory(IntValue.class, new IntegerCoordinateFactory());
        addCoordinateFactory(StringValue.class, new NominalCoordinateFactory());
    }

    private final Set<DataValue> m_desiredValues = new HashSet<DataValue>();

    private final List<MappingMethod> m_activeMethods =
            new LinkedList<MappingMethod>();

    private PolicyStrategy m_policy;

    /**
     * Adds a value which should have a tick.
     *
     * @param values the desired value
     */
    public abstract void addDesiredValues(final DataValue... values);

    /**
     * Returns the set for the desired values.
     *
     * @return the desired value set
     */
    protected Set<DataValue> getDesiredValuesSet() {
        return m_desiredValues;
    }

    /**
     * Clears the desired values.
     */
    public void clearDesiredValues() {
        m_desiredValues.clear();
    }

    /**
     * Returns the strategy for the given policy.
     *
     * @param policy the numeric tick policy
     * @return the according strategy
     */
    protected PolicyStrategy getPolicyStategy(final String policy) {
        Set<PolicyStrategy> strategies =
                POLICY_MAP.get(m_columnSpec.getType().getPreferredValueClass());
        if (strategies == null) {
            throw new IllegalArgumentException("No strategy available for "
                    + m_columnSpec.getType());
        }
        for (PolicyStrategy strat : strategies) {
            if (strat.getDisplayName().equals(policy)) {
                return strat;
            }
        }
        if (strategies.size() > 0) {
            return strategies.iterator().next();
        }
        return null;
    }

    /**
     * Returns the compatible policies for this data value.
     *
     * @return the compatible policies or <code>null</code> if none.
     */
    public Set<PolicyStrategy> getCompatiblePolicies() {
        return POLICY_MAP.get(m_columnSpec.getType().getPreferredValueClass());
    }

    /**
     * Registers a strategy.
     *
     * @param dataValue the according {@link DataValue}.
     * @param strategy the {@link PolicyStrategy}
     */
    public static void addPolicy(final Class<? extends DataValue> dataValue,
            final PolicyStrategy strategy) {
        Set<PolicyStrategy> strategies = POLICY_MAP.get(dataValue);
        if (strategies == null) {
            strategies = new HashSet<PolicyStrategy>();
        }
        strategies.add(strategy);
        POLICY_MAP.put(dataValue, strategies);
    }

    /**
     * Returns the current tick policy.
     *
     * @return the current tick policy, or <code>null</code> if none.
     */
    public PolicyStrategy getCurrentPolicy() {
        return m_policy;
    }

    /**
     * Sets the current {@link PolicyStrategy}.
     *
     * @param policy the new {@link PolicyStrategy}
     * @throws IllegalArgumentException if desired policy does not fit to this
     *             coordinate
     */
    public void setPolicy(final PolicyStrategy policy) {
        if (policy != null) {
            m_policy = policy;
        } else {
            throw new IllegalArgumentException("Policy is null.");
        }
    }

    /**
     * Returns the desired values.
     *
     * @return the desired values as array or <code>null</code>, if not set
     */
    public DataValue[] getDesiredValues() {
        return m_desiredValues.toArray(new DataValue[0]);
    }

    /**
     * Adds a coordinate factory class. Warning: Existing factory classes will
     * be replaced.
     *
     * @param valueClass the data value
     * @param factory the according coordinate factory
     */
    public static void addCoordinateFactory(
            final Class<? extends DataValue> valueClass,
            final CoordinateFactory factory) {
        MAP.put(valueClass, factory);
    }

    /**
     * Factory method to create a coordinate for a given column spec. The type
     * of the column is determined and dependent on that the corresponding
     * coordinate is created.
     *
     * @param dataColumnSpec the column spec to create the coordinate from
     * @return the created coordinate, <code>null</code> if not possible
     */
    public static Coordinate createCoordinate(
            final DataColumnSpec dataColumnSpec) {
        // check the column type first it must be compatible to a double
        // value to be a numeric coordinate
        if (dataColumnSpec == null) {
            return null;
        }

        // look up in hashmap
        CoordinateFactory factory =
                MAP.get(dataColumnSpec.getType().getPreferredValueClass());
        if (factory == null) { // no preferred coordinate? look for compatible
            // one
            for (Class<? extends DataValue> c : dataColumnSpec.getType()
                    .getValueClasses()) {
                if (MAP.get(c) != null) {
                    factory = MAP.get(c);
                    break;
                }
            }
        }
        if (factory != null) {
            return factory.createCoordinate(dataColumnSpec);
        }

        // else return null
        return null;
    }

    /**
     * Creates a coordinate from a data column spec.
     *
     * @param dataColumnSpec the underlying column spec to set
     */
    protected Coordinate(final DataColumnSpec dataColumnSpec) {
        // shouldn't be public .. default!
        if (dataColumnSpec == null) {
            throw new IllegalArgumentException("Column specification shouldn't"
                    + " be null.");
        }
        m_columnSpec = dataColumnSpec;
    }

    /**
     * Returns the underlying {@link DataColumnSpec}. If {@link MappingMethod}s
     * are set, they will be applied to the domain. The domain must not be equal
     * to the original {@link DataColumnSpec}.
     *
     * @return the underlying column spec of this coordinate
     */
    DataColumnSpec getDataColumnSpec() {
        DataColumnSpecCreator creator = new DataColumnSpecCreator(m_columnSpec);
        DataCell newLowerBound =
                applyMappingMethods(m_columnSpec.getDomain().getLowerBound());
        DataCell newUpperBound =
                applyMappingMethods(m_columnSpec.getDomain().getUpperBound());
        creator.setDomain(new DataColumnDomainCreator(newLowerBound,
                newUpperBound).createDomain());
        return creator.createSpec();
    }

    /**
     * Returns an array with the position of all ticks and their corresponding
     * domain values given an absolute length.
     *
     * @param absoluteLength the absolute length the domain is mapped on
     * @param naturalMapping if <code>true</code> the mapping values are
     *            rounded to the next integer equivalent
     * @return the mapping of tick positions and corresponding domain values
     *
     * @deprecated Use {@link #getTickPositions(double)} instead.
     */
    @Deprecated
    public CoordinateMapping[] getTickPositions(final double absoluteLength,
            final boolean naturalMapping) {
        return getTickPositionsInternal(absoluteLength);
    }

    /**
     * Returns an array with the position of all ticks and their corresponding
     * domain values given an absolute length.
     *
     * @param absoluteLength the absolute length the domain is mapped on
     *
     * @return the mapping of tick positions and corresponding domain values
     */
    public CoordinateMapping[] getTickPositions(final double absoluteLength) {
        return getTickPositionsInternal(absoluteLength);
    }

    /**
     * Returns an array with the position of all ticks and their corresponding
     * domain values given an absolute length.
     *
     * @param absoluteLength the absolute length the domain is mapped on
     *
     * @return the mapping of tick positions and corresponding domain values
     */
    public abstract CoordinateMapping[] getTickPositionsInternal(
            final double absoluteLength);

    /**
     * Returns the mapping of a domain value for this coordinate axis. The
     * mapping is done according to the given absolute length.
     * <p>
     * The value is not the position on the screen. Since the java coordinate
     * system is upside down simply subtract the returned value from the screen
     * height to calculate the screen position.
     *
     * @param domainValueCell the data cell with the domain value to map
     * @param absoluteLength the absolute length on which the domain value is
     *            mapped on
     *
     * @return the mapped value
     */
    public double calculateMappedValue(final DataCell domainValueCell,
            final double absoluteLength) {
        return calculateMappedValueInternal(
                applyMappingMethods(domainValueCell), absoluteLength);
    }

    /**
     * Returns the mapping of a domain value for this coordinate axis. The
     * mapping is done according to the given absolute length.
     *
     * The value is not the position on the screen. Since the java coordinate
     * system is upside down simply subtract the returned value from the screen
     * height to calculate the screen position.
     *
     * @param domainValueCell the data cell with the domain value to map
     * @param absoluteLength the absolute length on which the domain value is
     *            mapped on
     * @param naturalMapping if <code>true</code> the return value will be a
     *            double but with zeros after the decimal dot
     * @return the mapped value
     *
     * @deprecated Use {@link #calculateMappedValue(DataCell, double)} instead.
     */
    @Deprecated
    public double calculateMappedValue(final DataCell domainValueCell,
            final double absoluteLength, final boolean naturalMapping) {
        return calculateMappedValue(domainValueCell, absoluteLength);
    }

    /**
     * Returns the mapping of a domain value for this coordinate axis. The
     * mapping is done according to the given absolute length.
     * <p>
     * The value is not the position on the screen. Since the java coordinate
     * system is upside down simply subtract the returned value from the screen
     * height to calculate the screen position.
     *
     * @param domainValueCell the data cell with the domain value to map
     * @param absoluteLength the absolute length on which the domain value is
     *            mapped on
     *
     * @return the mapped value
     */
    public abstract double calculateMappedValueInternal(
            final DataCell domainValueCell, final double absoluteLength);

    /**
     * Whether this coordinate is a nominal one. Nominal coordinates must be
     * treated differently in some cases, i.e. when rendering in a scatterplot
     * nominal values are very likely to be drawn above each other which
     * requires jittering.
     *
     * @return <code>true</code>, if this coordinate is a nominal one
     */
    public abstract boolean isNominal();

    /**
     * Returns the range according to the mapping in which no values can have
     * values. This distance will not occur in floating point numbers. For
     * nominal values it is most likely to occur. For discrete values like
     * integers, it will happen when the integer range is smaller than the
     * available pixels.
     *
     * @param absoluteLength the absolute length available for this coordinate
     *
     * @return the unused mapping range per domain value
     */
    public abstract double getUnusedDistBetweenTicks(double absoluteLength);

    /**
     * Adds a {@link MappingMethod} to the internal registry.
     *
     * @param clazz the according class, must extend {@link DataValue}
     * @param mappingMethod a {@link MappingMethod}
     */
    public static void addMappingMethod(final Class<? extends DataValue> clazz,
            final MappingMethod mappingMethod) {
        Set<MappingMethod> mapMethods = MAPPING_METHODS.get(clazz);
        if (mapMethods == null) {
            mapMethods = new HashSet<MappingMethod>();
        }
        mapMethods.add(mappingMethod);
        MAPPING_METHODS.put(clazz, mapMethods);
    }

    /**
     * Returns the mapping methods compatible to this coordinate's data type.
     *
     * @return a {@link Set} of {@link MappingMethod}s
     */
    public Set<MappingMethod> getCompatibleMappingMethods() {
        return MAPPING_METHODS.get(getDataColumnSpec().getType()
                .getPreferredValueClass());
    }

    /**
     * Sets the mapping methods which should be applied.
     *
     * @param methods a {@link List} of {@link MappingMethod}s
     */
    public void setActiveMappingMethods(final List<MappingMethod> methods) {
        m_activeMethods.clear();
        if (methods != null) {
            m_activeMethods.addAll(methods);
        }
    }

    /**
     * Applies the mapping methods.
     *
     * @param datacell value to be mapped
     * @return the mapped value
     */
    protected DataCell applyMappingMethods(final DataCell datacell) {
        DataCell cell = datacell;
        if (m_activeMethods.size() > 0) {
                for (MappingMethod method : m_activeMethods) {
                    cell = method.doMapping(cell);
                }
        }
        return cell;
    }

    /**
     * Returns the domain used in this moment after applying active mapping
     * methods. This must not be equal to the original domain!
     *
     * @return the domain
     */
    public DataColumnDomain getDomain() {
        return getDataColumnSpec().getDomain();
    }

    /**
     * Returns the value for positive infinity after mapping.
     * Necessary to recognize infinity after scaling e.g..
     *
     * @return the value for positive infinity.
     */
    public double getPositiveInfinity() {
        return Double.POSITIVE_INFINITY;
    }

    /**
     * Returns the value for negative infinity after mapping.
     * Necessary to recognize infinity after scaling e.g..
     *
     * @return the value for negative infinity.
     */
    public double getNegativeInfinity() {
        return Double.NEGATIVE_INFINITY;
    }
}
