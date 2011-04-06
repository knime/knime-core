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
 * -------------------------------------------------------------------
 *
 * History
 *    24.03.2007 (Tobias Koetter): created
 */

package org.knime.base.node.mine.bayes.naivebayes.datamodel;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.Config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;


/**
 * This abstract class needs to be implemented by all attribute models and
 * provides missing value handling and some common methods.
 * @author Tobias Koetter, University of Konstanz
 */
public abstract class AttributeModel implements Comparable<AttributeModel> {

    private static final String ATTRIBUTE_NAME = "attributeName";
    private static final String MODEL_TYPE = "type";
    private static final String SKIP_MISSING_VALUES = "skipMissingVals";
    private static final String NO_OF_MISSING_VALUES = "numberOfMissingValues";
    private static final String INVALID_CAUSE = "invalidCause";
    private static final String MODEL_DATA_SECTION = "data";

    /**
     * Invalid cause if the model contains no records at all.
     */
    static final String MODEL_CONTAINS_NO_RECORDS =
        "No records";
    /**
     * Invalid cause if the model contains no class values.
     */
    static final String MODEL_CONTAINS_NO_CLASS_VALUES =
        "No class values";

    /**Column header of the missing value column.*/
    private static final String MISSING_VALUE_NAME = "MissingValue";

    private final String m_attributeName;

    private final boolean m_skipMissingVals;

    private int m_noOfMissingVals;

    private String m_invalidCause = null;


    /**Constructor for class ClassValue.
     * @param attributeName the name of the attribute
     * @param noOfMissingVals the number of missing values
     * @param skipMissingVals set to <code>true</code> if the missing values
     * should be skipped during learning and prediction
     */
    AttributeModel(final String attributeName, final int noOfMissingVals,
            final boolean skipMissingVals) {
        if (attributeName == null) {
            throw new NullPointerException("classValue must not be null");
        }
        m_attributeName = attributeName;
        m_noOfMissingVals = noOfMissingVals;
        m_skipMissingVals = skipMissingVals;
    }

    /**
     * @param config the <code>Config</code> object to read from
     * @return the attribute model for the given <code>Config</code> object
     * @throws InvalidSettingsException if the settings are invalid
     */
    static AttributeModel loadModel(final Config config)
    throws InvalidSettingsException {
        final String attrName = config.getString(ATTRIBUTE_NAME);
        final String modelType = config.getString(MODEL_TYPE);
        final boolean skipMissing = config.getBoolean(SKIP_MISSING_VALUES);
        final int noOfMissingVals = config.getInt(NO_OF_MISSING_VALUES);
        final String invalidCause = config.getString(INVALID_CAUSE);
        final Config modelConfig = config.getConfig(MODEL_DATA_SECTION);
        final AttributeModel model;
        if (NominalAttributeModel.MODEL_TYPE.equals(modelType)) {
            model = new NominalAttributeModel(attrName, noOfMissingVals,
                    skipMissing, modelConfig);
        } else if (NumericalAttributeModel.MODEL_TYPE.equals(modelType)) {
            model = new NumericalAttributeModel(attrName, skipMissing,
                    noOfMissingVals, modelConfig);
        } else if (ClassAttributeModel.MODEL_TYPE.equals(modelType)) {
            model = new ClassAttributeModel(attrName, noOfMissingVals,
                    skipMissing, modelConfig);
        } else if (BitVectorAttributeModel.MODEL_TYPE.equals(modelType)) {
            model = new BitVectorAttributeModel(attrName, skipMissing,
                    noOfMissingVals, modelConfig);
        } else {
            throw new InvalidSettingsException("Invalid model type: "
                    + modelType);
        }
        model.setInvalidCause(invalidCause);
        return model;
    }

    /**
     * @param config the <code>Config</code> object to write to
     */
    void saveModel(final Config config) {
        config.addString(ATTRIBUTE_NAME, getAttributeName());
        config.addString(MODEL_TYPE, getType());
        config.addBoolean(SKIP_MISSING_VALUES, m_skipMissingVals);
        config.addInt(NO_OF_MISSING_VALUES, m_noOfMissingVals);
        config.addString(INVALID_CAUSE, getInvalidCause());
        final Config internalConfig = config.addConfig(MODEL_DATA_SECTION);
        saveModelInternal(internalConfig);
    }

    /**
     * @param config the config object to save to
     */
    abstract void saveModelInternal(final Config config);

    /**
     * @return the unique type of the model
     */
    abstract String getType();

