/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ---------------------------------------------------------------------
 *
 */

package org.knime.core.data.convert.datacell;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellFactory;
import org.knime.core.data.DataType;
import org.knime.core.data.MissingCell;
import org.knime.core.data.convert.DataCellFactoryMethod;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeLogger;

/**
 * Implementation of {@link JavaToDataCellConverterFactory} using a {@link DataCellFactory} and one of its methods which
 * creates a {@link DataCell} from a Java {@link Object}. Used by {@link JavaToDataCellConverterRegistry} to create
 * converters from methods annotated with {@link DataCellFactoryMethod} in {@link DataCellFactory} implementations.
 *
 * @author Jonathan Hale
 *
 * @param <F> DataCellFactory type
 * @param <S> Source type
 * @since 3.2
 */
class FactoryMethodToDataCellConverterFactory<F extends DataCellFactory, S>
    implements JavaToDataCellConverterFactory<S> {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(FactoryMethodToDataCellConverterFactory.class);

    private final Constructor<F> m_factoryConstructor;

    private final Method m_method;

    private final Class<S> m_srcType;

    private final DataType m_destDataType;

    private final String m_metaType;

    private static final MissingCell MISSING = new MissingCell("Value was null.");

    /**
     * Creates a new converter from an annotated factory method to a data cell converter factory.
     *
     * @param factoryClass Class of the {@link DataCellFactory} used to create {@link DataCell DataCells}
     * @param method Method of <code>factoryClass</code> which will be called. Its return type must be assignable to
     *            {@link DataCell} and the first parameter should be assignable from <code>sourceType</code>.
     * @param sourceType type which can be converted by the converters produced by this factory.
     * @param destDataType destination DataType
     * @param type metatype of the sourceType
     * @throws NoSuchMethodException if an expected constructor of the factory class does not exist
     * @throws SecurityException if the constructor of the factory class cannot be accessed
     */
    FactoryMethodToDataCellConverterFactory(final Class<F> factoryClass, final Method method, final Class<S> sourceType,
        final DataType destDataType, final String type) throws NoSuchMethodException, SecurityException {
        // there should be exactly one parameter to FactoryMethods
        assert method.getParameterTypes().length == 1;
        // the sourceType has to match the parameter type
        // assert method.getParameterTypes()[0].isAssignableFrom(sourceType);
        // the return value needs to be a superclass of DataCell
        assert DataCell.class.isAssignableFrom(method.getReturnType());

        m_method = method;
        m_srcType = sourceType;
        m_destDataType = destDataType;
        m_metaType = type;

        Constructor<F> factoryContructor = null;
        try {
            /*
             * try getting a constructor with ExecutionContext parameter
             * first
             */
            factoryContructor = factoryClass.getConstructor(ExecutionContext.class);
        } catch (NoSuchMethodException e) {
            /* some DataCellFactories may only have default constructor */
            factoryContructor = factoryClass.getConstructor();
            /* if neither, another NoSuchMethodException will be thrown */
        }
        factoryContructor.setAccessible(true);
        m_factoryConstructor = factoryContructor;
    }

    @Override
    public JavaToDataCellConverter<S> create(final ExecutionContext context) {
        F factory = null;
        try {
            if (m_factoryConstructor.getParameterCount() == 0) {
                // some factories may only have default constructor
                factory = m_factoryConstructor.newInstance();
            } else {
                // call constructor with ExecutionContext parameter
                factory = m_factoryConstructor.newInstance(context);
            }
        } catch (final InstantiationException e) {
            // we were given a abstract class
            LOGGER.error("Class '" + m_factoryConstructor.getDeclaringClass().getName() + "' cannot be instantiated: "
                + e.getClass() + ": " + e.getMessage(), e);
            throw new RuntimeException(e);
        } catch (final IllegalAccessException e) {
            // will not happen since setAccessible was called on constructor
            LOGGER.error("Unexpected IllegalAccessException: " + e.getMessage(), e);
        } catch (final IllegalArgumentException e) {
            // will not happen, since checked in assertions in constructor
            LOGGER.error("Unexpected IllegalArgumentException: " + e.getMessage(), e);
        } catch (final InvocationTargetException e) {
            LOGGER.error("Constructor of '" + m_factoryConstructor.getDeclaringClass().getName()
                + "' threw an exception: " + e.getClass() + ": " + e.getMessage(), e);
            throw new RuntimeException(e);
        }
        final F finalFactory = factory;
        return new JavaToDataCellConverter<S>() {

            private final DataCellFactory m_factory = finalFactory;

            @Override
            public DataCell convert(final S source) throws Exception {
                if (source == null) {
                    return MISSING;
                }

                /*
                 * After about default 15 of these calls (can be set by
                 * sun.reflect.inflationThreshold), this should be optimized via
                 * "reflection inflation" which is enabled by default (ensure
                 * sun.reflect.noInflation is false)
                 *
                 * The following is equivalent to
                 * m_factory.factoryMethod(source)
                 */
                return (DataCell)m_method.invoke(m_factory, source);
            }
        };
    }

    @Override
    public Class<S> getSourceType() {
        return m_srcType;
    }

    @Override
    public DataType getDestinationType() {
        return m_destDataType;
    }

    @Override
    public String getName() {
        return m_metaType;
    }

    @Override
    public String toString() {
        return "FactoryMethodToDataCellConverterFactory(" + getIdentifier() + ")";
    }

    @Override
    public String getIdentifier() {
        return m_factoryConstructor.getDeclaringClass().getName() + "." + m_method.getName() + "(" + getSourceType()
            + ")";
    }
}
