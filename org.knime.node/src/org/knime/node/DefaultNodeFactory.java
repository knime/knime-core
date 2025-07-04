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
 *   Dec 11, 2024 (hornm): created
 */
package org.knime.node;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.apache.xmlbeans.XmlException;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ConfigurableNodeFactory;
import org.knime.core.node.NodeDescription;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeModel;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectHolder;
import org.knime.core.node.port.PortType;
import org.knime.core.webui.node.dialog.NodeDialog;
import org.knime.core.webui.node.dialog.NodeDialogFactory;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialog;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.view.NodeView;
import org.knime.core.webui.node.view.NodeViewFactory;
import org.knime.node.impl.description.DefaultNodeDescriptionUtil;
import org.knime.node.impl.description.PortDescription;
import org.knime.node.impl.port.PortGroup;
import org.xml.sax.SAXException;

/**
 * TODO doc
 *
 * TODO make sure it's quasi final
 */
public abstract class DefaultNodeFactory extends ConfigurableNodeFactory<NodeModel>
    implements NodeDialogFactory, NodeViewFactory<NodeModel> {

    private final DefaultNode m_node;

    /**
     * Set when the model is created, accessed within views.
     */
    private PortObjectHolder m_portObjectHolder;

    /**
     * TODO
     */
    protected DefaultNodeFactory(final DefaultNode node) {
        super(true);
        m_node = node;
        super.init();
    }

    @Override
    protected final NodeDescription createNodeDescription() throws SAXException, IOException, XmlException {
        var model = m_node.m_model;
        final var view = m_node.getView();
        final var inputPortsDescriptions = getPortDescriptions(m_node.m_ports, PortGroup.PortLocation.INPUT);
        final var outputPortsDescriptions = getPortDescriptions(m_node.m_ports, PortGroup.PortLocation.OUTPUT);
        return DefaultNodeDescriptionUtil.createNodeDescription(//
            m_node.m_name, //
            m_node.m_icon, //
            inputPortsDescriptions, //
            outputPortsDescriptions, //
            m_node.m_shortDescription, //
            m_node.m_fullDescription, //
            m_node.m_externalResources, //
            model.getSettingsClass().orElse(null), //
            view.flatMap(v -> v.getSettingsClass()).orElse(null), //
            view.map(v -> v.m_description).orElse(null), //
            m_node.m_nodeType, //
            m_node.m_keywords, //
            m_node.m_sinceVersion//
        );
    }

    private static List<PortDescription> getPortDescriptions(final List<PortGroup> ports,
        final PortGroup.PortLocation location) {
        return ports.stream().map(p -> p.getDescription(location)).flatMap(Optional::stream).toList();
    }

    @Override
    protected final Optional<PortsConfigurationBuilder> createPortsConfigBuilder() {
        if (m_node.m_ports.isEmpty()) {
            return Optional.empty();
        }
        final var portsBuilder = new PortsConfigurationBuilder();
        for (var portDef : m_node.m_ports) {
            addPortDefToBuilder(portsBuilder, portDef);
        }
        return Optional.of(portsBuilder);
    }

    /**
     * Unfortunately, it is not easily possible to deduplicate the code here, since the
     * {@link PortsConfigurationBuilder} uses different methods for each leaf configuration. The invocation of these
     * methods also needs to reside here since the builder is protected.
     */
    private static void addPortDefToBuilder(final PortsConfigurationBuilder portsBuilder, final PortGroup portDef) {
        final var groupId = portDef.getId();

        var lambdas = new PortGroup.PortGroupConfigBuilderLambdas(
            // FixedPortGroup
            type -> portsBuilder.addFixedInputPortGroup(groupId, type),
            type -> portsBuilder.addFixedOutputPortGroup(groupId, type),
            type -> portsBuilder.addFixedPortGroup(groupId, type),
            // OptionalPortGroup
            (defaultType, supportedTypes) -> portsBuilder.addOptionalInputPortGroupWithDefault(groupId, defaultType,
                supportedTypes),
            (defaultType, supportedTypes) -> portsBuilder.addOptionalOutputPortGroupWithDefault(groupId, defaultType,
                supportedTypes),
            (defaultType, supportedTypes) -> portsBuilder.addOptionalPortGroupWithDefault(groupId, defaultType,
                supportedTypes),
            (defaultType, supportedTypesPredicate) -> portsBuilder.addOptionalInputPortGroupWithDefault(groupId,
                defaultType, supportedTypesPredicate),
            (defaultType, supportedTypesPredicate) -> portsBuilder.addOptionalOutputPortGroupWithDefault(groupId,
                defaultType, supportedTypesPredicate),
            (defaultType, supportedTypesPredicate) -> portsBuilder.addOptionalPortGroupWithDefault(groupId, defaultType,
                supportedTypesPredicate),
            // ExtendablePortGroup
            (fixedTypes, defaultNonFixedTypes, supportedTypes) -> portsBuilder
                .addExtendableInputPortGroupWithDefault(groupId, fixedTypes, defaultNonFixedTypes, supportedTypes),
            (fixedTypes, defaultNonFixedTypes, supportedTypes) -> portsBuilder
                .addExtendableOutputPortGroupWithDefault(groupId, fixedTypes, defaultNonFixedTypes, supportedTypes),
            (fixedTypes, defaultNonFixedTypes, supportedTypes) -> portsBuilder
                .addExtendablePortGroupWithDefault(groupId, fixedTypes, defaultNonFixedTypes, supportedTypes),
            (fixedTypes, defaultNonFixedTypes, supportedTypesPredicate) -> portsBuilder
                .addExtendableInputPortGroupWithDefault(groupId, fixedTypes, defaultNonFixedTypes,
                    supportedTypesPredicate),
            (fixedTypes, defaultNonFixedTypes, supportedTypesPredicate) -> portsBuilder
                .addExtendableOutputPortGroupWithDefault(groupId, fixedTypes, defaultNonFixedTypes,
                    supportedTypesPredicate),
            (fixedTypes, defaultNonFixedTypes, supportedTypesPredicate) -> portsBuilder
                .addExtendablePortGroupWithDefault(groupId, fixedTypes, defaultNonFixedTypes, supportedTypesPredicate),
            // BoundExtendablePortGroup
            (boundGrpId, fixedNumPorts, defaultNumPorts) -> portsBuilder
                .addBoundExtendableInputPortGroupWithDefault(groupId, boundGrpId, fixedNumPorts, defaultNumPorts),
            (boundGrpId, fixedNumPorts, defaultNumPorts) -> portsBuilder
                .addBoundExtendableOutputPortGroupWithDefault(groupId, boundGrpId, fixedNumPorts, defaultNumPorts),
            (boundGrpId, fixedNumPorts, defaultNumPorts) -> portsBuilder.addBoundExtendablePortGroupWithDefault(groupId,
                boundGrpId, fixedNumPorts, defaultNumPorts),
            // ExchangablePortGroup
            (defaultType, supportedTypes) -> portsBuilder.addExchangeableInputPortGroup(groupId, defaultType,
                supportedTypes),
            (defaultType, supportedTypes) -> portsBuilder.addExchangeableOutputPortGroup(groupId, defaultType,
                supportedTypes),
            (defaultType, supportedTypes) -> portsBuilder.addExchangeablePortGroup(groupId, defaultType,
                supportedTypes),
            (defaultType, supportedTypesPredicate) -> portsBuilder.addExchangeableInputPortGroup(groupId, defaultType,
                supportedTypesPredicate),
            (defaultType, supportedTypesPredicate) -> portsBuilder.addExchangeableOutputPortGroup(groupId, defaultType,
                supportedTypesPredicate),
            (defaultType, supportedTypesPredicate) -> portsBuilder.addExchangeablePortGroup(groupId, defaultType,
                supportedTypesPredicate));
        portDef.addToPortGroupConfiguration(lambdas);
    }

    @Override
    public boolean hasNodeDialog() {
        var viewSettings = getViewSettingsClass();
        var modelSettings = getModelSettingsClass();
        return viewSettings.isPresent() || modelSettings.isPresent();
    }

    private Optional<Class<? extends DefaultNodeSettings>> getModelSettingsClass() {
        return m_node.m_model.getSettingsClass();
    }

    private Optional<Class<? extends DefaultNodeSettings>> getViewSettingsClass() {
        return m_node.getView().flatMap(DefaultView::getSettingsClass);
    }

    @Override
    public final NodeDialog createNodeDialog() {
        var viewSettings = getViewSettingsClass();
        var modelSettings = getModelSettingsClass();

        if (viewSettings.isEmpty() && modelSettings.isEmpty()) {
            throw new IllegalAccessError("Unable to create node dialog since the node does not have settings "
                + "and thus should not have a dialog.");
        }

        if (viewSettings.isPresent() && modelSettings.isPresent()) {
            return new DefaultNodeDialog(//
                SettingsType.MODEL, modelSettings.get(), //
                SettingsType.VIEW, viewSettings.get()//
            );
        } else if (modelSettings.isPresent()) {
            return new DefaultNodeDialog(SettingsType.MODEL, modelSettings.get());
        } else {
            // Only view settings present
            return new DefaultNodeDialog(SettingsType.VIEW, viewSettings.get()); // NOSONAR
        }

    }

    @Override
    protected final NodeModel createNodeModel(final NodeCreationConfiguration creationConfig) {
        final var portConfigOpt = creationConfig.getPortConfig();
        if (m_node.m_model instanceof DefaultModel.StandardDefaultModel standardModel) {
            final var createdModel = createStandardNodeModel(portConfigOpt, standardModel);
            m_portObjectHolder = createdModel;
            return createdModel;
        } else if (m_node.m_model instanceof DefaultModel.RearrangeColumnsDefaultModel rearrangeModel) {
            m_portObjectHolder = new PortObjectHolder() {

                @Override
                public void setInternalPortObjects(final PortObject[] portObjects) {
                    // Not used.
                }

                @Override
                public PortObject[] getInternalPortObjects() {
                    throw new IllegalStateException(
                        "The model only rearranges columns and does not have any internal port objects.");
                }
            };
            return createRearrangerNodeModel(portConfigOpt, rearrangeModel);
        } else {
            throw new IllegalStateException("Unknown model type: " + m_node.m_model.getClass().getName());
        }

    }

    private StandardDefaultModelToNodeModelAdapter createStandardNodeModel(
        final Optional<? extends PortsConfiguration> portConfigOpt,
        final DefaultModel.StandardDefaultModel standardModel) {
        if (portConfigOpt.isEmpty()) {
            return new StandardDefaultModelToNodeModelAdapter(standardModel, new PortType[0], new PortType[0],
                getViewSettingsClass().orElse(null));
        }
        final var portConfig = portConfigOpt.get();
        return new StandardDefaultModelToNodeModelAdapter(standardModel, portConfig.getInputPorts(),
            portConfig.getOutputPorts(), getViewSettingsClass().orElse(null));
    }

    private RearrangeColumnsDefaultModelToNodeModelAdapter createRearrangerNodeModel(
        final Optional<? extends PortsConfiguration> portConfigOpt,
        final DefaultModel.RearrangeColumnsDefaultModel rearrangeModel) {
        if (portConfigOpt.isEmpty()) {
            throw new IllegalStateException(
                "Add an input and output table to the node to rearrange columns in the model.");
        }
        final var portConfig = portConfigOpt.get();
        if (portConfig.getInputPorts().length != 1 || portConfig.getOutputPorts().length != 1
            || !BufferedDataTable.TYPE.equals(portConfig.getInputPorts()[0])
                | !BufferedDataTable.TYPE.equals(portConfig.getOutputPorts()[0])) {
            throw new IllegalStateException(
                "A node that rearranges columns must have exactly one input and one output table.");
        }
        return new RearrangeColumnsDefaultModelToNodeModelAdapter(rearrangeModel, getViewSettingsClass().orElse(null));
    }

    @Override
    public final boolean hasNodeView() {
        return m_node.getView().isPresent();
    }

    @Override
    public final NodeView createNodeView(final NodeModel nodeModel) {
        return new DefaultViewToNodeViewAdapter(m_node.getView().orElseThrow(IllegalStateException::new),
            () -> m_portObjectHolder);
    }

    @Override
    protected final boolean hasDialog() {
        return false;
    }

    @Override
    protected final NodeDialogPane createNodeDialogPane(final NodeCreationConfiguration creationConfig) {
        return null;
    }

    @Override
    protected final int getNrNodeViews() {
        return 0;
    }

    @Override
    public final org.knime.core.node.NodeView<NodeModel> createNodeView(final int viewIndex,
        final NodeModel nodeModel) {
        return null;
    }

}
