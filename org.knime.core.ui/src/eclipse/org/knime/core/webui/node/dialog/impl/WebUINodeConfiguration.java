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
package org.knime.core.webui.node.dialog.impl;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for a {@link WebUINodeFactory WebUI node}.
 *
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
public class WebUINodeConfiguration {

    private final String m_name;

    private final String m_icon;

    private final String m_shortDescription;

    private final String m_fullDescription;

    private final Class<? extends DefaultNodeSettings> m_modelSettingsClass;

    private final String[] m_inPortDescriptions;

    private final String[] m_outPortDescriptions;

    WebUINodeConfiguration(final String name, final String icon, final String shortDescription,
        final String fullDescription, final Class<? extends DefaultNodeSettings> modelSettingsClass,
        final String[] inPortDescriptions, final String[] outPortDescriptions) {
        m_name = name;
        m_icon = icon;
        m_shortDescription = shortDescription;
        m_fullDescription = fullDescription;
        m_modelSettingsClass = modelSettingsClass;
        m_inPortDescriptions = inPortDescriptions;
        m_outPortDescriptions = outPortDescriptions;
    }

    String getName() {
        return m_name;
    }

    String getIcon() {
        return m_icon;
    }

    String getShortDescription() {
        return m_shortDescription;
    }

    String getFullDescription() {
        return m_fullDescription;
    }

    Class<? extends DefaultNodeSettings> getModelSettingsClass() {
        return m_modelSettingsClass;
    }

    String[] getInPortDescriptions() {
        return m_inPortDescriptions;
    }

    String[] getOutPortDescriptions() {
        return m_outPortDescriptions;
    }

    /**
     * A builder for assembly of {@link WebUINodeConfiguration WebUINodeConfigurations}
     */
    public static class Builder {
        /**
         * @param name the name of the node, as shown in the node repository and description
         * @return the subsequent build stage
         */
        public RequireIcon name(final String name) {
            return icon -> shortDescription -> fullDescription -> modelSettingsClass -> new RequirePorts(name, icon,
                shortDescription, fullDescription, modelSettingsClass);
        }
    }

    /**
     * The build stage that requires an icon.
     */
    @FunctionalInterface
    public interface RequireIcon {
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
    public interface RequireShortDescription {
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
    public interface RequireFullDescription {
        /**
         * @param fullDescription the full node description
         * @return the subsequent build stage
         */
        RequireModelSettingsClass fullDescription(final String fullDescription);
    }

    /**
     * The build stage that requires the model settings.
     */
    @FunctionalInterface
    public interface RequireModelSettingsClass {
        /**
         * @param modelSettingsClass the type of the model settings
         * @return the subsequent build stage
         */
        RequirePorts modelSettingsClass(final Class<? extends DefaultNodeSettings> modelSettingsClass);
    }

    /**
     * The (final) build stage in which the ports are defined.
     */
    public static final class RequirePorts {

        private final String m_name;

        private final String m_icon;

        private final String m_shortDescription;

        private final String m_fullDescription;

        private final Class<? extends DefaultNodeSettings> m_modelSettingsClass;

        private final List<String> m_inputPortDescriptions = new ArrayList<>();

        private final List<String> m_outputPortDescriptions = new ArrayList<>();

        RequirePorts(final String name, final String icon, final String shortDescription, final String fullDescription,
            final Class<? extends DefaultNodeSettings> modelSettingsClass) {
            m_name = name;
            m_icon = icon;
            m_shortDescription = shortDescription;
            m_fullDescription = fullDescription;
            m_modelSettingsClass = modelSettingsClass;
        }

        /**
         * Adds another input port to the node.
         *
         * @param portDescription the description of the node's next input port
         * @return this build stage
         */
        public RequirePorts addInputPort(final String portDescription) {
            m_inputPortDescriptions.add(portDescription);
            return this;
        }

        /**
         * Adds another output port to the node.
         *
         * @param portDescription the description of the node's next output port
         * @return this build stage
         */
        public RequirePorts addOutputPort(final String portDescription) {
            m_outputPortDescriptions.add(portDescription);
            return this;
        }

        /**
         * @return the built node
         */
        public WebUINodeConfiguration build() {
            return new WebUINodeConfiguration(m_name, m_icon, m_shortDescription, m_fullDescription,
                m_modelSettingsClass, m_inputPortDescriptions.toArray(new String[0]),
                m_outputPortDescriptions.toArray(new String[0]));
        }
    }

}
