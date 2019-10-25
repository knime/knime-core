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
 *   Oct 11, 2019 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.core.node.context.ports.impl;

import java.util.LinkedHashMap;

import org.knime.core.node.context.ports.ModifiablePortsConfiguration;
import org.knime.core.node.context.ports.PortGroupConfiguration;
import org.knime.core.node.port.PortType;
import org.knime.core.node.util.CheckUtils;

/**
 * Builder to create an instance of @link {@link PortGroupConfiguration port group configurations}. Note that the order
 * how the port groups are added directly affect the node's port order.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 * @since 4.1
 */
public final class PortsConfigurationBuilder {

    private final LinkedHashMap<String, PortGroupConfiguration> m_portConfigs;

    /**
     * Constructor.
     */
    public PortsConfigurationBuilder() {
        m_portConfigs = new LinkedHashMap<>();
    }

    /**
     * Creates the {@code IPortsConfiguration}.
     *
     * @return an instance of {@code IPortsConfiguration}
     * @noreference This method is not intended to be referenced by clients.
     */
    public ModifiablePortsConfiguration build() {
        return new DefaultModifiablePortsConfiguration(m_portConfigs);
    }

    /**
     * Adds a static input port group configuration.
     *
     * @param pGrpIdentifier the port group identifier
     * @param staticPortTypes the static port types
     */
    public void addStaticInputPortGroup(final String pGrpIdentifier, final PortType... staticPortTypes) {
        addStaticPortGroup(pGrpIdentifier, staticPortTypes, true, false);
    }

    /**
     * Adds a static output port group configuration.
     *
     * @param pGrpIdentifier the port group identifier
     * @param staticPortTypes the static port types
     */
    public void addStaticOutputPortGroup(final String pGrpIdentifier, final PortType... staticPortTypes) {
        addStaticPortGroup(pGrpIdentifier, staticPortTypes, false, true);
    }

    /**
     * Adds a static port group configuration.
     *
     * @param pGrpIdentifier the port group identifier
     * @param staticPortTypes the static port types
     */
    public void addStaticPortGroup(final String pGrpIdentifier, final PortType... staticPortTypes) {
        addStaticPortGroup(pGrpIdentifier, staticPortTypes, true, true);
    }

    private void addStaticPortGroup(final String pGrpIdentifier, final PortType[] staticPortTypes,
        final boolean definesInputPorts, final boolean definesOutputPorts) {
        validateStaticPortGroupArguments(pGrpIdentifier, staticPortTypes);
        m_portConfigs.put(pGrpIdentifier,
            new DefaultStaticPortGroup(staticPortTypes, definesInputPorts, definesOutputPorts));
    }

    /**
     * Adds an extendable input port group configuration.
     *
     * @param pGrpIdentifier the port group identifier
     * @param supportedTypes the supported port types
     */
    public void addExtendableInputPortGroup(final String pGrpIdentifier, final PortType... supportedTypes) {
        addExtendableInputPortGroup(pGrpIdentifier, new PortType[0], supportedTypes);
    }

    /**
     * Adds an extendable input port group configuration.
     *
     * @param pGrpIdentifier the port group identifier
     * @param staticPortTypes the required port types
     * @param supportedTypes the supported port types
     */
    public void addExtendableInputPortGroup(final String pGrpIdentifier, final PortType[] staticPortTypes,
        final PortType... supportedTypes) {
        addExtendablePortGroup(pGrpIdentifier, staticPortTypes, supportedTypes, true, false);
    }

    /**
     * Adds an extendable output port group configuration.
     *
     * @param pGrpIdentifier the port group identifier
     * @param supportedTypes the supported port types
     */
    public void addExtendableOutputPortGroup(final String pGrpIdentifier, final PortType... supportedTypes) {
        addExtendableOutputPortGroup(pGrpIdentifier, new PortType[0], supportedTypes);
    }

