/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 *   02.03.2006 (gabriel): created
 */
package org.knime.base.node.mine.bfn;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.knime.base.node.mine.bfn.BasisFunctionLearnerTable.MissingValueReplacementFunction;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContent;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.property.hilite.DefaultHiLiteMapper;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.property.hilite.HiLiteTranslator;

/**
 * Abstract basisfunction model holding the trained rule table.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public abstract class BasisFunctionLearnerNodeModel extends NodeModel {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(BasisFunctionLearnerNodeModel.class);

    /** The choice of distance function. */
    private int m_distance = 0;

    /** Key for choice of distance measure. */
    public static final String DISTANCE = "distance_function";

    /** An array of possible distance measures. */
    public static final Distance[] DISTANCES = {Distance.getInstance()};

    /** NodeSettings key for <i>shrink_after_commit</i>. */
    public static final String SHRINK_AFTER_COMMIT = "shrink_after_commit";

    /** NodeSettings key for <i>max_class_coverage</i>. */
    public static final String MAX_CLASS_COVERAGE = "max_class_coverage";
    
    /** Key of the target column. */
    public static final String TARGET_COLUMNS = "target_column";

    /**
     * Keeps names of all columns used to make classification during training.
     */
    private String[] m_targetColumns = null;
    
    /** Keeps a value for missing replacement function index. */
    private int m_missing = 0;

    /** The <i>shrink_after_commit</i> flag. */
    private boolean m_shrinkAfterCommit = true;
    
    /** The <i>max_class_coverage</i> flag. */
    private boolean m_maxCoverage = true;
    
    /** Config key for maximum number of epochs. */
    public static final String MAX_EPOCHS = "max_epochs";
    
    /** Maximum number of epochs to train. */
    private int m_maxEpochs = -1;

    /** Contains model info after training. */
    private ModelContent m_modelInfo;

    /** Translates hilite events between model and training data. */
    private final HiLiteTranslator m_translator = new HiLiteTranslator();

    /**
     * Creates a new model with one data in and out port, and model out-port.
     * @param model the port type of the generated basisfunction model
     */
    protected BasisFunctionLearnerNodeModel(final PortType model) {
        super(new PortType[]{BufferedDataTable.TYPE},  
              new PortType[]{BufferedDataTable.TYPE, model});
    }

    /**
     * Reset the trained model.
     * 
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_modelInfo = null;
        m_translator.getFromHiLiteHandler().fireClearHiLiteEvent();
        m_translator.removeAllToHiliteHandlers();
        m_translator.setMapper(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HiLiteHandler getOutHiLiteHandler(final int outPortID) {
        return m_translator.getFromHiLiteHandler();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PortObjectSpec[] configure(final PortObjectSpec[] ins)
            throws InvalidSettingsException {
        DataTableSpec inSpec = (DataTableSpec) ins[0];
        // check if target column available
        if (m_targetColumns == null) {
            // find first non-numeric column
            for (int i = 0; i < inSpec.getNumColumns(); i++) {
                DataColumnSpec cspec = inSpec.getColumnSpec(i);
                if (!cspec.getType().isCompatible(DoubleValue.class)) {
                    m_targetColumns = new String[]{cspec.getName()};
                    super.setWarningMessage("Target column guessed as \""
                            + cspec.getName() + "\"");
                    break;
                }
            }
            if (m_targetColumns == null) {
                throw new InvalidSettingsException(
                        "Target columns not available.");
            }
        } else {
            for (String target : m_targetColumns) {
                if (!inSpec.containsName(target)) {
                    throw new InvalidSettingsException(
                        "Target \"" + target + "\" column not available.");
                }
                if (m_targetColumns.length > 1) {
                    if (!inSpec.getColumnSpec(target).getType().isCompatible(
                            DoubleValue.class)) {
                        throw new InvalidSettingsException(
                                "Target \"" + target 
                                + "\" column not of type DoubleValue.");
                    }
                }
            }
        }
        
        // check if double type column available
        if (!inSpec.containsCompatibleType(DoubleValue.class)) {
            throw new InvalidSettingsException(
                    "No data column of type DoubleValue found.");
        }
        
        List<String> targetHash = Arrays.asList(m_targetColumns);
        // if only one double type column, check if not the target column
        for (int i = 0; i < inSpec.getNumColumns(); i++) {
            DataColumnSpec cspec = inSpec.getColumnSpec(i);
            if (cspec.getType().isCompatible(DoubleValue.class)) {
                if (!targetHash.contains(cspec.getName())) {
                    break;
                }
            }
            // if last column was tested
            if (i + 1 == inSpec.getNumColumns()) {
                throw new InvalidSettingsException("Found only one column of"
                        + " type DoubleValue: " 
                        + Arrays.toString(m_targetColumns));
            }
        }
        
        // if no data columns are found, use all numeric columns
        String[] dataCols = BasisFunctionFactory.findDataColumns(
                inSpec, targetHash);
        DataTableSpec modelSpec = BasisFunctionFactory.createModelSpec(inSpec,
                dataCols, m_targetColumns, getModelType());
        return new DataTableSpec[]{modelSpec, modelSpec};
    }
    
    /**
     * @return the type of the learned model cells
     */
    public abstract DataType getModelType();
    
    /**
     * Creates a new basisfunction port object given the model content.
     * @param content basisfunction rules and spec
     * @return a new basisfunction port object
     */
    public abstract BasisFunctionPortObject createPortObject(
        final BasisFunctionModelContent content);

    /**
     * Starts the learning algorithm in the learner.
     * 
     * @param inData the input training data at index 0
     * @param exec the execution monitor
     * @return the output fuzzy rule model
     * @throws CanceledExecutionException if the training was canceled
     */
    @Override
    public PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws CanceledExecutionException {
        BufferedDataTable data = (BufferedDataTable) inData[0];
        // find all double cell columns in the data
        DataTableSpec tSpec = data.getDataTableSpec();
        LinkedHashSet<String> columns = new LinkedHashSet<String>(tSpec
                .getNumColumns());
        List<String> targetHash = Arrays.asList(m_targetColumns);
        for (int c = 0; c < tSpec.getNumColumns(); c++) {
            DataColumnSpec cSpec = tSpec.getColumnSpec(c);
            String name = cSpec.getName();
            if (!targetHash.contains(name)) {
                // TODO only numeric columns allowed
                if (cSpec.getType().isCompatible(DoubleValue.class)) {
                    columns.add(cSpec.getName());
                }
            }
        }
        // get all data columns without target columns
        String[] dataCols = BasisFunctionFactory.findDataColumns(
                tSpec, targetHash);
        columns.addAll(Arrays.asList(dataCols));
        // add target columns at the end
        columns.addAll(Arrays.asList(m_targetColumns));
        
        // filter selected columns from input data
        String[] cols = columns.toArray(new String[]{});
        ColumnRearranger colRe = new ColumnRearranger(tSpec);
        colRe.keepOnly(cols);
        BufferedDataTable trainData = exec.createColumnRearrangeTable(
                data, colRe, exec);

        // print settings info
        LOGGER.debug("distance      : " + getDistance());
        LOGGER.debug("missing       : " + getMissingFct());
        LOGGER.debug("target columns: " + m_targetColumns);
        LOGGER.debug("shrink commit : " + isShrinkAfterCommit());
        LOGGER.debug("max coverage  : " + isMaxClassCoverage());
        LOGGER.debug("max no. epochs: " + m_maxEpochs);

        // create factory
        BasisFunctionFactory factory = getFactory(
                trainData.getDataTableSpec());
        // start training
        BasisFunctionLearnerTable table = new BasisFunctionLearnerTable(
                trainData, dataCols, m_targetColumns, factory,
                BasisFunctionLearnerTable.MISSINGS[m_missing],
                m_shrinkAfterCommit, m_maxCoverage, m_maxEpochs, exec);
        DataTableSpec modelSpec = table.getDataTableSpec();
        DataColumnSpec[] modelSpecs = 
            new DataColumnSpec[modelSpec.getNumColumns()];
        for (int i = 0; i < modelSpecs.length; i++) {
            DataColumnSpecCreator creator = 
                new DataColumnSpecCreator(modelSpec.getColumnSpec(i));
            creator.removeAllHandlers();
            modelSpecs[i] = creator.createSpec();
        }

        // set translator mapping
        m_translator.setMapper(table.getHiLiteMapper());
        m_translator.addToHiLiteHandler(getInHiLiteHandler(0));

        ModelContent modelInfo = new ModelContent(MODEL_INFO);
        table.saveInfos(modelInfo);
        m_modelInfo = modelInfo;

        // return rules[0] and rule_model[1]
        return new PortObject[]{exec.createBufferedDataTable(
                table, exec), createPortObject(new BasisFunctionModelContent(
                        table.getDataTableSpec(), table.getBasisFunctions()))};
    }

    /**
     * Create factory to generate BasisFunctions.
     * 
     * @param spec the cleaned data for training
     * @return factory to create special basis function rules
     */
    public abstract BasisFunctionFactory getFactory(DataTableSpec spec);

    /**
     * @return get model info after training or <code>null</code>
     */
    public ModelContentRO getModelInfo() {
        return m_modelInfo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        StringBuffer msg = new StringBuffer();
        // get target columns
        String[] targetColumns = null;
        try {
            targetColumns = settings.getStringArray(TARGET_COLUMNS);
            if (targetColumns == null || targetColumns.length == 0) {
                msg.append("No target column specified.\n");
            }
        } catch (InvalidSettingsException ise) {
            // try to read only one target ref. to KNIME 1.2.0 and before
            targetColumns = 
                new String[]{settings.getString(TARGET_COLUMNS, null)};
        }
        if (targetColumns == null || targetColumns.length == 0) {
            msg.append("Target columns not found in settings.\n");
        }
        // distance function
        int distance = settings.getInt(DISTANCE, -1);
        if (distance < 0 || distance > DISTANCES.length) {
            msg.append("Distance function index out of range: " + distance);
        }
        // missing replacement method
        int missing = settings.getInt(BasisFunctionLearnerTable.MISSING, -1);
        if (missing < 0 
                || missing > BasisFunctionLearnerTable.MISSINGS.length) {
            msg.append("Missing replacement function index out of range: "
                    + missing);
        }
        // if message length contains chars
        if (msg.length() > 0) {
            throw new InvalidSettingsException(msg.toString());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // target columns for classification
        m_targetColumns = settings.getStringArray(
                TARGET_COLUMNS, (String[]) null);
        if (m_targetColumns == null) {
            // try to find single target column from version 1.2.0 and before
            m_targetColumns = new String[]{settings.getString(
                    TARGET_COLUMNS, null)};
        }
        // missing value replacement
        m_missing = settings.getInt(BasisFunctionLearnerTable.MISSING);
        // distance function
        m_distance = settings.getInt(DISTANCE);
        // shrink after commit
        m_shrinkAfterCommit = settings.getBoolean(SHRINK_AFTER_COMMIT);
        // max class coverage
        m_maxCoverage = settings.getBoolean(MAX_CLASS_COVERAGE, true);
        // maximum epochs
        m_maxEpochs = settings.getInt(MAX_EPOCHS, -1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) {
        // selected target columns
        settings.addStringArray(TARGET_COLUMNS, m_targetColumns);
        // missing value replacement function
        settings.addInt(BasisFunctionLearnerTable.MISSING, m_missing);
        // distance function
        settings.addInt(DISTANCE, m_distance);
        // shrink after commit
        settings.addBoolean(SHRINK_AFTER_COMMIT, m_shrinkAfterCommit);
        // max class coverage
        settings.addBoolean(MAX_CLASS_COVERAGE, m_maxCoverage);
        // maximum number of epochs
        settings.addInt(MAX_EPOCHS, m_maxEpochs);
    }

    /** Model info identifier. */
    public static final String MODEL_INFO = "model_info";

    /** Model info file extension. */
    public static final String MODEL_INFO_FILE_NAME = MODEL_INFO + ".pmml.gz";

    /** File name for hilite mapping. */
    public static final String HILITE_MAPPING_FILE_NAME = 
        "hilite_mapping.pmml.gz";

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) 
            throws IOException, CanceledExecutionException {
        // load model info
        exec.checkCanceled();
        exec.setProgress(0.1, "Loading model information");
        File file = new File(internDir, MODEL_INFO_FILE_NAME);
        m_modelInfo = (ModelContent)ModelContent
                .loadFromXML(new GZIPInputStream(new BufferedInputStream(
                        new FileInputStream(file))));
        // load hilite mapping
        exec.checkCanceled();
        exec.setProgress(0.5, "Loading hilite mapping");
        File mappingFile = new File(internDir, HILITE_MAPPING_FILE_NAME);
        NodeSettingsRO mapSettings = NodeSettings
                .loadFromXML(new GZIPInputStream(new BufferedInputStream(
                        new FileInputStream(mappingFile))));
        try {
            m_translator.setMapper(DefaultHiLiteMapper.load(mapSettings));
        } catch (InvalidSettingsException ise) {
            m_translator.setMapper(null);
            throw new IOException(ise.getMessage());
        } finally {
            m_translator.addToHiLiteHandler(getInHiLiteHandler(0));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        assert (m_modelInfo != null);
        // save model info
        exec.checkCanceled();
        exec.setProgress(0.1, "Saving model information");
        File file = new File(internDir, MODEL_INFO_FILE_NAME);
        m_modelInfo.saveToXML(new GZIPOutputStream(new BufferedOutputStream(
                new FileOutputStream(file))));
        // save hilite mapping
        exec.checkCanceled();
        exec.setProgress(0.5, "Saving hilite mapping");
        NodeSettings mapSettings = new NodeSettings(HILITE_MAPPING_FILE_NAME);
        DefaultHiLiteMapper mapper = (DefaultHiLiteMapper)m_translator
                .getMapper();
        mapper.save(mapSettings);
        File mappingFile = new File(internDir, HILITE_MAPPING_FILE_NAME);
        mapSettings.saveToXML(new GZIPOutputStream(new BufferedOutputStream(
                new FileOutputStream(mappingFile))));
    }

    /**
     * @return missing replacement function
     */
    public final MissingValueReplacementFunction getMissingFct() {
        return BasisFunctionLearnerTable.MISSINGS[m_missing];
    }

    /**
     * @return the target columns with class info
     */
    public final String[] getTargetColumns() {
        return m_targetColumns;
    }

    /**
     * @return <code>true</code> if shrink after commit
     */
    public final boolean isShrinkAfterCommit() {
        return m_shrinkAfterCommit;
    }

    /**
     * @return <code>true</code> if max class coverage
     */
    public final boolean isMaxClassCoverage() {
        return m_maxCoverage;
    }
    
    /**
     * @return the choice of distance function
     */
    public final int getDistance() {
        return m_distance;
    }
    
    /**
     * @return maximum number of epochs to train
     */
    public final int getMaxNrEpochs() {
        return m_maxEpochs;
    }
    
}
