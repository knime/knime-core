/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.knime.base.node.mine.bfn.BasisFunctionLearnerTable.MissingValueReplacementFunction;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
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
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.property.hilite.DefaultHiLiteHandler;
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

    /** Key of the target column. */
    public static final String TARGET_COLUMN = "target_column";

    /**
     * Keeps the name of the column which is used to make classification during
     * training. Also an indication if the node is executable.
     */
    private String m_target = null;

    /** Keeps a value for missing replacement function index. */
    private int m_missing = -1;

    /** The <i>shrink_after_commit</i> flag. */
    private boolean m_shrinkAfterCommit = true;
    
    /** Config key for maximum number of epochs. */
    public static final String MAX_EPOCHS = "max_epochs";
    
    /** Maximum number of epochs to train. */
    private int m_maxEpochs = -1;

    /** Contains model info after training. */
    private ModelContent m_modelInfo;

    private DataTableSpec m_modelSpec;

    private final Map<DataCell, List<BasisFunctionLearnerRow>> m_bfs;

    /** Translates hilite events between model and training data. */
    private final HiLiteTranslator m_translator;

    /**
     * Creates a new model with one data in and out port, and model outport.
     */
    protected BasisFunctionLearnerNodeModel() {
        super(1, 1, 0, 1);
        m_bfs = new LinkedHashMap<DataCell, List<BasisFunctionLearnerRow>>();
        m_translator = new HiLiteTranslator(new DefaultHiLiteHandler());
    }

    /**
     * Reset the trained model.
     * 
     * @see org.knime.core.node.NodeModel#reset()
     */
    @Override
    protected void reset() {
        m_modelInfo = null;
        m_modelSpec = null;
        m_bfs.clear();
    }

    /**
     * @see org.knime.core.node.NodeModel #setInHiLiteHandler(int,
     *      HiLiteHandler)
     */
    @Override
    protected void setInHiLiteHandler(final int id, final HiLiteHandler hdl) {
        assert (id == 0);
        m_translator.removeAllToHiliteHandlers();
        m_translator.addToHiLiteHandler(hdl);
    }

    /**
     * @see org.knime.core.node.NodeModel#getOutHiLiteHandler(int)
     */
    @Override
    public HiLiteHandler getOutHiLiteHandler(final int outPortID) {
        assert (outPortID == 0);
        return m_translator.getFromHiLiteHandler();
    }

    /**
     * @see org.knime.core.node.NodeModel
     *      #configure(org.knime.core.data.DataTableSpec[])
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] ins)
            throws InvalidSettingsException {
        // check if target column available
        if (m_target == null || !ins[0].containsName(m_target)) {
            throw new InvalidSettingsException("Target column not available.");
        }
        // check if double type column available
        if (!ins[0].containsCompatibleType(DoubleValue.class)) {
            throw new InvalidSettingsException(
                    "No double-type column(s) found.");
        }
        // if only one double type column, check if not the target column
        for (int i = 0; i < ins[0].getNumColumns(); i++) {
            DataColumnSpec cspec = ins[0].getColumnSpec(i);
            if (cspec.getType().isCompatible(DoubleValue.class)) {
                if (!cspec.getName().equals(m_target)) {
                    break;
                }
            }
            // if last column was tested
            if (i + 1 == ins[0].getNumColumns()) {
                assert ins[0].getColumnSpec(m_target).getType().isCompatible(
                        DoubleValue.class);
                throw new InvalidSettingsException("Found only one double-type"
                        + " column: " + m_target);
            }
        }
        return new DataTableSpec[]{BasisFunctionFactory.createModelSpec(ins[0],
                m_target, getModelType())};
    }

    /**
     * @return the type of the learned model cells
     */
    protected abstract DataType getModelType();

    /**
     * Starts the learning algorithm in the learner.
     * 
     * @param data the input training data at index 0
     * @param exec the execution monitor
     * @return the output fuzzy rule model
     * @throws CanceledExecutionException if the training was canceled
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] data,
            final ExecutionContext exec) throws CanceledExecutionException {
        // check input data
        assert (data != null && data.length == 1 && data[0] != null);

        // find all double cell columns in the data
        DataTableSpec tSpec = data[0].getDataTableSpec();
        LinkedHashSet<String> columns = new LinkedHashSet<String>(tSpec
                .getNumColumns());
        for (int c = 0; c < tSpec.getNumColumns(); c++) {
            DataColumnSpec cSpec = tSpec.getColumnSpec(c);
            String name = cSpec.getName();
            if (!name.equals(m_target)) {
                // TODO only numeric columns allowed
                if (cSpec.getType().isCompatible(DoubleValue.class)) {
                    columns.add(cSpec.getName());
                }
            }
        }
        // add target column at the end
        columns.add(m_target);

        // filter selected columns from input data
        String[] cols = columns.toArray(new String[]{});
        ColumnRearranger colRe = new ColumnRearranger(tSpec);
        colRe.keepOnly(cols);
        BufferedDataTable trainData = exec.createColumnRearrangeTable(
                data[0], colRe, exec);

        // print settings info
        LOGGER.debug("distance     : " + getDistance());
        LOGGER.debug("missing      : " + getMissingFct());
        LOGGER.debug("target       : " + m_target);
        LOGGER.debug("shrink_commit: " + isShrinkAfterCommit());
        LOGGER.debug("max #epochs  : " + m_maxEpochs);

        // create factory
        BasisFunctionFactory factory = getFactory(trainData.getDataTableSpec());
        // start training
        BasisFunctionLearnerTable table = new BasisFunctionLearnerTable(
                trainData, m_target, factory,
                BasisFunctionLearnerTable.MISSINGS[m_missing],
                m_shrinkAfterCommit, m_maxEpochs, exec);
        m_modelSpec = table.getDataTableSpec();

        // set translator mapping
        m_translator.setMapper(table.getHiLiteMapper());

        m_bfs.putAll(table.getBasisFunctions());

        ModelContent modelInfo = new ModelContent(MODEL_INFO);
        table.saveInfos(modelInfo);
        m_modelInfo = modelInfo;

        // set out data table
        return new BufferedDataTable[]{exec
                .createBufferedDataTable(table, exec)};
    } // execute(DataTable[], ExecutionMonitor)

    /**
     * Create factory to generate BasisFunctions.
     * 
     * @param spec the cleaned data for training
     * @return factory to create special basis function rules
     */
    protected abstract BasisFunctionFactory getFactory(DataTableSpec spec);

    /**
     * @return get model info after training or <code>null</code>
     */
    protected ModelContentRO getModelInfo() {
        return m_modelInfo;
    }

    /**
     * @see org.knime.core.node.NodeModel#validateSettings(NodeSettingsRO)
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        StringBuffer msg = new StringBuffer();
        // target column
        try {
            settings.getString(TARGET_COLUMN);
        } catch (InvalidSettingsException ise) {
            msg.append("Target column not found in settings.\n");
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
     * @see org.knime.core.node.NodeModel
     *      #loadValidatedSettingsFrom(NodeSettingsRO)
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // target column for classification
        m_target = settings.getString(TARGET_COLUMN);
        // missing value replacement
        m_missing = settings.getInt(BasisFunctionLearnerTable.MISSING);
        // distance function
        m_distance = settings.getInt(DISTANCE);
        // shrink after commit
        m_shrinkAfterCommit = settings.getBoolean(SHRINK_AFTER_COMMIT);
        // maximum epochs
        m_maxEpochs = settings.getInt(MAX_EPOCHS, -1);
    }

    /**
     * @see org.knime.core.node.NodeModel#saveSettingsTo(NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        // selected target column
        settings.addString(TARGET_COLUMN, m_target);
        // missing value replacement function
        settings.addInt(BasisFunctionLearnerTable.MISSING, m_missing);
        // distance function
        settings.addInt(DISTANCE, m_distance);
        // shrink after commit
        settings.addBoolean(SHRINK_AFTER_COMMIT, m_shrinkAfterCommit);
        // maximum number of epochs
        settings.addInt(MAX_EPOCHS, m_maxEpochs);
    }

    /** 
     * @see org.knime.core.node.NodeModel
     *  #saveModelContent(int, org.knime.core.node.ModelContentWO)
     */
    @Override
    protected void saveModelContent(final int index, final ModelContentWO pp)
            throws InvalidSettingsException {
        assert index == 0 : index;
        // add used columns
        assert m_modelSpec != null;
        ModelContentWO modelSpec = pp.addModelContent("model_spec");
        for (int i = 0; i < m_modelSpec.getNumColumns(); i++) {
            DataColumnSpec cspec = m_modelSpec.getColumnSpec(i);
            modelSpec.addDataType(cspec.getName(), cspec.getType());
        }
        // save basisfunctions
        ModelContentWO ruleSpec = pp.addModelContent("rules");
        for (DataCell key : m_bfs.keySet()) {
            List<BasisFunctionLearnerRow> list = m_bfs.get(key);
            for (BasisFunctionLearnerRow bf : list) {
                BasisFunctionPredictorRow predBf = bf.getPredictorRow();
                ModelContentWO bfParam = ruleSpec.addModelContent(bf.getKey()
                        .getId().toString());
                predBf.save(bfParam);
            }
        }
    }

    /** Model info identifier. */
    protected static final String MODEL_INFO = "model_info";

    /** Model info file extension. */
    protected static final String MODEL_INFO_FILE_NAME = MODEL_INFO
            + ".pmml.gz";

    /** File name for hilite mapping. */
    protected static final String HILITE_MAPPING_FILE_NAME = 
        "hilite_mapping.pmml.gz";

    /**
     * @see org.knime.core.node.NodeModel #loadInternals(java.io.File,
     *      org.knime.core.node.ExecutionMonitor)
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
        }
    }

    /**
     * @see org.knime.core.node.NodeModel #saveInternals(java.io.File,
     *      org.knime.core.node.ExecutionMonitor)
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
     * @return the target column with class info
     */
    public final String getTargetColumn() {
        return m_target;
    }

    /**
     * @return <code>true</code> if selected
     */
    public final boolean isShrinkAfterCommit() {
        return m_shrinkAfterCommit;
    }

    /**
     * @return the choice of distance function
     */
    public final int getDistance() {
        return m_distance;
    }
}
