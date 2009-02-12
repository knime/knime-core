/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 *
 * History
 *   01.05.2006 (koetter): created
 */
package org.knime.base.node.mine.bayes.naivebayes.datamodel;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.NominalValue;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.config.Config;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class represents the learned Naive Bayes model. This basic model
 * holds for each attribute an {@link AttributeModel}. Which provides the
 * probability information for each class value.
 * @author Tobias Koetter, University of Konstanz
 */
public class NaiveBayesModel {
    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(NaiveBayesModel.class);
    /*
    //Begin of XML tag names used to store and read the model file
     */
    private static final String RECORD_COUNTER = "RecordCounter";

    private static final String CLASS_COL_NAME = "ClassColumnName";

    private static final String SKIP_MISSING_VALS = "skipMissingVals";

    private static final String SKIPPED_ATTRIBUTE_SECTION =
        "SkippedAttributes";
    private static final String SKIPPED_ATTRIBUTE_COUNTER =
        "SkippedAttributesCounter";
    private static final String SKIPPED_ATTRIBUTE_DATA =
        "SkippedAttributeData_";

    private static final String ATTRIBUTE_MODEL_SECTION =
        "AttributeModelSection";
    private static final String ATTRIBUTE_MODEL_COUNTER =
        "AttributeModelCounter";
    private static final String ATTRIBUTE_MODEL_DATA =
        "AttributeModelData_";
    /*
    //End of XML tag names used to store and read the model file
     */
    /**
     * The <code>NumberFormater</code> to use in the html views.
     */
    public static final NumberFormat HTML_VALUE_FORMATER =
        new DecimalFormat("#.#####");

    /**
     * Holds the number of total records of the training data.
     */
    private int m_noOfRecs;

    /**
     * Holds the column name of class column.
     */
    private final String m_classColName;

    private final boolean m_skipMissingVals;

    private final Map<String, AttributeModel> m_modelByAttrName;

    private final List<AttributeModel> m_skippedAttributes;

    private String m_htmlView = null;

    /**Constructor which iterates through the <code>DataTable</code> to
     * calculate the needed Bayes variables.
     *
     * @param data The <code>BufferedDataTable</code> with the data
     * @param classColName The name of the column with the class
     * @param exec the <code>ExecutionContext</code> to provide progress
     * information and check for cancel
     * @param maxNoOfNominalVals the maximum number of supported unique
     * nominal attribute values
     * @param skipMissingVals set to <code>true</code> if the missing values
     * should be skipped during learning and prediction
     * @throws CanceledExecutionException if the user presses the cancel
     * button during model creation
     * @throws InvalidSettingsException if the input data contains no rows
     */
    public NaiveBayesModel(final BufferedDataTable data,
            final String classColName, final ExecutionContext exec,
            final int maxNoOfNominalVals, final boolean skipMissingVals)
        throws CanceledExecutionException, InvalidSettingsException {
        if (data == null) {
            throw new NullPointerException("Training table must not be null.");
        }
        if (data.getRowCount() < 1) {
            throw new InvalidSettingsException("Input data contains no rows");
        }
        if (classColName == null) {
            throw new NullPointerException("Class column must not be null.");
        }
        if (maxNoOfNominalVals < 0) {
            throw new IllegalArgumentException("The maximum number of unique "
                    + "nominal values must be greater zero");
        }
        final DataTableSpec tableSpec = data.getDataTableSpec();
        final int classColIdx = tableSpec.findColumnIndex(classColName);
        if (classColIdx < 0) {
            throw new IllegalArgumentException(
                    "Class column not found in table specification");
        }
        //initialise all internal variable
        m_classColName = classColName;
        m_skipMissingVals = skipMissingVals;
        m_skippedAttributes = new ArrayList<AttributeModel>();
        //initialise the row values
        m_modelByAttrName = createModelMap(tableSpec, classColName,
                maxNoOfNominalVals, skipMissingVals);
        //end of initialise all internal variable
        ExecutionMonitor subExec = null;
        if (exec != null) {
            exec.setMessage("Building \'Naive Bayesian\' model..");
            subExec = exec.createSubProgress(0.9);
        }
        createModel(data, subExec, classColIdx);
        if (exec != null) {
            exec.setMessage("Validating model...");
        }
        subExec = null;
        if (exec != null) {
            subExec = exec.createSubProgress(0.1);
        }
        validateModel(subExec);
        if (exec != null) {
            exec.setProgress(1, "Model validated");
        }
    }

