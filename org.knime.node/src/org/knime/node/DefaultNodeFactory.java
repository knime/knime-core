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

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.xmlbeans.XmlException;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ConfigurableNodeFactory;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDescription;
import org.knime.core.node.NodeDescription53Proxy;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.message.Message;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.util.Version;
import org.knime.core.webui.data.ApplyDataService;
import org.knime.core.webui.data.InitialDataService;
import org.knime.core.webui.data.RpcDataService;
import org.knime.core.webui.node.dialog.NodeDialog;
import org.knime.core.webui.node.dialog.NodeDialogFactory;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialog;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.impl.OptionsAdder;
import org.knime.core.webui.node.view.NodeView;
import org.knime.core.webui.node.view.NodeViewFactory;
import org.knime.core.webui.page.Page;
import org.knime.node.DefaultModel.ConfigureInput;
import org.knime.node.DefaultModel.ConfigureOutput;
import org.knime.node.DefaultModel.ExecuteInput;
import org.knime.node.DefaultModel.ExecuteOutput;
import org.knime.node.DefaultView.ViewInput;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
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
     * TODO
     */
    protected DefaultNodeFactory(final DefaultNode node) {
        m_node = node;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected NodeDescription createNodeDescription() throws SAXException, IOException, XmlException {
        var model = m_node.m_model;
        var view = m_node.m_view;
        return createNodeDescription(m_node.m_name, m_node.m_icon, model.m_inputPortDescriptions,
            model.m_outputPortDescriptions, m_node.m_shortDescription, m_node.m_fullDescription,
            m_node.m_externalResources, model.m_settingsClass, view.m_settingsClass, view.m_description,
            m_node.m_nodeType, m_node.m_keywords, m_node.m_sinceVersion);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Optional<PortsConfigurationBuilder> createPortsConfigBuilder() {
        // TODO
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final NodeDialog createNodeDialog() {
        var view = m_node.m_view;
        var model = m_node.m_model;
        if (view != null) {
            return new DefaultNodeDialog(SettingsType.MODEL, model.m_settingsClass, SettingsType.VIEW,
                view.m_settingsClass);
        } else {
            return new DefaultNodeDialog(SettingsType.MODEL, m_node.m_model.m_settingsClass);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final NodeModel createNodeModel(final NodeCreationConfiguration creationConfig) {
        // TODO configurable ports
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final NodeModel createNodeModel() {
        var inPorts = m_node.m_model.m_inputPortDescriptions.stream().map(portDesc -> portDesc.getType())
            .toArray(PortType[]::new);
        var outPorts = m_node.m_model.m_outputPortDescriptions.stream().map(portDesc -> portDesc.getType())
            .toArray(PortType[]::new);
        var model = m_node.m_model;
        return new NodeModel(inPorts, outPorts) {

            private DefaultNodeSettings m_modelSettings;

            @Override
            protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
                var outSpecs = new PortObjectSpec[outPorts.length];
                model.m_configure.accept(new ConfigureInput() {

                    @Override
                    public <S extends PortObjectSpec> S getInSpec(final int index) {
                        return (S)inSpecs[index];
                    }

                    @Override
                    public <S extends PortObjectSpec> S[] getInSpecs() {
                        return (S[])inSpecs;
                    }

                }, new ConfigureOutput() {

                    @Override
                    public <S extends DefaultNodeSettings> S getSettings() {
                        return (S)m_modelSettings;
                    }

                    @Override
                    public <S extends PortObjectSpec> void setOutSpec(final int index, final S spec) {
                        outSpecs[index] = spec;
                    }

                    @Override
                    public <S extends PortObjectSpec> void setOutSpec(final S... specs) {
                        System.arraycopy(specs, 0, outSpecs, 0, specs.length);
                    }

                });
                return outSpecs;
            }

            @Override
            protected PortObject[] execute(final PortObject[] inObjects,
                final org.knime.core.node.ExecutionContext exec) throws Exception {
                var outObjects = new PortObject[outPorts.length];
                model.m_execute.accept(new ExecuteInput() {

                    @Override
                    public <S extends DefaultNodeSettings> S getSettings() {
                        return (S)m_modelSettings;
                    }

                    @Override
                    public <D extends PortObject> D getInData(final int index) {
                        return (D)inObjects[index];
                    }

                    @Override
                    public <D extends PortObject> D[] getInData() {
                        // TODO
                        return null;
                    }

                    @Override
                    public ExecutionContext getExecutionContext() {
                        return exec;
                    }

                }, new ExecuteOutput() {

                    @Override
                    public <D extends PortObject> void setOutData(final int index, final D data) {
                        outObjects[index] = data;
                    }

                    @Override
                    public <D extends PortObject> void setOutData(final D... data) {
                        System.arraycopy(outObjects, 0, data, 0, data.length);
                    }

                    @Override
                    public <D> void setInternalData(final D data) {
                        // TODO
                    }

                    @Override
                    public void setInternalTables(final BufferedDataTable... data) {
                        // TODO
                    }

                    @Override
                    public void setInternalPortObjects(final PortObject... data) {
                        // TODO
                    }

                    @Override
                    public void setWarning(final Message message) {
                        this.setWarning(message);
                    }

                });
                return outObjects;
            }

            @Override
            protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
                // TODO Auto-generated method stub

            }

            @Override
            protected void saveSettingsTo(final NodeSettingsWO settings) {
                DefaultNodeSettings.saveSettings(model.m_settingsClass, m_modelSettings, settings);

            }

            @Override
            protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
                throws IOException, CanceledExecutionException {
                // TODO Auto-generated method stub

            }

            @Override
            protected void reset() {
                // TODO Auto-generated method stub

            }

            @Override
            protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
                m_modelSettings = DefaultNodeSettings.loadSettings(settings, model.m_settingsClass);
            }

            @Override
            protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
                throws IOException, CanceledExecutionException {
                // TODO view data
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final NodeView createNodeView(final NodeModel nodeModel) {
        var view = m_node.m_view;
        return new NodeView() {

            private ViewInput m_input;

            @Override
            public Page getPage() {
                return view.m_pageFct.apply(m_input);
            }

            @Override
            public <D> Optional<InitialDataService<D>> createInitialDataService() {
                if (view.m_initialDataServiceFct == null) {
                    return Optional.empty();
                } else {
                    return Optional.of((InitialDataService<D>)view.m_initialDataServiceFct.apply(m_input));
                }
            }

            @Override
            public Optional<RpcDataService> createRpcDataService() {
                if (view.m_rpcDataServiceFct == null) {
                    return Optional.empty();
                } else {
                    return Optional.of(view.m_rpcDataServiceFct.apply(m_input));
                }
            }

            @Override
            public <D> Optional<ApplyDataService<D>> createApplyDataService() {
                return Optional.empty();
            }

            @Override
            public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
                DefaultNodeSettings.loadSettings(settings, view.m_settingsClass);
            }

            @Override
            public void loadValidatedSettingsFrom(final NodeSettingsRO settings) {
                m_input = new ViewInput() {

                    @Override
                    public <S extends DefaultNodeSettings> S getSettings() {
                        try {
                            return (S)DefaultNodeSettings.loadSettings(settings, view.m_settingsClass);
                        } catch (InvalidSettingsException ex) {
                            // TODO Auto-generated catch block
                            throw new IllegalStateException();
                        }
                    }

                    @Override
                    public BufferedDataTable[] getInternalTables() {
                        // TODO get tables from node model (e.g. via BufferedDataTableHolder)
                        return null;
                    }

                    @Override
                    public PortObject[] getInternalPortObjects() {
                        // TODO Auto-generated method stub
                        return null;
                    }

                    @Override
                    public <D> D getInternalData() {
                        // TODO Auto-generated method stub
                        return null;
                    }
                };

            }

        };

    }

    // TODO remove!!
    @Override
    protected boolean hasDialog() {
        return false;
    }

    // TODO remove!!
    @Override
    protected NodeDialogPane createNodeDialogPane(final NodeCreationConfiguration creationConfig) {
        return null;
    }

    // TODO remove!!
    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    // TODO remove!!
    @Override
    public org.knime.core.node.NodeView<NodeModel> createNodeView(final int viewIndex, final NodeModel nodeModel) {
        return null;
    }

    /**
     * @param name the name of the node
     * @param icon relative path to the node icon
     * @param inPortDescriptions the descriptions of the node's input ports
     * @param outPortDescriptions the descriptions of the node's output ports
     * @param shortDescription the short node description
     * @param fullDescription the full node description
     * @param externalResources links to external resources
     * @param modelSettingsClass the type of the model settings, or null, if the node has no model settings
     * @param viewSettingsClass the type of the view settings, or null, if the node has no view settings
     * @param viewDescription the view description, or null, if the node has no view
     * @param type the type of the node, or null, if it should be determined automatically
     * @param keywords the keywords for search, or null.
     * @param sinceVersion the KNIME AP version since which this node is available, or null
     * @return a description for this node
     */
    private static NodeDescription createNodeDescription(final String name, final String icon, // NOSONAR
        final List<PortDescription> inPortDescriptions, final List<PortDescription> outPortDescriptions,
        final String shortDescription, final String fullDescription, final List<ExternalResource> externalResources,
        final Class<? extends DefaultNodeSettings> modelSettingsClass,
        final Class<? extends DefaultNodeSettings> viewSettingsClass, final String viewDescription, final NodeType type,
        final List<String> keywords, final Version sinceVersion) {
        var fac = NodeDescription.getDocumentBuilderFactory();
        DocumentBuilder docBuilder;
        try {
            docBuilder = fac.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            // should never happen
            throw new IllegalStateException("Problem creating node description", e);
        }
        var doc = docBuilder.newDocument();
        var node = doc.createElement("knimeNode");

        node.setAttribute("icon", icon);
        final NodeType nodeType;
        if (type != null) {
            nodeType = type;
        } else if (inPortDescriptions.size() == 0) {
            nodeType = NodeFactory.NodeType.Source;
        } else if (outPortDescriptions.size() == 0) {
            nodeType = NodeFactory.NodeType.Sink;
        } else {
            nodeType = NodeFactory.NodeType.Manipulator;
        }
        node.setAttribute("type", nodeType.toString());
        var nodeName = doc.createElement("name");
        nodeName.setTextContent(name);
        node.appendChild(nodeName);

        var shortDesc = doc.createElement("shortDescription");
        shortDesc.appendChild(parseDocumentFragment(shortDescription, docBuilder, doc));
        var fullDesc = doc.createElement("fullDescription");
        var intro = doc.createElement("intro");
        intro.appendChild(parseDocumentFragment(fullDescription, docBuilder, doc));
        fullDesc.appendChild(intro);
        node.appendChild(shortDesc);
        node.appendChild(fullDesc);

        fullDesc.appendChild(createOptionsTab(modelSettingsClass, viewSettingsClass, docBuilder, doc));

        if (!externalResources.isEmpty()) {
            for (final var resource : externalResources) {
                final var link = doc.createElement("link");
                link.setAttribute("href", resource.href());
                link.appendChild(parseDocumentFragment(resource.description(), docBuilder, doc));
                fullDesc.appendChild(link);
            }
        }

        // create ports
        var ports = doc.createElement("ports");
        addPorts(docBuilder, doc, ports, inPortDescriptions, pd -> pd.isConfigurable() ? "dynInPort" : "inPort");
        addPorts(docBuilder, doc, ports, outPortDescriptions, pd -> pd.isConfigurable() ? "dynOutPort" : "outPort");
        node.appendChild(ports);

        // create view (if exists)
        if (viewDescription != null) {
            final var views = doc.createElement("views");
            var view = doc.createElement("view");
            view.setAttribute("index", "0");
            view.setAttribute("name", name);
            view.appendChild(parseDocumentFragment(viewDescription, docBuilder, doc));
            views.appendChild(view);
            node.appendChild(views);
        }

        if (!keywords.isEmpty()) {
            final var keywordsElement = doc.createElement("keywords");
            for (String keyword : keywords) {
                final var keywordElement = keywordsElement.appendChild(doc.createElement("keyword"));
                keywordElement.setTextContent(keyword);
            }
            node.appendChild(keywordsElement);
        }

        doc.appendChild(node);
        try {
            return new NodeDescription53Proxy(doc, sinceVersion);
        } catch (XmlException e) {
            // should never happen
            throw new IllegalStateException("Problem creating node description", e);
        }
    }

    private static Element createOptionsTab(final Class<? extends DefaultNodeSettings> modelSettingsClass,
        final Class<? extends DefaultNodeSettings> viewSettingsClass, final DocumentBuilder docBuilder,
        final Document doc) {
        var tab = doc.createElement("tab");
        tab.setAttribute("name", "Options");
        OptionsAdder.addOptionsToTab(tab, modelSettingsClass, viewSettingsClass, (title, desc) -> {
            var option = doc.createElement("option");
            option.setAttribute("name", title);
            option.appendChild(parseDocumentFragment(desc, docBuilder, doc));
            return option;
        });
        return tab;
    }

    private static void addPorts(final DocumentBuilder docBuilder, final Document doc, final Element ports,
        final List<PortDescription> portDescs, final Function<PortDescription, String> tagName) {
        int index = 0;
        for (final PortDescription portDesc : portDescs) {
            var port = doc.createElement(tagName.apply(portDesc));
            port.setAttribute("name", portDesc.getName());
            if (portDesc.isConfigurable()) {
                port.setAttribute("insert-before", Integer.toString(index));
                port.setAttribute("group-identifier", portDesc.getName());
                port.setAttribute("configurable-via-menu", "false");
            } else {
                port.setAttribute("index", Integer.toString(index));
                index++;
            }
            port.appendChild(parseDocumentFragment(portDesc.getDescription(), docBuilder, doc));
            ports.appendChild(port);
        }
    }

    /*
     * Creates a fragment from an xml-string (usually html) such that it can be appended to other nodes as is (i.e.
     * without escaping the html).
     */
    private static DocumentFragment parseDocumentFragment(final String s, final DocumentBuilder docBuilder,
        final Document doc) {
        var wrapped = "<fragment>" + s + "</fragment>";
        Document parsed;
        try {
            parsed = docBuilder.parse(new InputSource(new StringReader(wrapped)));
        } catch (SAXException | IOException e) {
            // should never happen
            throw new IllegalStateException("Problem creating node description", e);
        }
        var fragment = doc.createDocumentFragment();
        var children = parsed.getDocumentElement().getChildNodes();
        for (var i = 0; i < children.getLength(); i++) {
            var child = doc.importNode(children.item(i), true);
            fragment.appendChild(child);
        }
        return fragment;
    }

}
