/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Jun 3, 2020 (carlwitt): created
 */
package org.knime.core.data.join.results;

import java.util.Arrays;

import org.knime.core.data.DataRow;
import org.knime.core.data.join.JoinSpecification;
import org.knime.core.data.join.JoinSpecification.InputTable;
import org.knime.core.data.join.JoinSpecification.OutputRowOrder;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.CanceledExecutionException.CancelChecker;
import org.knime.core.node.ExecutionContext;

/**
 * A minimal container for join results that can be output in any order, i.e., when the {@link JoinSpecification}
 * specifies {@link OutputRowOrder#ARBITRARY}. <br/>
 * This is the fastest way to collect join results, because we can avoid sorting and creating additional objects to
 * store row order.
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 * @since 4.2
 */
public class UnorderedJoinContainer extends JoinContainer {

    private static final int SINGLE_TABLE = 3;

    private final BufferedDataContainer[] m_containers;

    private final BufferedDataTable[] m_tables;

    /**
     * @param joinSpecification as in {@link JoinContainer#JoinContainer(JoinSpecification, ExecutionContext, boolean)}
     * @param exec as in {@link JoinContainer#JoinContainer(JoinSpecification, ExecutionContext, boolean)}
     * @param deduplicateResults as in {@link JoinContainer#JoinContainer(JoinSpecification, ExecutionContext, boolean)}
     * @param deferUnmatchedRows
     */
    public UnorderedJoinContainer(final JoinSpecification joinSpecification, final ExecutionContext exec,
        final boolean deduplicateResults, final boolean deferUnmatchedRows) {
        super(joinSpecification, exec, deduplicateResults, deferUnmatchedRows);

        m_containers =
            Arrays.stream(m_outputSpecs).map(exec::createDataContainer).toArray(BufferedDataContainer[]::new);
        m_tables = new BufferedDataTable[4];
    }

    private void add(final int rowType, final DataRow row) {
        m_containers[rowType].addRowToTable(row);
    }

    @Override
    public boolean doAddMatch(final DataRow left, final long leftOffset, final DataRow right, final long rightOffset) {
        DataRow match = m_joinSpecification.rowJoin(left, right);
        add(MATCHES, match);
        return true;
    }

    @Override
    public boolean doAddLeftOuter(final DataRow row, final long offset) {
        add(LEFT_OUTER, m_joinSpecification.rowProject(InputTable.LEFT, row));
        return true;
    }

    @Override
    public boolean doAddRightOuter(final DataRow row, final long offset) {
        add(RIGHT_OUTER, m_joinSpecification.rowProject(InputTable.RIGHT, row));
        return true;
    }

    private BufferedDataTable get(final int resultType) throws CanceledExecutionException {
        if (m_tables[resultType] == null) {
            if (resultType != MATCHES) {
                m_unmatchedRows[resultType].collectUnmatched();
            }
            m_containers[resultType].close();
            m_tables[resultType] = m_containers[resultType].getTable();
        }
        return m_tables[resultType];
    }

    @Override
    public BufferedDataTable getMatches() throws CanceledExecutionException {
        return get(MATCHES);
    }

    @Override
    public BufferedDataTable getLeftOuter() throws CanceledExecutionException {
        return get(LEFT_OUTER);
    }

    @Override
    public BufferedDataTable getRightOuter() throws CanceledExecutionException {
        return get(RIGHT_OUTER);
    }

    @Override
    public BufferedDataTable getSingleTable() throws CanceledExecutionException {

        if(m_tables[SINGLE_TABLE] == null) {

            // this is empty if matches are not retained
            // however, it has the correct spec (left + right included columns)
            // and is open after object construction until #getMatches is called
            final BufferedDataContainer result = m_containers[MATCHES];
            CancelChecker checkCanceled = CancelChecker.checkCanceledPeriodically(m_exec);

            // add left unmatched rows
            if (m_joinSpecification.isRetainUnmatched(InputTable.LEFT)) {
                JoinResults.iterateWithResources(getLeftOuter(), row -> result.addRowToTable(padRightWithMissing(row)),
                    checkCanceled);
            }

            // add right unmatched rows
            if (m_joinSpecification.isRetainUnmatched(InputTable.RIGHT)) {
                JoinResults.iterateWithResources(getRightOuter(), row -> result.addRowToTable(padLeftWithMissing(row)),
                    checkCanceled);
            }

            result.close();
            m_tables[SINGLE_TABLE] = result.getTable();

        }
        return m_tables[SINGLE_TABLE];

    }

}
