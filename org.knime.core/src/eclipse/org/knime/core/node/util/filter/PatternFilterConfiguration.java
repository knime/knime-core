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
 * ---------------------------------------------------------------------
 *
 * Created on Oct 4, 2013 by Patrick Winter, KNIME AG, Zurich, Switzerland
 */
package org.knime.core.node.util.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.filter.NameFilterConfiguration.FilterResult;

/**
 * Configuration to the PatternFilterPanel.
 *
 * @author Patrick Winter, KNIME AG, Zurich, Switzerland
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 * @since 3.4
 * @noreference This class is not intended to be referenced by clients outside KNIME core.
 */
public class PatternFilterConfiguration implements Cloneable {

    /** The identifier for this filter type. */
    public static final String TYPE = "name_pattern";

    /** Type of pattern filter. */
    enum PatternFilterType {
            /** The pattern will be interpreted as a wildcard (with '*' and '?'). */
            Wildcard, // NOSONAR: better settings strings
            /** The pattern will be interpreted as a regular expression. */
            Regex; // NOSONAR: better settings strings

        /** Parse from string, fail if invalid. */
        static PatternFilterType parseType(final String type) throws InvalidSettingsException {
            try {
                return valueOf(type);
            } catch (IllegalArgumentException | NullPointerException e) { // NOSONAR: these are user supplied and may be null
                throw new InvalidSettingsException("Illegal pattern type: " + type, e);
            }
        }

        /** Parse from string, return default if fail. */
        static PatternFilterType parseType(final String type, final PatternFilterType defaultValue) {
            try {
                return valueOf(type);
            } catch (IllegalArgumentException | NullPointerException e) { // NOSONAR: these are user supplied and may be null
                return defaultValue;
            }
        }
    }

    private static final String CFG_PATTERN = "pattern";

    private static final String CFG_TYPE = "type";

    private static final String CFG_CASESENSITIVE = "caseSensitive";

    private static final String CFG_EXCLUDEMATCHING = "excludeMatching";

    private String m_pattern = "";

    private PatternFilterType m_type = PatternFilterType.Wildcard;

    private boolean m_caseSensitive = true;

    private boolean m_excludeMatching = false;

    /**
     * Protected constructor.
     */
    protected PatternFilterConfiguration() {
    }

