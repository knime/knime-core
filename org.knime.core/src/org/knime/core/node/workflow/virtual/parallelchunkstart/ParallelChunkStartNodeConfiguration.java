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
 *   Mar 31, 2011 (wiswedel): created
 */
package org.knime.core.node.workflow.virtual.parallelchunkstart;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
final class ParallelChunkStartNodeConfiguration {
	
	static final int DEFAULT_PROC_COUNT = (int)Math.ceil(
			1.5 * Runtime.getRuntime().availableProcessors());

	private int m_chunkCount = -1;

	/** control if main branch will process a chunk or simply forward
	 * an @see InactiveBranchPortObject upon execution.
	 */
	private boolean m_inactiveMainBranch = false;

	/**
     * @return true of the main branch stays inactive during execution
     */
    public boolean hasInactiveMainBranch() {
        return m_inactiveMainBranch;
    }

    /**
     * @param imb true of the main branch is supposed to be inactive.
     */
    public void setInactiveMainBranch(final boolean imb) {
        m_inactiveMainBranch = imb;
    }

    /**
	 * @return the chunkCount
	 */
	public int getChunkCount() {
		return isAutomaticChunking() 
		? ParallelChunkStartNodeConfiguration.DEFAULT_PROC_COUNT
				: m_chunkCount;
	}

	/**
	 * @param chunkCount the chunkCount to set
	 */
	public void setChunkCount(final int chunkCount) {
		m_chunkCount = chunkCount;
	}
	
	public boolean isAutomaticChunking() {
		return m_chunkCount <= 0;
	}
	
	public void setAutomaticChunking() {
		m_chunkCount = -1;
	}
	
	void saveConfiguration(final NodeSettingsWO settings) {
		settings.addInt("chunkCount", m_chunkCount);
		settings.addBoolean("inactiveMainBranch", m_inactiveMainBranch);
	}
	
	void loadConfigurationDialog(final NodeSettingsRO settings) {
		m_chunkCount = settings.getInt("chunkCount", -1);
		m_inactiveMainBranch = settings.getBoolean("inactiveMainBranch", false);
	}
	
	void loadConfigurationModel(final NodeSettingsRO settings) 
		throws InvalidSettingsException {
		m_chunkCount = settings.getInt("chunkCount");
        m_inactiveMainBranch = settings.getBoolean("inactiveMainBranch", false);
	}

}
