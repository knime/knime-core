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
 *   Sep 30, 2015 (Lara): created
 */
package org.knime.core.node.port.database.binning;

import java.util.List;
import java.util.Map;

import org.knime.core.node.port.database.StatementManipulator;
import org.knime.core.util.Pair;

/**
 *
 * @author Lara Gorini
 * @since 3.1
 */
public interface BinningStatementGenerator {

    /**
     * @param statementManipulator The {@link StatementManipulator} to use
     * @param query The input query
     * @param binnedCols Names of columns that are binned
     * @param additionalCols Names of columns that are not binned but should be selected
     * @param boundariesMap Map containing limits of bins as values
     * @param boundariesOpenMap Map containing boolean which indicates if edge is open (true) or closed (false)
     * @param namingMap Map containing names of bins as values
     * @param appendMap Map containing names of columns that should be appended as values (can be null).
     * @return a SQL statement for binning
     */
    public String getBinnerStatement(StatementManipulator statementManipulator, String query, String[] binnedCols,
        String[] additionalCols, Map<String, List<Pair<Double, Double>>> boundariesMap,
        Map<String, List<Pair<Boolean, Boolean>>> boundariesOpenMap, Map<String, List<String>> namingMap,
        Map<String, String> appendMap);
}