    /**
     * Adds an extendable output port group configuration.
     *
     * @param pGrpIdentifier the port group identifier
     * @param staticPortTypes the required port types
     * @param supportedTypes the supported port types
     */
    public void addExtendableOutputPortGroup(final String pGrpIdentifier, final PortType[] staticPortTypes,
        final PortType... supportedTypes) {
        addExtendablePortGroup(pGrpIdentifier, staticPortTypes, supportedTypes, false, true);
    }

    /**
     * Adds an extendable port group configuration.
     *
     * @param pGrpIdentifier the port group identifier
     * @param supportedTypes the supported port types
     */
    public void addExtendablePortGroup(final String pGrpIdentifier, final PortType... supportedTypes) {
        addExtendablePortGroup(pGrpIdentifier, new PortType[0], supportedTypes);
    }

    /**
     * Adds an extendable port group configuration.
     *
     * @param pGrpIdentifier the port group identifier
     * @param staticPortTypes the required port types
     * @param supportedTypes the supported port types
     */
    public void addExtendablePortGroup(final String pGrpIdentifier, final PortType[] staticPortTypes,
        final PortType... supportedTypes) {
        addExtendablePortGroup(pGrpIdentifier, staticPortTypes, supportedTypes, true, true);
    }

    private void addExtendablePortGroup(final String pGrpIdentifier, final PortType[] staticPortTypes,
        final PortType[] supportedTypes, final boolean definesInputPorts, final boolean definesOutputPorts) {
        validateExtendablePortGroupArguments(pGrpIdentifier, staticPortTypes, supportedTypes);
        m_portConfigs.put(pGrpIdentifier,
            new DefaultExtendablePortGroup(staticPortTypes, supportedTypes, definesInputPorts, definesOutputPorts));
    }

    /**
     * Adds an optional input port group configuration.
     *
     * @param pGrpIdentifier the port group identifier
     * @param optionalPorts the optional port types
     */
    public void addOptionalInputPortGroup(final String pGrpIdentifier, final PortType... optionalPorts) {
        addOptionalPortGroup(pGrpIdentifier, optionalPorts, true, false);
    }

    /**
     * Adds an optional output port group configuration.
     *
     * @param pGrpIdentifier the port group identifier
     * @param optionalPorts the optional port types
     */
    public void addOptionalOutputPortGroup(final String pGrpIdentifier, final PortType... optionalPorts) {
        addOptionalPortGroup(pGrpIdentifier, optionalPorts, false, true);

    }

    /**
     * Adds an optional port group configuration.
     *
     * @param pGrpIdentifier the port group identifier
     * @param optionalPorts the optional port types
     */
    public void addOptionalPortGroup(final String pGrpIdentifier, final PortType... optionalPorts) {
        addOptionalPortGroup(pGrpIdentifier, optionalPorts, true, true);
    }

    private void addOptionalPortGroup(final String pGrpIdentifier, final PortType[] optionalPorts,
        final boolean definesInputPorts, final boolean definesOutputPorts) {
        validateOptionalPortGroupArguments(pGrpIdentifier, optionalPorts);
        m_portConfigs.put(pGrpIdentifier,
            new DefaultExtendablePortGroup(new PortType[]{}, optionalPorts, definesInputPorts, definesOutputPorts, 1));
    }

    private void validatePortGrpIdentifier(final String pGrpIdentifier) {
        CheckUtils.checkArgument(!m_portConfigs.containsKey(pGrpIdentifier),
            "A port group with the given name already exists");
        CheckUtils.checkArgument(pGrpIdentifier != null && !pGrpIdentifier.isEmpty(),
            "The group identifier has to contain at least one character");
    }

    private void validateStaticPortGroupArguments(final String pGrpIdentifier, final PortType... staticPortTypes) {
        validatePortGrpIdentifier(pGrpIdentifier);
        CheckUtils.checkArgumentNotNull(staticPortTypes, "The static port types cannot be null");
        CheckUtils.checkArgument(staticPortTypes != null && staticPortTypes.length > 0,
            "At least one static port type has to be set");
    }

