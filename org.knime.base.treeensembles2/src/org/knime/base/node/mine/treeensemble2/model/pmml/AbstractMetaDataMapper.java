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

import org.dmg.pmml.DerivedFieldDocument;
import org.dmg.pmml.PMMLDocument;
import org.knime.base.node.mine.treeensemble2.data.TreeAttributeColumnMetaData;
import org.knime.base.node.mine.treeensemble2.data.TreeMetaData;
import org.knime.base.node.mine.treeensemble2.data.TreeTargetColumnMetaData;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.port.pmml.PMMLDataDictionaryTranslator;
import org.knime.core.node.port.pmml.preproc.DerivedFieldMapper;
import org.knime.core.node.util.CheckUtils;

/**
 * This implementation of MetaDataMapper only supports models that were build with
 * ordinary KNIME tables containing no vectors.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
abstract class AbstractMetaDataMapper <T extends TreeTargetColumnMetaData> implements MetaDataMapper<T> {

    private final DataTableSpec m_learnSpec;
    private final Map<String, AbstractAttributeColumnHelper<?>> m_colHelperMap;
    private final TargetColumnHelper<T> m_targetColumnHelper;

    /**
     * Creates a SimpleMetaDataMapper by extracting the meta data information from the data dictionary of the provided
     * PMMLDocument.
     * @param pmmlDoc PMMLDocument from whose data dictionary meta data information is to be extracted
     * @param targetName the name of the target field
     *
     */
    public AbstractMetaDataMapper(final PMMLDocument pmmlDoc, final String targetName) {
        this(extractTableSpecFromPMMLDoc(pmmlDoc), targetName, new DerivedFieldMapper(pmmlDoc));
    }

    public AbstractMetaDataMapper(final DataTableSpec tableSpec, final String targetName,
        final DerivedFieldMapper derivedFieldMapper) {
        ColumnRearranger cr = new ColumnRearranger(tableSpec);
        // move target column to last position
        cr.move(targetName, tableSpec.getNumColumns());
        m_learnSpec = cr.createSpec();
        cr.remove(targetName);
        m_colHelperMap = createColumnHelperMapFromSpec(cr.createSpec(), derivedFieldMapper);
        m_targetColumnHelper = createTargetColumnHelper(tableSpec.getColumnSpec(targetName));
    }

    static AbstractMetaDataMapper<?> createMetaDataMapper(final DataTableSpec tableSpec) {
        DataColumnSpec targetCol = tableSpec.getColumnSpec(tableSpec.getNumColumns() - 1);
        DataType targetType = targetCol.getType();
        // we need the pmml to instantiate the actual mapper which we don't have at this point
        DerivedFieldMapper dummyDerivedFieldMapper = new DerivedFieldMapper(new DerivedFieldDocument.DerivedField[] {});
        if (targetType.isCompatible(StringValue.class)) {
            return new ClassificationMetaDataMapper(tableSpec, targetCol.getName(), dummyDerivedFieldMapper);
        } else if (targetType.isCompatible(DoubleValue.class)) {
            return new RegressionMetaDataMapper(tableSpec, targetCol.getName(), dummyDerivedFieldMapper);
        }
        throw new IllegalArgumentException("The target column \"" + targetCol + "\" is not numeric or nominal.");
    }

    private static DataTableSpec extractTableSpecFromPMMLDoc(final PMMLDocument pmmlDoc) {
        PMMLDataDictionaryTranslator dataDictTrans = new PMMLDataDictionaryTranslator();
        dataDictTrans.initializeFrom(pmmlDoc);
        return dataDictTrans.getDataTableSpec();
    }

    protected abstract TargetColumnHelper<T> createTargetColumnHelper(final DataColumnSpec targetSpec);

    private static Map<String, AbstractAttributeColumnHelper<?>> createColumnHelperMapFromSpec(
        final DataTableSpec tableSpec, final DerivedFieldMapper derivedFieldMapper) {
        Map<String, AbstractAttributeColumnHelper<?>> map = new HashMap<>();
        for (int i = 0; i < tableSpec.getNumColumns(); i++) {
            DataColumnSpec colSpec = tableSpec.getColumnSpec(i);
            checkForVectorColumn(colSpec);
            DataType colType = colSpec.getType();
            String derivedName = derivedFieldMapper.getDerivedFieldName(colSpec.getName());
            if (colType.isCompatible(StringValue.class)) {
                map.put(derivedName, new NominalAttributeColumnHelper(colSpec, i));
            } else if (colType.isCompatible(DoubleValue.class)) {
                map.put(derivedName, new NumericAttributeColumnHelper(colSpec, i));
            } else {
                throw new IllegalStateException("Only default KNIME types are supported right now.");
            }
        }
        return map;
    }

    /**
     * Checks if <b>colSpec</b> could originate from a vector column.
     * If it does, this method throws an exception.
     * @param colSpec {@link DataColumnSpec} to check
     * @throws IllegalArgumentException if <b>colSpec</b> could originate from a vector column
     */
    private static void checkForVectorColumn(final DataColumnSpec colSpec) {
        final boolean possibleVectorName = TranslationUtil.isVectorFieldName(colSpec.getName());
        DataType type = colSpec.getType();
        DataColumnDomain domain = colSpec.getDomain();
        boolean domainInformationIsMissing = false;
        if (type.isCompatible(StringValue.class)) {
            if (domain.hasValues()) {
                domainInformationIsMissing = domain.getValues().isEmpty();
            } else {
                domainInformationIsMissing = true;
            }
        } else if (type.isCompatible(DoubleValue.class)) {
            domainInformationIsMissing = !domain.hasBounds();
        }
        CheckUtils.checkArgument(!(possibleVectorName && domainInformationIsMissing), "The column %s seems to "
            + "originate from a vector column. A model learned on a vector can currently not be imported.", colSpec);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ColumnHelper<?> getColumnHelper(final String field) {
        ColumnHelper<?> metaData = m_colHelperMap.get(field);
        CheckUtils.checkNotNull(metaData, "The provided field \"%s\" is not part of the data dictionary.", field);
        return m_colHelperMap.get(field);
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
        ColumnHelper<?> colHelper = getColumnHelper(field);
        return colHelper instanceof NominalAttributeColumnHelper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NominalAttributeColumnHelper getNominalColumnHelper(final String field) {
        ColumnHelper<?> colHelper = getColumnHelper(field);
        CheckUtils.checkArgument(colHelper instanceof NominalAttributeColumnHelper,
            "The provided field \"%s\" is not nominal.", field);
        return (NominalAttributeColumnHelper)colHelper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NumericAttributeColumnHelper getNumericColumnHelper(final String field) {
        ColumnHelper<?> metaData = getColumnHelper(field);
        CheckUtils.checkArgument(metaData instanceof NumericAttributeColumnHelper,
            "The provided field \"%s\" is not numeric.", field);
        return (NumericAttributeColumnHelper)metaData;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TargetColumnHelper<T> getTargetColumnHelper() {
        return m_targetColumnHelper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TreeMetaData getTreeMetaData() {
        TreeAttributeColumnMetaData[] attributes = m_colHelperMap.values().stream()
                .map(c -> c.getMetaData())
                .sorted((m1, m2) -> Integer.compare(m1.getAttributeIndex(), m2.getAttributeIndex()))
                .toArray(s -> new TreeAttributeColumnMetaData[s]);
        return TreeMetaData.createTreeMetaData(attributes, m_targetColumnHelper.getMetaData());
    }

}
