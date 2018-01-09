/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 * ---------------------------------------------------------------------
 *
 * History
 *   05.09.2016 (Jonathan Hale): created
 */
package org.knime.base.node.jsnippet.type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.convert.datacell.JavaToDataCellConverterFactory;
import org.knime.core.data.convert.datacell.JavaToDataCellConverterRegistry;
import org.knime.core.data.convert.java.DataCellToJavaConverterFactory;
import org.knime.core.data.convert.java.DataCellToJavaConverterRegistry;
import org.knime.core.data.date.DateAndTimeCell;

/**
 * Utility class to keep all calls to the {@link DataCellToJavaConverterRegistry} and
 * {@link JavaToDataCellConverterRegistry} in one place.
 *
 * This is where return values can be adjusted to handle deprecated {@link DataValue} types for example.
 *
 * @author Jonathan Hale, KNIME.com, Konstanz, Germany
 * @noreference This class is not intended to be referenced by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noextend This class is not intended to be subclassed by clients.
 */
public class ConverterUtil {

    /**
     * Convenience method to get the first {@link JavaToDataCellConverterFactory} returned by
     * {@link JavaToDataCellConverterRegistry#getConverterFactories(Class, DataType)}.
     *
     * @param source type which should be convertible
     * @param dest type to which should be converted
     * @return the preferred {@link JavaToDataCellConverterFactory} for given <code>sourceType</code> and
     *         <code>destType</code>.
     */
    public static Optional<JavaToDataCellConverterFactory<?>> getConverterFactory(final Class<?> source,
        final DataType dest) {
        Collection<?> converterFactories =
            JavaToDataCellConverterRegistry.getInstance().getConverterFactories(source, dest);
        Optional<?> factory = converterFactories.stream().findFirst();
        if (!factory.isPresent()) {
            // check deprecated types
            return DeprecatedDateAndTimeUtil.getConverterFactory(source, dest);
        }
        return (Optional<JavaToDataCellConverterFactory<?>>)factory;
    }

    /**
     * Convenience method to get the first {@link DataCellToJavaConverterFactory} returned by
     * {@link DataCellToJavaConverterRegistry#getConverterFactories(DataType, Class)}, which creates a converter that is
     * able to convert the preferred data value of the given sourceType into the given destType. Since {@link DataCell
     * DataCells} of a certain {@link DataType} are required to implement the preferred {@link DataValue} interface, the
     * resulting converter is therefore guaranteed to be able to convert {@link DataCell DataCells} of the requested
     * type.
     *
     * @param knimeType type which should be convertible
     * @param javaType type to which should be converted
     * @return the preferred {@link DataCellToJavaConverterFactory} for given <code>sourceType</code> and
     *         <code>destType</code>.
     */
    public static Optional<DataCellToJavaConverterFactory<?, ?>> getConverterFactory(final DataType knimeType,
        final Class<?> javaType) {
        final Optional<?> factory =
            DataCellToJavaConverterRegistry.getInstance().getPreferredConverterFactory(knimeType, javaType);
        if (!factory.isPresent()) {
            return DeprecatedDateAndTimeUtil.getConverterFactory(knimeType.getPreferredValueClass(), javaType);
        }
        return (Optional<DataCellToJavaConverterFactory<?, ?>>)factory;
    }