    private void validateExtendablePortGroupArguments(final String pGrpIdentifier, final PortType[] staticPorts,
        final PortType[] supportedTypes) {
        validatePortGrpIdentifier(pGrpIdentifier);
        CheckUtils.checkArgumentNotNull(staticPorts, "The static ports cannot be null");
        CheckUtils.checkArgumentNotNull(supportedTypes, "The supported port types cannot be null");
        CheckUtils.checkArgument(supportedTypes != null && supportedTypes.length > 0,
            "The supported port types have to contain at least element");
    }

    private void validateOptionalPortGroupArguments(final String pGrpIdentifier, final PortType... optionalPorts) {
        validatePortGrpIdentifier(pGrpIdentifier);
        CheckUtils.checkArgumentNotNull(optionalPorts, "The optional ports cannot be null");
        CheckUtils.checkArgument(optionalPorts != null && optionalPorts.length > 0,
            "The optional ports have to contain at least one element");
    }

    //    TODO: Uncomment to support exchangeable port groups
    //    /**
    //     * Adds an exchangeable input port group configuration.
    //     *
    //     * @param pGrpIdentifier the port group identifier
    //     * @param defaultType the default port type
    //     * @param supportedTypes the supported port types (has to include the default type itself)
    //     */
    //    public void addExchangeableInputPortGroup(final String pGrpIdentifier, final PortType defaultType,
    //        final PortType... supportedTypes) {
    //        addExchangeablePortGroup(pGrpIdentifier, defaultType, supportedTypes, true, false);
    //    }
    //
    //    /**
    //     * Adds an exchangeable output port group configuration.
    //     *
    //     * @param pGrpIdentifier the port group identifier
    //     * @param defaultType the default port type
    //     * @param supportedTypes the supported port types (has to include the default type itself)
    //     */
    //    public void addExchangeableOutputPortGroup(final String pGrpIdentifier, final PortType defaultType,
    //        final PortType... supportedTypes) {
    //        addExchangeablePortGroup(pGrpIdentifier, defaultType, supportedTypes, false, true);
    //    }
    //
    //    /**
    //     * Adds an exchangeable port group configuration.
    //     *
    //     * @param pGrpIdentifier the port group identifier
    //     * @param defaultType the default port type
    //     * @param supportedTypes the supported port types (has to include the default type itself)
    //     */
    //    public void addExchangeablePortGroup(final String pGrpIdentifier, final PortType defaultType,
    //        final PortType... supportedTypes) {
    //        addExchangeablePortGroup(pGrpIdentifier, defaultType, supportedTypes, true, true);
    //    }
    //
    //    private void addExchangeablePortGroup(final String pGrpIdentifier, final PortType defaultType,
    //        final PortType[] supportedTypes, final boolean definesInputPort, final boolean definesOutputPort) {
    //        validateExchangeablePortGroupArguments(pGrpIdentifier, defaultType, supportedTypes);
    //        m_portConfigs.put(pGrpIdentifier,
    //            new ExchangeablePortGroup(defaultType, supportedTypes, definesInputPort, definesOutputPort));
    //    }
    //
    //    private void validateExchangeablePortGroupArguments(final String pGrpIdentifier, final PortType defaultType,
    //        final PortType... supportedTypes) {
    //        validatePortGrpIdentifier(pGrpIdentifier);
    //        CheckUtils.checkArgumentNotNull(defaultType, "The default port type cannot be null");
    //        CheckUtils.checkArgumentNotNull(supportedTypes, "The supported port types cannot be null");
    //        CheckUtils.checkArgument(supportedTypes != null && supportedTypes.length > 1,
    //            "The supported types have to contain at least two elements");
    //        CheckUtils.checkArgument(ArrayUtils.contains(supportedTypes, defaultType),
    //            "The supported port types have to contain the default port type");
    //    }

}
