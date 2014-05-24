/*
 * ------------------------------------------------------------------
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
 * --------------------------------------------------------------------- *
 *
 * History
 *   May 18, 2006 (ritmeier): created
 */
package org.knime.testing.node.differNode;

import java.util.Iterator;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.testing.internal.nodes.image.ImageDifferNodeFactory;


/**
 *
 * @author ritmeier, University of Konstanz
 *
 * @deprecated use the new image comparator {@link ImageDifferNodeFactory} and the extension point for difference
 *             checker instead
 */
@Deprecated
public class NegativeDataTableDiffer implements TestEvaluator {
    /**
     *
     * @throws TestEvaluationException
     * @see org.knime.testing.node.differNode.TestEvaluator#compare(
     *      org.knime.core.data.DataTable,
     *      org.knime.core.data.DataTable)
     */
    @Override
    public void compare(final DataTable table1, final DataTable table2)
            throws TestEvaluationException {
        boolean testResult = true;
        if (!table1.getDataTableSpec().equalStructure(
                table2.getDataTableSpec())) {
            testResult = false;
        }
        Iterator<DataRow> rowIt1 = table1.iterator();
        Iterator<DataRow> rowIt2 = table2.iterator();
        while (rowIt1.hasNext() && rowIt2.hasNext()) {
            Iterator<DataCell> cellIt1 = rowIt1.next().iterator();
            Iterator<DataCell> cellIt2 = rowIt2.next().iterator();
            while (cellIt1.hasNext() && cellIt2.hasNext()) {
                if (!cellIt1.next().equals(cellIt2.next())) {
                    testResult = false;
                }
            }
        }
        if (testResult) {
            throw new TestEvaluationException(
                    "DataTables are the same. They should be differend in negitive testing");
        }
    }

}
