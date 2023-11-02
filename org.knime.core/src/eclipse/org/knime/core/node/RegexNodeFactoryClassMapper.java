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
import java.util.Optional;

import org.knime.core.node.extension.NodeFactoryProvider;
import org.knime.core.util.Pair;

/**
 * Base class for {@link NodeFactoryClassMapper}s which map factory classes via regex rules.
 *
 * <p>
 * This class exposes the regex rules used for the node mappings, such that the mappings can be replicated in other
 * platforms.
 * </p>
 *
 * @author Alison Walter, KNIME GmbH, Konstanz, Germany
 * @since 4.1
 */
public abstract class RegexNodeFactoryClassMapper extends NodeFactoryClassMapper {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(RegexNodeFactoryClassMapper.class);

    private Map<String, Pair<String, String>> m_regexRules;

    /**
     * Returns a read only {@code Map} containing the regex rules for this mapper.
     * <p>
     * The {@code Map} should have keys which represent the regex patterns to try and match. The map values should be a
     * {@link Pair} where the first {@code Object} is the regex pattern to match and replace, and the second
     * {@code Object} is the literal replacement {@code String}.
     * </p>
     *
     * @return a read only {@code Map} containing this mappers regex rules
     */
    public final Map<String, Pair<String, String>> getRegexRules() {
        if (m_regexRules == null) {
            setRegexRules();
        }
        return m_regexRules;
    }

    @Override
    public final NodeFactory<? extends NodeModel> mapFactoryClassName(final String factoryClassName) {
        for (final String regexPattern : getRegexRules().keySet()) {
            if (factoryClassName.matches(regexPattern)) {
                final Pair<String, String> replacement = getRegexRules().get(regexPattern);
                final String newClassName =
                    factoryClassName.replaceAll(replacement.getFirst(), replacement.getSecond());
                try {
                    Optional<NodeFactory<NodeModel>> replacementFacOptional =
                            NodeFactoryProvider.getInstance().getNodeFactory(newClassName);
                    if (replacementFacOptional.isPresent()) {
                        return replacementFacOptional.get();
                    }
                } catch (final Exception e) {
                    LOGGER.debug("Could not find class " + newClassName + " for " + factoryClassName);
                }
            }
        }
        return null;
    }

    /**
     * Returns the {@code Map} containing the regex rules for this mapper.
     * <p>
     * The {@code Map} should have keys which represent the regex patterns to try and match. The map values should be a
     * {@link Pair} where the first {@code Object} is the regex pattern to match and replace, and the second
     * {@code Object} is the literal replacement {@code String}.
     * </p>
     *
     * <p>
     * This method is for internal use only, the {@link #getRegexRules()} method calls this method and ensures the
     * returned {@code Map} is read only and non-null.
     * </p>
     *
     * @return a {@code Map} containing this mappers regex rules
     */
    protected abstract Map<String, Pair<String, String>> getRegexRulesInternal();

    // -- helper methods --

    /**
     * This method calls {@link #getRegexRulesInternal()}, and caches the result.
     */
    private synchronized void setRegexRules() {
        if (m_regexRules != null) {
            return;
        }
        final Map<String, Pair<String, String>> returned = getRegexRulesInternal();
        m_regexRules = returned == null ? Collections.emptyMap() : Collections.unmodifiableMap(returned);
    }
}