    /**
     * Loads the configuration from the given settings object. Fails if not valid.
     *
     * @param settings Settings object containing the configuration.
     * @throws InvalidSettingsException If settings are invalid
     */
    protected void loadConfigurationInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_pattern = settings.getString(CFG_PATTERN);
        if (m_pattern == null) {
            throw new InvalidSettingsException("Pattern must not be null");
        }
        final var typeS = settings.getString(CFG_TYPE);
        m_type = PatternFilterType.parseType(typeS);
        m_caseSensitive = settings.getBoolean(CFG_CASESENSITIVE);
        // since 4.5.0
        m_excludeMatching = settings.getBoolean(CFG_EXCLUDEMATCHING, false);
        try {
            compilePattern(m_pattern, m_type, m_caseSensitive);
        } catch (PatternSyntaxException e) {
            throw new InvalidSettingsException("The pattern is invalid", e);
        }
    }

    /**
     * Loads the configuration from the given settings object. Sets defaults if invalid.
     *
     * @param settings Settings object containing the configuration.
     */
    protected void loadConfigurationInDialog(final NodeSettingsRO settings) {
        m_pattern = settings.getString(CFG_PATTERN, null);
        if (m_pattern == null) { // can also be deliberately null from the settings
            m_pattern = "";
        }
        final var typeS = settings.getString(CFG_TYPE, null);
        m_type = PatternFilterType.parseType(typeS, PatternFilterType.Wildcard);
        m_caseSensitive = settings.getBoolean(CFG_CASESENSITIVE, true);
        // since 4.5.0
        m_excludeMatching = settings.getBoolean(CFG_EXCLUDEMATCHING, false);
    }

    /**
     * Save the current configuration inside the given settings object.
     *
     * @param settings Settings object the current configuration will be put into.
     */
    protected void saveConfiguration(final NodeSettingsWO settings) {
        settings.addString(CFG_PATTERN, m_pattern);
        settings.addString(CFG_TYPE, m_type.name());
        settings.addBoolean(CFG_CASESENSITIVE, m_caseSensitive);
        // since 4.5.0
        settings.addBoolean(CFG_EXCLUDEMATCHING, m_excludeMatching);
    }

    /**
     * Applies this configuration to the array of given names.
     *
     * @param names The names to check
     * @return FilterResult with the included and excluded names
     */
    public FilterResult applyTo(final String[] names) {
        final var regex = compilePattern(m_pattern, m_type, m_caseSensitive);
        List<String> matched = new ArrayList<>();
        List<String> notMatched = new ArrayList<>();
        for (String name : names) {
            if (regex.matcher(name).matches()) {
                matched.add(name);
            } else {
                notMatched.add(name);
            }
        }
        if (!m_excludeMatching) {
            return new FilterResult(matched, notMatched, new ArrayList<>(), new ArrayList<>());
        } else {
            return new FilterResult(notMatched, matched, new ArrayList<>(), new ArrayList<>());
        }
    }

    /**
     * Creates a regex pattern from the given pattern string and with the given settings.
     *
     * @param pattern The string containing the pattern
     * @param type The strings type of pattern
     * @param caseSensitive If case sensitivity should be enabled
     * @return The regex pattern
     * @throws PatternSyntaxException If the pattern could not be compiled
     */
    public static Pattern compilePattern(final String pattern, final PatternFilterType type,// NOSONAR: keep because of backwards compatibility
        final boolean caseSensitive) {
        Pattern regex;
        var regexString = pattern;
        if (type == PatternFilterType.Wildcard) {
            regexString = wildcardToRegex(pattern);
        }
        if (caseSensitive) {
            regex = Pattern.compile(regexString);
        } else {
            regex = Pattern.compile(regexString, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        }
        return regex;
    }

    /**
     * @return the pattern
     */
    String getPattern() {
        return m_pattern;
    }

    /**
     * @param pattern the pattern to set
     */
    void setPattern(final String pattern) {
        m_pattern = pattern;
    }

    /**
     * @return the type
     */
    PatternFilterType getType() {
        return m_type;
    }

    /**
     * @param type the type to set
     */
    void setType(final PatternFilterType type) {
        m_type = type;
    }

    /**
     * @return whether the pattern is case sensitive
     */
    boolean isCaseSensitive() {
        return m_caseSensitive;
    }

    /**
     * @param caseSensitive the caseSensitive to set
     */
    void setCaseSensitive(final boolean caseSensitive) {
        m_caseSensitive = caseSensitive;
    }

    /**
     * @return whether matching names shall be excluded
     * @since 4.5.0
     */
    boolean isExcludeMatching() {
        return m_excludeMatching;
    }

    /**
     * @param excludeMatching whether matching names shall be excluded
     * @since 4.5.0
     */
    void setExcludeMatching(final boolean excludeMatching) {
        m_excludeMatching = excludeMatching;
    }


    /** {@inheritDoc} */
    @Override
    public String toString() {
        return String.format("(%s - %scase sensitive - %s matching): %s", m_type, m_caseSensitive ? "" : "not ",
            m_excludeMatching ? "exclude" : "include", m_pattern);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(m_caseSensitive, m_excludeMatching, m_pattern, m_type);
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
        PatternFilterConfiguration other = (PatternFilterConfiguration)obj;
        return m_caseSensitive == other.m_caseSensitive && m_excludeMatching == other.m_excludeMatching
            && Objects.equals(m_pattern, other.m_pattern) && m_type == other.m_type;
    }

    /** {@inheritDoc} */
    @Override
    protected PatternFilterConfiguration clone() {// NOSONAR: other methods depend on this clone
        try {
            return (PatternFilterConfiguration)super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Object not clonable although it implements java.lang.Clonable", e); // NOSONAR: this shouldn't happen
        }
    }

    private static String wildcardToRegex(final String wildcard) {// NOSONAR: most cases are simple fallthroughs
        final var buf = new StringBuilder(wildcard.length() + 20);
        for (var i = 0; i < wildcard.length(); i++) {
            final var c = wildcard.charAt(i);
            switch (c) {
                case '*':
                    buf.append(".*");
                    break;
                case '?':
                    buf.append(".");
                    break;
                case '\\':
                case '^':
                case '$':
                case '[':
                case ']':
                case '{':
                case '}':
                case '(':
                case ')':
                case '|':
                case '+':
                case '.':
                    buf.append("\\");
                    buf.append(c);
                    break;
                default:
                    buf.append(c);
            }
        }
        return buf.toString();
    }

}
