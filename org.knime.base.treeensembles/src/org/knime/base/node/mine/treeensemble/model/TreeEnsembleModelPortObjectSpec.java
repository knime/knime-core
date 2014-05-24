/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 *
 * History
 *   Jan 10, 2012 (wiswedel): created
 */
package org.knime.base.node.mine.treeensemble.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.knime.base.data.filter.column.FilterColumnTable;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.NominalValue;
import org.knime.core.data.vector.bitvector.BitVectorValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.port.AbstractSimplePortObjectSpec;

/**
 * 
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public final class TreeEnsembleModelPortObjectSpec extends AbstractSimplePortObjectSpec {

    private DataTableSpec m_learnSpec;

    /** Framework constructor, not to be used by node itself. */
    public TreeEnsembleModelPortObjectSpec() {
        // needed for loading
    }

    /**
     * Constructor for class TreeEnsembleModelPortObjectSpec.
     * 
     * @param learnSpec the {@link DataTableSpec} of the training data table
     */
    public TreeEnsembleModelPortObjectSpec(final DataTableSpec learnSpec) {
        if (learnSpec == null) {
            throw new NullPointerException("Argument must not be null");
        }
        m_learnSpec = learnSpec;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void load(final ModelContentRO model) throws InvalidSettingsException {
        m_learnSpec = DataTableSpec.load(model.getModelContent("learnSpec"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void save(final ModelContentWO model) {
        m_learnSpec.save(model.addModelContent("learnSpec"));
    }

    /**
     * Get the spec of the training data. The last column is nominal and represent the target column (does not need to
     * be present in the test set)
     * 
     * @return the learn spec, rearranged such that it only contains the relevant columns and the target column at the
     *         last column index.
     */
    public DataTableSpec getTableSpec() {
        return m_learnSpec;
    }

    public DataColumnSpec getTargetColumn() {
        return m_learnSpec.getColumnSpec(m_learnSpec.getNumColumns() - 1);
    }

    /**
     * A map containing the possible values of the target column (training set), whereby the map key is the toString()
     * representation of the value.
     * 
     * <p>
     * Used in the learner to ensure there are no duplicates in the target column (learner is using plain strings, not
     * DataCell) and in the predictor to return DataCells of the correct type.
     * 
     * @return Such a map or null if there are no possible values.
     * @throws InvalidSettingsException If duplicates in the toString representation are encountered.
     */
    public Map<String, DataCell> getTargetColumnPossibleValueMap() throws InvalidSettingsException {
        DataColumnSpec targetCol = getTargetColumn();
        Set<DataCell> values = targetCol.getDomain().getValues();
        if (values == null) {
            return null;
        }
        Map<String, DataCell> result = new LinkedHashMap<String, DataCell>();
        for (DataCell v : values) {
            String toString = v.toString();
            DataCell old = result.put(toString, v);
            if (old != null) {
                throw new InvalidSettingsException("The target column contains "
                    + "distinct values whose string representations are "
                    + "identical and therefore not unique; convert the " + "target column to a plain string first. "
                    + "(Problematic value: \"" + toString + "\")");
            }
        }
        return result;
    }

    public DataTableSpec getLearnTableSpec() {
        // remove all but last column
        return FilterColumnTable.createFilterTableSpec(m_learnSpec, false, m_learnSpec.getNumColumns() - 1);
    }

    public void assertTargetTypeMatches(final boolean isRegression) throws InvalidSettingsException {
        if (isRegression) {
            if (!getTargetColumn().getType().isCompatible(DoubleValue.class)) {
                throw new InvalidSettingsException("Can't apply classification model for regression tasks");
            }
        } else {
            if (!getTargetColumn().getType().isCompatible(NominalValue.class)) {
                throw new InvalidSettingsException("Can't apply regression model for classification tasks");
            }
        }
    }

    public int[] calculateFilterIndices(final DataTableSpec testTableInput) throws InvalidSettingsException {
        DataTableSpec learnSpec = getLearnTableSpec();
        // check existence and types of columns, create reordering
        int[] result = new int[learnSpec.getNumColumns()];
        for (int i = 0; i < learnSpec.getNumColumns(); i++) {
            DataColumnSpec learnCol = learnSpec.getColumnSpec(i);
            final String colName = learnCol.getName();
            int dataColIndex = testTableInput.findColumnIndex(colName);
            if (dataColIndex < 0) {
                throw new InvalidSettingsException("Required data column \"" + colName + "\" does not exist in table");
            }
            DataColumnSpec dataCol = testTableInput.getColumnSpec(dataColIndex);
            DataType eType = learnCol.getType(); // expected type
            DataType aType = dataCol.getType(); // actual type
            String errorType = null;
            if (eType.isCompatible(NominalValue.class) && !aType.isCompatible(NominalValue.class)) {
                errorType = "nominal";
            }
            if (eType.isCompatible(DoubleValue.class) && !aType.isCompatible(DoubleValue.class)) {
                errorType = "numeric";
            }
            if (eType.isCompatible(BitVectorValue.class) && !aType.isCompatible(BitVectorValue.class)) {
                errorType = "fingerprint/bitvector";
            }
            if (errorType != null) {
                throw new InvalidSettingsException("Column \"" + colName + "\" does exist in the data but"
                    + "is not of the expected " + errorType + " type");
            }
            result[i] = dataColIndex;
        }
        return result;
    }

}
