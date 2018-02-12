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
 *   12.02.2018 (thor): created
 */
package org.knime.product.profiles;

import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Abstract class for replacing variables in preference values. A replacer looks for patterns such as
 * <tt>${prefix:NAME}</tt> and replaces it with a value. The prefix is specific to the replacer and NAME is the
 * variable's name.
 *
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 */
abstract class VariableReplacer {
    /**
     * Replaces environment variables, prefix "env".
     */
    static class EnvVariableReplacer extends VariableReplacer {
        EnvVariableReplacer() {
            super("env");
        }

        @Override
        Optional<String> getVariableValue(final String varName) {
            return Optional.ofNullable(System.getenv(varName));
        }
    }

    /**
     * Replaces system properties, prefix "sysprop".
     */
    static class SyspropVariableReplacer extends VariableReplacer {
        SyspropVariableReplacer() {
            super("sysprop");
        }

        @Override
        Optional<String> getVariableValue(final String varName) {
            return Optional.ofNullable(System.getProperty(varName));
        }
    }

    /**
     * Replaces profile-specific values. Currently only the local profile location ("location") and the name ("name").
     * Prefix is "profile".
     */
    static class ProfileVariableReplacer extends VariableReplacer {
        private final Path m_profileLocation;

        ProfileVariableReplacer(final Path profileLocation) {
            super("profile");
            m_profileLocation = profileLocation;
        }

        @Override
        Optional<String> getVariableValue(final String varName) {
            switch (varName) {
                case "location":  return Optional.of(m_profileLocation.toString());
                case "name":  return Optional.of(m_profileLocation.getFileName().toString());
                default: return Optional.empty();
            }
        }
    }


    private final Pattern m_pattern;

    /**
     * Creates a new replacer with the given variable prefix.
     *
     * @param prefix the prefix, must not be <code>null</code>
     */
    protected VariableReplacer(final String prefix) {
        m_pattern = Pattern.compile("(?<!\\$)(\\$\\{" + prefix + ":([^\\}]+)\\})");
    }

    String replaceVariables(final String value) {
        String newValue = value;
        Matcher m = m_pattern.matcher(value);
        while (m.find()) {
            String replacement = getVariableValue(m.group(2)).orElse(m.group(1));
            newValue = newValue.replace(m.group(1), replacement);
        }

        return newValue;
    }

    /**
     * Get the value of the given variable. If no such variable exists, an empty optional is returned.
     *
     * @param varName the variable's name
     * @return the variable's value or an empty optional
     */
    abstract Optional<String> getVariableValue(String varName);
}
