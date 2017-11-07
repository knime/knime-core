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
 * ---------------------------------------------------------------------
 *
 * Created on 2013.08.17. by Gabor Bakos
 */
package org.knime.base.node.mine.decisiontree2;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import org.dmg.pmml.ArrayType;
import org.dmg.pmml.ArrayType.Type;
import org.dmg.pmml.CompoundPredicateDocument;
import org.dmg.pmml.CompoundPredicateDocument.CompoundPredicate;
import org.dmg.pmml.FalseDocument;
import org.dmg.pmml.SimplePredicateDocument.SimplePredicate;
import org.dmg.pmml.SimpleSetPredicateDocument.SimpleSetPredicate;
import org.dmg.pmml.TrueDocument;
import org.knime.core.node.port.pmml.preproc.DerivedFieldMapper;

/**
 * Condition translator between raw PMML and KNIME's representation. <br>
 * Based on the former {@link PMMLDecisionTreeTranslator} from wenlin, Zementis Inc., Apr 2011, now, that is a subclass.
 *
 * @author Gabor Bakos
 * @since 2.9
 */
public abstract class PMMLConditionTranslator {

    private static final String COMPOUND = "Compound";

    private static final String TRUE = "True";

    private static final String FALSE = "False";

    private static final String BACKSLASH = "\\";

    private static final String DOUBLE_QUOT = "\"";

    private static final String SPACE = " ";

    private static final String TAB = "\t";

    /** The {@link DerivedFieldMapper}. */
    protected DerivedFieldMapper m_nameMapper;

    /**
     * Default constructor.
     */
    public PMMLConditionTranslator() {
        super();
    }

    /**
     * Create a KNIME compound predicate from a PMML compound predicate. Note that the "order" of the sub-predicates is
     * important (because of surrogate predicate). Therefore, we need to use xmlCursor to retrieve the order of the
     * predicates
     *
     * @param xmlCompoundPredicate the PMML Compound Predicate element
     * @return the KNIME Compound Predicate
     */
    protected PMMLCompoundPredicate parseCompoundPredicate(final CompoundPredicate xmlCompoundPredicate) {
        List<PMMLPredicate> tempPredicateList = new ArrayList<PMMLPredicate>();

        if (xmlCompoundPredicate.sizeOfSimplePredicateArray() != 0) {
            for (SimplePredicate xmlSubSimplePredicate : xmlCompoundPredicate.getSimplePredicateList()) {
                tempPredicateList.add(parseSimplePredicate(xmlSubSimplePredicate));
            }
        }

        if (xmlCompoundPredicate.sizeOfCompoundPredicateArray() != 0) {
            for (CompoundPredicate xmlSubCompoundPredicate : xmlCompoundPredicate.getCompoundPredicateList()) {
                tempPredicateList.add(parseCompoundPredicate(xmlSubCompoundPredicate));
            }
        }

        if (xmlCompoundPredicate.sizeOfSimpleSetPredicateArray() != 0) {
            for (SimpleSetPredicate xmlSubSimpleSetPredicate : xmlCompoundPredicate.getSimpleSetPredicateList()) {
                tempPredicateList.add(parseSimpleSetPredicate(xmlSubSimpleSetPredicate));
            }
        }

        if (xmlCompoundPredicate.sizeOfTrueArray() != 0) {
            for (int i = 0; i < xmlCompoundPredicate.sizeOfTrueArray(); i++) {
                tempPredicateList.add(new PMMLTruePredicate());
            }
        }

        if (xmlCompoundPredicate.sizeOfFalseArray() != 0) {
            for (int i = 0; i < xmlCompoundPredicate.sizeOfFalseArray(); i++) {
                tempPredicateList.add(new PMMLFalsePredicate());
            }
        }

        List<String> predicateNames = new ArrayList<String>();
        XmlCursor xmlCursor = xmlCompoundPredicate.newCursor();

        if (xmlCursor.toFirstChild()) {
            do {
                XmlObject xmlElement = xmlCursor.getObject();
                XmlCursor elementCursor = xmlElement.newCursor();

                if (xmlElement instanceof CompoundPredicateDocument.CompoundPredicate) {
                    predicateNames.add(COMPOUND);
                } else if (xmlElement instanceof TrueDocument.True) {
                    predicateNames.add(TRUE);
                } else if (xmlElement instanceof FalseDocument.False) {
                    predicateNames.add(FALSE);
                } else {
                    elementCursor.toFirstAttribute();
                    do {
                        if ("field".equals(elementCursor.getName().getLocalPart())) {
                            predicateNames.add(m_nameMapper.getColumnName(elementCursor.getTextValue()));
                            break;
                        }
                    } while (elementCursor.toNextAttribute());

                }

            } while (xmlCursor.toNextSibling());
        }

        // ------------------------------------------------------
        // sort the predicate list
        List<PMMLPredicate> predicateList = new ArrayList<PMMLPredicate>();
        List<PMMLPredicate> compoundList = new ArrayList<PMMLPredicate>();
        for (PMMLPredicate tempPredicate : tempPredicateList) {
            if (tempPredicate instanceof PMMLCompoundPredicate) {
                compoundList.add(tempPredicate);
            }
        }

        for (String name : predicateNames) {
            if (name.equals(COMPOUND)) {
                predicateList.add(compoundList.get(0));
                compoundList.remove(0);
            } else if (name.equals(TRUE)) {
                predicateList.add(new PMMLTruePredicate());
            } else if (name.equals(FALSE)) {
                predicateList.add(new PMMLFalsePredicate());
            } else {
                int foundIndex = -1, i = 0;
                for (PMMLPredicate tempPredicate : tempPredicateList) {
                    if (tempPredicate instanceof PMMLSimplePredicate) {
                        if (name.equals(((PMMLSimplePredicate)tempPredicate).getSplitAttribute())) {
                            predicateList.add(tempPredicate);
                            foundIndex = i;
                            break;
                        }
                    } else if (tempPredicate instanceof PMMLSimpleSetPredicate) {
                        if (name.equals(((PMMLSimpleSetPredicate)tempPredicate).getSplitAttribute())) {
                            predicateList.add(tempPredicate);
                            foundIndex = i;
                            break;
                        }
                    }
                    ++i;
                }
                assert foundIndex >= 0 : tempPredicateList + "\n" + name;
                tempPredicateList.remove(foundIndex);
            }
        }

        LinkedList<PMMLPredicate> subPredicates = new LinkedList<PMMLPredicate>(predicateList);

        String operator = xmlCompoundPredicate.getBooleanOperator().toString();
        PMMLCompoundPredicate compoundPredicate = newCompoundPredicate(operator);
        compoundPredicate.setPredicates(subPredicates);

        return compoundPredicate;
    }

