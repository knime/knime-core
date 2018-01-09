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
 *   24.01.2012 (hofer): created
 */
package org.knime.base.node.jsnippet.type.flowvar;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Abstract implementation of type converter that can convert to and from
 * a given list of java types.
 *
 * @author Heiko Hofer
 */
@SuppressWarnings("rawtypes")
public abstract class AbstractTypeConverter implements TypeConverter {
        private Class[] m_canJavaTypes;
        private Class m_preferredJavaType;

        /**
         * Create a new converter that can convert to the given java types.
         *
         * @param canJavaTypes the java class this converter can convert to.
         */
        public AbstractTypeConverter(final Class... canJavaTypes) {
            super();
            m_preferredJavaType = canJavaTypes[0];
            Arrays.sort(canJavaTypes, new Comparator<Class>() {

                @Override
                public int compare(final Class o1, final Class o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });
            m_canJavaTypes = canJavaTypes;
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public Class[] canProvideJavaTypes() {
            return m_canJavaTypes;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Class getPreferredJavaType() {
            return m_preferredJavaType;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean canProvideJavaType(final Class javaType) {
            return Arrays.binarySearch(m_canJavaTypes, javaType,
                    new Comparator<Class>() {

                @Override
                public int compare(final Class o1, final Class o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            }) >= 0;
        }

        /**
        * {@inheritDoc}
        */
        @Override
        public Class[] canCreatedFromJavaTypes() {
            // By default it is assumed that a knime cell or flow variable
            // can be created from the data types that it can provide.
            return m_canJavaTypes;
        }
}
