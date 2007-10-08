/* -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2007
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   01.05.2006 (koetter): created
 */
package org.knime.base.node.mine.bayes.naivebayes.datamodel;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.config.Config;

/**
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public class NaiveBayesModel {
    
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
        final int numColumns = tableSpec.getNumColumns();
        m_skippedAttributes = new ArrayList<AttributeModel>();
        //initialise the row values
        m_modelByAttrName = new HashMap<String, AttributeModel>(numColumns);
        for (int i = 0, length = tableSpec.getNumColumns(); i < length; i++) {
            final DataColumnSpec colSpec = tableSpec.getColumnSpec(i);
            final String colName = colSpec.getName();
            final DataType colType = colSpec.getType();
            final AttributeModel val;
            if (colName.equals(m_classColName)) {
                    val = new ClassAttributeModel(classColName, skipMissingVals,
                            maxNoOfNominalVals);
            } else if (colType.isCompatible(DoubleValue.class)) {
                val = new NumericalAttributeModel(colName, skipMissingVals);
            } else if (colType.isCompatible(NominalValue.class)) {
                val = new NominalAttributeModel(colName, skipMissingVals, 
                        maxNoOfNominalVals);
            } else {
                continue;
            }
            m_modelByAttrName.put(colName, val);
        }
        
        final int noOfRows = data.getRowCount();
        final double progressPerRow;
        if (noOfRows != 0) {
            progressPerRow = 1.0 / noOfRows;
        } else {
            progressPerRow = 1;
        }
        double progress = 0.0;
        if (exec != null) {
            exec.setProgress(progress, 
                "Building 'Naive Bayesian' model..");
        }
        //end of initialise all internal variable
        //start to proceed row by row
        for (DataRow row : data) {
            final DataCell classCell = row.getCell(classColIdx);
            if (classCell.isMissing()) {
                if (m_skipMissingVals) {
                    continue;
                }
                //check if the class value is missing
                throw new InvalidSettingsException(
                        "Missing class value found in row " + row.getKey()
                        + " to skip missing values tick the box in the dialog");
            }
            final String classVal = classCell.toString();
            for (int i = 0; i < numColumns; i++) {
                final AttributeModel model = 
                    m_modelByAttrName.get(tableSpec.getColumnSpec(i).getName());
                if (model != null) {
                    final DataCell cell = row.getCell(i);
                    try {
                        model.addValue(classVal, cell);
                    } catch (TooManyValuesException e) {
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
            if (exec != null) {
                exec.setProgress(progress);
                exec.checkCanceled();
            }
            progress += progressPerRow;
            m_noOfRecs++;
        }
        if (exec != null) {
            exec.setProgress(0.9, " validating learned model");
        }
        validateModels();
        if (exec != null) {
            exec.setProgress(1, "Model created");
        }
    }

    
    /**
     * 
     */
    private void validateModels() {
        final Collection<AttributeModel> mapModels = m_modelByAttrName.values();
        final Collection<AttributeModel> models = 
            new ArrayList<AttributeModel>(mapModels.size());
        models.addAll(mapModels);
        for (AttributeModel model : models) {
            try {
                model.validate();
            } catch (Exception e) {
                m_modelByAttrName.remove(model.getAttributeName());
                m_skippedAttributes.add(model);
            }
        }
    }


    /**Constructor for class NaiveBayesModel.
     * @param predParams the <code>ModelContentRO</code> to read from
     * @throws InvalidSettingsException if a mandatory key is not available
     */
    public NaiveBayesModel(final ModelContentRO predParams) 
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
    public void savePredictorParams(final ModelContentWO predParams) {
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
        for (AttributeModel model : m_skippedAttributes) {
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
        for (AttributeModel model : m_modelByAttrName.values()) {
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
     * @return the skipped attributes or an empty list
     */
    public List<AttributeModel> getSkippedAttributes() {
        return Collections.unmodifiableList(m_skippedAttributes);
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
     * @return a HTML representation of all attribute models
     */
    public String getHTMLView() {
    StringBuilder buf = new StringBuilder();
    //show the class model first
    final AttributeModel classModel = m_modelByAttrName.get(m_classColName);
    buf.append("<div>");
    buf.append("<h3>");
    buf.append(classModel.getHTMLViewHeadLine());
//    buf.append("\t|\t");
//    buf.append(model.getType());
    buf.append("</h3>");
    buf.append(classModel.getHTMLView());
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
//    buf.append("<h2 align='center'>Probabilities</h2>");
    //sort the attribute models by name
    final List<AttributeModel> models = new ArrayList<AttributeModel>();
    models.addAll(m_modelByAttrName.values());
    Collections.sort(models);
    for (AttributeModel model : models) {
        if (model.equals(classModel)) {
            continue;
        }
        buf.append("<div>");
        buf.append("<h3>");
        buf.append(model.getHTMLViewHeadLine());
//        buf.append("\t|\t");
//        buf.append(model.getType());
        buf.append("</h3>");
        buf.append(model.getHTMLView());
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
        for (AttributeModel model : m_modelByAttrName.values()) {
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
        for (String classValue : classValues) {
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
            for (String classValue : classValues) {
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
            Double probability = model.getProbability(classValue, cell);
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
        StringBuffer buf = new StringBuffer();
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
        for (AttributeModel model : attrModels) {
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
