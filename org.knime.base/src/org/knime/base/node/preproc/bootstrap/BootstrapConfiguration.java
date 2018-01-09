/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 *
 * History
 *   Nov 9, 2012 (Patrick Winter): created
 */
package org.knime.base.node.preproc.bootstrap;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Configuration for the node.
 *
 *
 * @author Patrick Winter, KNIME AG, Zurich, Switzerland
 */
final class BootstrapConfiguration {

    private boolean m_inPercent;

    private int m_size;

    private float m_percent;

    private boolean m_useSeed;

    private long m_seed;

    private boolean m_appendOccurrences;

    private boolean m_appendOriginalRowId;

    private String m_rowIdSeparator;

    /**
     * @return the inPercent
     */
    public boolean getInPercent() {
        return m_inPercent;
    }

    /**
     * @param inPercent the inPercent to set
     */
    public void setInPercent(final boolean inPercent) {
        m_inPercent = inPercent;
    }

    /**
     * @return the size
     */
    public int getSize() {
        return m_size;
    }

    /**
     * @param size the size to set
     */
    public void setSize(final int size) {
        m_size = size;
    }

    /**
     * @return the percent
     */
    public float getPercent() {
        return m_percent;
    }

    /**
     * @param percent the percent to set
     */
    public void setPercent(final float percent) {
        m_percent = percent;
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
     * @param seed the seed to set
     */
    public void setSeed(final long seed) {
        m_seed = seed;
    }

    /**
     * @return the appendOccurrences
     */
    public boolean getAppendOccurrences() {
        return m_appendOccurrences;
    }

    /**
     * @param appendOccurrences the appendOccurrences to set
     */
    public void setAppendOccurrences(final boolean appendOccurrences) {
        m_appendOccurrences = appendOccurrences;
    }

    /**
     * @return the appendOriginalRowId
     */
    public boolean getAppendOriginalRowId() {
        return m_appendOriginalRowId;
    }

    /**
     * @param appendOriginalRowId the appendOriginalRowId to set
     */
    public void setAppendOriginalRowId(final boolean appendOriginalRowId) {
        m_appendOriginalRowId = appendOriginalRowId;
    }

    /**
     * @return the rowIdSeparator
     */
    public String getRowIdSeparator() {
        return m_rowIdSeparator;
    }

    /**
     * @param rowIdSeparator the rowIdSeparator to set
     */
    public void setRowIdSeparator(final String rowIdSeparator) {
        m_rowIdSeparator = rowIdSeparator;
    }

    /**
     * Save the configuration.
     *
     *
     * @param settings The <code>NodeSettings</code> to write to
     */
    void save(final NodeSettingsWO settings) {
        settings.addBoolean("inpercent", m_inPercent);
        settings.addInt("size", m_size);
        settings.addFloat("percent", m_percent);
        settings.addBoolean("useseed", m_useSeed);
        settings.addLong("seed", m_seed);
        settings.addBoolean("appendoccurrences", m_appendOccurrences);
        settings.addBoolean("appendoriginalrowid", m_appendOriginalRowId);
        settings.addString("rowidseparator", m_rowIdSeparator);
    }

    /**
     * Load the configuration.
     *
     *
     * @param settings The <code>NodeSettings</code> to read from
     */
    void load(final NodeSettingsRO settings) {
        m_inPercent = settings.getBoolean("inpercent", true);
        m_size = settings.getInt("size", 100);
        m_percent = settings.getFloat("percent", 100f);
        m_useSeed = settings.getBoolean("useseed", false);
        m_seed = settings.getLong("seed", System.currentTimeMillis());
        m_appendOccurrences = settings.getBoolean("appendoccurrences", false);
        m_appendOriginalRowId = settings.getBoolean("appendoriginalrowid", false);
        m_rowIdSeparator = settings.getString("rowidseparator", "_");
    }

    /**
     * Load the configuration and check for validity.
     *
     *
     * @param settings The <code>NodeSettings</code> to read from
     * @throws InvalidSettingsException If one of the settings is not valid
     */
    void loadAndValidate(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_inPercent = settings.getBoolean("inpercent");
        m_size = settings.getInt("size");
        m_percent = settings.getFloat("percent");
        m_useSeed = settings.getBoolean("useseed");
        m_seed = settings.getLong("seed");
        m_appendOccurrences = settings.getBoolean("appendoccurrences");
        m_appendOriginalRowId = settings.getBoolean("appendoriginalrowid");
        m_rowIdSeparator = settings.getString("rowidseparator");
    }

}
