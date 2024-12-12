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
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.apache.commons.lang.ArrayUtils;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeFactory.NodeType;
import org.knime.core.node.message.Message;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.Version;
import org.knime.core.webui.data.InitialDataService;
import org.knime.core.webui.data.RpcDataService;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.impl.ExternalResource;
import org.knime.core.webui.node.impl.PortDescription;
import org.knime.core.webui.node.impl.WebUINodeFactory;
import org.knime.core.webui.page.Page;

/**
 * TODO update
 *
 * Configuration for a {@link WebUINodeFactory WebUI node}.
 *
 * NOTE:
 *
 * Builder pattern principles:
 * <ul>
 * <li>no build-method</li>
 * <li>no initial 'builder'-method</li>
 * <li>required properties first</li>
 * <li>optional properties last</li>
 * <li>no prefixes for setters</li>
 * <li>'add'-prefix to allow multiple invocations (e.g. lists)</li>
 * <li>method/lambda parameters are defined in single objects, i.e. input/output objects; i.e. no combinatoric parameter
 * variations we'd need account for</li>
 * <li>complex parameters/nested objects follow same builder pattern; builders are as parameter</li>
 * <li>builders are meant to build the respective objects by composition only and must not be designed in a way to allow
 * enable inheritance, too
 * <li>
 * </ul>
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
/*
 * Open TODOs:
 *
 * Base View node model version dependent defaults (respect that in default node model implementation)
 * Validate default view&model settings
 * Multiple views
 * Custom node model implementation
 * Configurable ports
 * Execute input to implement other context objects
 */
public final class DefaultNode {

    final String m_name;

    final String m_icon;

    final String m_shortDescription;

    final String m_fullDescription;

    // TODO lazy init?
    List<ExternalResource> m_externalResources = new ArrayList<>();

    NodeType m_nodeType;

    Class<? extends DefaultNodeSettings> m_modelSettingsClass;

    private BiConsumer<ConfigureInput, ConfigureOutput> m_configure;

    private BiConsumer<ExecuteInput, ExecuteOutput> m_execute;

    // TODO lazy init?
    final List<PortDescription> m_inputPortDescriptions = new ArrayList<>();

    // TODO lazy init?
    final List<PortDescription> m_outputPortDescriptions = new ArrayList<>();

    // TODO lazy init?
    List<String> m_keywords = new ArrayList<>();

    Version m_sinceVersion;

    DefaultNode(final String name, final String icon, final String shortDescription, final String fullDescription) {
        m_name = name;
        m_icon = icon;
        m_shortDescription = shortDescription;
        m_fullDescription = fullDescription;
    }

