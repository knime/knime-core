/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 *
 * History
 *   Jun 20, 2012 (wiswedel): created
 */
package org.knime.core.node.port;

/** Object describing a metanode port. Used in the action to modify metanode
 * port orders, types, etc. It comprises the port type, whether it's connected
 * (only if created from the WFM) and what its index in the list of all
 * in/out ports is.
 *
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @since 2.6
 */
public final class MetaPortInfo {

    private final PortType m_type;
    private final boolean m_isConnected;
    private final String m_message;
    private int m_oldIndex;
    private int m_newIndex;


    /**
     * Creates a new instance from the passed builder.
     */
    private MetaPortInfo(final Builder builder) {
        if(builder.m_type == null) {
            throw new IllegalArgumentException("No port type set.");
        }
        m_type = builder.m_type;
        m_isConnected = builder.m_isConnected;
        m_message = builder.m_message;
        m_oldIndex = builder.m_oldIndex;
        m_newIndex = builder.m_newIndex;
    }

    /** @return the type */
    public PortType getType() {
        return m_type;
    }

    /** @return the isConnected */
    public boolean isConnected() {
        return m_isConnected;
    }

    /** @return the message */
    public String getMessage() {
        return m_message;
    }

    /** @return the oldIndex */
    public int getOldIndex() {
        return m_oldIndex;
    }

    /** @return the newIndex */
    public int getNewIndex() {
        return m_newIndex;
    }

    @Override
    public String toString() {
        return "%s, old->new: %d->%d, connected: %b".formatted(m_type, m_oldIndex, m_newIndex, m_isConnected);
    }

    /**
     * @return a new builder with default values
     * @since 3.5
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @param mpi the object to take the initial values from
     * @return a new builder with the values initialized by the ones given by the passed {@link MetaPortInfo}
     * @since 3.5
     */
    public static Builder builder(final MetaPortInfo mpi) {
        return new Builder().copyFrom(mpi);
    }

    /**
     * Builder to create immutable instances of the {@link MetaPortInfo}-class.
     * @since 3.5
     */
    public static final class Builder {

        private PortType m_type;
        private boolean m_isConnected = false;
        private String m_message = null;
        private int m_oldIndex = -1;
        private int m_newIndex = -1;

        private Builder() {
            //
        }

        private Builder copyFrom(final MetaPortInfo mpi) {
            m_type = mpi.m_type;
            m_isConnected = mpi.m_isConnected;
            m_message = mpi.m_message;
            m_oldIndex = mpi.m_oldIndex;
            m_newIndex = mpi.m_newIndex;
            return this;
        }

        /**
         * @param type the port type
         * @return this
         */
        public Builder setPortType(final PortType type) {
            m_type = type;
            return this;
        }

        /**
         * @param isConnected .. if connected somewhere in (or outside) the flow
         * @return this
         */
        public Builder setIsConnected(final boolean isConnected) {
            m_isConnected = isConnected;
            return this;
        }

        /**
         * @param message The tooltip (only if isConnected)
         * @return this
         */
        public Builder setMessage(final String message) {
            m_message = message;
            return this;
        }

        /**
         * @param oldIndex the port index
         * @return this
         */
        public Builder setOldIndex(final int oldIndex) {
            m_oldIndex = oldIndex;
            return this;
        }

        /** @param newIndex the newIndex to set
         * @return this*/
        public Builder setNewIndex(final int newIndex) {
            m_newIndex = newIndex;
            return this;
        }

        /**
         * @return a new {@link MetaPortInfo}-instance from this builder
         */
        public MetaPortInfo build() {
            return new MetaPortInfo(this);
        }
    }

}
