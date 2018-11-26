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
package org.knime.base.node.mine.bayes.naivebayes.datamodel3;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.dmg.pmml.BayesInputDocument.BayesInput;
import org.dmg.pmml.BayesInputsDocument.BayesInputs;
import org.dmg.pmml.BayesOutputDocument.BayesOutput;
import org.dmg.pmml.DATATYPE.Enum;
import org.dmg.pmml.DataDictionaryDocument.DataDictionary;
import org.dmg.pmml.DataFieldDocument.DataField;
import org.dmg.pmml.MININGFUNCTION;
import org.dmg.pmml.NaiveBayesModelDocument;
import org.dmg.pmml.PMMLDocument.PMML;
import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.NominalValue;
import org.knime.core.data.def.BooleanCell.BooleanCellFactory;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.DoubleCell.DoubleCellFactory;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.IntCell.IntCellFactory;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.def.StringCell.StringCellFactory;
import org.knime.core.data.vector.bitvector.BitVectorValue;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.config.Config;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;
import org.knime.core.node.port.pmml.PMMLDataDictionaryTranslator;
import org.knime.core.node.port.pmml.preproc.DerivedFieldMapper;

/**
 * This class represents the learned Naive Bayes model. This basic model holds for each attribute an
 * {@link AttributeModel}. Which provides the probability information for each class value.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class NaiveBayesModel {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(NaiveBayesModel.class);

    /*
    //Begin of XML tag names used to store and read the model file
     */
    private static final String BIT_VECTOR_MODEL_COUNT = "BitVectorModelCount";

    private static final String BIT_VECTOR_MODEL_PREFIX = "BitVectorModel_";

    private static final String CLASS_COL_NAME = "ClassColumnName";

    private static final String CLASS_COL_TYPE = "ClassColumnType";

    private static final String THRESHOLD = "Threshold";

    private static final String IGNORE_MISSING_VALS = "skipMissingVals";

    private static final String SKIPPED_ATTRIBUTE_SECTION = "SkippedAttributes";

    private static final String SKIPPED_ATTRIBUTE_COUNTER = "SkippedAttributesCounter";

    private static final String SKIPPED_ATTRIBUTE_DATA = "SkippedAttributeData_";

    private static final String ATTRIBUTE_MODEL_SECTION = "AttributeModelSection";

    private static final String ATTRIBUTE_MODEL_COUNTER = "AttributeModelCounter";

    private static final String ATTRIBUTE_MODEL_DATA = "AttributeModelData_";

    /*
    //End of XML tag names used to store and read the model file
     */
    /**
     * The <code>NumberFormater</code> to use in the html views.
     */
    public static final NumberFormat HTML_VALUE_FORMATER = new DecimalFormat("#.#####");

    /**
     * Holds the column name of class column.
     */
    private final String m_classColName;

    private final DataType m_classColType;

    private final boolean m_ignoreMissingVals;

    private final LinkedHashMap<String, AttributeModel> m_modelByAttrName;

    private final List<AttributeModel> m_skippedAttributes;

    private String m_htmlView = null;

    private final Double m_pmmlZeroProbThreshold;

    private BufferedDataTable m_statisticsTable;

    /**
     * Constructor which iterates through the <code>DataTable</code> to calculate the needed Bayes variables.
     *
     * @param data The <code>BufferedDataTable</code> with the data
     * @param classColName The name of the column with the class
     * @param exec the <code>ExecutionContext</code> to provide progress information and check for cancel
     * @param maxNoOfNominalVals the maximum number of supported unique nominal attribute values
     * @param ignoreMissingVals set to <code>true</code> if the missing values should be ignored during learning and
     *            prediction
     * @param pmmlCompatible flag that indicates that a PMML compatible model should be learned
     * @param probabilityThreshold the probability to use in lieu of P(Ij | Tk) when count[IjTi] is zero for categorical
     *            fields or when the calculated probability of the distribution falls below the threshold for continuous
     *            fields.
     * @throws CanceledExecutionException if the user presses the cancel button during model creation
     * @throws InvalidSettingsException if the input data contains no rows
     */
    public NaiveBayesModel(final BufferedDataTable data, final String classColName, final ExecutionContext exec,
        final int maxNoOfNominalVals, final boolean ignoreMissingVals, final boolean pmmlCompatible,
        final double probabilityThreshold) throws CanceledExecutionException, InvalidSettingsException {
        if (exec == null) {
            throw new IllegalArgumentException("exec must not be null");
        }
        if (probabilityThreshold < 0) {
            throw new IllegalArgumentException("Probability threshold should be positive");
        }
        if (data == null) {
            throw new NullPointerException("Training table must not be null.");
        }
        if (data.size() < 1) {
            throw new InvalidSettingsException("Input data contains no rows");
        }
        if (classColName == null) {
            throw new NullPointerException("Class column must not be null.");
        }
        if (maxNoOfNominalVals < 0) {
            throw new IllegalArgumentException("The maximum number of unique nominal values must be greater zero");
        }
        final DataTableSpec tableSpec = data.getDataTableSpec();
        final int classColIdx = tableSpec.findColumnIndex(classColName);
        if (classColIdx < 0) {
            throw new IllegalArgumentException("Class column not found in table specification");
        }
        final DataColumnSpec classColSpec = tableSpec.getColumnSpec(classColIdx);
        //initialise all internal variable
        m_classColName = classColSpec.getName();
        m_classColType = classColSpec.getType();
        m_ignoreMissingVals = ignoreMissingVals;
        m_skippedAttributes = new ArrayList<>();
        //initialise the row values
        m_modelByAttrName =
            createModelMap(tableSpec, m_classColName, maxNoOfNominalVals, ignoreMissingVals, pmmlCompatible);
        m_pmmlZeroProbThreshold = probabilityThreshold;
        //end of initialise all internal variable
        ExecutionMonitor subExec = null;
        exec.setMessage("Building model");
        subExec = exec.createSubProgress(0.8);
        createModel(data, subExec, classColIdx);
        exec.setMessage("Model created");
        exec.checkCanceled();
        exec.setMessage("Validating model");
        subExec = exec.createSubProgress(0.1);
        validateModel(subExec);
        exec.checkCanceled();
        subExec.setProgress(1, "Model validated");
        exec.setMessage("Creating data tables");
        subExec = exec.createSubProgress(0.1);
        final BufferedDataContainer nodc =
            exec.createDataContainer(createStatisticsTableSpec(getClassColumnDataType(), m_ignoreMissingVals));
        final List<AttributeModel> models = new ArrayList<>();
        models.addAll(m_modelByAttrName.values());
        Collections.sort(models);
        int counter = 1;
        final AtomicInteger rowId = new AtomicInteger(0);
        for (final AttributeModel model : models) {
            subExec.setProgress(counter / (double)m_modelByAttrName.size(),
                "Processing model " + counter + " of " + m_modelByAttrName.size());
            exec.checkCanceled();
            final ExecutionMonitor subSubExec = subExec.createSubProgress(1.0 / m_modelByAttrName.size());
            model.createDataRows(subSubExec, nodc, m_ignoreMissingVals, rowId);
        }
        nodc.close();
        m_statisticsTable = nodc.getTable();
        subExec.setProgress(1, "Statistics tables created");

    }

    /**
     * @param classColDataType the {@link DataType} of the class column
     * @param ignoreMissingVals <code>true</code> if missing value should be ignored
     * @return the {@link DataTableSpec} of the numerical statistics table
     */
    public static DataTableSpec createStatisticsTableSpec(final DataType classColDataType,
        final boolean ignoreMissingVals) {
        final List<DataColumnSpec> specs = new LinkedList<>();
        final DataColumnSpecCreator creator = new DataColumnSpecCreator("Attribute", classColDataType);
        specs.add(creator.createSpec());
        creator.setName("Value");
        specs.add(creator.createSpec());
        creator.setName("Class");
        specs.add(creator.createSpec());
        creator.setName("Count");
        creator.setType(IntCell.TYPE);
        specs.add(creator.createSpec());
        if (!ignoreMissingVals) {
            creator.setName("Missing value count");
            specs.add(creator.createSpec());
        }
        creator.setName("Mean");
        creator.setType(DoubleCell.TYPE);
        specs.add(creator.createSpec());
        creator.setName("Standard deviation");
        specs.add(creator.createSpec());
        return new DataTableSpec(specs.toArray(new DataColumnSpec[0]));
    }

    /**
     * @return the statistics table
     */
    public BufferedDataTable getStatisticsTable() {
        return m_statisticsTable;
    }

    private static LinkedHashMap<String, AttributeModel> createModelMap(final DataTableSpec tableSpec,
        final String classColName, final int maxNoOfNominalVals, final boolean skipMissingVals,
        final boolean pmmlCompatible) {
        final int numColumns = tableSpec.getNumColumns();
        final LinkedHashMap<String, AttributeModel> modelMap = new LinkedHashMap<>(numColumns);
        for (int i = 0; i < numColumns; i++) {
            final DataColumnSpec colSpec = tableSpec.getColumnSpec(i);
            final AttributeModel model =
                getCompatibleModel(colSpec, classColName, maxNoOfNominalVals, skipMissingVals, pmmlCompatible);
            if (model != null) {
                modelMap.put(colSpec.getName(), model);
            }
        }
        return modelMap;
    }

    /**
     * Returns the compatible {@link AttributeModel} or <code>null</code> if the data type is not supported.
     *
     * @param colSpec {@link DataColumnSpec}
     * @param classColName name of the class column
     * @param maxNoOfNominalVals maximum number of nominal values
     * @param ignoreMissingVals flag that indicates if missing values should be ignored
     * @param pmmlCompatible flag that indicates if the model should be PMML compliant
     * @return the corresponding {@link AttributeModel} or <code>null</code> if the data type of the given column is not
     *         supported
     * @since 2.10
     */
    public static AttributeModel getCompatibleModel(final DataColumnSpec colSpec, final String classColName,
        final int maxNoOfNominalVals, final boolean ignoreMissingVals, final boolean pmmlCompatible) {
        final String colName = colSpec.getName();
        final DataType colType = colSpec.getType();
        if (colName.equals(classColName)) {
            return new ClassAttributeModel(colName, ignoreMissingVals, maxNoOfNominalVals);
        }
        if (colType.isCompatible(DoubleValue.class)) {
            return new NumericalAttributeModel(colName, ignoreMissingVals);
        }
        if (colType.isCompatible(NominalValue.class)) {
            return new NominalAttributeModel(colName, ignoreMissingVals, maxNoOfNominalVals);
        }
        if (!pmmlCompatible && colType.isCompatible(BitVectorValue.class)) {
            //ignore bit vector columns in pmml compatibility mode
            return new BitVectorAttributeModel(colName, ignoreMissingVals);
        }
        return null;
    }

    private void createModel(final BufferedDataTable data, final ExecutionMonitor exec, final int classColIdx)
        throws InvalidSettingsException, CanceledExecutionException {
        final DataTableSpec tableSpec = data.getDataTableSpec();
        final double noOfRows = data.size();
        long progress = 0;
        //start to proceed row by row
        for (final DataRow row : data) {
            updateModel(row, tableSpec, classColIdx);
            if (exec != null) {
                exec.setProgress(progress / noOfRows);
                exec.checkCanceled();
            }
        }
        if (exec != null) {
            exec.setProgress(1.0, "\'Naive Bayesian\' created ");
        }
    }

    /**
     * Updates the current {@link NaiveBayesModel} with the values from the given {@link DataRow}.
     *
     * @param row DataRow with values for update
     * @param tableSpec underlying DataTableSpec
     * @param classColIdx the index of the class column
     * @throws InvalidSettingsException if missing values occur in class column or an attribute has too many values.
     */
    public void updateModel(final DataRow row, final DataTableSpec tableSpec, final int classColIdx)
        throws InvalidSettingsException {
        if (row == null) {
            throw new NullPointerException("Row must not be null");
        }
        if (tableSpec == null) {
            throw new NullPointerException("TableSpec must not be null");
        }
        final DataCell classCell = row.getCell(classColIdx);
        if (classCell.isMissing()) {
            if (m_ignoreMissingVals) {
                return;
            }
            //check if the class value is missing
            throw new InvalidSettingsException("Missing class value found in row " + row.getKey()
                + " to skip missing values tick the box in the dialog");
        }
        final String classVal = classCell.toString();
        final int numColumns = tableSpec.getNumColumns();
        for (int i = 0; i < numColumns; i++) {
            final AttributeModel model = m_modelByAttrName.get(tableSpec.getColumnSpec(i).getName());
            if (model != null) {
                final DataCell cell = row.getCell(i);
                try {
                    model.addValue(classVal, cell);
                } catch (final TooManyValuesException e) {
                    if (model instanceof ClassAttributeModel) {
                        throw new InvalidSettingsException("Class attribute has too many unique values. "
                            + "To avoid this exception increase the maximum number of allowed nominal "
                            + "values in the node dialog");
                    }
                    //delete the model if it contains too many unique values
                    m_modelByAttrName.remove(model.getAttributeName());
                    model.setInvalidCause("Too many values");
                    m_skippedAttributes.add(model);
                }
            }
        }
    }

    private void validateModel(final ExecutionMonitor exec) throws CanceledExecutionException {
        final Collection<AttributeModel> mapModels = m_modelByAttrName.values();
        final int noOfModels = mapModels.size();
        final Collection<AttributeModel> models = new ArrayList<>(noOfModels);
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

    /**
     * Constructor for class NaiveBayesModel.
     *
     * @param predParams the <code>ModelContentRO</code> to read from
     * @throws InvalidSettingsException if a mandatory key is not available
     */
    public NaiveBayesModel(final ConfigRO predParams) throws InvalidSettingsException {
        if (predParams == null) {
            throw new NullPointerException("PredParams must not be null");
        }
        m_classColName = predParams.getString(CLASS_COL_NAME);
        m_classColType = predParams.getDataType(CLASS_COL_TYPE);
        m_ignoreMissingVals = predParams.getBoolean(IGNORE_MISSING_VALS);
        m_pmmlZeroProbThreshold = Double.valueOf(predParams.getDouble(THRESHOLD, Double.NaN));
        //load the skipped models
        final Config skippedConfig = predParams.getConfig(SKIPPED_ATTRIBUTE_SECTION);
        final int noOfSkipped = skippedConfig.getInt(SKIPPED_ATTRIBUTE_COUNTER);
        m_skippedAttributes = new ArrayList<>(noOfSkipped);
        for (int i = 0; i < noOfSkipped; i++) {
            final Config modelConfig = skippedConfig.getConfig(SKIPPED_ATTRIBUTE_DATA + i);
            final AttributeModel model = AttributeModel.loadModel(modelConfig);
            m_skippedAttributes.add(model);
        }
        //load the valid models
        final Config modelConfigSect = predParams.getConfig(ATTRIBUTE_MODEL_SECTION);
        final int noOfAttrs = modelConfigSect.getInt(ATTRIBUTE_MODEL_COUNTER);
        m_modelByAttrName = new LinkedHashMap<>(noOfAttrs);
        for (int i = 0; i < noOfAttrs; i++) {
            final Config modelConfig = modelConfigSect.getConfig(ATTRIBUTE_MODEL_DATA + i);
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
        predParams.addDataType(CLASS_COL_TYPE, m_classColType);
        predParams.addBoolean(IGNORE_MISSING_VALS, m_ignoreMissingVals);
        predParams.addDouble(THRESHOLD, m_pmmlZeroProbThreshold.doubleValue());
        //create the skipped attributes section
        final Config skippedConfig = predParams.addConfig(SKIPPED_ATTRIBUTE_SECTION);
        //save the number of skipped attribute models
        skippedConfig.addInt(SKIPPED_ATTRIBUTE_COUNTER, m_skippedAttributes.size());
        //save the skipped attribute models
        int modelIndex = 0;
        for (final AttributeModel model : m_skippedAttributes) {
            final Config modelConfig = skippedConfig.addConfig(SKIPPED_ATTRIBUTE_DATA + modelIndex++);
            model.saveModel(modelConfig);
        }
        //create the model config section
        final Config modelConfigSect = predParams.addConfig(ATTRIBUTE_MODEL_SECTION);
        //save the number of attribute models
        modelConfigSect.addInt(ATTRIBUTE_MODEL_COUNTER, m_modelByAttrName.size());
        //save the attribute models
        modelIndex = 0;
        for (final AttributeModel model : m_modelByAttrName.values()) {
            final Config modelConfig = modelConfigSect.addConfig(ATTRIBUTE_MODEL_DATA + modelIndex++);
            model.saveModel(modelConfig);
        }
    }

    /**
     * @param pmml the {@link PMML} document to read from
     * @throws InvalidSettingsException if the document contains invalid settings
     */
    public NaiveBayesModel(final PMML pmml) throws InvalidSettingsException {
        final List<org.dmg.pmml.NaiveBayesModelDocument.NaiveBayesModel> naiveBayesModelList =
            pmml.getNaiveBayesModelList();
        if (naiveBayesModelList.size() != 1) {
            throw new InvalidSettingsException("Only one Naive Bayes model supported per PMML document");
        }
        final org.dmg.pmml.NaiveBayesModelDocument.NaiveBayesModel bayesModel = naiveBayesModelList.get(0);
        //set ignore missing values to true as it has no effect on the prediction
        m_ignoreMissingVals = true;
        m_pmmlZeroProbThreshold = Double.valueOf(bayesModel.getThreshold());
        m_skippedAttributes = null;
        final BayesInputs inputs = bayesModel.getBayesInputs();
        m_modelByAttrName = new LinkedHashMap<>(inputs.getBayesInputList().size() + 1);
        for (BayesInput input : inputs.getBayesInputList()) {
            final AttributeModel attributeModel = AttributeModel.loadModel(input);
            m_modelByAttrName.put(attributeModel.getAttributeName(), attributeModel);
        }
        final Map<String, String> inputExtension =
            PMMLNaiveBayesModelTranslator.convertToMap(inputs.getExtensionList());
        if (inputExtension.containsKey(BIT_VECTOR_MODEL_COUNT)) {
            final int bitVecorModels =
                PMMLNaiveBayesModelTranslator.getIntExtension(inputExtension, BIT_VECTOR_MODEL_COUNT);
            for (int idx = 0; idx < bitVecorModels; idx++) {
                final BitVectorAttributeModel model =
                    BitVectorAttributeModel.readExtension(inputExtension, BIT_VECTOR_MODEL_PREFIX + idx);
                m_modelByAttrName.put(model.getAttributeName(), model);
            }
        }
        final BayesOutput output = bayesModel.getBayesOutput();
        final ClassAttributeModel classModel = ClassAttributeModel.loadClassAttributeFromPMML(output);
        m_classColName = classModel.getAttributeName();
        m_classColType = getDataType(pmml.getDataDictionary(), m_classColName);
        m_modelByAttrName.put(m_classColName, classModel);
    }

    private DataType getDataType(final DataDictionary dataDictionary, final String classColName) {
        List<DataField> fieldList = dataDictionary.getDataFieldList();
        for (DataField field : fieldList) {
            if (field.getName().equals(classColName)) {
                return PMMLDataDictionaryTranslator.getKNIMEDataType(field.getDataType());
            }
        }
        return StringCell.TYPE;
    }

    /**
     * @param bayesModel the {@link NaiveBayesModelDocument} document to export to
     */
    void exportToPMML(final org.dmg.pmml.NaiveBayesModelDocument.NaiveBayesModel bayesModel,
        final DerivedFieldMapper mapper) {
        bayesModel.setIsScorable(true);
        bayesModel.setModelName("KNIME PMML Naive Bayes model");
        bayesModel.setThreshold(m_pmmlZeroProbThreshold.doubleValue());
        bayesModel.setFunctionName(MININGFUNCTION.CLASSIFICATION);
        final BayesInputs bayesInputs = bayesModel.addNewBayesInputs();
        final Collection<AttributeModel> models = getAttributeModels();
        ClassAttributeModel classAttributeModel = null;
        final Collection<String> bitVecotorModelNames = new LinkedList<>();
        for (final AttributeModel attributeModel : models) {
            if (attributeModel instanceof BitVectorAttributeModel) {
                bitVecotorModelNames.add(attributeModel.getAttributeName());
            } else if (attributeModel instanceof ClassAttributeModel) {
                classAttributeModel = (ClassAttributeModel)attributeModel;
            } else {
                final BayesInput bayesInput = bayesInputs.addNewBayesInput();
                attributeModel.exportToPMML(bayesInput, mapper);
            }
        }
        if (!bitVecotorModelNames.isEmpty()) {
            //export the bit vector models as extension since they are not supported by the PMML standard
            PMMLNaiveBayesModelTranslator.setIntExtension(bayesInputs.addNewExtension(), BIT_VECTOR_MODEL_COUNT,
                bitVecotorModelNames.size());
            int idx = 0;
            for (String bitVectorModelName : bitVecotorModelNames) {
                final BitVectorAttributeModel bitVectorModel =
                    (BitVectorAttributeModel)getAttributeModel(bitVectorModelName);
                bitVectorModel.writeExtension(bayesInputs, BIT_VECTOR_MODEL_PREFIX + idx++);
            }
        }
        if (classAttributeModel == null) {
            throw new IllegalStateException("No class model found");
        }
        final BayesOutput bayesOutput = bayesModel.addNewBayesOutput();
        bayesOutput.setFieldName(classAttributeModel.getAttributeName());
        classAttributeModel.exportClassAttributeToPMML(bayesOutput);
    }

    /**
     * @return a <code>String</code> <code>Collection</code> with all class values
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
            final List<String> sortedClassVals = new ArrayList<>(classValues.size());
            sortedClassVals.addAll(classValues);
            Collections.sort(sortedClassVals);
            return sortedClassVals;
        }
        throw new IllegalStateException("No model found for class column" + m_classColName);
    }

    /**
     * @param classValue the value of the class we want the probability for
     * @return the prior probability for the given class
     */
    public double getClassPriorProbability(final String classValue) {
        if (classValue == null) {
            throw new NullPointerException("ClassValue must not be null");
        }

        final AttributeModel classModel = m_modelByAttrName.get(m_classColName);
        if (classModel == null) {
            throw new IllegalArgumentException("Class model not found");
        }
        if (classModel.hasRecs4ClassValue(classValue)) {
            throw new IllegalArgumentException("Class value: " + classValue + " not found");
        }
        final long noOfRecs4Class = classModel.getNoOfRecs4ClassValue(classValue);
        final long noOfRecords = getNoOfRecs();
        if (noOfRecords == 0) {
            throw new IllegalArgumentException("Model contains no records");
        }
        return (double)noOfRecs4Class / noOfRecords;
    }

    /**
     * @return the total number of training records
     */
    public int getNoOfRecs() {
        final AttributeModel classModel = m_modelByAttrName.get(m_classColName);
        if (classModel == null) {
            return 0;
        }
        return ((ClassAttributeModel)classModel).getTotalNoOfRecs();
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
        return m_classColType;
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
        buf.append("<style type='text/css'>  td{ background-color: #F0F0F0 } </style>");
        buf.append("<div>");
        buf.append("<h3>");
        buf.append(classModel.getHTMLViewHeadLine());
        buf.append("</h3>");
        buf.append(classModel.getHTMLView(getNoOfRecs()));
        buf.append("<hr>");
        buf.append("</div>");
        if (!m_pmmlZeroProbThreshold.isNaN()) {
            buf.append("<div>");
            buf.append("<b>Threshold to used for zero probabilities:</b> ");
            buf.append(m_pmmlZeroProbThreshold.toString());
            buf.append("<hr>");
            buf.append("</div>");
        }
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
        if (m_ignoreMissingVals) {
            buf.append("<div>");
            buf.append("Missing values are ignored during learning and prediction phase.");
            buf.append("<hr>");
            buf.append("</div>");
        }
        final List<AttributeModel> models = new ArrayList<>();
        models.addAll(m_modelByAttrName.values());
        Collections.sort(models);
        for (final AttributeModel model : models) {
            if (model.equals(classModel)) {
                continue;
            }
            buf.append("<div>");
            buf.append("<h3>");
            buf.append(model.getHTMLViewHeadLine());
            buf.append("</h3>");
            buf.append(model.getHTMLView(getNoOfRecs()));
            buf.append("<br><hr>");
            buf.append("</div>");
        }
        return buf.toString();
    }

    /**
     * @return the name of all attributes which has at least one missing value during learning or an empty list
     */
    public List<String> getAttributesWithMissingVals() {
        final List<String> missingModels = new ArrayList<>();
        for (final AttributeModel model : m_modelByAttrName.values()) {
            if (model.getNoOfMissingVals() > 0) {
                missingModels.add(model.getAttributeName());
            }
        }
        return missingModels;
    }

    /**
     * @param attributeName the name of the attribute
     * @return the model for the given attribute name or <code>null</code> if the attribute is not known
     */
    public AttributeModel getAttributeModel(final String attributeName) {
        if (attributeName == null) {
            throw new NullPointerException("AttributeName must not be null");
        }
        return m_modelByAttrName.get(attributeName);
    }

    /**
     * @return an unmodifiable <code>Collection</code> with all <code>AttributeModel</code> objects
     */
    public Collection<AttributeModel> getAttributeModels() {
        return Collections.unmodifiableCollection(m_modelByAttrName.values());
    }

    /**
     * @return {@link List} with all PMML compatible learning columns
     * @since 2.10
     */
    public List<String> getPMMLLearningCols() {
        final List<String> names = new LinkedList<>();
        for (AttributeModel model : m_modelByAttrName.values()) {
            if ((model instanceof NumericalAttributeModel) || (model instanceof NominalAttributeModel)) {
                if (model.getInvalidCause() == null) {
                    names.add(model.getAttributeName());
                }
            }
        }
        return names;
    }

    /**
     * @return {@link List} with all attribute names
     * @since 2.10
     */
    public List<String> getAttributeNames() {
        final List<String> names = new LinkedList<>();
        for (AttributeModel model : m_modelByAttrName.values()) {
            if (model.getInvalidCause() == null) {
                names.add(model.getAttributeName());
            }
        }
        return names;
    }

    /**
     * @param attributeNames the name of the attributes we want the normalized probability values for
     * @param row the row with the values in the same order like the attribute names
     * @param classValues the class values to calculate the probability for
     * @param normalize set to <code>true</code> if the probability values should be normalized
     * @return the probability values in the same order like the class values
     * @since 2.10
     */
    public double[] getClassProbabilities(final String[] attributeNames, final DataRow row,
        final List<String> classValues, final boolean normalize) {
        if (row == null) {
            throw new NullPointerException("Row must not be null");
        }
        if (classValues == null || classValues.size() < 1) {
            throw new IllegalArgumentException("Class value list must not be empty");
        }
        if (attributeNames == null) {
            throw new NullPointerException("ColumSpec must not be null");
        }
        if (attributeNames.length != row.getNumCells()) {
            throw new IllegalArgumentException("Attribute names array and data row must be the same size");
        }

        // get the logarithmic probabilities
        final double[] logProbs = classValues.stream()//
            .mapToDouble(c -> getLogClassProbability(attributeNames, row, c))//
            .toArray();

        // TODO: here we used the prior probs if all probs are 0
        if (normalize) {
            /* p(x_k) / \sum_{i = 0}^n p(x_i) = 1 / \sum_{i = 0}^n p(x_i) / p(x_k)
             * since p(x_k) is actually the log probability "p(x_i)/ p(x_k)" becomes exp(p(x_i)-p(x_k))
             */
            final double[] normProbs = new double[logProbs.length];
            for (int i = 0, length = logProbs.length; i < length; i++) {
                final int idx = i;
                normProbs[i] = 1d / Arrays.stream(logProbs).map(prob -> Math.exp(prob - logProbs[idx])).sum();
            }
            return normProbs;
        } else {
            for (int i = 0; i < logProbs.length; i++) {
                logProbs[i] = Math.exp(logProbs[i]);
            }
            return logProbs;
        }
    }

    /**
     * Returns the name of the class with the highest probability for the given row.
     *
     * @param attributeNames the attribute names in the same order they appear in the given data row
     * @param row the row with the attributes in the same order like in the training data table
     * @return the class attribute with the highest probability for the given attribute values.
     */
    public String getMostLikelyClass(final String[] attributeNames, final DataRow row) {
        if (row == null) {
            throw new NullPointerException("Row must not be null");
        }
        if (attributeNames == null) {
            throw new NullPointerException("ColumSpec must not be null");
        }
        if (attributeNames.length != row.getNumCells()) {
            throw new IllegalArgumentException("Attribute names array and data row must be the same size");
        }
        double maxProbability = Double.NEGATIVE_INFINITY;
        String mostLikelyClass = null;

        for (final String classValue : getClassValues()) {
            final double classProbability = getLogClassProbability(attributeNames, row, classValue);
            if (classProbability >= maxProbability) {
                maxProbability = classProbability;
                mostLikelyClass = classValue;
            }
        }

        // TODO: check if this is still necessary
        //        if (maxProbability == 0) {
        //            //all classes have a combined probability of zero for this row ->
        //            //use only the prior probability
        //            for (final String classValue : classValues) {
        //                final double classPriorProbability = getClassPriorProbability(classValue);
        //                if (classPriorProbability >= maxProbability) {
        //                    maxProbability = classPriorProbability;
        //                    mostLikelyClass = classValue;
        //                }
        //            }
        //        }

        // TODO: Check if we have NaN's
        if (mostLikelyClass == null) {
            throw new IllegalStateException("Most likely class must not be null");
        }
        return mostLikelyClass;
    }

    /**
     * Returns the {@link DataCell} of the class with the highest probability for the given row.
     *
     * @param attributeNames the attribute names in the same order they appear in the given data row
     * @param row the row with the attributes in the same order like in the training data table
     * @return the class attribute {@link DataCell} with the highest probability for the given attribute values.
     */
    public DataCell getMostLikelyClassCell(final String[] attributeNames, final DataRow row) {
        final String mostLikelyClass = getMostLikelyClass(attributeNames, row);
        return createPredictedClassCell(getClassColumnDataType(), mostLikelyClass);
    }

    /**
     * @param dataType the {@link DataType} to convert to
     * @param val the value to convert
     * @return a {@link DataCell} of the given type with the given value
     *
     * @see PMMLDataDictionaryTranslator#getKNIMEDataType(Enum)
     */
    private static DataCell createPredictedClassCell(final DataType dataType, final String val) {
        if (dataType.isCompatible(BooleanValue.class)) {
            return BooleanCellFactory.create(Boolean.parseBoolean(val));
        } else if (dataType.isCompatible(IntValue.class)) {
            return IntCellFactory.create(Integer.parseInt(val));
        } else if (dataType.isCompatible(DoubleValue.class)) {
            return DoubleCellFactory.create(Double.parseDouble(val));
        }
        return StringCellFactory.create(val);
    }

    /**
     * Returns the probability of the row to be member of the given class. All not known attributes are skipped. If none
     * of the given attributes is known the method returns the class prior probability.
     *
     * @param attrNames the name of the attributes
     * @param row the row with the value per attribute in the same order like the attribute names
     * @param classValue the class value to compute the probability for
     * @param useLog <code>true</code> if probabilities should be combined by adding the logs
     * @return the probability of this row to belong to the given class value
     */
    private double getClassProbability(final String[] attrNames, final DataRow row, final String classValue) {
        // TODO: to be deleted
        double combinedProbability = Math.log(getClassPriorProbability(classValue));
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
            final double probabilityThreshold;
            if (m_pmmlZeroProbThreshold.isNaN()) {
                probabilityThreshold = 0;
            } else {
                probabilityThreshold = m_pmmlZeroProbThreshold.doubleValue();
            }
            final DataCell cell = row.getCell(i);
            Double probability = model.getProbability(classValue, cell, probabilityThreshold);
            if (probability != null) {
                if (probability.doubleValue() <= 0) {
                    //set the probability to the given corrector if the probability is zero and the pmml threshold
                    //method should be used
                    probability = m_pmmlZeroProbThreshold;
                }
                combinedProbability += Math.log(probability.doubleValue());
            }
        }
        combinedProbability = Math.exp(combinedProbability);
        return combinedProbability;
    }

    /**
     * Returns the probability of the row to be member of the given class. All not known attributes are skipped. If none
     * of the given attributes is known the method returns the class prior probability.
     *
     * @param attrNames the name of the attributes
     * @param row the row with the value per attribute in the same order like the attribute names
     * @param classValue the class value to compute the probability for
     * @param useLog <code>true</code> if probabilities should be combined by adding the logs
     * @return the probability of this row to belong to the given class value
     */
    private double getLogClassProbability(final String[] attrNames, final DataRow row, final String classValue) {
        // the prior probability
        double combinedProbability = Math.log(getClassPriorProbability(classValue));

        for (int i = 0, length = row.getNumCells(); i < length; i++) {
            final String attrName = attrNames[i];
            final AttributeModel model = m_modelByAttrName.get(attrName);
            //skip unknown attributes and the class value column
            if (model != null && !(model instanceof ClassAttributeModel)) {
                final double probabilityThreshold;
                if (m_pmmlZeroProbThreshold.isNaN()) {
                    probabilityThreshold = 0;
                } else {
                    probabilityThreshold = m_pmmlZeroProbThreshold.doubleValue();
                }
                final double probability = model.getLogProbability(classValue, row.getCell(i), probabilityThreshold);
                // TODO: null should not happen
                if (probability != Double.NaN) {
                    // TODO: is this necessary? we have getProbability already with this threshold
                    if (Double.isFinite(probability)) {
                        // TODO: we want that the method of the model returns the log probability
                        combinedProbability += probability;
                    } else {
                        //set the probability to the given corrector if the probability is zero and the pmml threshold
                        //method should be used
                        combinedProbability += probabilityThreshold;

                    }
                }
            }
        }
        // TODO: check if this is right
        // we don't return the exp but the log probability
        //        combinedProbability = Math.exp(combinedProbability);
        return combinedProbability;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        final StringBuffer buf = new StringBuffer();
        buf.append("Number of records: ");
        buf.append(getNoOfRecs());
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
     * Checks if the given table specification contains columns which are not covered by the learned model. Either
     * because the name is not known or the type is wrong.
     *
     * @param tableSpec the <code>DataTableSpec</code> to check for unknown columns
     * @return the name of the unknown columns or an empty <code>List</code>
     */
    public List<String> check4UnknownCols(final DataTableSpec tableSpec) {
        if (tableSpec == null) {
            throw new NullPointerException("TableSpec must not be null");
        }
        final List<String> unknownCols = new ArrayList<>();
        for (int i = 0, length = tableSpec.getNumColumns(); i < length; i++) {
            final DataColumnSpec colSpec = tableSpec.getColumnSpec(i);
            final AttributeModel attrModel = getAttributeModel(colSpec.getName());
            if (attrModel == null || !attrModel.isCompatible(colSpec.getType())) {
                unknownCols.add(colSpec.getName());
            }
        }
        return unknownCols;
    }

    /**
     * Checks if the model contains attributes which are not present in the given table specification which could
     * influence the prediction result.
     *
     * @param tableSpec the <code>DataTableSpec</code> to check for missing columns
     * @return the name of the missing columns or an empty <code>List</code>
     */
    public List<String> check4MissingCols(final DataTableSpec tableSpec) {
        final List<String> missingInputCols = new ArrayList<>();
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