    public static RequireName create() {
        return name -> icon -> shortDescription -> fullDescription -> model -> {
            var defaultNode = new DefaultNode(name, icon, shortDescription, fullDescription);
            model.apply(settingsClass -> {
                defaultNode.m_modelSettingsClass = settingsClass;
                return defaultNode.createRequirePortsAndConfigure();
            });
            return defaultNode;
        };
    }

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
        RequireModel fullDescription(final String fullDescription);
    }

    /**
     * The build stage that requires the model settings.
     */
    @FunctionalInterface
    public interface RequireModel { // NOSONAR
        /**
         * @param modelSettingsClass the type of the model settings
         * @return the subsequent build stage
         */
        DefaultNode model(Function<RequireModelSettings, RequireNothing> nodeModel);
    }

    public interface RequireModelSettings {
        RequirePortsAndConfigure settingsClass(Class<? extends DefaultNodeSettings> settingsClass);
    }

    private RequirePortsAndConfigure createRequirePortsAndConfigure() {
        return new RequirePortsAndConfigure();
    }

    public class RequirePortsAndConfigure {

        /**
         * Adds another input table to the node.
         *
         * @param name the name of the node's next input table
         * @param description the description of the node's next input table
         * @return this build stage
         */
        public RequirePortsAndConfigure addInputTable(final String name, final String description) {
            return addInputTable(name, description, false);
        }

        /**
         * Adds another input table to the node.
         *
         * @param name the name of the node's next input table
         * @param description the description of the node's next input table
         * @param configurable whether the port is configurable
         * @return this build stage
         */
        public RequirePortsAndConfigure addInputTable(final String name, final String description,
            final boolean configurable) {
            return addInputPort(name, BufferedDataTable.TYPE, description, configurable);
        }

        /**
         * Adds another input port to the node.
         *
         * @param name the name of the node's next input port
         * @param type the type of the node's next input port
         * @param description the description of the node's next input port
         * @return this build stage
         */
        public RequirePortsAndConfigure addInputPort(final String name, final PortType type, final String description) {
            return addInputPort(name, type, description, false);
        }

        /**
         * Adds another input port to the node.
         *
         * @param name the name of the node's next input port
         * @param type the type of the node's next input port
         * @param description the description of the node's next input port
         * @param configurable whether the port is configurable
         * @return this build stage
         */
        public RequirePortsAndConfigure addInputPort(final String name, final PortType type, final String description,
            final boolean configurable) {
            m_inputPortDescriptions.add(new PortDescription(name, type, description, configurable));
            return this;
        }

        /**
         * Adds another output table to the node.
         *
         * @param name the name of the node's next output table
         * @param description the description of the node's next output table
         * @return this build stage
         */
        public RequirePortsAndConfigure addOutputTable(final String name, final String description) {
            return addOutputTable(name, description, false);
        }

        /**
         * Adds another output table to the node.
         *
         * @param name the name of the node's next output table
         * @param description the description of the node's next output table
         * @param configurable whether the port is configurable
         * @return this build stage
         */
        public RequirePortsAndConfigure addOutputTable(final String name, final String description,
            final boolean configurable) {
            return addOutputPort(name, BufferedDataTable.TYPE, description, configurable);
        }

        /**
         * Adds another output port to the node.
         *
         * @param name the name of the node's next output port
         * @param type the type of the node's next output port
         * @param description the description of the node's next output port
         * @return this build stage
         */
        public RequirePortsAndConfigure addOutputPort(final String name, final PortType type,
            final String description) {
            return addOutputPort(name, type, description, false);
        }

        /**
         * Adds another output port to the node.
         *
         * @param name the name of the node's next output port
         * @param type the type of the node's next output port
         * @param description the description of the node's next output port
         * @param configurable whether the port is configurable
         * @return this build stage
         */
        public RequirePortsAndConfigure addOutputPort(final String name, final PortType type, final String description,
            final boolean configurable) {
            m_outputPortDescriptions.add(new PortDescription(name, type, description, configurable));
            return this;
        }

        public RequireExecute configure(final BiConsumer<ConfigureInput, ConfigureOutput> configure) {
            m_configure = configure;
            return execute -> {
                m_execute = execute;
                return new RequireNothing() {
                };
            };
        }
    }

    public interface ConfigureInput {

        <S extends PortObjectSpec> S getInSpec(int index);

        <S extends PortObjectSpec> S[] getInSpecs();

    }

    public interface ConfigureOutput {

        <S extends DefaultNodeSettings> S getSettings();

        <S extends PortObjectSpec> void setOutSpec(int index, S spec);

        <S extends PortObjectSpec> void setOutSpec(S... specs);

    }

    public interface ExecuteInput {

        <S extends DefaultNodeSettings> S getSettings();

        <D extends PortObject> D getInData(int index);

        <D extends PortObject> D[] getInData();

        ExecutionContext getExecutionContext();

    }

    public interface ExecuteOutput {

        <D extends PortObject> void setOutData(int index, D data);

        <D extends PortObject> void setOutData(D... data);

        // TODO serializable data
        <D> void setInternalData(D data);

        void setInternalTables(BufferedDataTable... data);

        void setInternalPortObjects(PortObject... data);

        void setWarning(Message message);

    }

    public interface RequireExecute {
        RequireNothing execute(BiConsumer<ExecuteInput, ExecuteOutput> execute);

    }

    // TODO naming!
    public interface RequireNothing {
    }

    /**
     * TODO
     *
     * @param nodeView
     * @return
     */
    public DefaultNode view(final Function<RequireViewSettings, RequireDataServices> nodeView) {
        return this;
    }

    public interface RequireViewSettings {
        RequireViewDescription settingsClass(Class<? extends DefaultNodeSettings> settingsClass);
    }

    public interface RequireViewDescription {
        RequireViewPage viewDescription(String viewDescription);
    }

    public interface RequireViewPage {
        // TODO refactor page builder to follow new principles
        RequireDataServices page(Function<ViewInput, Page> page);
    }

    public interface RequireDataServices {
        // TODO refactor build to refactor to follow new principles
        RequireDataServices initialDataService(InitialDataService initialDataService);

        <D> RequireDataServices initialData(Function<ViewInput, D> initialDataSupplier);

        // TODO refactor build to refactor to follow new principles
        RequireDataServices rpcDataService(RpcDataService dataService);

        <S> RequireDataServices dataService(Function<ViewInput, S> dataService);

    }

    public interface ViewInput {

        <S extends DefaultNodeSettings> S getSettings();

        <D> D getInternalData();

        BufferedDataTable[] getInternalTables();

        PortObject[] getInternalPortObjects();

    }

    /* NODE OPTIONALS */

    /**
     * Sets the node type. This affects how the node is shown, e.g., Loop Start nodes are shown in light blue and form a
     * small opening bracket that matches the way Loop End nodes are displayed.
     *
     * @param nodeType the node type
     * @return this build stage
     */
    public DefaultNode nodeType(final NodeType nodeType) {
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
