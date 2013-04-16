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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 */
package org.knime.base.node.preproc.binnerdictionary;

import org.knime.core.data.BoundedValue;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.NominalValue;
import org.knime.core.data.date.DateAndTimeValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;

/** Configuration proxy to node.
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
final class BinByDictionaryConfiguration {

    private String m_valueColumnPort0;
    private String m_lowerBoundColumnPort1;
    private boolean m_lowerBoundInclusive;
    private String m_upperBoundColumnPort1;
    private boolean m_upperBoundInclusive;
    private String m_labelColumnPort1;
    private boolean m_failIfNoRuleMatches;

    /** @return the valueColumnPort0 */
    String getValueColumnPort0() {
        return m_valueColumnPort0;
    }
    /** @param colName the valueColumnPort0 to set */
    void setValueColumnPort0(final String colName) {
        m_valueColumnPort0 = colName;
    }
    /** @return the column name for lower bound.  */
    String getLowerBoundColumnPort1() {
        return m_lowerBoundColumnPort1;
    }
    /** @param colName the column name for lower bound. */
    void setLowerBoundColumnPort1(final String colName) {
        m_lowerBoundColumnPort1 = colName;
    }
    /** @return the lowerBoundInclusive */
    boolean isLowerBoundInclusive() {
        return m_lowerBoundInclusive;
    }
    /** @param lowerBoundInclusive the lowerBoundInclusive to set */
    void setLowerBoundInclusive(final boolean lowerBoundInclusive) {
        m_lowerBoundInclusive = lowerBoundInclusive;
    }
    /** @return the column name for upper bound. */
    String getUpperBoundColumnPort1() {
        return m_upperBoundColumnPort1;
    }
    /** @param colName the column name for upper bound. */
    void setUpperBoundColumnPort1(final String colName) {
        m_upperBoundColumnPort1 = colName;
    }
    /** @return the upperBoundInclusive */
    boolean isUpperBoundInclusive() {
        return m_upperBoundInclusive;
    }
    /** @param upperBoundInclusive the upperBoundInclusive to set */
    void setUpperBoundInclusive(final boolean upperBoundInclusive) {
        m_upperBoundInclusive = upperBoundInclusive;
    }
    /** @return the labelColumnPort1 */
    String getLabelColumnPort1() {
        return m_labelColumnPort1;
    }
    /** @param labelColumn the label column to set */
    void setLabelColumnPort1(final String labelColumn) {
        m_labelColumnPort1 = labelColumn;
    }

    /** @return the failIfNoRuleMatches */
    boolean isFailIfNoRuleMatches() {
        return m_failIfNoRuleMatches;
    }
    /** @param failIfNoRuleMatches the failIfNoRuleMatches to set */
    void setFailIfNoRuleMatches(final boolean failIfNoRuleMatches) {
        m_failIfNoRuleMatches = failIfNoRuleMatches;
    }
    /** Load settings in model.
     * @param settings To load from.
     * @throws InvalidSettingsException */
    void loadSettingsModel(final NodeSettingsRO settings)
        throws InvalidSettingsException {
        m_valueColumnPort0 = settings.getString("valueColumnPort0");
        m_lowerBoundColumnPort1 = settings.getString("lowerBoundColumnPort1");
        m_lowerBoundInclusive = settings.getBoolean("lowerBoundInclusive");
        m_upperBoundColumnPort1 = settings.getString("upperBoundColumnPort1");
        m_upperBoundInclusive = settings.getBoolean("upperBoundInclusive");
        m_labelColumnPort1 = settings.getString("labelColumnPort1");
        m_failIfNoRuleMatches = settings.getBoolean("failIfNoRuleMatches");
    }

    /** Load settings in dialog.
      * @param settings To load from.
      * @param ins Input specs for initialization.
      * @throws NotConfigurableException if no appropriate columns in input. */
    void loadSettingsDialog(final NodeSettingsRO settings,
            final DataTableSpec[] ins) throws NotConfigurableException {
        String valueColPort0 = null;
        String lowerBoundColPort1 = null;
        String upperBoundColPort1 = null;
        String labelColumnPort1 = null;

        // find default values in the table, try be smart and favor
        // date columns over int over double over ... etc.
        @SuppressWarnings("unchecked")
        Class<? extends DataValue>[] typeCandidates = new Class[] {
                DateAndTimeValue.class, IntValue.class, DoubleValue.class,
                LongValue.class, BoundedValue.class, DataValue.class};

        for (Class<? extends DataValue> valueClass : typeCandidates) {
            for (DataColumnSpec c : ins[0]) {
                if (c.getType().isCompatible(valueClass)) {
                    valueColPort0 = c.getName();
                    break;
                }
            }
            if (valueColPort0 == null) {
                // no such type in input, continue with next one
                continue;
            }
            for (int i = ins[1].getNumColumns(); --i >= 0;) {
                DataColumnSpec c = ins[1].getColumnSpec(i);
                if (c.getType().isCompatible(valueClass)) {
                    if (upperBoundColPort1 == null) {
                        upperBoundColPort1 = c.getName();
                    } else if (lowerBoundColPort1 == null) {
                        lowerBoundColPort1 = c.getName();
                        break;
                    }
                }
            }
            if (upperBoundColPort1 != null) {
                // found end value and possibly a start value, break
                // and accept these columns as default
                break;
            }
        }
        if (valueColPort0 == null) {
            throw new NotConfigurableException(
                    "No value column in first input");
        }
        if (upperBoundColPort1 == null) {
            throw new NotConfigurableException(
                    "No reasonable column in second input");
        }
        m_valueColumnPort0 = settings.getString(
                "valueColumnPort0", valueColPort0);
        m_lowerBoundColumnPort1 = settings.getString(
                "lowerBoundColumnPort1", lowerBoundColPort1);
        m_lowerBoundInclusive =
            settings.getBoolean("lowerBoundInclusive", false);
        m_upperBoundColumnPort1 = settings.getString(
                "upperBoundColumnPort1", upperBoundColPort1);
        m_upperBoundInclusive =
            settings.getBoolean("upperBoundInclusive", true);

        for (DataColumnSpec c : ins[1]) {
            if (labelColumnPort1 == null) {
                labelColumnPort1 = c.getName();
            } else if (c.getType().isCompatible(NominalValue.class)) {
                labelColumnPort1 = c.getName();
            }
        }
        m_labelColumnPort1 = settings.getString(
                "labelColumnPort1", labelColumnPort1);

        if (!ins[0].containsName(m_valueColumnPort0)) {
            m_valueColumnPort0 = valueColPort0;
        }
        if (!ins[1].containsName(m_upperBoundColumnPort1)) {
            m_upperBoundColumnPort1 = upperBoundColPort1;
            m_lowerBoundColumnPort1 = lowerBoundColPort1;
        }
        if (!ins[1].containsName(m_labelColumnPort1)) {
            m_labelColumnPort1 = labelColumnPort1;
        }
        m_failIfNoRuleMatches =
            settings.getBoolean("failIfNoRuleMatches", false);
    }

    /** Save current configuration.
      * @param settings To save to. */
    void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addString("valueColumnPort0", m_valueColumnPort0);
        settings.addString("lowerBoundColumnPort1", m_lowerBoundColumnPort1);
        settings.addBoolean("lowerBoundInclusive", m_lowerBoundInclusive);
        settings.addString("upperBoundColumnPort1", m_upperBoundColumnPort1);
        settings.addBoolean("upperBoundInclusive", m_upperBoundInclusive);
        settings.addString("labelColumnPort1", m_labelColumnPort1);
        settings.addBoolean("failIfNoRuleMatches", m_failIfNoRuleMatches);
    }

}
