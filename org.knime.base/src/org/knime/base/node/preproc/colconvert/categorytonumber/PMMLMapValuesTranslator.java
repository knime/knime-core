/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   09.09.2011 (hofer): created
 */
package org.knime.base.node.preproc.colconvert.categorytonumber;

import java.util.List;
import java.util.Map.Entry;

import javax.xml.namespace.QName;

import org.apache.xmlbeans.XmlCursor;
import org.dmg.pmml.DerivedFieldDocument.DerivedField;
import org.dmg.pmml.ExtensionDocument.Extension;
import org.dmg.pmml.FieldColumnPairDocument.FieldColumnPair;
import org.dmg.pmml.InlineTableDocument.InlineTable;
import org.dmg.pmml.LocalTransformationsDocument.LocalTransformations;
import org.dmg.pmml.MapValuesDocument.MapValues;
import org.dmg.pmml.RowDocument.Row;
import org.dmg.pmml.TransformationDictionaryDocument.TransformationDictionary;
import org.knime.core.data.DataCell;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.port.pmml.preproc.DerivedFieldMapper;
import org.knime.core.node.port.pmml.preproc.PMMLPreprocTranslator;


/**
 * Create PMML DerivedField with MapValues Element.
 *
 * @author Heiko Hofer
 */
public class PMMLMapValuesTranslator implements PMMLPreprocTranslator {
    private final MapValuesConfiguration m_config;
    private final DerivedFieldMapper m_mapper;

    /**
     * @param config information about the PMML MapValues element
     * @param mapper mapper of PMML fields to KNIME columns
     */
    public PMMLMapValuesTranslator(
            final MapValuesConfiguration config,
            final DerivedFieldMapper mapper) {
        m_config = config;
        m_mapper = mapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Integer> initializeFrom(final DerivedField[] derivedFields) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TransformationDictionary exportToTransDict() {
        TransformationDictionary dictionary = TransformationDictionary.Factory
                .newInstance();
        dictionary.setDerivedFieldArray(createDerivedFields());
        return dictionary;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LocalTransformations exportToLocalTrans() {
        LocalTransformations localTrans = LocalTransformations.Factory
                .newInstance();
        localTrans.setDerivedFieldArray(createDerivedFields());
        return localTrans;
    }

    private DerivedField[] createDerivedFields() {
        DerivedField df = DerivedField.Factory.newInstance();
        df.setExtensionArray(createSummaryExtension());
        /* The field name must be retrieved before creating a new derived
         * name for this derived field as the map only contains the
         * current mapping. */
        String fieldName = m_mapper.getDerivedFieldName(m_config.getInColumn());
        if (m_config.getInColumn().equals(m_config.getOutColumn())) {
            String name = m_config.getInColumn();
            df.setDisplayName(name);
            df.setName(m_mapper.createDerivedFieldName(name));
        } else {
            df.setName(m_config.getOutColumn());
        }
        df.setOptype(m_config.getOpType());
        df.setDataType(m_config.getOutDataType());
        MapValues mapValues = df.addNewMapValues();
        // the element in the InlineTable representing the output column
        // Use dummy name instead of m_config.getOutColumn() since the
        // input column could contain characters that are not allowed in XML
        final QName xmlOut = new QName("http://www.dmg.org/PMML-4_0", "out");
        mapValues.setOutputColumn(xmlOut.getLocalPart());
        mapValues.setDataType(m_config.getOutDataType());
        if (!m_config.getDefaultValue().isMissing()) {
            mapValues.setDefaultValue(m_config.getDefaultValue().toString());
        }
        if (!m_config.getMapMissingTo().isMissing()) {
            mapValues.setMapMissingTo(m_config.getMapMissingTo().toString());
        }
        // the mapping of input field <-> element in the InlineTable
        FieldColumnPair fieldColPair = mapValues.addNewFieldColumnPair();
        fieldColPair.setField(fieldName);
        // Use dummy name instead of m_config.getInColumn() since the
        // input column could contain characters that are not allowed in XML
        final QName xmlIn = new QName("http://www.dmg.org/PMML-4_0", "in");
        fieldColPair.setColumn(xmlIn.getLocalPart());
        InlineTable table = mapValues.addNewInlineTable();
        for (Entry<DataCell, ? extends DataCell> entry
                : m_config.getEntries().entrySet()) {
            Row row = table.addNewRow();
            XmlCursor cursor = row.newCursor();
            cursor.toNextToken();
            cursor.insertElementWithText(xmlIn, entry.getKey().toString());
            cursor.insertElementWithText(xmlOut, entry.getValue().toString());
            cursor.dispose();
        }
        return new DerivedField[]{df};
    }

    private Extension[] createSummaryExtension() {
        Extension extension = Extension.Factory.newInstance();
        extension.setName("summary");
        extension.setExtender(PMMLPortObjectSpec.KNIME);
        extension.setValue(m_config.getSummary());
        return new Extension[] {extension};
    }

}
