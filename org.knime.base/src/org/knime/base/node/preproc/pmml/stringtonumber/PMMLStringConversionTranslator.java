/* ------------------------------------------------------------------
  * This source code, its documentation and all appendant files
  * are protected by copyright law. All rights reserved.
  *
  * Copyright, 2008 - 2013
  * KNIME.com, Zurich, Switzerland
  *
  * You may not modify, publish, transmit, transfer or sell, reproduce,
  * create derivative works from, distribute, perform, display, or in
  * any way exploit any of the content, in whole or in part, except as
  * otherwise expressly permitted in writing by the copyright owner or
  * as specified in the license file distributed with this product.
  *
  * If you have any questions please contact the copyright holder:
  * website: www.knime.com
  * email: contact@knime.com
  * ---------------------------------------------------------------------
  *
  * History
  *   May 24, 2011 (morent): created
  */

package org.knime.base.node.preproc.pmml.stringtonumber;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.dmg.pmml.DATATYPE.Enum;
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

/**
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 *
 */
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
        Enum dataType = PMMLDataDictionaryTranslator.getPMMLDataType(
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
