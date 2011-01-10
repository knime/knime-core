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
 * History
 *   09.07.2010 (hofer): created
 */
package org.knime.base.node.preproc.autobinner;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This class hold the settings for the Logistic Learner Node.
 *
 * @author Heiko Hofer
 */
final public class AutoBinnerLearnSettings {
    /**
     * The name of the autobinning method
     * @author Heiko Hofer
     */
    public enum Method {
        /** Fixed number of bins. */
        fixedNumber,
        /** Estimated sample quantiles. */
        sampleQuantiles
    }

    /**
     * The method for naming bins
     * @author Heiko Hofer
     */
    public enum BinNaming {
        /** Numbered starting from one: Bin 1, Bin2, ... */
        numbered,
        /** Use edges for defining bins: (-,0] (0,1], ... */
        edges
    }


    private static final String CFG_TARGET_COLUMN = "targetColumn";
    private static final String CFG_INCLUDE_ALL = "includeAll";
    private static final String CFG_METHOD = "method";
    private static final String CFG_BIN_COUNT = "binCount";
    private static final String CFG_SAMPLE_QUANTILES = "sampleQuantiles";
    private static final String CFG_BIN_NAMING = "binNaming";
    private static final String CFG_REPLACE_COLUMN = "replaceColumn";

    private String[] m_targetCols = null;
    private boolean m_includeAll = true;
    private Method m_method = Method.fixedNumber;
    private int m_binCount = 5;
    private double[] m_sampleQuantiles = new double[] {0, 0.25, 0.5, 0.75, 1};
    private BinNaming m_binNaming = BinNaming.numbered;
    private boolean m_replaceColumn = false;
    /**
     * @return the targetColumn
     */
    public String[] getTargetColumn() {
        return m_targetCols;
    }
    /**
     * @param targetColumn the targetColumn to set
     */
    public void setTargetColumn(final String[] targetColumn) {
        m_targetCols = targetColumn;
    }
    /**
     * @return the includeAll
     */
    public boolean getIncludeAll() {
        return m_includeAll;
    }
    /**
     * @param includeAll the includeAll to set
     */
    public void setIncludeAll(final boolean includeAll) {
        m_includeAll = includeAll;
    }

    /**
     * @return the method
     */
    public Method getMethod() {
        return m_method;
    }
    /**
     * @param method the method to set
     */
    public void setMethod(final Method method) {
        m_method = method;
    }
    /**
     * @return the binCount
     */
    public int getBinCount() {
        return m_binCount;
    }
    /**
     * @param binCount the binCount to set
     */
    public void setBinCount(final int binCount) {
        m_binCount = binCount;
    }
    /**
     * @return the sampleQuantiles
     */
    public double[] getSampleQuantiles() {
        return m_sampleQuantiles;
    }
    /**
     * @param sampleQuantiles the sampleQuantiles to set
     */
    public void setSampleQuantiles(final double[] sampleQuantiles) {
        m_sampleQuantiles = sampleQuantiles;
    }
    /**
     * @return the binNaming
     */
    public BinNaming getBinNaming() {
        return m_binNaming;
    }
    /**
     * @param binNaming the binNaming to set
     */
    public void setBinNaming(final BinNaming binNaming) {
        m_binNaming = binNaming;
    }
    /**
     * @return the replaceColumn
     */
    public boolean getReplaceColumn() {
        return m_replaceColumn;
    }
    /**
     * @param replaceColumn the replaceColumn to set
     */
    public void setReplaceColumn(final boolean replaceColumn) {
        m_replaceColumn = replaceColumn;
    }


    /**
     * Loads the settings from the node settings object.
     *
     * @param settings a node settings object
     * @throws InvalidSettingsException if some settings are missing
     */
    public void loadSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_targetCols = settings.getStringArray(CFG_TARGET_COLUMN);
        m_method = Method.valueOf(settings.getString(CFG_METHOD));
        m_binCount = settings.getInt(CFG_BIN_COUNT);
        m_sampleQuantiles = settings.getDoubleArray(CFG_SAMPLE_QUANTILES);
        m_binNaming = BinNaming.valueOf(settings.getString(CFG_BIN_NAMING));
        m_replaceColumn = settings.getBoolean(CFG_REPLACE_COLUMN);
        m_includeAll = settings.getBoolean(CFG_INCLUDE_ALL);
    }

    /**
     * Loads the settings from the node settings object using default values if
     * some settings are missing.
     *
     * @param settings a node settings object
     */
    public void loadSettingsForDialog(final NodeSettingsRO settings) {
        m_targetCols =
            settings.getStringArray(CFG_TARGET_COLUMN, (String[])null);
        m_method = Method.valueOf(settings.getString(CFG_METHOD,
                Method.fixedNumber.toString()));
        m_binCount = settings.getInt(CFG_BIN_COUNT, 5);
        m_sampleQuantiles = settings.getDoubleArray(CFG_SAMPLE_QUANTILES,
                new double[] {0, 0.25, 0.5, 0.75, 1});
        m_binNaming = BinNaming.valueOf(settings.getString(CFG_BIN_NAMING,
                BinNaming.numbered.toString()));
        m_replaceColumn = settings.getBoolean(CFG_REPLACE_COLUMN, false);
        m_includeAll = settings.getBoolean(CFG_INCLUDE_ALL, true);
    }

    /**
     * Saves the settings into the node settings object.
     *
     * @param settings a node settings object
     */
    public void saveSettings(final NodeSettingsWO settings) {
        settings.addStringArray(CFG_TARGET_COLUMN, m_targetCols);
        settings.addString(CFG_METHOD, m_method.name());
        settings.addInt(CFG_BIN_COUNT, m_binCount);
        settings.addDoubleArray(CFG_SAMPLE_QUANTILES, m_sampleQuantiles);
        settings.addString(CFG_BIN_NAMING, m_binNaming.name());
        settings.addBoolean(CFG_REPLACE_COLUMN, m_replaceColumn);
        settings.addBoolean(CFG_INCLUDE_ALL, m_includeAll);
    }

}
