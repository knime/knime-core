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
 * -------------------------------------------------------------------
 *
 * History
 *   Mar 24, 2024 (wiswedel): created
 */
package org.knime.core.customization.filter;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A predicate for node identifiers based on regular expressions or plain text patterns.
 * It determines node matches for filter/customization settings.
 *
 * @author Bernd Wiswedel
 */
public final class PatternPredicate implements CustomizationPredicate {

    private final List<Pattern> m_patterns;

    private final boolean m_isRegex;

    /** Jackson deserializer. */
    @JsonCreator
    public PatternPredicate(@JsonProperty("patterns") final List<String> patternStrings,
        @JsonProperty("isRegex") final boolean isRegex) {
        m_patterns = patternStrings.stream() //
            .map(patternString -> isRegex ? //
                Pattern.compile(patternString) : //
                Pattern.compile(Pattern.quote(patternString))) //
            .toList();
        m_isRegex = isRegex;
    }

    /**
     * @return the patterns this filter matches
     */
    @JsonProperty("patterns")
    public List<Pattern> getPatterns() {
        return m_patterns;
    }

    @JsonProperty("isRegex")
    boolean isRegex() {
        return m_isRegex;
    }

    @Override
    public boolean matches(final String nodeAndId) {
        return m_patterns.stream().anyMatch(pattern -> pattern.matcher(nodeAndId).matches());
    }

    @Override
    public String toString() {
        String patternStrings = m_patterns.stream().map(Pattern::pattern).collect(Collectors.joining(", "));
        return String.format("PatternFilter{patterns=[%s]}", patternStrings);
    }

}
