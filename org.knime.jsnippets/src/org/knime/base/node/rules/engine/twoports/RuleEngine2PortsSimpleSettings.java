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
 *   1 June 2015 (Gabor): created
 */
package org.knime.base.node.rules.engine.twoports;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * The common settings for the Rule * (Dictionary) nodes.
 *
 * @author Gabor Bakos
 */
class RuleEngine2PortsSimpleSettings {

    /**
     * Config key for rules column.
     */
    protected static final String RULES_COLUMN = "rules.column";
    /** Config key for the outcomes column (which can be {@code <none>}). */
    protected static final String OUTCOMES_COLUMN = "outcomes.column";
    /** Config key for whether we need to force int compatibility at the output. */
    private static final String DISALLOW_LONG_OUTPUT_FOR_COMPATIBILITY = "disallowLongOutputForCompatibility";
    /** The name of the rule column. */
    private String m_ruleColumn = null;
    /** The name of the outcome column.*/
    private String m_outcomeColumn = null;
    /** since 3.2 the node can produce long output ($$ROWINDEX$$) but old instances of that node in old workflows
     * will map that to int ... will force that using this (hidden) setting. */
    private boolean m_disallowLongOutputForCompatibility = false;

    /**
     * Constructs the simple settings.
     */
    public RuleEngine2PortsSimpleSettings() {
        super();
    }

    /**
     * @return the ruleColumn
     */
    protected final String getRuleColumn() {
        return m_ruleColumn;
    }

    /**
     * @param ruleColumn the ruleColumn to set
     */
    protected final void setRuleColumn(final String ruleColumn) {
        this.m_ruleColumn = ruleColumn;
    }

    /**
     * @return the disallowLongOutputForCompatibility
     * @since 3.2
     */
    protected final boolean isDisallowLongOutputForCompatibility() {
        return m_disallowLongOutputForCompatibility;
    }

    /**
     * @param outcomeColumn the outcomeColumn to set
     */
    public void setOutcomeColumn(final String outcomeColumn) {
        this.m_outcomeColumn = outcomeColumn;
    }

    /**
     * @return the outcomeColumn
     */
    protected String getOutcomeColumn() {
        return m_outcomeColumn;
    }

    /**
     * @param inSpec An input table's spec.
     * @param cls The compatible {@link Class} of the expected column's type.
     * @return The first column's name with the specified type, or {@code null}.
     */
    protected static String columnNameOrNull(final DataTableSpec inSpec, final Class<? extends DataValue> cls) {
        if (!inSpec.containsCompatibleType(cls)) {
            return null;
        }
        for (DataColumnSpec dataColumnSpec : inSpec) {
            if (dataColumnSpec.getType().isCompatible(cls)) {
                return dataColumnSpec.getName();
            }
        }
        assert false : inSpec;
        return null;
    }

    /**
     * Called from dialog when settings are to be loaded.
     *
     * @param settings To load from
     * @param inSpec Input data spec
     * @param secondSpec Rules table spec
     */
    protected void loadSettingsDialog(final NodeSettingsRO settings, final DataTableSpec inSpec, final DataTableSpec secondSpec) {
        String ruleColumn = inSpec == null ? null : columnNameOrNull(secondSpec, StringValue.class);
        m_ruleColumn = settings.getString(RULES_COLUMN, ruleColumn);
        m_outcomeColumn = settings.getString(OUTCOMES_COLUMN, null);
        m_disallowLongOutputForCompatibility = settings.getBoolean(DISALLOW_LONG_OUTPUT_FOR_COMPATIBILITY, false);
    }

    /**
     * Called from model when settings are to be loaded.
     *
     * @param settings To load from
     * @throws InvalidSettingsException If settings are invalid.
     */
    protected void loadSettingsModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_ruleColumn = settings.getString(RULES_COLUMN);
        m_outcomeColumn = settings.getString(OUTCOMES_COLUMN);
        // added in 3.2
        m_disallowLongOutputForCompatibility = settings.getBoolean(DISALLOW_LONG_OUTPUT_FOR_COMPATIBILITY, true);
    }

    /**
     * Called from model and dialog to save current settings.
     *
     * @param settings To save to.
     */
    protected void saveSettings(final NodeSettingsWO settings) {
        settings.addString(RULES_COLUMN, m_ruleColumn);
        settings.addString(OUTCOMES_COLUMN, m_outcomeColumn);
        settings.addBoolean(DISALLOW_LONG_OUTPUT_FOR_COMPATIBILITY, m_disallowLongOutputForCompatibility);
    }
}