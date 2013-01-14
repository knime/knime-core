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
 * History
 *   29.04.2011 (hofer): created
 */
package org.knime.testing.node.file;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This is the settings of the Differ File Node.
 * 
 * @author Heiko Hofer
 */
class DifferFileNodeSettings {

	    private static final String TEST_FILE_FLOW_VAR = "testFileFlowVar";
	    private static final String REFERENCE_FILE_FLOW_VAR = 
	    	"referenceFileFlowVar";
	    

	    private String m_TestFileFlowVar = null;
	    private String m_referenceFileFlowVar = null;	    

		/**
		 * @return the testFileFlowVar
		 */
		String getTestFileFlowVar() {
			return m_TestFileFlowVar;
		}

		/**
		 * @param testFileFlowVar the testFileFlowVar to set
		 */
		void setTestFileFlowVar(String testFileFlowVar) {
			m_TestFileFlowVar = testFileFlowVar;
		}

		/**
		 * @return the referenceFileFlowVar
		 */
		String getReferenceFileFlowVar() {
			return m_referenceFileFlowVar;
		}

		/**
		 * @param referenceFileFlowVar the referenceFileFlowVar to set
		 */
		void setReferenceFileFlowVar(String referenceFileFlowVar) {
			m_referenceFileFlowVar = referenceFileFlowVar;
		}

		/** Called from dialog when settings are to be loaded.
	     * @param settings To load from
	     */
	    void loadSettingsDialog(final NodeSettingsRO settings) {
	        m_TestFileFlowVar = settings.getString(TEST_FILE_FLOW_VAR, null);
	        m_referenceFileFlowVar = 
	        	settings.getString(REFERENCE_FILE_FLOW_VAR, null);
	    }
	    
		/** Called from model when settings are to be loaded.
	     * @param settings To load from
	     * @throws InvalidSettingsException If settings are invalid.
	     */
	    void loadSettingsModel(final NodeSettingsRO settings)
	        throws InvalidSettingsException {
	        m_TestFileFlowVar = settings.getString(TEST_FILE_FLOW_VAR);
	        m_referenceFileFlowVar = 
	        	settings.getString(REFERENCE_FILE_FLOW_VAR);
	    }

	    /** Called from model and dialog to save current settings.
	     * @param settings To save to.
	     */
	    void saveSettings(final NodeSettingsWO settings) {
	        settings.addString(TEST_FILE_FLOW_VAR, m_TestFileFlowVar);
	        settings.addString(REFERENCE_FILE_FLOW_VAR, m_referenceFileFlowVar);

	    }

	}
