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
 *   Nov 2, 2015 (Lara): created
 */
package org.knime.base.node.io.database.binning;

import java.util.List;
import java.util.Map;

import org.knime.core.node.port.database.StatementManipulator;
import org.knime.core.util.Pair;

/**
 * This class is to collect required Maps containing parameters for binning operation in {@link StatementManipulator}.
 *
 * @author Lara Gorini
 */
public class DBBinnerMaps {

    private Map<String, List<Pair<Double, Double>>> m_limitsMap;

    private Map<String, List<Pair<Boolean, Boolean>>> m_boundariesOpenMap;

    private Map<String, List<String>> m_namingMap;

    private Map<String, String> m_appendMap;

    /**
     * @param limitsMap Map containing edges of bins
     * @param boundariesOpenMap Map holding information if boundaries of the interval are open (excluded)
     * @param namingMap Map containing names of bins
     * @param appendMap Map containing name of columns which has to be appended. Value will be null, if column is not appended
     */
    public DBBinnerMaps(final Map<String, List<Pair<Double, Double>>> limitsMap,
        final Map<String, List<Pair<Boolean, Boolean>>> boundariesOpenMap, final Map<String, List<String>> namingMap,
        final Map<String, String> appendMap) {
        m_limitsMap = limitsMap;
        m_boundariesOpenMap = boundariesOpenMap;
        m_namingMap = namingMap;
        m_appendMap = appendMap;
    }

    /**
     * @return the limitsMap
     */
    public Map<String, List<Pair<Double, Double>>> getBoundariesMap() {
        return m_limitsMap;
    }

    /**
     * @return the includeMap
     */
    public Map<String, List<Pair<Boolean, Boolean>>> getBoundariesOpenMap() {
        return m_boundariesOpenMap;
    }

    /**
     * @return the namingMap
     */
    public Map<String, List<String>> getNamingMap() {
        return m_namingMap;
    }

    /**
     * @return the appendMap
     */
    public Map<String, String> getAppendMap() {
        return m_appendMap;
    }

}