    /**
     * @return the attribute name
     */
    public String getAttributeName() {
        return m_attributeName;
    }

    /**
     * @return the <code>DataValue</code> class to check if the rows
     * are compatible
     */
    abstract Class<? extends DataValue> getCompatibleType();

    /**
     * Adds the given value to this attribute model.
     * @param classValue the class value
     * @param attrValue the attribute value. Could be a missing value.
     * @throws TooManyValuesException if the column contains more unique
     * values than supported by this attribute model
     */
    void addValue(final String classValue,
            final DataCell attrValue) throws TooManyValuesException {
        if (classValue == null) {
            throw new NullPointerException("ClassValue must not be null");
        }
        if (attrValue == null) {
            throw new NullPointerException("AttributeValue must not be null");
        }
        if (attrValue.isMissing()) {
            m_noOfMissingVals++;
            if (m_skipMissingVals) {
                return;
            }
        } else if (!attrValue.getType().isCompatible(getCompatibleType())) {
            throw new IllegalArgumentException(
                    "Attribute value type is not compatible");
        }
        addValueInternal(classValue, attrValue);
    }

    /**
     * Adds the given value to the concrete implementation. Should handle
     * missing values as well.
     * @param classValue the class value
     * @param attrValue the attribute value. Could be a missing value.
     * @throws TooManyValuesException if the column contains more unique
     * values than supported by this attribute model
     */
    abstract void addValueInternal(final String classValue,
            final DataCell attrValue) throws TooManyValuesException;

    /**
     * @return the noOfMissingVals
     */
    int getNoOfMissingVals() {
        return m_noOfMissingVals;
    }

    /**
     * @param colNames all column names of the table to check for uniqueness
     * @return the missing value header or <code>null</code> if this model
     * contains no missing attribute values
     */
    String getMissingValueHeader(final Collection<String>colNames) {
        if (!m_skipMissingVals && getNoOfMissingVals() > 0) {
            String missingHeading = MISSING_VALUE_NAME;
            int i = 1;
            while (colNames.contains(missingHeading)) {
                missingHeading = missingHeading + "(" + i++ + ")";
            }
            return missingHeading;
        }
        return null;
    }

    /**
     * @return all class values
     */
    abstract Collection<String> getClassValues();

    /**
     * @param classValue the class value we want the number of records for
     * @return the number of records with the given class value or
     * <code>null</code> if only missing values where in this row for the
     * given class value or the class value wasn't found at all
     */
    abstract Integer getNoOfRecs4ClassValue(final String classValue);

    /**
     * @param classValue the class value to calculate the probability for
     * @param attributeValue the attribute value to calculate the
     * probability for. Could be a missing value.
     * @param laplaceCorrector the Laplace corrector to use. A value greater 0
     * overcomes zero counts.
     * @return the calculated probability or null if the cell was a missing
     * one and missing values should be skipped
     */
    Double getProbability(final String classValue,
            final DataCell attributeValue, final double laplaceCorrector) {
        if (!attributeValue.getType().isCompatible(getCompatibleType())) {
            throw new IllegalArgumentException(
                    "Attribute value type is not compatible");
        }
        if (attributeValue.isMissing() && m_skipMissingVals) {
            return null;
        }
        return new Double(getProbabilityInternal(classValue, attributeValue,
                laplaceCorrector));
    }

    /**
     * This should also handle missing values.
     * @param classValue the class value to calculate the probability for
     * @param attributeValue the attribute value to calculate the
     * probability for. Could be a missing value.
     * @param laplaceCorrector the Laplace corrector to use. A value greater 0
     * overcomes zero counts.
     * @return the calculated probability
     */
    abstract double getProbabilityInternal(final String classValue,
            final DataCell attributeValue, double laplaceCorrector);

    /**
     * @param totalNoOfRecs the total number of records in the training data
     * @return the HTML view of this attribute model
     */
    abstract String getHTMLView(final int totalNoOfRecs);

    /**
     * @param vals the <code>Collection</code> to sort
     * @return the given <code>Collection</code> in her natural order
     */
    static List<String> sortCollection(final Collection<String> vals) {
        if (vals == null) {
            return null;
        }
        final List<String> sortedValues =
            new ArrayList<String>(vals.size());
        for (final String classVal : vals) {
            sortedValues.add(classVal);
        }
        Collections.sort(sortedValues);
        return sortedValues;
    }

    /**
     * Called after all training rows where added to validate the model.
     * @throws InvalidSettingsException if the model isn't valid
     */
    abstract void validate() throws InvalidSettingsException;

