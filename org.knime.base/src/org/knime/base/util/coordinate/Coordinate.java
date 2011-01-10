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
 *
 * History
 *   02.02.2006 (sieb): created
 *   20.03.2008 (sellien): redesigned
 */
package org.knime.base.util.coordinate;

import java.util.HashMap;
import java.util.HashSet;
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

    private static final Map<Class<? extends DataValue>, CoordinateFactory> MAP =
            new HashMap<Class<? extends DataValue>, CoordinateFactory>();

    private static final Map<Class<? extends DataValue>, Set<PolicyStrategy>> POLICY_MAP =
            new HashMap<Class<? extends DataValue>, Set<PolicyStrategy>>();

    private static final Map<String, PolicyStrategy> POLICY_ID_MAP =
            new HashMap<String, PolicyStrategy>();

    private static final Map<Class<? extends DataValue>, Set<MappingMethod>> MAPPING_METHODS =
            new HashMap<Class<? extends DataValue>, Set<MappingMethod>>();

    private static final Map<String, MappingMethod> MAPPING_ID =
            new HashMap<String, MappingMethod>();

    static {
        addCoordinateFactory(DoubleValue.class, new DoubleCoordinateFactory());
        addCoordinateFactory(IntValue.class, new IntegerCoordinateFactory());
        addCoordinateFactory(StringValue.class, new NominalCoordinateFactory());

        AscendingNumericTickPolicyStrategy ascending =
                new AscendingNumericTickPolicyStrategy();

        DescendingNumericTickPolicyStrategy descending =
                new DescendingNumericTickPolicyStrategy();

        PercentagePolicyStrategy percentage = new PercentagePolicyStrategy();

        addPolicy(DoubleValue.class, AscendingNumericTickPolicyStrategy.ID,
                ascending);
        addPolicy(DoubleValue.class, DescendingNumericTickPolicyStrategy.ID,
                descending);
        addPolicy(DoubleValue.class, PercentagePolicyStrategy.ID, percentage);

        LogarithmicMappingMethod lnMappingMethod =
                new LogarithmicMappingMethod();
        LogarithmicMappingMethod log10MappingMethod =
                new LogarithmicMappingMethod(10);
        LogarithmicMappingMethod ldMappingMethod =
                new LogarithmicMappingMethod(2);

        addMappingMethod(DoubleValue.class, LogarithmicMappingMethod.ID_BASE_E,
                lnMappingMethod);
        addMappingMethod(DoubleValue.class,
                LogarithmicMappingMethod.ID_BASE_10, log10MappingMethod);
        addMappingMethod(DoubleValue.class, LogarithmicMappingMethod.ID_BASE_2,
                ldMappingMethod);

        SquareRootMappingMethod sqrt = new SquareRootMappingMethod();
        addMappingMethod(
                DoubleValue.class, SquareRootMappingMethod.ID_SQRT, sqrt);

    }

    private final Set<DataValue> m_desiredValues = new HashSet<DataValue>();

    private MappingMethod m_activeMethod = null;

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
        Set<PolicyStrategy> strategies = new HashSet<PolicyStrategy>();
        for (Class<? extends DataValue> cl : m_columnSpec.getType()
                .getValueClasses()) {
            Set<PolicyStrategy> temp = POLICY_MAP.get(cl);
            if (temp != null && temp.size() > 0) {
                strategies.addAll(temp);
            }
        }
        if (strategies.size() == 0) {
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
        Set<PolicyStrategy> strategies = new HashSet<PolicyStrategy>();
        for (Class<? extends DataValue> cl : m_columnSpec.getType()
                .getValueClasses()) {
            Set<PolicyStrategy> temp = POLICY_MAP.get(cl);
            if (temp != null && temp.size() > 0) {
                strategies.addAll(temp);
            }
        }

        return strategies;
    }

    /**
     * Registers a strategy.
     *
     * @param dataValue the according {@link DataValue}.
     * @param id a unique identifier
     * @param strategy the {@link PolicyStrategy}
     */
    public static void addPolicy(final Class<? extends DataValue> dataValue,
            final String id, final PolicyStrategy strategy) {
        Set<PolicyStrategy> strategies = POLICY_MAP.get(dataValue);
        if (strategies == null) {
            strategies = new HashSet<PolicyStrategy>();
        }
        strategies.add(strategy);
        POLICY_MAP.put(dataValue, strategies);
        POLICY_ID_MAP.put(id, strategy);
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
     * Sets the current {@link PolicyStrategy}.
     *
     * @param id the unique identifier of the desired policy
     * @throws IllegalArgumentException if desired policy does not exist.
     */
    public void setPolicy(final String id) {
        PolicyStrategy policy = POLICY_ID_MAP.get(id);
        if (policy != null) {
            m_policy = policy;
        } else {
            throw new IllegalArgumentException("Policy " + id
                    + "does not exist.");
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
                applyMappingMethod(m_columnSpec.getDomain().getLowerBound());
        DataCell newUpperBound =
                applyMappingMethod(m_columnSpec.getDomain().getUpperBound());
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
        return getTickPositionsWithLabels(absoluteLength);
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
        return getTickPositionsWithLabels(absoluteLength);
    }

    /**
     * Returns an array with the position of all ticks and their corresponding
     * domain values given an absolute length.
     *
     * @param absoluteLength the absolute length the domain is mapped on
     *
     * @return the mapping of tick positions and corresponding domain values
     */
    protected abstract CoordinateMapping[] getTickPositionsWithLabels(
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
                applyMappingMethod(domainValueCell), absoluteLength);
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
    protected abstract double calculateMappedValueInternal(
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
     * @param id a unique identifier
     * @param mappingMethod a {@link MappingMethod}
     */
    public static void addMappingMethod(final Class<? extends DataValue> clazz,
            final String id, final MappingMethod mappingMethod) {
        Set<MappingMethod> mapMethods = MAPPING_METHODS.get(clazz);
        if (mapMethods == null) {
            mapMethods = new HashSet<MappingMethod>();
        }
        mapMethods.add(mappingMethod);
        MAPPING_METHODS.put(clazz, mapMethods);
        MAPPING_ID.put(id, mappingMethod);
    }

    /**
     * Returns the mapping methods compatible to this coordinate's data type.
     *
     * @return a {@link Set} of {@link MappingMethod}s
     */
    public Set<MappingMethod> getCompatibleMappingMethods() {
        Set<MappingMethod> methods = new HashSet<MappingMethod>();
        for (Class<? extends DataValue> cl : m_columnSpec.getType()
                .getValueClasses()) {
            Set<MappingMethod> temp = MAPPING_METHODS.get(cl);
            if (temp != null && temp.size() > 0) {
                methods.addAll(temp);
            }
        }

        return methods;
    }

    /**
     * Returns the {@link MappingMethod} with the given id if available.
     *
     * @param id the unique identifier
     * @return a {@link MappingMethod} or <code>null</code> if id is not
     *         registered.
     */
    public MappingMethod getMappingMethod(final String id) {
        return MAPPING_ID.get(id);
    }

    /**
     * Sets the mapping method which should be applied.
     *
     * @param method a {@link MappingMethod}
     */
    public void setActiveMappingMethod(final MappingMethod method) {
        m_activeMethod = method;
    }

    /**
     * Gets the mapping method which should be applied.
     *
     * @return the {@link MappingMethod} which currently will be applied or
     *         <code>null</code> if none
     */
    public MappingMethod getActiveMappingMethod() {
        return m_activeMethod;
    }

    /**
     * Applies the mapping method.
     *
     * @param datacell value to be mapped
     * @return the mapped value
     */
    protected DataCell applyMappingMethod(final DataCell datacell) {

        // System.out.print("Mapping " + datacell);

        DataCell cell = datacell;
        if (m_activeMethod != null) {
            cell = m_activeMethod.doMapping(cell);
        }
        // System.out.println(" to: " + cell);
        return cell;
    }

    /**
     * Returns the domain used in this moment after applying the active mapping
     * method. This must not be equal to the original domain!
     *
     * @return the domain
     */
    public DataColumnDomain getDomain() {
        return getDataColumnSpec().getDomain();
    }

    /**
     * Returns the value for positive infinity after mapping. Necessary to
     * recognize infinity after scaling e.g..
     *
     * @return the value for positive infinity.
     */
    public double getPositiveInfinity() {
        return Double.POSITIVE_INFINITY;
    }

    /**
     * Returns the value for negative infinity after mapping. Necessary to
     * recognize infinity after scaling e.g..
     *
     * @return the value for negative infinity.
     */
    public double getNegativeInfinity() {
        return Double.NEGATIVE_INFINITY;
    }
}
