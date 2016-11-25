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
 *   Jul 15, 2016 (wiswedel): created
 */
package org.knime.core.node.tableview;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * The search string as entered by the user (Ctrl-F) with some more search options.
 * @author wiswedel
 */
final class SearchString {

    private final String m_searchString;
    private final boolean m_ignoreCase;
    private final boolean m_isRegex;
    private final Pattern m_pattern;

    /**
     * @param searchString
     * @param ignoreCase
     * @param isRegex
     * @throws PatternSyntaxException
     */
    SearchString(final String searchString, final boolean ignoreCase, final boolean isRegex) {
        m_searchString = searchString;
        m_ignoreCase = ignoreCase;
        m_isRegex = isRegex;
        if (isRegex) {
            int flags = m_ignoreCase ? Pattern.CASE_INSENSITIVE : 0;
            m_pattern = Pattern.compile(searchString, flags);
        } else {
            m_pattern = null;
        }
    }

    /** Matches the string according to the settings.
     * @param str ...
     * @return ...
     */
    boolean matches(final String str) {
        if (str == null) {
            return false;
        }
        if (m_isRegex) {
            return m_pattern.matcher(str).matches();
        }
        if (m_ignoreCase) {
            return StringUtils.containsIgnoreCase(str, m_searchString);
        }
        return StringUtils.contains(str, m_searchString);
    }

    /** @return the searchString */
    String getSearchString() {
        return m_searchString;
    }

    /** @return the ignoreCase */
    boolean isIgnoreCase() {
        return m_ignoreCase;
    }

    /** @return the isRegex */
    boolean isRegex() {
        return m_isRegex;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj, true);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this, true);
    }

}
