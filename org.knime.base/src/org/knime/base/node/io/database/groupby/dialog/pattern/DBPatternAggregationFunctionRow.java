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
 *   20.08.2014 (koetter): created
 */
package org.knime.base.node.io.database.groupby.dialog.pattern;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.knime.base.node.io.database.groupby.dialog.AbstractDBAggregationFunctionRow;
import org.knime.base.node.io.database.groupby.dialog.DBAggregationFunctionProvider;
import org.knime.base.util.WildcardMatcher;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.database.DatabaseUtility;
import org.knime.core.node.port.database.aggregation.AggregationFunctionProvider;
import org.knime.core.node.port.database.aggregation.DBAggregationFunction;

/**
 *
 * @author Tobias Koetter, KNIME.com, Zurich, Switzerland
 */
public class DBPatternAggregationFunctionRow extends AbstractDBAggregationFunctionRow<DBAggregationFunction> {

    private static final String CNFG_INPUT_PATTERN = "inputPattern";
    private static final String CNFG_IS_REGEX = "isRegularExpression";

    private final String m_inputPattern;

    private final boolean m_isRegex;

    private final Pattern m_pattern;

    /**
     * @param pattern the input pattern entered by the user
     * @param isRegex <code>true</code> if the pattern is a regular expression otherwise it is interpreted as
     * a string with wildcards.
     * @param method the {@link DBAggregationFunction}
     */
    public DBPatternAggregationFunctionRow(final String pattern, final boolean isRegex,
        final DBAggregationFunction method) {
        super(method);
        m_inputPattern = pattern;
        m_isRegex = isRegex;
        Pattern regexPattern = null;
        if (m_isRegex) {
            try {
                regexPattern = Pattern.compile(pattern);
            } catch (PatternSyntaxException e) {
                //catch regular expression syntax exceptions
            }
        } else {
            final String wildcardToRegex = WildcardMatcher.wildcardToRegex(pattern);
            try {
                regexPattern = Pattern.compile(wildcardToRegex);
            } catch (PatternSyntaxException ex) {
                //catch regular expression syntax exceptions
            }
        }
        m_pattern = regexPattern;
        setValid(m_pattern != null);
    }

    /**
     * @return <code>true</code> if the input pattern was a regular expression otherwise <code>false</code>
     */
    public boolean isRegex() {
        return m_isRegex;
    }

    /**
     * @return the pattern or <code>null</code> if the regular expression is not valid
     * @see #isValid()
     */
    public Pattern getRegexPattern() {
        return m_pattern;
    }

    /**
     * @return the search pattern the user has entered
     */
    public String getInputPattern() {
        return m_inputPattern;
    }

    /**
     * @param settings {@link NodeSettingsWO}
     * @param key the config key
     * @param rows the {@link DBPatternAggregationFunctionRow}s to save
     */
    public static void saveFunctions(final NodeSettingsWO settings, final String key,
        final List<DBPatternAggregationFunctionRow> rows) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("key must not be empty");
        }
        if (settings == null) {
            throw new NullPointerException("settings must not be null");
        }
        if (rows == null) {
            return;
        }
        final NodeSettingsWO root = settings.addNodeSettings(key);
        for (int i = 0, length = rows.size(); i < length; i++) {
            final NodeSettingsWO cfg = root.addNodeSettings("f_" + i);
            final DBPatternAggregationFunctionRow row = rows.get(i);
            final String inputPattern = row.getInputPattern();
            final boolean isRegex = row.isRegex();
            cfg.addString(CNFG_INPUT_PATTERN, inputPattern);
            cfg.addBoolean(CNFG_IS_REGEX, isRegex);
            DBAggregationFunction function = row.getFunction();
            AbstractDBAggregationFunctionRow.saveFunction(cfg, function);
        }
    }


    /**
     * Loads the functions and handles invalid aggregation functions graceful.
     * @param settings {@link NodeSettingsRO}
     * @param key the config key
     * @param dbIdentifier the {@link AggregationFunctionProvider}
     * @param tableSpec the input {@link DataTableSpec}
     * @return {@link List} of {@link DBPatternAggregationFunctionRow}s
     * @throws InvalidSettingsException if the settings are invalid
     */
    public static List<DBPatternAggregationFunctionRow> loadFunctions(final NodeSettingsRO settings, final String key,
        final String dbIdentifier, final DataTableSpec tableSpec) throws InvalidSettingsException {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("key must not be null");
        }
        if (dbIdentifier == null || dbIdentifier.isEmpty()) {
            throw new IllegalArgumentException("dbIdentifier must not be empty");
        }
        if (settings == null || !settings.containsKey(key)) {
            return Collections.EMPTY_LIST;
        }
        final DatabaseUtility utility = DatabaseUtility.getUtility(dbIdentifier);
        DBAggregationFunctionProvider functionProvider = new DBAggregationFunctionProvider(utility);
        final NodeSettingsRO root = settings.getNodeSettings(key);
        final Set<String> settingsKeys = root.keySet();
        final List<DBPatternAggregationFunctionRow> colAggrList = new ArrayList<>(settingsKeys.size());
        for (String settingsKey : settingsKeys) {
            final NodeSettingsRO cfg = root.getNodeSettings(settingsKey);
            final String inputPattern = cfg.getString(CNFG_INPUT_PATTERN);
            final boolean isRegex = cfg.getBoolean(CNFG_IS_REGEX);
            final DBAggregationFunction function =
                    AbstractDBAggregationFunctionRow.loadFunction(tableSpec, functionProvider, cfg);
            final DBPatternAggregationFunctionRow aggrFunctionRow =
                    new DBPatternAggregationFunctionRow(inputPattern, isRegex, function);
            colAggrList.add(aggrFunctionRow);
        }
        return colAggrList;
    }
}
