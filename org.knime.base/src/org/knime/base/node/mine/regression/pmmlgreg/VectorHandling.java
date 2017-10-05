/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *   2015. nov. 11. (Gabor Bakos): created
 */
package org.knime.base.node.mine.regression.pmmlgreg;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.knime.core.node.util.CheckUtils;

/**
 * This class helps generate and parse the name of vector references.
 *
 * @author Gabor Bakos
 * @since 3.1
 */
public final class VectorHandling {
    /**
     * Hide constructor.
     */
    private VectorHandling() {

    }
    /**
     * Class to represent a pair of column name and m_index values.
     */
    public static final class NameAndIndex {
        private final String m_name;
        private final int m_index;
        /**
         * @param name The name of the column.
         * @param index The {@code 0}-based position of the value within the vector.
         */
        public NameAndIndex(final String name, final int index) {
            super();
            this.m_name = name;
            CheckUtils.checkArgument(index >= 0, "Index should be nonnegative");
            this.m_index = index;
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return m_name + "[" + m_index + "]";
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + m_index;
            result = prime * result + ((m_name == null) ? 0 : m_name.hashCode());
            return result;
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            NameAndIndex other = (NameAndIndex)obj;
            if (m_index != other.m_index) {
                return false;
            }
            if (m_name == null) {
                if (other.m_name != null) {
                    return false;
                }
            } else if (!m_name.equals(other.m_name)) {
                return false;
            }
            return true;
        }
        /**
         * @return the name
         */
        public String getName() {
            return m_name;
        }
        /**
         * @return the index
         */
        public int getIndex() {
            return m_index;
        }
    }

    //Non-escaped: (.*?)\[\d+\]
    private static final Pattern REFERENCE_PATTERN = Pattern.compile("(.*?)\\[(\\d+)\\]");

    /**
     * Generates vector value reference to a given (nonnegative) position.
     * @param vectorColumnName The name of the vector column.
     * @param index The ({@code 0}-based) m_index of the position.
     * @return The column name followed by {@code [} m_index and {@code ]}.
     */
    public static String valueAt(final String vectorColumnName, final int index) {
        CheckUtils.checkArgumentNotNull(vectorColumnName);
        CheckUtils.checkArgument(index >= 0, "The vector m_index should be nonnegative");
        return vectorColumnName + "[" + index + "]";
    }

    /**
     * Parses a {@link String} like this: {@code colName[23]} and returns a {@link NameAndIndex} if it follows that format, otherwise returns {@link Optional#empty()} or throws an exception..
     *
     * @param nameAndIndex A non-{@code null} {@link String}.
     * @return The parsed information.
     * @throws IllegalArgumentException When the index is not valid.
     */
    public static Optional<NameAndIndex> parse(final String nameAndIndex) {
        Matcher matcher = REFERENCE_PATTERN.matcher(nameAndIndex);
        if (matcher.matches()) {
            return Optional.of(new NameAndIndex(matcher.group(1), Integer.parseInt(matcher.group(2))));
        }
        return Optional.empty();
    }
}
