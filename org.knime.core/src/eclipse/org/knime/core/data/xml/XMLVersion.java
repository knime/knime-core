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
 *   Aug 24, 2024 (lw): created
 */
package org.knime.core.data.xml;

import java.util.regex.Pattern;

import org.apache.commons.lang3.RegExUtils;
import org.knime.core.data.xml.io.XMLCellWriterFactory;

/**
 * Denotes all XML versions and their (in-)valid character ranges.
 *
 * @author Leon Wenzler, KNIME GmbH, Konstanz, Germany
 * @since 5.4
 */
public enum XMLVersion {

        /**
         * XML version {@code 1.0}. <br> Valid character range is defined in the
         * <a href="https://www.w3.org/TR/xml/#charsets">W3C recommendation for XML 1.0</a>.
         */
        V_1_0("1.0", "\\x09\\x0A\\x0D\\x20-\\uD7FF\\uE000-\\uFFFD\\u10000-\\u10FFFF"),

        /**
         * XML version {@code 1.1}. <br> Valid character range is defined in the
         * <a href="https://www.w3.org/TR/2006/REC-xml11-20060816/#charsets">W3C recommendation for XML 1.1</a>.
         */
        V_1_1("1.1", "\\x01-\\uD7FF\\uE000-\\uFFFD\\u10000-\\u10FFFF");

    private final String m_version;

    private final Pattern m_invalidChars;

    XMLVersion(final String version, final String validChars) {
        m_version = version;
        m_invalidChars = Pattern.compile("[^%s]".formatted(validChars));
    }

    /**
     * Returns the default {@link XMLVersion} used by the {@link XMLCellWriterFactory}.
     *
     * @return default XML version
     */
    public static XMLVersion getCellDefault() {
        return XMLVersion.V_1_0;
    }

    /**
     * Pattern matching the range invalid characters in this XML version. Useful for cleaning
     * XML strings before parsing, e.g. with {@link RegExUtils#removeAll(String, Pattern)}.
     *
     * @return pattern
     */
    public Pattern invalidChars() {
        return m_invalidChars;
    }

    /**
     * Returns the actual version string (e.g. "1.0") instead of the {@link #name()}.
     */
    @Override
    public String toString() {
        return m_version;
    }
}
