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
 * ------------------------------------------------------------------------
 */
package org.knime.base.node.preproc.pmml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.dmg.pmml.DATATYPE;
import org.dmg.pmml.DerivedFieldDocument.DerivedField;
import org.dmg.pmml.FieldRefDocument.FieldRef;
import org.dmg.pmml.LocalTransformationsDocument.LocalTransformations;
import org.dmg.pmml.OPTYPE;
import org.dmg.pmml.TransformationDictionaryDocument.TransformationDictionary;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.port.pmml.PMMLDataDictionaryTranslator;
import org.knime.core.node.port.pmml.preproc.DerivedFieldMapper;
import org.knime.core.node.port.pmml.preproc.PMMLPreprocTranslator;

public class PMMLStringConversionTranslator implements PMMLPreprocTranslator {
    private final DerivedFieldMapper m_mapper;
    private DataType m_parseType;
    private final List<String> m_includeCols;

    /**
     * @param derivedFieldMapper the derived field mapper
     */
    public PMMLStringConversionTranslator(
            final DerivedFieldMapper derivedFieldMapper) {
        m_parseType = null;
        m_mapper = derivedFieldMapper;
        m_includeCols = new ArrayList<String>();
    }

    /**
     * @param includeList the names of the included colums
     * @param parseType the resulting type
     * @param derivedFieldMapper the derived field mapper
     */
    public PMMLStringConversionTranslator(final List<String> includeList,
            final DataType parseType,
            final DerivedFieldMapper derivedFieldMapper) {
        m_parseType = parseType;
        m_mapper = derivedFieldMapper;
        m_includeCols = includeList;
    }
    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<Integer> initializeFrom(final DerivedField[] derivedFields) {
        if (derivedFields == null) {
            return Collections.EMPTY_LIST;
        }
        int num = derivedFields.length;
        List<Integer> consumed = new ArrayList<Integer>(num);
        for (int i = 0; i < derivedFields.length; i++) {
            DerivedField df = derivedFields[i];
            /** This field contains the name of the column in KNIME that
             * corresponds to the derived field in PMML. This is necessary if
             * derived fields are defined on other derived fields and the
             * columns in KNIME are replaced with the preprocessed values.
             * In this case KNIME has to know the original names (e.g. A) while
             * PMML references to A*, A** etc. */
            String displayName = df.getDisplayName();

            if (!df.isSetFieldRef()) {
                //only reading field references
                continue;
            }
            DataType dataType = PMMLDataDictionaryTranslator.getKNIMEDataType(
                    df.getDataType());
            if (dataType.isCompatible(IntValue.class)) {
                m_parseType = IntCell.TYPE;
            } else if (dataType.isCompatible(DoubleValue.class)) {
                m_parseType = DoubleCell.TYPE;
            } else if (dataType == StringCell.TYPE) {
                m_parseType = StringCell.TYPE;
            } else {
                // only processing int, double and string conversions
                continue;
            }

            FieldRef fieldRef = df.getFieldRef();
            if (displayName != null) {
                m_includeCols.add(displayName);
            } else {
                m_includeCols.add(m_mapper.getColumnName(fieldRef.getField()));
            }
            consumed.add(i);
        }
        return consumed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TransformationDictionary exportToTransDict() {
        TransformationDictionary dictionary =
                TransformationDictionary.Factory.newInstance();
        dictionary.setDerivedFieldArray(createDerivedFields());
        return dictionary;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LocalTransformations exportToLocalTrans() {
        LocalTransformations localtrans =
                LocalTransformations.Factory.newInstance();
        localtrans.setDerivedFieldArray(createDerivedFields());
        return localtrans;
    }

    private DerivedField[] createDerivedFields() {
        DATATYPE.Enum dataType = PMMLDataDictionaryTranslator.getPMMLDataType(
                m_parseType);
        OPTYPE.Enum optype = PMMLDataDictionaryTranslator.getOptype(
                m_parseType);
        int num = m_includeCols.size();
        DerivedField[] derivedFields = new DerivedField[num];
        for (int i = 0; i < num; i++) {
            DerivedField df = DerivedField.Factory.newInstance();
            String name = m_includeCols.get(i);
            df.setDisplayName(name);
            /* The field name must be retrieved before creating a new derived
             * name for this derived field as the map only contains the
             * current mapping. */
            String fieldName = m_mapper.getDerivedFieldName(name);
            df.setName(m_mapper.createDerivedFieldName(name));
            df.setDataType(dataType);
            df.setOptype(optype);
            FieldRef fieldRef = df.addNewFieldRef();
            fieldRef.setField(fieldName);
            derivedFields[i] = df;
        }
        return derivedFields;
    }

    /**
     * @return the parseType
     */
    public DataType getParseType() {
        return m_parseType;
    }

    /**
     * @return the includeCols
     */
    public List<String> getIncludeCols() {
        return m_includeCols;
    }

}
