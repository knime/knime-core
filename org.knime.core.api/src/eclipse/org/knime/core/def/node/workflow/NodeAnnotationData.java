/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 *
 * History
 *   Oct 27, 2011 (wiswedel): created
 */
package org.knime.core.def.node.workflow;

/**
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public final class NodeAnnotationData extends AnnotationData {

    private final boolean m_isDefault;

    /**  */
    private NodeAnnotationData(final Builder builder) {
        super(builder.setAlignment(TextAlignment.CENTER).setBgColor(0xFFFFFF));
        m_isDefault = builder.m_isDefault;

    }

    /** @return the isDefault */
    public boolean isDefault() {
        return m_isDefault;
    }

    /** {@inheritDoc} */
    @Override
    public NodeAnnotationData clone() {
        //we should not provide a clone method
        //e.g. conflicts with the final-fields
        //see also https://stackoverflow.com/questions/2427883/clone-vs-copy-constructor-which-is-recommended-in-java
        throw new UnsupportedOperationException();
    }

    public static NodeAnnotationData createFromObsoleteCustomName(
            final String customName) {
        if (customName == null) {
            return NodeAnnotationData.builder().setIsDefault(true).build();
        }
        NodeAnnotationData result = NodeAnnotationData.builder().setIsDefault(false).setText(customName).build();
        return result;
    }

    /** @return builder with defaults */
    public static final Builder builder() {
        return new Builder();
    }

    /**
     * @param annoData object to copy the values from
     * @param includeBounds Whether to also update x, y, width, height. If false, it will only a copy the text with
         *            its styles
     * @return new Builder with the values copied from the passed argument
     */
    public static final Builder builder(final NodeAnnotationData annoData, final boolean includeBounds) {
        return new Builder().copyFrom(annoData, includeBounds);
    }

    /** Builder pattern for {@link NodeAnnotationData} */
    public static final class Builder extends AnnotationData.Builder<Builder, NodeAnnotationData> {

        private boolean m_isDefault;

        /**
         *
         */
        public Builder() {
            //
        }

        /**
         * Copy content, styles, position from a {@link NodeAnnotationData}-object.
         *
         * @param otherData To copy from.
         * @param includeBounds Whether to also update x, y, width, height. If false, it will only a copy the text with
         *            its styles
         * @return this
         */
        public Builder copyFrom(final NodeAnnotationData otherData,
                final boolean includeBounds) {
            m_isDefault = otherData.m_isDefault;
            super.copyFrom(otherData, includeBounds);
            return this;
        }

        /**
         * @param isDefault
         * @return this
         */
        public Builder setIsDefault(final boolean isDefault) {
            m_isDefault = isDefault;
            return this;
        }

        /** @return {@link NodeAnnotationData} with current values. */
        @Override
        public NodeAnnotationData build() {
            return new NodeAnnotationData(this);
        }

    }

}
