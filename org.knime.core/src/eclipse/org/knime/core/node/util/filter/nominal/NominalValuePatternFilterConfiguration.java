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
 *   Feb 26, 2017 (ferry): created
 */
package org.knime.core.node.util.filter.nominal;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.filter.PatternFilterConfiguration;
import org.knime.core.node.util.filter.nominal.NominalValueFilterConfiguration.NominalValueFilterResult;

/**
 *
 * @author Ferry Abt, KNIME.com AG, Zurich, Switzerland
 * @since 3.4
 */
public class NominalValuePatternFilterConfiguration extends PatternFilterConfiguration {

    private static final String CFG_INCLUDEMISSING = "includeMissing";

    private boolean m_includeMissing = false;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadConfigurationInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadConfigurationInModel(settings);
        m_includeMissing = settings.getBoolean(CFG_INCLUDEMISSING, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadConfigurationInDialog(final NodeSettingsRO settings) {
        super.loadConfigurationInDialog(settings);
        m_includeMissing = settings.getBoolean(CFG_INCLUDEMISSING, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveConfiguration(final NodeSettingsWO settings) {
        super.saveConfiguration(settings);
        settings.addBoolean(CFG_INCLUDEMISSING, m_includeMissing);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NominalValueFilterResult applyTo(final String[] names) {
        return new NominalValueFilterResult(super.applyTo(names), m_includeMissing);
    }

    /**
     * @return whether Missing Values will be included
     */
    boolean isIncludeMissing() {
        return m_includeMissing;
    }

    /**
     * @param caseSensitive the caseSensitive to set
     */
    void setIncludeMissing(final boolean includeMissing) {
        m_includeMissing = includeMissing;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Boolean.valueOf(m_includeMissing).hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        NominalValuePatternFilterConfiguration o = (NominalValuePatternFilterConfiguration)obj;
        return o.m_includeMissing == m_includeMissing;
    }

}
