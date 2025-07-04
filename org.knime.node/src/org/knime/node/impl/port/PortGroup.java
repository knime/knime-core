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
 *   Jul 3, 2025 (Paul Bärnreuther): created
 */
package org.knime.node.impl.port;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.knime.core.node.ConfigurableNodeFactory;
import org.knime.core.node.port.PortType;
import org.knime.node.DefaultNodeFactory;
import org.knime.node.impl.description.PortDescription;

/**
 * A port group is a definition of a set of ports and how they can be configured. E.g. a port group can consist of a
 * single optional input port of a fixed type or an extendable set of arbitrary typed ports both at the input and the
 * output of a node.
 *
 * @author Paul Bärnreuther
 */
public interface PortGroup {

    /**
     * A port group has a unique identifier.
     *
     * @return the unique identifier that is used to retrieve the indices of the ports of this group.
     */
    String getId();

    /**
     * see {@link PortGroupLocation}
     *
     * @return the location of ports
     */
    PortGroupLocation getLocation();

    /**
     * A port group defines the description of its ports.
     *
     * @param location to describe (i.e. input or output, there are no descriptions for both sides)
     * @return the description of ports of this group at that location. In case the group cannot have such ports the
     *         returned value is empty.
     */
    Optional<PortDescription> getDescription(PortGroup.PortLocation location);

    /**
     * Method used by the {@link DefaultNodeFactory} for legacy reasons (the builder that can be used to construct
     * configurable ports is protected in {@link ConfigurableNodeFactory}.
     *
     * @param lambdas a set of methods for adding port groups (e.g. to a builder)
     */
    void addToPortGroupConfiguration(PortGroup.PortGroupConfigBuilderLambdas lambdas);

    /**
     * The location of a concrete port.
     */
    enum PortLocation {
            /**
             * Input port
             */
            INPUT,
            /**
             * Output port
             */
            OUTPUT;
    }

    /**
     * In contrast to {@link PortLocation} a port group can span both sides.
     *
     * @author Paul Bärnreuther
     */
    enum PortGroupLocation {
            /**
             * Input port group, all of whose ports are input ports.
             */
            INPUT,
            /**
             * Output port group, all of whose ports are output ports.
             */
            OUTPUT,
            /**
             * Paired input-and-output port group. When a port is added/modified by the user, an identical operation is
             * performed in the opposite side.
             */
            BOTH_SIDES;
    }

    /**
     * Record holding all builder lambdas for each port group type
     */
    @SuppressWarnings("javadoc")
    record PortGroupConfigBuilderLambdas(//
        Consumer<PortType> addFixedInputPortGroup, //
        Consumer<PortType> addFixedOutputPortGroup, //
        Consumer<PortType> addFixedPortGroup, //
        // OptionalPortGroup
        BiConsumer<PortType, PortType[]> addOptionalInputPortGroupWithDefault,
        BiConsumer<PortType, PortType[]> addOptionalOutputPortGroupWithDefault,
        BiConsumer<PortType, PortType[]> addOptionalPortGroupWithDefault,
        BiConsumer<PortType, Predicate<PortType>> addOptionalInputPortGroupWithDefaultPredicate,
        BiConsumer<PortType, Predicate<PortType>> addOptionalOutputPortGroupWithDefaultPredicate,
        BiConsumer<PortType, Predicate<PortType>> addOptionalPortGroupWithDefaultPredicate,
        // ExtendablePortGroup
        TriConsumer<PortType[], PortType[], PortType[]> addExtendableInputPortGroupWithDefault,
        TriConsumer<PortType[], PortType[], PortType[]> addExtendableOutputPortGroupWithDefault,
        TriConsumer<PortType[], PortType[], PortType[]> addExtendablePortGroupWithDefault,
        TriConsumer<PortType[], PortType[], Predicate<PortType>> addExtendableInputPortGroupWithDefaultPredicate,
        TriConsumer<PortType[], PortType[], Predicate<PortType>> addExtendableOutputPortGroupWithDefaultPredicate,
        TriConsumer<PortType[], PortType[], Predicate<PortType>> addExtendablePortGroupWithDefaultPredicate,
        // BoundExtendablePortGroup
        TriConsumer<String, Integer, Integer> addBoundExtendableInputPortGroupWithDefault,
        TriConsumer<String, Integer, Integer> addBoundExtendableOutputPortGroupWithDefault,
        TriConsumer<String, Integer, Integer> addBoundExtendablePortGroupWithDefault,
        // ExchangablePortGroup
        BiConsumer<PortType, PortType[]> addExchangeableInputPortGroup,
        BiConsumer<PortType, PortType[]> addExchangeableOutputPortGroup,
        BiConsumer<PortType, PortType[]> addExchangeablePortGroup,
        BiConsumer<PortType, Predicate<PortType>> addExchangeableInputPortGroupPredicate,
        BiConsumer<PortType, Predicate<PortType>> addExchangeableOutputPortGroupPredicate,
        BiConsumer<PortType, Predicate<PortType>> addExchangeablePortGroupPredicate) {
    }

    @SuppressWarnings("javadoc")
    @FunctionalInterface
    interface TriConsumer<T, U, V> {
        void accept(T t, U u, V v);

    }

}