    private static Map<String, AttributeModel> createModelMap(
            final DataTableSpec tableSpec, final String classColName,
            final int maxNoOfNominalVals, final boolean skipMissingVals) {
        final int numColumns = tableSpec.getNumColumns();
        final Map<String, AttributeModel> modelMap =
            new HashMap<String, AttributeModel>(numColumns);
        for (int i = 0; i < numColumns; i++) {
            final DataColumnSpec colSpec = tableSpec.getColumnSpec(i);
            final String colName = colSpec.getName();
            final DataType colType = colSpec.getType();
            final AttributeModel model;
            if (colName.equals(classColName)) {
                    model = new ClassAttributeModel(colName,
                            skipMissingVals, maxNoOfNominalVals);
            } else if (colType.isCompatible(DoubleValue.class)) {
                model = new NumericalAttributeModel(colName, skipMissingVals);
            } else if (colType.isCompatible(NominalValue.class)) {
                model = new NominalAttributeModel(colName, skipMissingVals,
                        maxNoOfNominalVals);
            } else {
                continue;
            }
            modelMap.put(colName, model);
        }
        return modelMap;
    }

    private void createModel(final BufferedDataTable data,
            final ExecutionMonitor exec, final int classColIdx)
            throws InvalidSettingsException, CanceledExecutionException {
        final DataTableSpec tableSpec = data.getDataTableSpec();
        final int noOfRows = data.getRowCount();
        final double progressPerRow;
        if (noOfRows != 0) {
            progressPerRow = 1.0 / noOfRows;
        } else {
            progressPerRow = 1;
        }
        double progress = 0.0;
        //start to proceed row by row
        for (final DataRow row : data) {
            updateModel(row, tableSpec, classColIdx);
            if (exec != null) {
                exec.setProgress(progress);
                exec.checkCanceled();
            }
            progress += progressPerRow;
        }
        if (exec != null) {
            exec.setProgress(1.0, "\'Naive Bayesian\' created ");
        }
    }

    /**
     * Updates the current {@link NaiveBayesModel} with the values from the
     * given {@link DataRow}.
     * @param row DataRow with values for update
     * @param tableSpec underlying DataTableSpec
     * @param classColIdx the index of the class column
     * @throws InvalidSettingsException if missing values occur in class column
     * or an attribute has too many values.
     */
    public void updateModel(final DataRow row, final DataTableSpec tableSpec,
            final int classColIdx) throws InvalidSettingsException {
        if (row == null) {
            throw new NullPointerException("Row must not be null");
        }
        if (tableSpec == null) {
            throw new NullPointerException("TableSpec must not be null");
        }
        final DataCell classCell = row.getCell(classColIdx);
        if (classCell.isMissing()) {
            if (m_skipMissingVals) {
                return;
            }
            //check if the class value is missing
            throw new InvalidSettingsException(
                    "Missing class value found in row " + row.getKey()
                    + " to skip missing values tick the box in the dialog");
        }
        final String classVal = classCell.toString();
        final int numColumns = tableSpec.getNumColumns();
        for (int i = 0; i < numColumns; i++) {
            final AttributeModel model =
                m_modelByAttrName.get(tableSpec.getColumnSpec(i).getName());
            if (model != null) {
                final DataCell cell = row.getCell(i);
                try {
                    model.addValue(classVal, cell);
                } catch (final TooManyValuesException e) {
                    if (model instanceof ClassAttributeModel) {
                        throw new InvalidSettingsException(
                            "Class attribute has too many unique values. "
                                + "To avoid this exception increase the "
                                + "maximum number of allowed nominal "
                                + "values in the node dialog");
                    }
                    //delete the model if it contains to many unique values
                    m_modelByAttrName.remove(model.getAttributeName());
                    model.setInvalidCause("To many values");
                    m_skippedAttributes.add(model);
                }
            }
        }

        m_noOfRecs++;
    }


    private void validateModel(final ExecutionMonitor exec)
    throws CanceledExecutionException {
        final Collection<AttributeModel> mapModels = m_modelByAttrName.values();
        final int noOfModels = mapModels.size();
        final Collection<AttributeModel> models =
            new ArrayList<AttributeModel>(noOfModels);
        models.addAll(mapModels);
        final double progressPerRow;
        if (noOfModels != 0) {
            progressPerRow = 1.0 / noOfModels;
        } else {
            progressPerRow = 1;
        }
        int modelCounter = 0;
        for (final AttributeModel model : models) {
            try {
                model.validate();
            } catch (final Exception e) {
                m_modelByAttrName.remove(model.getAttributeName());
                m_skippedAttributes.add(model);
            }
            modelCounter++;
            if (exec != null) {
                exec.setProgress(progressPerRow * modelCounter);
                exec.checkCanceled();
            }
        }
    }


