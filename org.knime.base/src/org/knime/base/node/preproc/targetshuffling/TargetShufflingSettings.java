/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ------------------------------------------------------------------------
 */
package org.knime.base.node.preproc.targetshuffling;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This class holds the settings for the y-scrambling node.
 *
 * @author Thorsten Meinl, University of Konstanz
 * @author Tim-Oliver Buchholz, University of Konstanz
 */
class TargetShufflingSettings {
    private static final String CFGKEY_COLUMNNAME = "columnName";
    private static final String CFGKEY_SEED = "seed";
    private static final String CFGKEY_USESEED = "useSeed";
    private String m_columnName;
    private boolean m_useSeed;
    private long m_seed;


    /**
     * Returns the choosen column name.
     *
     * @return the column name
     */
    public String columnName() {
        return m_columnName;
    }

    /**
     * Sets the choosen column name.
     *
     * @param name the column name
     */
    public void columnName(final String name) {
        m_columnName = name;
    }

    /**
     * Loads the settings from the node settings object.
     *
     * @param settings the node settings
     *
     * @throws InvalidSettingsException if one of the settings is missing
     */
    public void loadSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_columnName = settings.getString(CFGKEY_COLUMNNAME);
        m_seed = settings.getLong(CFGKEY_SEED);
        m_useSeed = settings.getBoolean(CFGKEY_USESEED);
    }


    /**
     * Saves the settings to the node settings object.
     *
     * @param settings the node settings
     */
    public void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addString(CFGKEY_COLUMNNAME, m_columnName);
        settings.addLong(CFGKEY_SEED, m_seed);
        settings.addBoolean(CFGKEY_USESEED, m_useSeed);
    }

    /**
     * @return the useSeed
     */
    public boolean getUseSeed() {
        return m_useSeed;
    }

    /**
     * @param useSeed the useSeed to set
     */
    public void setUseSeed(final boolean useSeed) {
        m_useSeed = useSeed;
    }

    /**
     * @return the seed
     */
    public long getSeed() {
        return m_seed;
    }

    /**
     * @param l the seed to set
     */
    public void setSeed(final long l) {
        m_seed = l;
    }
}
