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

import org.knime.core.data.convert.AbstractConverterFactoryRegistry;
import org.knime.core.data.convert.datacell.JavaToDataCellConverterFactory;
import org.knime.core.data.convert.datacell.JavaToDataCellConverterRegistry;

/**
 * Per source type producer registry.
 *
 * Place to register consumers for a specific destination type.
 *
 * @author Jonathan Hale, KNIME, Konstanz, Germany
 * @param <ExternalType> Type of the external type
 * @param <SourceType> Type of {@link Destination} for which this registry holds consumers.
 * @since 3.6
 */
public class ProducerRegistry<ExternalType, SourceType extends Source<ExternalType>> extends
    AbstractConverterFactoryRegistry<ExternalType, Class<?>, CellValueProducerFactory<SourceType, ExternalType, ?, ?>, ProducerRegistry<ExternalType, SourceType>> {

    /**
     * Constructor
     */
    protected ProducerRegistry() {
    }

    /**
     * Set parent source type.
     *
     * Makes this registry inherit all producers of the parent type. Will always priorize producers of the more
     * specialized type.
     *
     * @param parentType type of {@link Destination}, which should be this types parent.
     * @return reference to self (for method chaining)
     */
    public ProducerRegistry<ExternalType, SourceType> setParent(final Class<? extends Source> parentType) {
        m_parent = MappingFramework.forSourceType(parentType);
        return this;
    }

    /**
     * Get production paths that can map the given external type to a DataCell.
     *
     * @param externalType The external type
     * @return All possible production paths
     */
    public List<ProductionPath> getAvailableProductionPaths(final ExternalType externalType) {
        final ArrayList<ProductionPath> cp = new ArrayList<>();

        for (final CellValueProducerFactory<SourceType, ExternalType, ?, ?> producerFactory : getFactoriesForSourceType(
            externalType)) {

            for (final JavaToDataCellConverterFactory<?> f : JavaToDataCellConverterRegistry.getInstance()
                .getFactoriesForSourceType(producerFactory.getDestinationType())) {
                cp.add(new ProductionPath(producerFactory, f));
            }
        }

        if (m_parent != null) {
            cp.addAll(m_parent.getAvailableProductionPaths(externalType));
        }

        return cp;
    }

    /**
     * Unregister all consumers
     *
     * @return self (for method chaining)
     */
    public ProducerRegistry<ExternalType, SourceType> unregisterAllProducers() {
        m_byDestinationType.clear();
        m_bySourceType.clear();
        m_byIdentifier.clear();
        m_factories.clear();
        return this;
    }
}