    /**Constructor for class NaiveBayesModel.
     * @param predParams the <code>ModelContentRO</code> to read from
     * @throws InvalidSettingsException if a mandatory key is not available
     */
    public NaiveBayesModel(final ConfigRO predParams)
    throws InvalidSettingsException {
        if (predParams == null) {
            throw new NullPointerException("PredParams must not be null");
        }
        m_classColName = predParams.getString(CLASS_COL_NAME);
        m_skipMissingVals = predParams.getBoolean(SKIP_MISSING_VALS);
        //load the skipped models
        final Config skippedConfig =
            predParams.getConfig(SKIPPED_ATTRIBUTE_SECTION);
        final int noOfSkipped = skippedConfig.getInt(SKIPPED_ATTRIBUTE_COUNTER);
        m_skippedAttributes = new ArrayList<AttributeModel>(noOfSkipped);
        for (int i = 0; i < noOfSkipped; i++) {
            final Config modelConfig =
                skippedConfig.getConfig(SKIPPED_ATTRIBUTE_DATA + i);
            final AttributeModel model = AttributeModel.loadModel(modelConfig);
            m_skippedAttributes.add(model);
        }
        //load the valid models
        final Config modelConfigSect =
            predParams.getConfig(ATTRIBUTE_MODEL_SECTION);
        m_noOfRecs = modelConfigSect.getInt(RECORD_COUNTER);
        final int noOfAttrs = modelConfigSect.getInt(ATTRIBUTE_MODEL_COUNTER);
        m_modelByAttrName = new HashMap<String, AttributeModel>(noOfAttrs);
        for (int i = 0; i < noOfAttrs; i++) {
            final Config modelConfig =
                modelConfigSect.getConfig(ATTRIBUTE_MODEL_DATA + i);
            final AttributeModel model = AttributeModel.loadModel(modelConfig);
            m_modelByAttrName.put(model.getAttributeName(), model);
        }
    }

    /**
     * @param predParams to save the model
     */
    public void savePredictorParams(final ConfigWO predParams) {
        //Save the classifier column
        predParams.addString(CLASS_COL_NAME, m_classColName);
        predParams.addBoolean(SKIP_MISSING_VALS, m_skipMissingVals);
        //create the skipped attributes section
        final Config skippedConfig =
            predParams.addConfig(SKIPPED_ATTRIBUTE_SECTION);
        //save the number of skipped attribute models
        skippedConfig.addInt(SKIPPED_ATTRIBUTE_COUNTER,
                m_skippedAttributes.size());
        //save the skipped attribute models
        int modelIndex = 0;
        for (final AttributeModel model : m_skippedAttributes) {
            final Config modelConfig =
                skippedConfig.addConfig(SKIPPED_ATTRIBUTE_DATA + modelIndex++);
            model.saveModel(modelConfig);
        }
        //create the model config section
        final Config modelConfigSect =
            predParams.addConfig(ATTRIBUTE_MODEL_SECTION);
        //Save the total number of records
        modelConfigSect.addInt(RECORD_COUNTER, m_noOfRecs);
        //save the number of attribute models
        modelConfigSect.addInt(ATTRIBUTE_MODEL_COUNTER,
                m_modelByAttrName.size());
        //save the attribute models
        modelIndex = 0;
        for (final AttributeModel model : m_modelByAttrName.values()) {
            final Config modelConfig =
                modelConfigSect.addConfig(
                        ATTRIBUTE_MODEL_DATA + modelIndex++);
            model.saveModel(modelConfig);
        }
    }

    /**
     * @return a <code>String</code> <code>Enumeration</code> with all class
     * values
     */
    private Collection<String> getClassValues() {
        final AttributeModel classModel = m_modelByAttrName.get(m_classColName);
        if (classModel != null) {
            return classModel.getClassValues();
        }
        throw new IllegalStateException("Class column model not found");
    }

    /**
     * @return <code>true</code> if the model contains skipped attributes
     */
    public boolean containsSkippedAttributes() {
        return m_skippedAttributes != null && !m_skippedAttributes.isEmpty();
    }

    /**
     * @return the skipped attributes or an empty list
     */
    public List<AttributeModel> getSkippedAttributes() {
        return Collections.unmodifiableList(m_skippedAttributes);
    }