    /**
     * @param operator The {@link String} representation of the operator name.
     * @return A {@link PMMLCompoundPredicate} implementation with {@code operator} set.
     */
    protected abstract PMMLCompoundPredicate newCompoundPredicate(String operator);

    /**
     * Create a KNIME simple set predicate from a PMML simple set predicate.
     *
     * @param xmlSimpleSetPredicate the PMML simple set predicate element
     * @return the KNIME Simple Set Predicate
     */
    protected PMMLPredicate parseSimpleSetPredicate(final SimpleSetPredicate xmlSimpleSetPredicate) {
        String field = m_nameMapper.getColumnName(xmlSimpleSetPredicate.getField());
        String operator = xmlSimpleSetPredicate.getBooleanOperator().toString();
        PMMLSimpleSetPredicate simpleSetPredicate = new PMMLSimpleSetPredicate(field, operator);
        ArrayType pmmlArray = xmlSimpleSetPredicate.getArray();
        PMMLArrayType arrayType = PMMLArrayType.STRING;
        if (Type.REAL == pmmlArray.getType()) {
            arrayType = PMMLArrayType.REAL;
        } else if (Type.INT == pmmlArray.getType()) {
            arrayType = PMMLArrayType.INT;
        }
        simpleSetPredicate.setArrayType(arrayType);

        String content = pmmlArray.newCursor().getTextValue();
        String[] stringValues;
        content = content.trim();

        if (content.contains(DOUBLE_QUOT)) {
            content = content.replace(BACKSLASH + DOUBLE_QUOT, TAB);
            // ==> <Array n="3" type="string">"Cheval  Blanc" "TABTAB"
            // "Latour"</Array>

            stringValues = content.split(DOUBLE_QUOT + SPACE);

            for (int i = 0; i < stringValues.length; i++) {
                stringValues[i] = stringValues[i].replace(DOUBLE_QUOT, "");
                stringValues[i] = stringValues[i].replace(TAB, DOUBLE_QUOT);
                if (PMMLArrayType.STRING != arrayType) {
                    // do not trim string values
                    stringValues[i] = stringValues[i].trim();
                }
            }
        } else {
            stringValues = content.split("\\s+");
        }

        List<String> valueList = new ArrayList<String>();
        for (String stringValue : stringValues) {
            valueList.add(stringValue);
        }

        simpleSetPredicate.setValues(valueList);
        return simpleSetPredicate;

    }

    /**
     * Create a KNIME simple predicate from a PMML simple predicate.
     *
     * @param xmlSimplePredicate the PMML simple predicate element
     * @return the KNIME Simple Set Predicate
     */
    protected PMMLPredicate parseSimplePredicate(final SimplePredicate xmlSimplePredicate) {
        String field = m_nameMapper.getColumnName(xmlSimplePredicate.getField());
        String operator = xmlSimplePredicate.getOperator().toString();
        String value = xmlSimplePredicate.getValue();
        return newSimplePredicate(field, operator, value);
    }

    /**
     * @param field The name of the column.
     * @param operator The {@link String} representation of the operator name.
     * @param value The value to compare to, can be {@code null} when we want to check for missing/non-missing values.
     * @return A {@link PMMLSimplePredicate} implementation with {@code operator} set.
     */
    protected abstract PMMLSimplePredicate newSimplePredicate(String field, String operator, String value);
}
