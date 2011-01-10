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
 *   11.05.2010 (hofer): created
 */
package org.knime.base.node.mine.regression.logistic.predict;

import java.util.HashMap;
import java.util.Map;

import org.knime.base.node.mine.regression.pmmlgreg.PMMLPPCell;

/**
 * A convenient class to access the data of PMMLPPCells.
 *
 * @author Heiko Hofer
 */
public class PPMatrix {
    private Map<Key, String> m_ppMatrix;
    private Map<String, String> m_ppMap;

    /**
     * Creates an instance which is a decorate for the given cells.
     *
     * @param cells the cells that are decorated by this instance
     */
    public PPMatrix(final PMMLPPCell... cells) {
        m_ppMatrix = new HashMap<Key, String>();
        m_ppMap = new HashMap<String, String>();
        for (PMMLPPCell cell : cells) {
            m_ppMatrix.put(new Key(cell.getParameterName(),
                    cell.getPredictorName(), cell.getTargetCategory()),
                    cell.getValue());
            m_ppMap.put(cell.getParameterName(), cell.getPredictorName());
        }
    }

    /**
     * @param parameterName The name of the parameter as defined in the element
     * ParameterList
     * @param predictorName The name of the predictor as defined in the element
     * MiningSchema
     * @param targetCategory the category of the target or null if not present
     * @return The value of the parameter to predictor matrix
     */
    public String getValue(final String parameterName,
            final String predictorName, final String targetCategory) {
        return m_ppMatrix.get(new Key(parameterName, predictorName,
                targetCategory));
    }

    /**
     * @param parameterName The name of the parameter as defined in the element
     * ParameterList
     * @return the predictor name for the given parameter
     */
    public String getPredictor(final String parameterName) {
        return m_ppMap.get(parameterName);
    }

    private static class Key {
        private String m_paramterName;
        private String m_predictorName;
        private String m_targetCategory;
        /**
         * @param paramterName
         * @param predictorName
         * @param targetCategory
         */
        public Key(final String paramterName, final String predictorName,
                final String targetCategory) {
            m_paramterName = paramterName;
            m_predictorName = predictorName;
            m_targetCategory = targetCategory;
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result =
                    prime
                            * result
                            + ((m_paramterName == null) ? 0 : m_paramterName
                                    .hashCode());
            result =
                    prime
                            * result
                            + ((m_predictorName == null) ? 0 : m_predictorName
                                    .hashCode());
            result =
                    prime
                            * result
                            + ((m_targetCategory == null) ? 0
                                    : m_targetCategory.hashCode());
            return result;
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(final Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Key other = (Key)obj;
            if (m_paramterName == null) {
                if (other.m_paramterName != null)
                    return false;
            } else if (!m_paramterName.equals(other.m_paramterName))
                return false;
            if (m_predictorName == null) {
                if (other.m_predictorName != null)
                    return false;
            } else if (!m_predictorName.equals(other.m_predictorName))
                return false;
            if (m_targetCategory == null) {
                if (other.m_targetCategory != null)
                    return false;
            } else if (!m_targetCategory.equals(other.m_targetCategory))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "[" + m_paramterName + ", " + m_predictorName + ", "
                + m_targetCategory + "]";
        }
    }

}