    /**
     * Query into which DataTypes can be converted. Returned list is sorted, since this is only used for UI.
     *
     * @return a {@link Collection} of all possible data types into which can be converted.
     */
    public static Collection<DataType> getAllDestinationDataTypes() {
        final ArrayList<DataType> destinationTypes =
            new ArrayList<DataType>(JavaToDataCellConverterRegistry.getInstance().getAllDestinationTypes());
        destinationTypes.add(DateAndTimeCell.TYPE);

        // sort output alphabetically for nicer display in UI
        destinationTypes.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));

        return destinationTypes;
    }

    /**
     * Get all {@link DataCellToJavaConverterFactory converter factories} which create converter which convert into a
     * specific {@link Class destType}.
     *
     * @param sourceType DataType to query converter factories for
     * @return a {@link Collection} of all possible source types which can be converted into the given
     *         <code>destType</code>. The first is always the preferred type.
     */
    public static Collection<DataCellToJavaConverterFactory<?, ?>>
        getFactoriesForSourceType(final DataType sourceType) {
        if (sourceType == DateAndTimeCell.TYPE) {
            return Arrays.asList(DeprecatedDateAndTimeUtil.toCalendarConverterFactory,
                DeprecatedDateAndTimeUtil.toDateConverterFactory);
        } else if (sourceType.isCollectionType() && sourceType.getCollectionElementType() == DateAndTimeCell.TYPE) {
            DataCellToJavaConverterRegistry registry = DataCellToJavaConverterRegistry.getInstance();
            return Arrays.asList(
                registry.getCollectionConverterFactory(DeprecatedDateAndTimeUtil.toCalendarConverterFactory),
                registry.getCollectionConverterFactory(DeprecatedDateAndTimeUtil.toDateConverterFactory));
        }

        return DataCellToJavaConverterRegistry.getInstance().getFactoriesForSourceType(sourceType);
    }

    /**
     * Get the {@link DataCellToJavaConverterFactory} with the given identifier.
     *
     * @param id unique identifier for the factory
     * @return an optional of a converter factory or empty if no converter factory with given id could be found
     */
    public static Optional<DataCellToJavaConverterFactory<?, ?>> getDataCellToJavaConverterFactory(final String id) {
        final Optional<DataCellToJavaConverterFactory<?, ?>> factory =
            DataCellToJavaConverterRegistry.getInstance().getConverterFactory(id);

        if (!factory.isPresent()) {
            return DeprecatedDateAndTimeUtil.getDataCellToJavaConverterFactory(id);
        }

        return factory;
    }

    /**
     * Get the {@link JavaToDataCellConverterFactory} with the given identifier.
     *
     * @param id unique identifier for the factory
     * @return an optional of a converter factory or empty if no converter factory with given id could be found
     */
    public static Optional<JavaToDataCellConverterFactory<?>> getJavaToDataCellConverterFactory(final String id) {
        final Optional<JavaToDataCellConverterFactory<?>> factory =
            JavaToDataCellConverterRegistry.getInstance().getConverterFactory(id);

        if (!factory.isPresent()) {
            return DeprecatedDateAndTimeUtil.getJavaToDataCellConverterFactory(id);
        }

        return factory;
    }

    /**
     * @return a collection of all available {@link JavaToDataCellConverterFactory}
     */
    public static Collection<JavaToDataCellConverterFactory<?>> getAllJavaToDataCellConverterFactories() {
        ArrayList<JavaToDataCellConverterFactory<?>> factories =
            new ArrayList<>(JavaToDataCellConverterRegistry.getInstance().getAllFactories());
        factories.addAll(DeprecatedDateAndTimeUtil.getAllJavaToDataCellConverterFactories());
        return factories;
    }

    /**
     * Get all converter factories for given destination {@link DataType}.
     *
     * @param dataType Destination type
     * @return All {@link JavaToDataCellConverterFactory} available for given dataType
     */
    public static Collection<JavaToDataCellConverterFactory<?>>
        getFactoriesForDestinationType(final DataType dataType) {
        if (dataType == DateAndTimeCell.TYPE) {
            return DeprecatedDateAndTimeUtil.getAllJavaToDataCellConverterFactories();
        } else if (dataType.isCollectionType() && dataType.getCollectionElementType() == DateAndTimeCell.TYPE) {
            return DeprecatedDateAndTimeUtil.getAllJavaToDataCellConverterFactories().stream()
                    .map(s -> JavaToDataCellConverterRegistry.getInstance().getArrayConverterFactory(s))
                    .collect(Collectors.toList());
        }
        return JavaToDataCellConverterRegistry.getInstance().getFactoriesForDestinationType(dataType);
    }

    /**
     * The converter framework cannot provide the guarantee for a preferred conversion from java type to a data cell. We
     * try to make an educated guess by finding the destination type of the DataCellToJavaConverter (opposite of
     * requested) and returning a converter which converts that into the given data type, if available.
     *
     * @param dataType Destination type
     * @return All {@link JavaToDataCellConverterFactory} available for given dataType
     */
    public static Optional<JavaToDataCellConverterFactory<?>> getPreferredFactoryForDestinationType(final DataType dataType) {
        if (dataType == DateAndTimeCell.TYPE) {
            return DeprecatedDateAndTimeUtil.getAllJavaToDataCellConverterFactories().stream().findFirst();
        }

        // try to find the preferred dest type:
        final Optional<DataCellToJavaConverterFactory<?, ?>> first =
            DataCellToJavaConverterRegistry.getInstance().getFactoriesForSourceType(dataType).stream().findFirst();

        if (first.isPresent()) {
            // use the factories destination type as source type for conversion *to* dataType
            final Class<?> sourceType = first.get().getDestinationType();
            final Optional<?> factory = JavaToDataCellConverterRegistry.getInstance()
                .getConverterFactories(sourceType, dataType).stream().findFirst();

            if (factory.isPresent()) {
                return Optional.of((JavaToDataCellConverterFactory<?>)factory.get());
            }
        }
        // Fallback: Get first factory available.
        return JavaToDataCellConverterRegistry.getInstance().getFactoriesForDestinationType(dataType).stream().findFirst();
    }

}
