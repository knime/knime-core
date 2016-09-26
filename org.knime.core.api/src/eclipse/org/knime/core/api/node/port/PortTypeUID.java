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
 * History
 *   Sep 20, 2016 (hornm): created
 */
package org.knime.core.api.node.port;

import org.knime.core.node.util.CheckUtils;

/**
 * A unique identifier for a PortType. The identifier is composed of the fully-qualified class name of the respective
 * PortObject and some additional meta information (e.g. whether it's optional etc.).
 *
 * @author Martin Horn, KNIME.com
 */
public class PortTypeUID {

    private final String m_name;
    private final String m_className;
    private final boolean m_isOptional;
    private final int m_color;
    private final boolean m_isHidden;

    private PortTypeUID(final Builder builder) {
        CheckUtils.checkArgumentNotNull(builder.m_className, "Class name must not be null.");
        m_name = builder.m_name;
        m_className = builder.m_className;
        m_isOptional = builder.m_isOptional;
        m_isHidden = builder.m_isHidden;
        m_color = builder.m_color;
    }

    /**
     * @return a human-readable name for this port type
     */
    public String getName() {
        return m_name;
    }

    /**
     * @return the associated port object's fully-qualified class name
     */
    public String getPortObjectClassName() {
        return m_className;
    }

    /**
     * @return true if this port does not need to be connected.
     */
    public boolean isOptional() {
        return m_isOptional;
    }

    /**
     * Returns a color for the port type.
     * @return color as int
     */
    public int getColor() {
        return m_color;
    }

    /**
     * Returns whether this port type should be shown to the user in e.g. dialogs.
     *
     * @return <code>true</code> if this type should be hidden, <code>false</code> otherwise
     */
    public boolean isHidden() {
        return m_isHidden;
    }


    /**
     * @param className the associated port object's fully-qualified class name
     * @return a new {@link Builder} with default values (except for the mandatory attributes).
     */
    public static Builder builder(final String className) {
        return new Builder(className);
    }

    /**
     * Builder to create {@link PortTypeUID} objects.
     */
    public static final class Builder {

        private String m_name;
        private String m_className;
        private boolean m_isOptional;
        private int m_color;
        private boolean m_isHidden;

        private Builder(final String className) {
            m_className = className;
        }

        /**
         * Sets a human-readable name for this port type.
         * @param name
         * @return this
         */
        public Builder setName(final String name) {
            m_name = name;
            return this;
        }

        /**
         * Sets the fully-qualified class name of the node factory.
         *
         * @param name
         * @return this
         */
        public Builder setPortObjectClassName(final String name) {
            m_className = name;
            return this;
        }

        /**
         * Sets whether it's an optional port or not
         *
         * @param isOptional
         * @return this
         */
        public Builder setIsOptional(final boolean isOptional) {
            m_isOptional = isOptional;
            return this;
        }

        /**
         * Sets whether the port should be shown to the user or not.
         * @param isHidden
         * @return this
         */
        public Builder setIsHidden(final boolean isHidden) {
            m_isHidden = isHidden;
            return this;
        }

        /**
         * @param color the color of the port
         * @return this
         */
        public Builder setColor(final int color) {
            m_color = color;
            return this;
        }


        /**
         * @return the newly created {@link PortTypeUID} from this builder
         */
        public PortTypeUID build() {
            return new PortTypeUID(this);
        }

    }

}
