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
 *   10 Nov 2022 (marcbux): created
 */
package org.knime.node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.lang.ArrayUtils;
import org.knime.core.node.FluentNodeAPI;
import org.knime.core.node.NodeFactory.NodeType;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.Version;
import org.knime.node.DefaultModel.RequireModelSettings;
import org.knime.node.DefaultView.RequireViewSettings;
import org.knime.node.RequirePorts.DynamicPortsAdder;
import org.knime.node.impl.description.ExternalResource;
import org.knime.node.impl.port.DefaultNodePorts;
import org.knime.node.impl.port.PortGroup;

/**
 * Fluent API to create/compose a node as required by the {@link DefaultNodeFactory}. <br>
 *
 * Start with {@link #create()}.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public final class DefaultNode implements FluentNodeAPI {

    /**
     * Entry point to the node creator
     *
     * @return the first stage of the creator
     */
    public static RequireName create() {
        return name -> icon -> shortDescription -> fullDescription -> portsFct -> modelFct -> new DefaultNode(name,
            icon, shortDescription, fullDescription, portsFct, modelFct.apply(DefaultModel.create()));

    }

    final String m_name;

    final String m_icon;

    final String m_shortDescription;

    final String m_fullDescription;

    final List<PortGroup> m_ports;

    final DefaultModel m_model;

    private DefaultView m_view;

    Optional<DefaultView> getView() {
        return Optional.ofNullable(m_view);
    }

    List<ExternalResource> m_externalResources = new ArrayList<>();

    NodeType m_nodeType;

    List<String> m_keywords = new ArrayList<>();

    Version m_sinceVersion;

    /*
     * Constructor that receives all the required properties.
     */
    private DefaultNode(final String name, final String icon, final String shortDescription,
        final String fullDescription, final Consumer<DynamicPortsAdder> portsFct, final DefaultModel model) {
        m_name = name;
        m_icon = icon;
        m_shortDescription = shortDescription;
        m_fullDescription = fullDescription;
        m_ports = new DefaultNodePorts().extractPortDefinitions(portsFct);
        m_model = model;
    }

    /* REQUIRED PROPERTIES */

    /**
     * The build stage that requires the name of the node.
     */
    public interface RequireName {
        /**
         * @param name the name of the node, as shown in the node repository and description
         * @return the subsequent build stage
         */
        RequireIcon name(final String name);
    }

    /**
     * The build stage that requires an icon.
     */
    @FunctionalInterface
    public interface RequireIcon { // NOSONAR
        /**
         * @param icon relative path to the node icon
         * @return the subsequent build stage
         */
        RequireShortDescription icon(final String icon);
    }

    /**
     * The build stage that requires a short description.
     */
    @FunctionalInterface
    public interface RequireShortDescription { // NOSONAR
        /**
         * @param shortDescription the short node description
         * @return the subsequent build stage
         */
        RequireFullDescription shortDescription(final String shortDescription);
    }

    /**
     * The build stage that requires a full description.
     */
    @FunctionalInterface
    public interface RequireFullDescription { // NOSONAR
        /**
         * @param fullDescription the full node description
         * @return the subsequent build stage
         */
        RequirePorts fullDescription(final String fullDescription);
    }

    /**
     * The build stage that requires the model settings.
     */
    @FunctionalInterface
    public interface RequireModel { // NOSONAR
        /**
         * @param model a function receiving the first stage to create the {@link DefaultModel} and returning the
         *            {@link DefaultModel}
         * @return the {@link DefaultNode}
         */
        DefaultNode model(Function<RequireModelSettings, DefaultModel> model);

    }

    /* OPTIONAL PROPERTIES */

    /**
     * @param view a function receiving the first stage to create the {@link DefaultView} and returning the
     *            {@link DefaultView}
     * @return this
     */
    public DefaultNode addView(final Function<RequireViewSettings, DefaultView> view) {
        if (m_view != null) {
            throw new UnsupportedOperationException("Multiple views per node are not supported yet.");
        }
        m_view = view.apply(DefaultView.create());
        return this;
    }

    /**
     * Sets the node type. This affects how the node is shown, e.g., Loop Start nodes are shown in light blue and form a
     * small opening bracket that matches the way Loop End nodes are displayed.
     *
     * @param nodeType the node type
     * @return this build stage
     */
    public DefaultNode nodeType(final NodeType nodeType) {
        if (m_nodeType != null) {
            throw new IllegalStateException("nodeType() has already been called; a node can only have one node type.");
        }
        m_nodeType = nodeType;
        return this;
    }

    /**
     * Adds a list of keywords that are used/index for searching nodes. The list itself is not visible to the user.
     *
     * @param keywords List of keywords.
     * @return this build stage
     */
    public DefaultNode keywords(final String... keywords) {
        if (!m_keywords.isEmpty()) {
            throw new IllegalStateException(
                "keywords() has already been called; a node can only have one set of keywords.");
        }
        CheckUtils.checkArgumentNotNull(keywords);
        CheckUtils.checkArgument(!ArrayUtils.contains(keywords, null), "keywords list must not contain null");
        m_keywords.addAll(Arrays.asList(keywords));
        return this;
    }

    /**
     * Specify since which KNIME AP version this node is available
     *
     * @param major major version
     * @param minor minor version
     * @param revision patch revision
     * @return this build stage
     */
    public DefaultNode sinceVersion(final int major, final int minor, final int revision) {
        if (m_sinceVersion != null) {
            throw new IllegalStateException(
                "sinceVersion() has already been called; a node can only have one version.");
        }
        m_sinceVersion = new Version(major, minor, revision);
        return this;
    }

    /**
     * Adds a link to an external resource to the full description of the node.
     *
     * @param href resource URL
     * @param description resource description
     * @return this build stage
     */
    public DefaultNode addExternalResource(final String href, final String description) {
        m_externalResources.add(
            new ExternalResource(CheckUtils.checkArgumentNotNull(href), CheckUtils.checkArgumentNotNull(description)));
        return this;
    }

}
