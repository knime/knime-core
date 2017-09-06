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
 *   05.09.2017 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.model.pmml;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.dmg.pmml.PMMLDocument;
import org.knime.base.node.mine.treeensemble2.data.NominalValueRepresentation;
import org.knime.base.node.mine.treeensemble2.data.TreeAttributeColumnMetaData;
import org.knime.base.node.mine.treeensemble2.data.TreeNominalColumnMetaData;
import org.knime.base.node.mine.treeensemble2.data.TreeNumericColumnMetaData;
import org.knime.base.node.mine.treeensemble2.data.TreeTargetColumnMetaData;
import org.knime.base.node.mine.treeensemble2.data.TreeTargetNominalColumnMetaData;
import org.knime.base.node.mine.treeensemble2.data.TreeTargetNumericColumnMetaData;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.port.pmml.PMMLDataDictionaryTranslator;
import org.knime.core.node.util.CheckUtils;

/**
 * This implementation of MetaDataMapper only supports models that were build with
 * ordinary KNIME tables containing no vectors.
 *
 * @author Adrian Nembach, KNIME
 */
class SimpleMetaDataMapper implements MetaDataMapper {

    private final DataTableSpec m_learnSpec;
    private final Map<String, TreeAttributeColumnMetaData> m_metaDataMap;
    private final TreeTargetColumnMetaData m_targetColumnMetaData;

    /**
     * Creates a SimpleMetaDataMapper by extracting the meta data information from the data dictionary of the provided
     * PMMLDocument.
     * @param pmmlDoc PMMLDocument from whose data dictionary meta data information is to be extracted
     *
     */
    public SimpleMetaDataMapper(final PMMLDocument pmmlDoc) {
        PMMLDataDictionaryTranslator dataDictTrans = new PMMLDataDictionaryTranslator();
        dataDictTrans.initializeFrom(pmmlDoc);
        DataTableSpec tableSpec = dataDictTrans.getDataTableSpec();
        m_learnSpec = tableSpec;
        ColumnRearranger cr = new ColumnRearranger(tableSpec);
        // Remove the target column (assumes that the last column is the target)
        final int targetIdx = tableSpec.getNumColumns() - 1;
        cr.remove(targetIdx);
        m_metaDataMap = createMetaDataMapFromSpec(cr.createSpec());
        m_targetColumnMetaData = createTargetMetaDataFromSpec(tableSpec.getColumnSpec(targetIdx));
    }

    private static TreeTargetColumnMetaData createTargetMetaDataFromSpec(final DataColumnSpec targetSpec) {
        if (targetSpec.getType().isCompatible(DoubleCell.class)) {
            return new TreeTargetNumericColumnMetaData(targetSpec.getName());
        } else if (targetSpec.getType().isCompatible(StringCell.class)) {
            return new TreeTargetNominalColumnMetaData(targetSpec.getName(), extractNomValReps(targetSpec));
        }
        throw new IllegalArgumentException("The target column is of incompatible type \"" + targetSpec.getType()
        + "\".");
    }

    private static Map<String, TreeAttributeColumnMetaData> createMetaDataMapFromSpec(final DataTableSpec tableSpec) {
        Map<String, TreeAttributeColumnMetaData> map = new HashMap<>();
        for (int i = 0; i < tableSpec.getNumColumns(); i++) {
            DataColumnSpec colSpec = tableSpec.getColumnSpec(i);
            DataType colType = colSpec.getType();
            if (colType.isCompatible(StringCell.class)) {
                map.put(colSpec.getName(), createNominalMetaData(colSpec, i));
            } else if (colType.isCompatible(DoubleCell.class)) {
                map.put(colSpec.getName(), createNumericMetaData(colSpec, i));
            } else {
                throw new IllegalStateException("Only default KNIME types are supported right now.");
            }
        }
        return null;
    }

    /**
     * @param colSpec the {@link DataColumnSpec} for which to create the {@link TreeNumericColumnMetaData} object.
     * @return a {@link TreeNumericColumnMetaData} object for the provided colSpec
     */
    private static TreeNumericColumnMetaData createNumericMetaData(final DataColumnSpec colSpec, final int colIdx) {
        TreeNumericColumnMetaData metaData = new TreeNumericColumnMetaData(colSpec.getName());
        metaData.setAttributeIndex(colIdx);
        return metaData;
    }

    private static TreeNominalColumnMetaData createNominalMetaData(final DataColumnSpec colSpec, final int colIdx) {
        TreeNominalColumnMetaData metaData = new TreeNominalColumnMetaData(colSpec.getName(),
            extractNomValReps(colSpec));
        metaData.setAttributeIndex(colIdx);
        return metaData;
    }

    private static NominalValueRepresentation[] extractNomValReps(final DataColumnSpec colSpec) {
        DataColumnDomain domain = colSpec.getDomain();
        CheckUtils.checkArgument(domain.hasValues(),
            "The data dictionary of the field \"%s\" has no possible values assigned.", colSpec.getName());
        Set<DataCell> kvalues = domain.getValues();
        int assignedInteger = 0;
        NominalValueRepresentation[] values = new NominalValueRepresentation[kvalues.size()];
        for (DataCell cell : kvalues) {
            values[assignedInteger] = new NominalValueRepresentation(cell.toString(), assignedInteger);
            assignedInteger++;
        }
        return values;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TreeAttributeColumnMetaData getMetaData(final String field) {
        TreeAttributeColumnMetaData metaData = m_metaDataMap.get(field);
        CheckUtils.checkNotNull(metaData, "The provided field \"%s\" is not part of the data dictionary.", field);
        return m_metaDataMap.get(field);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataTableSpec getLearnSpec() {
        return m_learnSpec;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isNominal(final String field) {
        TreeAttributeColumnMetaData metaData = getMetaData(field);
        return metaData instanceof TreeNominalColumnMetaData;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TreeNominalColumnMetaData getNominalColumnMetaData(final String field) {
        TreeAttributeColumnMetaData metaData = getMetaData(field);
        CheckUtils.checkArgument(metaData instanceof TreeNominalColumnMetaData,
            "The provided field \"%s\" is not nominal.", field);
        return (TreeNominalColumnMetaData)metaData;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TreeNumericColumnMetaData getNumericColumnMetaData(final String field) {
        TreeAttributeColumnMetaData metaData = getMetaData(field);
        CheckUtils.checkArgument(metaData instanceof TreeNumericColumnMetaData,
            "The provided field \"%s\" is not numeric.", field);
        return (TreeNumericColumnMetaData)metaData;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TreeTargetColumnMetaData getTargetColumnMetaData() {
        return m_targetColumnMetaData;
    }

}
