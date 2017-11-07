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
 * ------------------------------------------------------------------------
 *
 * History
 *   Dec 27, 2011 (wiswedel): created
 */
package org.knime.base.node.mine.treeensemble2.data;

import java.util.Comparator;

import org.knime.base.node.mine.treeensemble2.model.AbstractTreeEnsembleModel.TreeType;
import org.knime.base.node.mine.treeensemble2.node.learner.TreeEnsembleLearnerConfiguration;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.NominalValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.sort.DataTableSorter;
import org.knime.core.data.vector.bitvector.BitVectorValue;
import org.knime.core.data.vector.bytevector.ByteVectorValue;
import org.knime.core.data.vector.doublevector.DoubleVectorValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.util.CheckUtils;

/**
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class TreeDataCreator {

    private final TreeEnsembleLearnerConfiguration m_configuration;

    private final TreeAttributeColumnDataCreator[] m_attrColCreators;

    private final TreeTargetColumnDataCreator m_targetColCreator;

    private final DataContainer m_dataRowsForHiliteContainer;

    private final TreeType m_treeType;

    private String m_warningMessage;

    private String m_viewMessage;

    /**
     * @param configuration
     * @param learnSpec
     * @param nrRows
     */
    public TreeDataCreator(final TreeEnsembleLearnerConfiguration configuration, final DataTableSpec learnSpec,
        final int nrRows) {
        m_configuration = configuration;
        final boolean isRegression = configuration.isRegression();
        final int nrLearnCols = learnSpec.getNumColumns() - 1;
        if (nrLearnCols == 0) {
            throw new IllegalStateException("No learning columns");
        }
        m_attrColCreators = new TreeAttributeColumnDataCreator[nrLearnCols];
        final DataColumnSpec targetCSpec = learnSpec.getColumnSpec(nrLearnCols);
        if (isRegression) {
            m_targetColCreator = new TreeTargetNumericColumnDataCreator(targetCSpec);
        } else {
            m_targetColCreator = new TreeTargetNominalColumnDataCreator(targetCSpec);
        }
        TreeType treeType = null;
        for (int i = 0; i < nrLearnCols; i++) {
            DataColumnSpec col = learnSpec.getColumnSpec(i);
            DataType colType = col.getType();
            if (colType.isCompatible(NominalValue.class)) {
                m_attrColCreators[i] = new TreeNominalColumnDataCreator(col);
                treeType = TreeType.Ordinary;
            } else if (colType.isCompatible(DoubleValue.class)) {
                m_attrColCreators[i] = new TreeOrdinaryNumericColumnDataCreator(col);
                treeType = TreeType.Ordinary;
            } else if (colType.isCompatible(BitVectorValue.class)) {
                m_attrColCreators[i] = new TreeBitVectorColumnDataCreator(col);
                if (nrLearnCols > 1) {
                    throw new IllegalStateException("Can't use multiple " + "columns for bit vector based tree");
                }
                treeType = TreeType.BitVector;
            } else if (colType.isCompatible(ByteVectorValue.class)) {
                m_attrColCreators[i] = new TreeByteNumericColumnDataCreator(col);
                if (nrLearnCols > 1) {
                    throw new IllegalStateException("Can't use multiple columns for byte vector based tree");
                }
                treeType = TreeType.ByteVector;
            } else if (colType.isCompatible(DoubleVectorValue.class)) {
                m_attrColCreators[i] = new TreeDoubleVectorNumericColumnDataCreator(col);
                if (nrLearnCols > 1) {
                    throw new IllegalStateException("Can't use multiple columns for double vector based tree");
                }
                treeType = TreeType.DoubleVector;
            } else {
                throw new IllegalStateException("Unsupported column at index " + i + " (column \"" + col.getName()
                    + "\"): " + colType);
            }
        }
        final int nrHilitePatterns = configuration.getNrHilitePatterns();
        if (nrHilitePatterns > 0) {
            m_dataRowsForHiliteContainer = new DataContainer(learnSpec, true);
        } else {
            m_dataRowsForHiliteContainer = null;
        }
        assert treeType != null;
        m_treeType = treeType;
    }

    /**
     * Reads the data from <b>learnData</b> into memory.
     * Each column is represented by a TreeColumnData object corresponding to its type
     * and whether it is a attribute or target column.
     *
     * @param learnData
     * @param configuration
     * @param exec
     * @return the TreeData object that holds all data in memory
     * @throws CanceledExecutionException
     */
    public TreeData readData(final BufferedDataTable learnData, final TreeEnsembleLearnerConfiguration configuration,
        final ExecutionMonitor exec) throws CanceledExecutionException {
        if (learnData.size() <= 1) {
            throw new IllegalArgumentException("The input table must contain at least 2 rows!");
        }
        int index = 0;
        final long nrRows = learnData.size();
        final int nrLearnCols = m_attrColCreators.length;
        final boolean[] supportMissings = new boolean[nrLearnCols];
        for (int i = 0; i < nrLearnCols; i++) {
            supportMissings[i] = m_attrColCreators[i].acceptsMissing();
        }
        int rejectedMissings = 0;
        final int nrHilitePatterns = m_configuration.getNrHilitePatterns();

        // sort learnData according to the target column to enable equal size sampling
        final int targetColIdx = learnData.getDataTableSpec().findColumnIndex(m_configuration.getTargetColumn());
        Comparator<DataCell> targetComp = learnData.getDataTableSpec().getColumnSpec(targetColIdx).getType().getComparator();
        DataTableSorter sorter = new DataTableSorter(learnData, learnData.size(), new Comparator<DataRow>() {

            @Override
            public int compare(final DataRow arg0, final DataRow arg1) {
                return targetComp.compare(arg0.getCell(targetColIdx), arg1.getCell(targetColIdx));
            }

        });
        final ExecutionMonitor sortExec = exec.createSubProgress(0.5);
        final DataTable sortedTable = sorter.sort(sortExec);
        final ExecutionMonitor readExec = exec.createSubProgress(0.5);

        for (DataRow r : sortedTable) {
            double progress = index / (double)nrRows;
            readExec.setProgress(progress, "Row " + index + " of " + nrRows + " (\"" + r.getKey() + "\")");
            readExec.checkCanceled();
            boolean shouldReject = false;
            for (int i = 0; i < nrLearnCols; i++) {
                DataCell c = r.getCell(i);
                if (c.isMissing() && !supportMissings[i]) {
                    shouldReject = true;
                    break;
                }
            }
            DataCell targetCell = r.getCell(nrLearnCols);
            if (targetCell.isMissing()) {
                shouldReject = true;
            }
            if (shouldReject) {
                rejectedMissings += 1;
                continue;
            }
            if (index < nrHilitePatterns) {
                m_dataRowsForHiliteContainer.addRowToTable(r);
            }
            final RowKey key = r.getKey();
            for (int i = 0; i < nrLearnCols; i++) {
                DataCell c = r.getCell(i);
                m_attrColCreators[i].add(key, c);
            }
            m_targetColCreator.add(key, targetCell);
            index++;
        }
        if (nrHilitePatterns > 0 && index > nrHilitePatterns) {
            m_viewMessage =
                "Hilite (& color graphs) are based on a subset of " + "the data (" + nrHilitePatterns + "/" + index
                    + ")";
        }
        if (rejectedMissings > 0) {
            StringBuffer warnMsgBuilder = new StringBuffer();
            warnMsgBuilder.append(rejectedMissings).append("/");
            warnMsgBuilder.append(learnData.size());
            warnMsgBuilder.append(" row(s) were ignored because they ");
            warnMsgBuilder.append("contain missing values.");
            m_warningMessage = warnMsgBuilder.toString();
        }
        CheckUtils.checkArgument(rejectedMissings < learnData.size(),
            "No rows left after removing missing values (table has %d row(s))", learnData.size());
        int nrLearnAttributes = 0;
        for (int i = 0; i < m_attrColCreators.length; i++) {
            nrLearnAttributes += m_attrColCreators[i].getNrAttributes();
        }
        TreeAttributeColumnData[] columns = new TreeAttributeColumnData[nrLearnAttributes];
        int learnAttributeIndex = 0;
        for (int i = 0; i < m_attrColCreators.length; i++) {
            TreeAttributeColumnDataCreator creator = m_attrColCreators[i];
            for (int a = 0; a < creator.getNrAttributes(); a++) {
                final TreeAttributeColumnData columnData = creator.createColumnData(a, configuration);
                columnData.getMetaData().setAttributeIndex(learnAttributeIndex);
                columns[learnAttributeIndex++] = columnData;
            }
        }
        TreeTargetColumnData targetCol = m_targetColCreator.createColumnData();
        return new TreeData(columns, targetCol, m_treeType);
    }

    /**
     * @return the warning message
     */
    public String getAndClearWarningMessage() {
        String result = m_warningMessage;
        m_warningMessage = null;
        return result;
    }

    /** @return the viewMessage */
    public String getViewMessage() {
        return m_viewMessage;
    }

    /** @return the dataRowsForHiliteContainer */
    public DataTable getDataRowsForHilite() {
        if (m_dataRowsForHiliteContainer != null) {
            m_dataRowsForHiliteContainer.close();
            return m_dataRowsForHiliteContainer.getTable();
        }
        return null;
    }
}
