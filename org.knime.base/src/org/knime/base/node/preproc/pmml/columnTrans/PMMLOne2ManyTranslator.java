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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   Jul 15, 2011 (morent): created
 */
package org.knime.base.node.preproc.pmml.columnTrans;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.dmg.pmml.DATATYPE;
import org.dmg.pmml.DerivedFieldDocument.DerivedField;
import org.dmg.pmml.LocalTransformationsDocument.LocalTransformations;
import org.dmg.pmml.NormDiscreteDocument.NormDiscrete;
import org.dmg.pmml.OPTYPE;
import org.dmg.pmml.TransformationDictionaryDocument.TransformationDictionary;
import org.knime.core.node.port.pmml.preproc.DerivedFieldMapper;
import org.knime.core.node.port.pmml.preproc.PMMLPreprocTranslator;
import org.knime.core.util.Pair;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * @author morent
 *
 */
public class PMMLOne2ManyTranslator implements PMMLPreprocTranslator {
    private final Map<String, List<Pair<String, String>>> m_columnMapping;
    private final DerivedFieldMapper m_mapper;

    /**
     * Creates an initialized translator that can export its configuration.
     *
     * @param columnMapping a mapping of column names to their associated
     *      discretized columns
     * @param mapper mapping data column names to PMML derived field names and
     *      vice versa
     */
    public PMMLOne2ManyTranslator(
            final Map<String, List<Pair<String, String>>> columnMapping,
            final DerivedFieldMapper mapper) {
        m_columnMapping = columnMapping;
        m_mapper = mapper;
    }


    /**
     * Not yet implemented!
     * {@inheritDoc}
     */
    @Override
    public List<Integer> initializeFrom(final DerivedField[] derivedFields) {
       throw new NotImplementedException();
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
       LocalTransformations localTrans
               = LocalTransformations.Factory.newInstance();
       localTrans.setDerivedFieldArray(createDerivedFields());
       return localTrans;
    }

    private DerivedField[] createDerivedFields() {
        List<DerivedField> derivedFields = new ArrayList<DerivedField>();
        for (Map.Entry<String, List<Pair<String, String>>> entry
                : m_columnMapping.entrySet()) {
            String columnName = entry.getKey();
            String derivedName = m_mapper.getDerivedFieldName(columnName);
            for (Pair<String, String> nameValue : entry.getValue()) {
                DerivedField derivedField = DerivedField.Factory.newInstance();
                derivedField.setName(nameValue.getFirst());
                derivedField.setOptype(OPTYPE.ORDINAL);
                derivedField.setDataType(DATATYPE.INTEGER);
                NormDiscrete normDiscrete = derivedField.addNewNormDiscrete();
                normDiscrete.setField(derivedName);
                normDiscrete.setValue(nameValue.getSecond());
                normDiscrete.setMapMissingTo(0);
                derivedFields.add(derivedField);
            }
        }
        return derivedFields.toArray(new DerivedField[0]);
    }
}
