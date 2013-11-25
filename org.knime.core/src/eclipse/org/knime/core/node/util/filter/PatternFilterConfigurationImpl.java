/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 * Created on Oct 4, 2013 by Patrick Winter, KNIME.com AG, Zurich, Switzerland
 */
package org.knime.core.node.util.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.ConvenienceMethods;
import org.knime.core.node.util.filter.NameFilterConfiguration.FilterResult;

/**
 * Configuration to the PatternFilterPanel.
 *
 * @author Patrick Winter, KNIME.com AG, Zurich, Switzerland
 * @since 2.9
 */
final class PatternFilterConfigurationImpl implements Cloneable {

    /** The identifier for this filter type. */
    public static final String TYPE = "name_pattern";

    /** Type of pattern filter. */
    enum PatternFilterType {
        /** The pattern will be interpreted as a wildcard (with '*' and '?'). */
        Wildcard,
        /** The pattern will be interpreted as a regular expression. */
        Regex;

        /** Parse from string, fail if invalid. */
        static PatternFilterType parseType(final String type) throws InvalidSettingsException {
            try {
                return valueOf(type);
            } catch (Exception e) {
                throw new InvalidSettingsException("Illegal pattern type: " + type, e);
            }
        }

        /** Parse from string, return default if fail. */
        static PatternFilterType parseType(final String type, final PatternFilterType defaultValue) {
            try {
                return valueOf(type);
            } catch (Exception e) {
                return defaultValue;
            }
        }
    }

    private static final String CFG_PATTERN = "pattern";

    private static final String CFG_TYPE = "type";

    private static final String CFG_CASESENSITIVE = "caseSensitive";

    private String m_pattern = "";

    private PatternFilterType m_type = PatternFilterType.Wildcard;

    private boolean m_caseSensitive = true;

    /** Loads the configuration from the given settings object. Fails if not valid.
     * @param settings Settings object containing the configuration.
     * @throws InvalidSettingsException If settings are invalid
     */
    void loadConfigurationInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_pattern = settings.getString(CFG_PATTERN);
        if (m_pattern == null) {
            throw new InvalidSettingsException("Pattern must not be null");
        }
        String typeS = settings.getString(CFG_TYPE);
        m_type = PatternFilterType.parseType(typeS);
        m_caseSensitive = settings.getBoolean(CFG_CASESENSITIVE);
        try {
            compilePattern(m_pattern, m_type, m_caseSensitive);
        } catch (PatternSyntaxException e) {
            throw new InvalidSettingsException("The pattern is invalid", e);
        }
    }

    /** Loads the configuration from the given settings object. Sets defaults if invalid.
     * @param settings Settings object containing the configuration.
     */
    void loadConfigurationInDialog(final NodeSettingsRO settings) {
        m_pattern = settings.getString(CFG_PATTERN, null);
        if (m_pattern == null) { // can also be deliberately null from the settings
            m_pattern = "";
        }
        String typeS = settings.getString(CFG_TYPE, null);
        m_type = PatternFilterType.parseType(typeS, PatternFilterType.Wildcard);
        m_caseSensitive = settings.getBoolean(CFG_CASESENSITIVE, true);
    }

    /** Save the current configuration inside the given settings object.
     * @param settings Settings object the current configuration will be put into.
     */
    void saveConfiguration(final NodeSettingsWO settings) {
        settings.addString(CFG_PATTERN, m_pattern);
        settings.addString(CFG_TYPE, m_type.name());
        settings.addBoolean(CFG_CASESENSITIVE, m_caseSensitive);
    }

    /**
     * Applies this configuration to the array of given names.
     *
     * @param names The names to check
     * @return FilterResult with the included and excluded names
     */
    public FilterResult applyTo(final String[] names) {
        Pattern regex = compilePattern(m_pattern, m_type, m_caseSensitive);
        List<String> incls = new ArrayList<String>();
        List<String> excls = new ArrayList<String>();
        for (String name : names) {
            if (regex.matcher(name).matches()) {
                incls.add(name);
            } else {
                excls.add(name);
            }
        }
        return new FilterResult(incls, excls, new ArrayList<String>(), new ArrayList<String>());
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
    public static Pattern compilePattern(final String pattern, final PatternFilterType type,
        final boolean caseSensitive) throws PatternSyntaxException {
        Pattern regex;
        String regexString = pattern;
        if (type.equals(PatternFilterType.Wildcard)) {
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
     * @return the caseSensitive
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

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        // eclipse auto-generated
        final int prime = 31;
        int result = 1;
        result = prime * result + (m_caseSensitive ? 1231 : 1237);
        result = prime * result + ((m_pattern == null) ? 0 : m_pattern.hashCode());
        result = prime * result + ((m_type == null) ? 0 : m_type.hashCode());
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof PatternFilterConfigurationImpl)) {
            return false;
        }
        PatternFilterConfigurationImpl o = (PatternFilterConfigurationImpl)obj;
        if (o.m_caseSensitive != m_caseSensitive) {
            return false;
        }
        if (!ConvenienceMethods.areEqual(o.m_pattern, m_pattern)) {
            return false;
        }
        if (!ConvenienceMethods.areEqual(o.m_type, m_type)) {
            return false;
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return String.format("(%s - %s): %s", m_type, (m_caseSensitive ? "" : "not ") + "case sensitive", m_pattern);
    }

    /** {@inheritDoc} */
    @Override
    protected PatternFilterConfigurationImpl clone() {
        try {
            return (PatternFilterConfigurationImpl)super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Object not clonable although it implements java.lang.Clonable", e);
        }
    }

    private static String wildcardToRegex(final String wildcard) {
        StringBuilder buf = new StringBuilder(wildcard.length() + 20);
        for (int i = 0; i < wildcard.length(); i++) {
            char c = wildcard.charAt(i);
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
