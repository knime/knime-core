/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 * ------------------------------------------------------------------------
 */
package org.knime.base.node.preproc.pmml.columnTrans;

import java.util.List;

import org.dmg.pmml.ApplyDocument.Apply;
import org.dmg.pmml.DATATYPE;
import org.dmg.pmml.DerivedFieldDocument.DerivedField;
import org.dmg.pmml.LocalTransformationsDocument.LocalTransformations;
import org.dmg.pmml.OPTYPE;
import org.dmg.pmml.TransformationDictionaryDocument.TransformationDictionary;
import org.knime.core.node.port.pmml.preproc.PMMLPreprocTranslator;


/**
 * Adds a derived field to a pmml document's preprocessing description.
 * For each row in the input table if one of the input fields has the value 1,
 * the derived field contains this column's name
 *
 * @author Alexander Fillbrunn
 *
 */
public class PMMLMany2OneTranslator implements PMMLPreprocTranslator {

    private String m_appendedCol;
    private String[] m_sourceCols;

    /**
     * Constructor for PMMLMany2OneTranslator.
     * @param appendedCol The name of the column that is appended to the input table
     * @param sourceCols The columns that are evaluated to determine the content of the appended column
     */
    public PMMLMany2OneTranslator(final String appendedCol, final String[] sourceCols) {
        m_appendedCol = appendedCol;
        m_sourceCols = sourceCols;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Integer> initializeFrom(final DerivedField[] derivedFields) {
        throw new UnsupportedOperationException(getClass().getName() + "#initializeFrom(.) not implemented.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TransformationDictionary exportToTransDict() {
        TransformationDictionary dictionary =
            TransformationDictionary.Factory.newInstance();
        dictionary.setDerivedFieldArray(new DerivedField[]{createDerivedField()});
        return dictionary;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LocalTransformations exportToLocalTrans() {
        LocalTransformations localTrans = LocalTransformations.Factory.newInstance();
        localTrans.setDerivedFieldArray(new DerivedField[]{createDerivedField()});
        return localTrans;
    }

    private DerivedField createDerivedField() {
        DerivedField derivedField = DerivedField.Factory.newInstance();
        derivedField.setName(m_appendedCol);
        derivedField.setDataType(DATATYPE.STRING);
        derivedField.setOptype(OPTYPE.CATEGORICAL);

        Apply parentApply = null;
        for (String col : m_sourceCols) {
            Apply ifApply;
            if (parentApply == null) {
                ifApply = derivedField.addNewApply();
            } else {
                ifApply = parentApply.addNewApply();
            }
            ifApply.setFunction("if");
            Apply innerIf = ifApply.addNewApply();
            innerIf.setFunction("equal");
            innerIf.addNewFieldRef().setField(col);
            innerIf.addNewConstant().setStringValue("1");
            ifApply.addNewConstant().setStringValue(col);
            parentApply = ifApply;
        }
        if (parentApply != null) {
            parentApply.addNewConstant().setStringValue("missing");
        }
        return derivedField;
    }

}
