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
 *   04.06.2018 (Jonathan Hale): created
 */
package org.knime.core.data.convert.map;

import java.util.ArrayList;
import java.util.List;

import org.knime.core.data.DataType;
import org.knime.core.data.convert.AbstractConverterFactoryRegistry;
import org.knime.core.data.convert.java.DataCellToJavaConverterFactory;
import org.knime.core.data.convert.java.DataCellToJavaConverterRegistry;

/**
 * Per destination type consumer registry.
 *
 * Place to register consumers for a specific destination type.
 *
 * @author Jonathan Hale, KNIME, Konstanz, Germany
 * @param <D> Type of {@link Destination} for which this registry holds consumers.
 * @param <ET> External type
 * @since 3.6
 */
public class ConsumerRegistry<ET, D extends Destination<ET>> extends
    AbstractConverterFactoryRegistry<Class<?>, ET, CellValueConsumerFactory<D, ?, ET, ?>, ConsumerRegistry<ET, D>> {

    /**
     * Constructor
     */
    protected ConsumerRegistry() {
    }

    /**
     * Set parent destination type.
     *
     * Makes this registry inherit all consumers of the parent type. Will always priorize consumers of the more
     * specialized type.
     *
     * @param parentType type of {@link Destination}, which should be this types parent.
     * @return reference to self (for method chaining)
     * @param <PT> Type of parent registry
     */
    public <PT extends Destination<ET>> ConsumerRegistry<ET, D>
        setParent(final Class<PT> parentType) {
        m_parent = (ConsumerRegistry<ET, D>)MappingFramework.forDestinationType(parentType);
        return this;
    }

    /**
     * @param type Data type that should be converted.
     * @return List of conversion paths
     */
    public List<ConsumptionPath> getAvailableConsumptionPaths(final DataType type) {
        final ArrayList<ConsumptionPath> consumptionPaths = new ArrayList<>();

        for (final DataCellToJavaConverterFactory<?, ?> converterFactory : DataCellToJavaConverterRegistry.getInstance()
            .getFactoriesForSourceType(type)) {
            for (final CellValueConsumerFactory<D, ?, ?, ?> consumerFactory : getFactoriesForSourceType(
                converterFactory.getDestinationType())) {
                if (consumerFactory != null) {
                    consumptionPaths.add(new ConsumptionPath(converterFactory, consumerFactory));
                }
            }
        }

        return consumptionPaths;
    }

    /**
     * Unregister all consumers
     *
     * @return self (for method chaining)
     */
    public ConsumerRegistry<ET, D> unregisterAllConsumers() {
        m_byDestinationType.clear();
        m_bySourceType.clear();
        m_byIdentifier.clear();
        m_factories.clear();
        return this;
    }
}