    /**
     * @param max2Show the maximum number of missing attributes to display
     * @return a String that shows the skipped attributes
     */
    public String getSkippedAttributesString(final int max2Show) {
        final StringBuilder buf = new StringBuilder();
        buf.append("The following attributes are skipped: ");
        for (int i = 0, length = m_skippedAttributes.size(); i < length; i++) {
            if (i != 0) {
                buf.append(", ");
            }
            if (i > max2Show) {
                buf.append("...(see node view)");
                break;
            }
            final AttributeModel model = m_skippedAttributes.get(i);
            buf.append(model.getAttributeName());
            buf.append("/");
            buf.append(model.getInvalidCause());
        }
        return buf.toString();
    }

    /**
     * @return all class values in natural order
     */
    public List<String> getSortedClassValues() {
        final AttributeModel classModel = m_modelByAttrName.get(m_classColName);
        if (classModel != null) {
            final Collection<String> classValues = classModel.getClassValues();
            final List<String> sortedClassVals =
                new ArrayList<String>(classValues.size());
            sortedClassVals.addAll(classValues);
            Collections.sort(sortedClassVals);
            return sortedClassVals;
        }
        throw new IllegalStateException("No model found for class column"
                + m_classColName);
    }

    /**
     * @param classValue the value of the class we want the probability for
     * @return the probability for the given class
     */
    public double getClassPriorProbability(final String classValue) {
        if (classValue == null) {
            throw new NullPointerException("ClassValue must not be null");
        }

        final AttributeModel classModel = m_modelByAttrName.get(m_classColName);
        if (classModel == null) {
            throw new IllegalArgumentException("Class model not found");
        }
        final Integer noOfRecs4Class =
            classModel.getNoOfRecs4ClassValue(classValue);
        if (noOfRecs4Class == null) {
            throw new IllegalArgumentException("Class value: " + classValue
                + " not found");
        }
        if (m_noOfRecs == 0) {
            throw new IllegalArgumentException("Model contains no records");
        }
        return (double) noOfRecs4Class.intValue() / m_noOfRecs;
    }

    /**
     * @param attributeNames the name of the attributes we want the normalized
     * probability values for
     * @param row the row with the values in the same order like the
     * attribute names
     * @param classValues the class values to calculate the probability for
     * @param normalize set to <code>true</code> if the probability values
     * should be normalized
     * @return the normalized probability values in the same order like the
     * class values
     */
    public double[] getClassPobabilites(
            final String[] attributeNames, final DataRow row,
            final List<String> classValues, final boolean normalize) {
        if (row == null) {
            throw new NullPointerException("Row must not be null");
        }
        if (classValues == null || classValues.size() < 1) {
            throw new IllegalArgumentException(
                    "Class value list must not be empty");
        }
        if (attributeNames == null) {
            throw new NullPointerException("ColumSpec must not be null");
        }
        if (attributeNames.length != row.getNumCells()) {
            throw new IllegalArgumentException("Attribute names array and "
                    + "data row must be the same size");
        }
        final double[] probs = new double[classValues.size()];
        double sum = 0;
        for (int i = 0, length = classValues.size(); i < length; i++) {
            probs[i] = getClassProbability(attributeNames, row,
                    classValues.get(i));
            sum += probs[i];
        }
        if (sum == 0) {
            //all classes have a combined probability of zero -> use only
            //the prior probability
            for (int i = 0, length = classValues.size(); i < length; i++) {
                probs[i] = getClassPriorProbability(classValues.get(i));
            }
        } else if (normalize) {
            for (int i = 0, length = probs.length; i < length; i++) {
                probs[i] = probs[i] / sum;
            }
        }
        return probs;
    }


    /**
     * @return the total number of training records
     */
    public int getNoOfRecs() {
        return m_noOfRecs;
    }

    /**
     * @return the name of the column with the class attribute.
     */
    public String getClassColumnName() {
        return m_classColName;
    }

    /**
     * @return the <code>DataType</code> of the column with the class attribute.
     */
    public DataType getClassColumnDataType() {
        return StringCell.TYPE;
    }

    /**
     * @return the a summary of the model
     */
    public String getSummary() {
        final StringBuilder buf = new StringBuilder();
        buf.append("NaiveBayes Model. Class column: ");
        buf.append(getClassColumnName());
        buf.append(" Number of attributes: ");
        //minus 1 because of the class model
        buf.append(getAttributeModels().size() - 1);
        if (containsSkippedAttributes()) {
            buf.append(" ");
            buf.append(getSkippedAttributesString(3));
        }
        return buf.toString();
    }

