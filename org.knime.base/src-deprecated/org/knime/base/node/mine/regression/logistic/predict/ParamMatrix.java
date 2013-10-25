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
 *   11.05.2010 (hofer): created
 */
package org.knime.base.node.mine.regression.logistic.predict;

import java.util.HashMap;
import java.util.Map;

import org.knime.base.node.mine.regression.pmmlgreg.PMMLPCell;

/**
 * Convenient class to access the contents of PMMLPCells.
 *
 * @author Heiko Hofer
 */
final class ParamMatrix {
    private Map<Key, Double> m_beta;
    private Map<Key, Integer> m_df;

    /**
     * Creates an instance which is a decorate for the given cells.
     *
     * @param cells the cells that are decorated by this instance
     */
    ParamMatrix(final PMMLPCell... cells) {
        m_beta = new HashMap<Key, Double>();
        m_df = new HashMap<Key, Integer>();
        for (PMMLPCell cell : cells) {
            m_beta.put(new Key(cell.getParameterName(),
                    cell.getTargetCategory()),
                    cell.getBeta());
            m_df.put(new Key(cell.getParameterName(),
                    cell.getTargetCategory()),
                    cell.getDf());
        }
    }

    /**
     * Gives the coefficient for the given parameter and the given category
     * of the target.
     * @param parameterName the parameter
     * @param targetCategory the category of the target
     * @return the coefficient
     */
    public double getBeta(final String parameterName,
            final String targetCategory) {
        return m_beta.get(new Key(parameterName, targetCategory));
    }

    /**
     * Gives the degrees of freedom for the given parameter and the given
     * category of the target.
     *
     * @param parameterName the parameter
     * @param targetCategory the category of the target
     * @return the degrees of freedom
     */
    public Integer getDegreesOfFreedom(final String parameterName,
            final String targetCategory) {
        return m_df.get(new Key(parameterName, targetCategory));
    }

    private static class Key {
        private String m_paramterName;
        private String m_targetCategory;
        /**
         * @param paramterName
         * @param targetCategory
         */
        public Key(final String paramterName,
                final String targetCategory) {
            m_paramterName = paramterName;
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
            if (m_targetCategory == null) {
                if (other.m_targetCategory != null)
                    return false;
            } else if (!m_targetCategory.equals(other.m_targetCategory))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "[" + m_paramterName + ", " + m_targetCategory + "]";
        }
    }

}
