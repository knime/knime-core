package org.knime.base.node.mine.knn.pmml;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.port.pmml.PMMLPortObjectSpecCreator;


/**
 * This is the model implementation of PMMLFragmentMergerNodeModel.
 *
 *
 * @author Alexander Fillbrunn
 */
public class PMMLKNNNodeModel extends NodeModel {

    /**
     * Constructor for the node model.
     */
    protected PMMLKNNNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE}, new PortType[]{PMMLPortObject.TYPE});
    }

    private static final String CFG_MAX_NUM_ROWS = "maxRows";

    private static final String CFG_LIMIT_ROWS = "limitRows";

    private static final String CFG_PRED_COL_NAME = "predColName";

    private static final String CFG_NUM_NEIGHBORS = "numNeighbors";

    private static final String CFG_LEARNING_COLS = "learningColumns";

    /**
     * Creates a new SettingsModel for the boolean value indicating whether rows should be filtered.
     * @return the settings model
     */
    public static SettingsModelBoolean createLimitRowsSettingsModel() {
        return new SettingsModelBoolean(CFG_LIMIT_ROWS, true);
    }

    /**
     * Creates a new SettingsModel for the predicted column's name.
     * @return the settings model
     */
    public static SettingsModelString createPredColumnNameSettingsModel() {
        return new SettingsModelString(CFG_PRED_COL_NAME, null);
    }

    /**
     * Creates a new SettingsModel for the maximum number of rows to add to the PMML.
     * @return the settings model
     */
    public static SettingsModelInteger createMaxNumRowsSettingsModel() {
        return new SettingsModelInteger(CFG_MAX_NUM_ROWS, 200);
    }

    /**
     * Creates a SettingsModel for the number of neighbors to take into account.
     * @return the settings model
     */
    public static SettingsModelInteger createNumNeighborsSettingsModel() {
        return new SettingsModelInteger(CFG_NUM_NEIGHBORS, 3);
    }

    /**
     * Creates a SettingsModel for the selection of learning columns.
     * @return the settings model
     */
    public static SettingsModelColumnFilter2 createLearningColumnsSettingsModel() {
        return new SettingsModelColumnFilter2(CFG_LEARNING_COLS, DoubleValue.class);
    }

    private SettingsModelBoolean m_limitRows = createLimitRowsSettingsModel();

    private SettingsModelString m_predColumnName = createPredColumnNameSettingsModel();

    private SettingsModelInteger m_maxNumRows = createMaxNumRowsSettingsModel();

    private SettingsModelInteger m_numNeighbors = createNumNeighborsSettingsModel();

    private SettingsModelColumnFilter2 m_learningColumns = createLearningColumnsSettingsModel();

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inPorts,
            final ExecutionContext exec) throws Exception {
        BufferedDataTable inTable = (BufferedDataTable)inPorts[0];

        if (m_limitRows.getBooleanValue() && inTable.getRowCount() > m_maxNumRows.getIntValue()) {
            setWarningMessage("The number of records in the table (" + inTable.getRowCount()
                              + ") exceeds the maximum record number of " + m_maxNumRows.getIntValue());
        }

        PMMLPortObject outPort = new PMMLPortObject(createSpec(inTable.getDataTableSpec()));

        // -1 means that rows are not limited at all
        int maxRows = -1;
        if (m_limitRows.getBooleanValue()) {
            maxRows = m_maxNumRows.getIntValue();
        }
        String[] includes = m_learningColumns.applyTo(inTable.getDataTableSpec()).getIncludes();

        if(includes.length == 0) {
            throw new InvalidSettingsException("No learning columns are selected.");
        }

        outPort.addModelTranslater(new PMMLKNNTranslator(inTable, maxRows, m_numNeighbors.getIntValue(), includes));
        return new PortObject[]{outPort};
    }

    /**
     * @param dataTableSpec
     * @return
     * @throws InvalidSettingsException when the input table contains invalid columns
     */
    private PMMLPortObjectSpec createSpec(final DataTableSpec dataTableSpec) throws InvalidSettingsException {
        List<DataColumnSpec> learningColumns = new ArrayList<DataColumnSpec>();
        DataTableSpecCreator dataDictCreator = new DataTableSpecCreator();

        String[] selectedColumns = m_learningColumns.applyTo(dataTableSpec).getIncludes();
        if (selectedColumns.length == 0) {
            throw new InvalidSettingsException("No learning columns are selected.");
        }

        for (String lc : selectedColumns) {
            DataColumnSpec cs = dataTableSpec.getColumnSpec(lc);
            dataDictCreator.addColumns(cs);
            learningColumns.add(cs);
        }

        if (m_predColumnName.getStringValue() == null
                || !dataTableSpec.containsName(m_predColumnName.getStringValue())) {
            for (int i = 0; i < dataTableSpec.getNumColumns(); i++) {
                DataColumnSpec cspec = dataTableSpec.getColumnSpec(i);
                if (cspec.getType().isCompatible(StringValue.class)) {
                    m_predColumnName.setStringValue(cspec.getName());
                    setWarningMessage("No target column selected. Using \"" + cspec.getName() + "\".");
                    break;
                }
            }
        }

        if (m_predColumnName.getStringValue() == null) {
            throw new InvalidSettingsException("The table does not contain a suitable target column.");
        }

        dataDictCreator.addColumns(dataTableSpec.getColumnSpec(m_predColumnName.getStringValue()));

        PMMLPortObjectSpecCreator specCreator = new PMMLPortObjectSpecCreator(dataDictCreator.createSpec());
        specCreator.setTargetCol(dataTableSpec.getColumnSpec(m_predColumnName.getStringValue()));
        specCreator.setLearningCols(learningColumns);
        return specCreator.createSpec();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        return new PortObjectSpec[] {createSpec((DataTableSpec)inSpecs[0])};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_predColumnName.saveSettingsTo(settings);
        m_maxNumRows.saveSettingsTo(settings);
        m_numNeighbors.saveSettingsTo(settings);
        m_learningColumns.saveSettingsTo(settings);
        m_limitRows.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_predColumnName.loadSettingsFrom(settings);
        m_maxNumRows.loadSettingsFrom(settings);
        m_numNeighbors.loadSettingsFrom(settings);
        m_learningColumns.loadSettingsFrom(settings);
        m_limitRows.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_predColumnName.validateSettings(settings);
        m_maxNumRows.validateSettings(settings);
        m_numNeighbors.validateSettings(settings);
        m_learningColumns.validateSettings(settings);
        m_limitRows.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

}

