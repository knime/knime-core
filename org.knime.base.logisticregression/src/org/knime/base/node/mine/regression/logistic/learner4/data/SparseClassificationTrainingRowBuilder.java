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
 *   23.05.2017 (Adrian Nembach): created
 */
package org.knime.base.node.mine.regression.logistic.learner4.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.NominalValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;

/**
 *
 * @author Adrian Nembach, KNIME.com
 */
public final class SparseClassificationTrainingRowBuilder extends AbstractTrainingRowBuilder<ClassificationTrainingRow> {

    private final int m_targetIdx;
    private final List<DataCell> m_targetDomain;
    /**
     * @param data
     * @param pmmlSpec
     * @param targetReferenceCategory
     * @param sortTargetCategories
     * @param sortFactorsCategories
     * @throws InvalidSettingsException
     */
    public SparseClassificationTrainingRowBuilder(final BufferedDataTable data, final PMMLPortObjectSpec pmmlSpec,
        final DataCell targetReferenceCategory, final boolean sortTargetCategories, final boolean sortFactorsCategories) throws InvalidSettingsException {
        super(data, pmmlSpec, sortFactorsCategories);
        DataColumnSpec targetSpec = pmmlSpec.getTargetCols().get(0);
        if (!targetSpec.getType().isCompatible(NominalValue.class)) {
            throw new IllegalArgumentException("The selected target column \"" + targetSpec.getName()
            + "\" is not nominal!");
        }
        m_targetIdx = data.getDataTableSpec().findColumnIndex(pmmlSpec.getTargetCols().get(0).getName());
        List<DataCell> valueList = new ArrayList<DataCell>();
        final DataColumnDomain domain = targetSpec.getDomain();
        if (domain == null || domain.getValues() == null) {
            throw new IllegalStateException("Calculate the possible values of " + targetSpec.getName());
        }
        valueList.addAll(domain.getValues());
        if (sortTargetCategories) {
            Collections.sort(valueList, targetSpec.getType().getComparator());
        }
        if (targetReferenceCategory != null) {
            // targetReferenceCategory must be the last element
            boolean removed = valueList.remove(targetReferenceCategory);
            if (!removed) {
                throw new InvalidSettingsException(
                    "The target reference category (\"" + targetReferenceCategory
                    + "\") is not found in the target column");
            }
            valueList.add(targetReferenceCategory);
        }
        m_targetDomain = valueList;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ClassificationTrainingRow createTrainingRow(final DataRow row,final int[] nonZeroFeatures,
        final float[] values, final int id) {
        DataCell targetCell = row.getCell(m_targetIdx);
        DataType type = targetCell.getType();
        if (!type.isCompatible(NominalValue.class)) {
            throw new IllegalStateException("The DataCell \"" + targetCell.toString() + " is not nominal!");
        }
        int target = m_targetDomain.indexOf(targetCell);
        if (target < 0) {
            throw new IllegalStateException("DataCell \"" + row.getCell(target).toString()
                + "\" is not in the DataColumnDomain of target column. " + "Please apply a "
                + "Domain Calculator on the target column.");
        }

        return new SparseClassificationTrainingRow(values, nonZeroFeatures, id, target);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getTargetDimension() {
        return m_targetDomain.size() - 1;
    }

}