    /**
     * @return a HTML representation of all attribute models
     */
    public String getHTMLView() {
        if (m_htmlView == null) {
            //cache the html view since the creation takes some time
            final long startTime = System.currentTimeMillis();
            m_htmlView = createHTMLView();
            final long endTime = System.currentTimeMillis();
            final long durationTime = endTime - startTime;
            LOGGER.debug("Time to create html view: " + durationTime + " ms");
        }
        return m_htmlView;
    }

    private String createHTMLView() {
        final StringBuilder buf = new StringBuilder();
        //show the class model first
        final AttributeModel classModel = m_modelByAttrName.get(m_classColName);
        buf.append("<div>");
        buf.append("<h3>");
        buf.append(classModel.getHTMLViewHeadLine());
//        buf.append("\t|\t");
//        buf.append(model.getType());
        buf.append("</h3>");
        buf.append(classModel.getHTMLView(getNoOfRecs()));
        buf.append("<hr>");
        buf.append("</div>");
        final List<AttributeModel> skippedAttrs = getSkippedAttributes();
        if (skippedAttrs.size() > 0) {
            buf.append("<div>");
            buf.append("<b>Skipped attributes:</b> ");
            for (int i = 0, length = skippedAttrs.size(); i < length; i++) {
                if (i != 0) {
                    buf.append(", ");
                }
                final AttributeModel model = skippedAttrs.get(i);
                buf.append(model.getAttributeName());
                buf.append("/");
                buf.append(model.getInvalidCause());
            }
            buf.append("<hr>");
            buf.append("</div>");
        }
        final List<String> missingValAttrs = getAttributesWithMissingVals();
        if (missingValAttrs.size() > 0) {
            buf.append("<div>");
            buf.append("<b>Attributes with at least one missing value:</b> ");
            for (int i = 0, length = missingValAttrs.size(); i < length; i++) {
                if (i != 0) {
                    buf.append(", ");
                }
                buf.append(missingValAttrs.get(i));
            }
            buf.append("<hr>");
            buf.append("</div>");
        }
        if (m_skipMissingVals) {
            buf.append("<div>");
            buf.append("Rows with at least one missing value will be skipped "
                    + "during learning and prediction phase");
            buf.append("<hr>");
            buf.append("</div>");
        }
//        buf.append("<h2 align='center'>Probabilities</h2>");
        //sort the attribute models by name
        final List<AttributeModel> models = new ArrayList<AttributeModel>();
        models.addAll(m_modelByAttrName.values());
        Collections.sort(models);
        for (final AttributeModel model : models) {
            if (model.equals(classModel)) {
                continue;
            }
            buf.append("<div>");
            buf.append("<h3>");
            buf.append(model.getHTMLViewHeadLine());
//            buf.append("\t|\t");
//            buf.append(model.getType());
            buf.append("</h3>");
            buf.append(model.getHTMLView(getNoOfRecs()));
            buf.append("<br><hr>");
            buf.append("</div>");
        }
        return buf.toString();
    }

    /**
     * @return the name of all attributes which has at least one missing value
     * during learning or an empty list
     */
    public List<String> getAttributesWithMissingVals() {
        final List<String> missingModels =
            new ArrayList<String>();
        for (final AttributeModel model : m_modelByAttrName.values()) {
            if (model.getNoOfMissingVals() > 0) {
                missingModels.add(model.getAttributeName());
            }
        }
        return missingModels;
    }


    /**
     * @param attributeName the name of the attribute
     * @return the model for the given attribute name or <code>null</code>
     * if the attribute is not known
     */
    public AttributeModel getAttributeModel(final String attributeName) {
        if (attributeName == null) {
            throw new NullPointerException("AttributeName must not be null");
        }
        return m_modelByAttrName.get(attributeName);
    }

    /**
     * @return an unmodifiable <code>Collection</code> with all
     * <code>AttributeModel</code> objects
     */
    public Collection<AttributeModel> getAttributeModels() {
        return Collections.unmodifiableCollection(m_modelByAttrName.values());
    }

