/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
package org.knime.base.node.preproc.equalsizesampling;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.NominalValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;

/**
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
final class EqualSizeSamplingConfiguration {

    enum SamplingMethod {
        Approximate,
        Exact
    }

    private String m_classColumn;

    private Long m_seed;

    private SamplingMethod m_samplingMethod = SamplingMethod.Exact;

    /** @return the classColumn */
    String getClassColumn() {
        return m_classColumn;
    }

    /** @param classColumn the classColumn to set */
    void setClassColumn(final String classColumn) {
        m_classColumn = classColumn;
    }

    /** @return the seed */
    Long getSeed() {
        return m_seed;
    }

    /** @param seed the seed to set */
    void setSeed(final Long seed) {
        m_seed = seed;
    }

    /** @param samplingMethod the samplingMethod to set */
    void setSamplingMethod(final SamplingMethod samplingMethod) {
        if (samplingMethod == null) {
            throw new NullPointerException("Argument must not be null");
        }
        m_samplingMethod = samplingMethod;
    }

    /** @return the samplingMethod */
    SamplingMethod getSamplingMethod() {
        return m_samplingMethod;
    }

    void saveConfiguration(final NodeSettingsWO settings) {
        settings.addString("classColumn", m_classColumn);
        String seedS = m_seed == null ? null : Long.toString(m_seed);
        settings.addString("seed", seedS);
        settings.addString("samplingMethod", m_samplingMethod.name());
    }

    void loadConfigurationInModel(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_classColumn = settings.getString("classColumn");
        if (m_classColumn == null || m_classColumn.length() == 0) {
            throw new InvalidSettingsException(
                    "Class column must not be empty/null");
        }
        String seedS = settings.getString("seed");
        if (seedS == null) {
            m_seed = null;
        } else {
            try {
                m_seed = Long.parseLong(seedS);
            } catch (NumberFormatException nfe) {
                throw new InvalidSettingsException("Can't parse seed \""
                        + seedS + "\": " + nfe.getMessage(), nfe);
            }
        }
        String sMethod = settings.getString("samplingMethod");
        try {
            m_samplingMethod = SamplingMethod.valueOf(sMethod);
        } catch (Exception e) {
            throw new InvalidSettingsException(
                    "Invalid sampling method: " + sMethod, e);
        }
    }

    void loadConfigurationInDialog(final NodeSettingsRO settings,
            final DataTableSpec spec) throws NotConfigurableException {
        String defClassColumn = null;
        for (DataColumnSpec c : spec) {
            if (c.getType().isCompatible(NominalValue.class)) {
                defClassColumn = c.getName();
            }
        }
        if (defClassColumn == null) {
            throw new NotConfigurableException(
                    "No nominal attribute column in input");
        }
        m_classColumn = settings.getString("classColumn", defClassColumn);
        String seedS = settings.getString("seed", null);
        if (seedS == null) {
            m_seed = null;
        } else {
            try {
                m_seed = Long.parseLong(seedS);
            } catch (NumberFormatException nfe) {
                m_seed = null;
            }
        }
        String sMethod = settings.getString(
                "samplingMethod", SamplingMethod.Exact.name());
        try {
            m_samplingMethod = SamplingMethod.valueOf(sMethod);
        } catch (Exception e) {
            m_samplingMethod = SamplingMethod.Exact;
        }
    }
}