    /**
     * @param cause the cause why this model is invalid
     */
    void setInvalidCause(final String cause) {
        m_invalidCause = cause;
    }

    /**
     * @return if the model is invalid this method returns the reason why
     * otherwise it returns null.
     */
    public String getInvalidCause() {
        return m_invalidCause;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(final AttributeModel o) {
        return m_attributeName.compareTo(o.getAttributeName());
    }

    /**
     * @return the headline of this model to use in the HTML view
     */
    abstract String getHTMLViewHeadLine();

    /**
     * @param tableHeading the optional table headline
     * @param keyHeading the optional headline for the key row
     * @param valueHeading the optional headline for the value row
     * @param noOfRows the number of rows displayed per row
     * @param map the map to create the html table for
     * @param addLineBreak if each sub table should be displayed on a new line
     * @return a html table with the keys and values of the given map
     */
    static String createHTMLTable(final String tableHeading,
            final String keyHeading, final String valueHeading,
            final int noOfRows, final Map<String, ? extends Object> map,
            final boolean addLineBreak) {
        //create the partial maps
        final List<String> sortedClassValues = sortCollection(map.keySet());
        final List<String> keys = new ArrayList<String>(noOfRows);
        final List<Object> vals = new ArrayList<Object>(noOfRows);
        final StringBuilder buf = new StringBuilder();
        final int noOfVals = map.size();
        //copy the number of rows from the original map to the value map
        for (int i = 0; i < noOfVals; i++) {
            if (i % noOfRows == 0) {
                keys.clear();
                vals.clear();
            }
            final String classVal = sortedClassValues.get(i);
            keys.add(classVal);
            vals.add(map.get(classVal));
            if (i % noOfRows == noOfRows - 1 || i == noOfVals - 1) {
                buf.append(createPartialHTMLTable(tableHeading, keyHeading,
                        valueHeading, keys, vals));
                if (addLineBreak) {
                    buf.append("<br>");
                }
            }
        }
        return buf.toString();
    }

    private static String createPartialHTMLTable(final String tableHeading,
            final String keyHeading, final String valueHeading,
            final List<String> keys, final List<Object> vals) {
        final boolean rowHeading = (keyHeading != null || valueHeading != null);
        final int noOfVals = vals.size();
        final int tableHeadColspan;
        if (rowHeading) {
            tableHeadColspan = noOfVals + 1;
        } else {
            tableHeadColspan = noOfVals;
        }
        final StringBuilder buf = new StringBuilder();
        buf.append("<table border='1' width='100%'>");
        if (tableHeading != null) {
            buf.append("<tr>");
            buf.append("<th colspan='" + tableHeadColspan + "'>");
            buf.append(tableHeading);
            buf.append("</th>");
            buf.append("</tr>");
        }
        if (rowHeading || keys != null) {
            buf.append("<tr>");
            if (rowHeading) {
                buf.append("<th>");
                if (keyHeading != null) {
                    buf.append(keyHeading);
                } else {
                    buf.append("&nbsp;");
                }
                buf.append("</th>");
            }
            if (keys != null) {
                for (final String classVal : keys) {
                    buf.append("<th>");
                    buf.append(classVal);
                    buf.append("</th>");
                }
            }
            buf.append("</tr>");
        }
        buf.append("<tr>");
        if (rowHeading) {
            buf.append("<th>");
            if (valueHeading != null) {
                buf.append(valueHeading);
            } else {
                buf.append("&nbsp;");
            }
            buf.append("</th>");
        }
        for (final Object classVal : vals) {
            buf.append("<td align='center'>");
            buf.append(classVal.toString());
            buf.append("</td>");
        }
        buf.append("</tr>");
        buf.append("</table>");
        return buf.toString();
    }

    /**
     * @param firstHeading the optional first heading
     * @param headings the head lines
     * @param lastHeading the optional last heading
     * @return the string with a html table head line row
     */
    protected static String createTableHeader(final String firstHeading,
            final List<String> headings, final String lastHeading) {
        final StringBuilder buf = new StringBuilder();
        buf.append("<tr>");
        if (firstHeading != null) {
            buf.append("<th>");
            buf.append(firstHeading);
            buf.append("</th>");
        }
        //create the header
        for (final String attrVal : headings) {
            buf.append("<th>");
            buf.append(attrVal);
            buf.append("</th>");
        }
        if (lastHeading != null) {
            buf.append("<th>");
            buf.append(lastHeading);
            buf.append("</th>");
        }
        buf.append("</tr>");
        return buf.toString();
    }
}