    /**
     * Returns the name of the class with the highest probability for the
     * given row.
     * @param attrNames the attribute names in the same order
     * they appear in the given data row
     * @param row the row with the attributes in the same order like in the
     * training data table
     * @return the class attribute with the highest probability for the given
     * attribute values.
     */
    public String getMostLikelyClass(final String[] attrNames,
            final DataRow row) {
        if (row == null) {
            throw new NullPointerException("Row must not be null");
        }
        if (attrNames == null) {
            throw new NullPointerException("ColumSpec must not be null");
        }
        if (attrNames.length != row.getNumCells()) {
            throw new IllegalArgumentException("Attribute names array and "
                    + "data row must be the same size");
        }
        double maxProbability = -1;
        String mostLikelyClass = null;
        final Collection<String> classValues = getClassValues();
        for (final String classValue : classValues) {
            final double classProbability =
                getClassProbability(attrNames, row, classValue);
            if (classProbability >= maxProbability) {
                maxProbability = classProbability;
                mostLikelyClass = classValue;
            }
        }
        if (maxProbability == 0) {
            //all classes have a combined probability of zero for this row ->
            //use only the prior probability
            for (final String classValue : classValues) {
                final double classPriorProbability =
                    getClassPriorProbability(classValue);
                if (classPriorProbability >= maxProbability) {
                    maxProbability = classPriorProbability;
                    mostLikelyClass = classValue;
                }
            }
        }
        if (mostLikelyClass == null) {
            throw new IllegalStateException(
                    "Most likely class must not be null");
        }
        return mostLikelyClass;
    }


    /**
     * Returns the probability of the row to be member of the given class.
     * All not known attributes are skipped. If none of the given attributes
     * is known the method returns the class prior probability.
     * @param attrNames the name of the attributes
     * @param row the row with the value per attribute in the same order
     * like the attribute names
     * @param classValue the class value to compute the probability for
     * @return the probability of this row to belong to the given class value
     */
    private double getClassProbability(final String[] attrNames,
            final DataRow row, final String classValue) {
        double combinedProbability = getClassPriorProbability(classValue);
        for (int i = 0, length = row.getNumCells(); i < length; i++) {
            final String attrName = attrNames[i];
            final AttributeModel model = m_modelByAttrName.get(attrName);
            if (model == null) {
                //skip unknown attributes
                continue;
            }
            if (model instanceof ClassAttributeModel) {
                //skip the class value column
                continue;
            }
            final DataCell cell = row.getCell(i);
            final Double probability = model.getProbability(classValue, cell);
            if (probability != null) {
                combinedProbability *= probability.doubleValue();
            }
        }
        return combinedProbability;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        final StringBuffer buf = new StringBuffer();
        buf.append("Number of records: ");
        buf.append(m_noOfRecs);
        buf.append("\nNumber of columns: ");
        buf.append(m_modelByAttrName.size());
        buf.append("\nClass column: ");
        buf.append(m_classColName);
        buf.append("\nDistinct class values: ");
        buf.append(getClassValues().size());
        buf.append("\n");
        return buf.toString();
    }


    /**
     * Checks if the given table specification contains columns which are not
     * covered by the learned model. Either because the name is not known
     * or the type is wrong.
     * @param tableSpec the <code>DataTableSpec</code> to check for unknown
     * columns
     * @return the name of the unknown columns or an empty <code>List</code>
     */
    public List<String> check4UnknownCols(final DataTableSpec tableSpec) {
        if (tableSpec == null) {
            throw new NullPointerException("TableSpec must not be null");
        }
        final List<String> unknownCols = new ArrayList<String>();
        for (int i = 0, length = tableSpec.getNumColumns();
                i < length; i++) {
            final DataColumnSpec colSpec = tableSpec.getColumnSpec(i);
            final AttributeModel attrModel =
                getAttributeModel(colSpec.getName());
            if (attrModel == null || !colSpec.getType().isCompatible(
                    attrModel.getCompatibleType())) {
                unknownCols.add(colSpec.getName());
            }
        }
        return unknownCols;
    }


    /**
     * Checks if the model contains attributes which are not present in the
     * given table specification which could influence the prediction result.
     * @param tableSpec the <code>DataTableSpec</code> to check for missing
     * columns
     * @return the name of the missing columns or an empty <code>List</code>
     */
    public List<String> check4MissingCols(final DataTableSpec tableSpec) {
        final List<String> missingInputCols = new ArrayList<String>();
        final Collection<AttributeModel> attrModels = getAttributeModels();
        for (final AttributeModel model : attrModels) {
            if (!model.getType().equals(ClassAttributeModel.MODEL_TYPE)) {
                //check only for none class value columns
                if (tableSpec.getColumnSpec(model.getAttributeName()) == null) {
                    missingInputCols.add(model.getAttributeName());
                }
            }
        }
        return missingInputCols;
    }
}
