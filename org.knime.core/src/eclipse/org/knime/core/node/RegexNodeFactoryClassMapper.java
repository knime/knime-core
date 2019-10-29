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
 *   Oct 29, 2019 (awalter): created
 */
package org.knime.core.node;

import java.util.Collections;
import java.util.Map;

import org.knime.core.eclipseUtil.GlobalClassCreator;
import org.knime.core.util.Pair;

/**
 * Base class for {@link NodeFactoryClassMapper}s which map factory classes via regex rules.
 *
 * @author Alison Walter, KNIME GmbH, Konstanz, Germany
 * @since 4.1
 */
@SuppressWarnings("deprecation")
public abstract class RegexNodeFactoryClassMapper extends NodeFactoryClassMapper {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(RegexNodeFactoryClassMapper.class);

    private final Map<String, Pair<String, String>> m_regexRules;

    /**
     * Creates a new regex mapper with the given rules.
     *
     * @param regexRules a {@code Map}. The keys should be a regex pattern for matching factory classes, that should be
     *            mapped by this mapper. The map values should be a {@link Pair} where the first {@code Object} is the
     *            regex pattern to match and replace, and the second {@code Object} is the literal replacement
     *            {@code String}.
     */
    protected RegexNodeFactoryClassMapper(final Map<String, Pair<String, String>> regexRules) {
        m_regexRules = regexRules == null ? Collections.emptyMap() : regexRules;
    }

    /**
     * Returns the {@code Map} containing the regex rules for this mapper.
     * <p>
     * The {@code Map} should have keys which represent the regex patterns to try and match. The map values should be a
     * {@link Pair} where the first {@code Object} is the regex pattern to match and replace, and the second
     * {@code Object} is the literal replacement {@code String}.
     * </p>
     *
     * @return a {@code Map} containing this mappers regex rules
     */
    public Map<String, Pair<String, String>> getRegexRules() {
        return Collections.unmodifiableMap(m_regexRules);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public NodeFactory<? extends NodeModel> mapFactoryClassName(final String factoryClassName) {
        for (final String regexPattern : m_regexRules.keySet()) {
            if (factoryClassName.matches(regexPattern)) {
                final Pair<String, String> replacement = m_regexRules.get(regexPattern);
                final String newClassName =
                    factoryClassName.replaceAll(replacement.getFirst(), replacement.getSecond());
                try {
                    return (NodeFactory<NodeModel>)((GlobalClassCreator.createClass(newClassName)).newInstance());
                } catch (final Exception e) {
                    LOGGER.debug("Could not find class " + newClassName + " for " + factoryClassName);
                }
            }
        }
        return null;
    }

}
