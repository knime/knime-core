package org.knime.base.node.preproc.pmml.missingval.compute;

import java.io.File;
import java.io.IOException;

import org.knime.base.node.preproc.pmml.missingval.MVSettings;
import org.knime.base.node.preproc.pmml.missingval.MissingCellReplacingDataTable;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.DataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSpecCreator;

/**
 * This is the model implementation of CompiledModelReader.
 *
 * @author Alexander Fillbrunn
 * @since 3.5
 * @noreference This class is not intended to be referenced by clients.
 */
public class MissingValueHandlerNodeModel extends NodeModel {

    /**
     * Constructor for the node model.
     */
    protected MissingValueHandlerNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE}, new PortType[]{BufferedDataTable.TYPE, PMMLPortObject.TYPE});
    }

    private MVSettings m_settings = new MVSettings();

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {

        BufferedDataTable inTable = (BufferedDataTable)inData[0];
        DataTableSpec inSpec = inTable.getDataTableSpec();

        MissingCellReplacingDataTable mvTable = new MissingCellReplacingDataTable(inSpec, m_settings);

        // Calculate the statistics
        exec.setMessage("Calculating statistics");
        mvTable.init(inTable, exec.createSubExecutionContext(0.5));

        long rowCounter = 0;
        final long numOfRows = inTable.size();

        DataContainer container = exec.createDataContainer(mvTable.getDataTableSpec());
        ExecutionContext tableSubExec = exec.createSubExecutionContext(0.4);
        exec.setMessage("Replacing missing values");
        for (DataRow row : mvTable) {
            tableSubExec.checkCanceled();
            if(row != null) {
                tableSubExec.setProgress(++rowCounter / (double)numOfRows,
                    "Processed row " + rowCounter + "/" + numOfRows + " (\"" + row.getKey() + "\")");
                container.addRowToTable(row);
            } else {
                tableSubExec.setProgress(++rowCounter / (double)numOfRows,
                    "Processed row " + rowCounter + "/" + numOfRows);
            }
        }
        container.close();

        // Collect warning messages
        String warnings = mvTable.finish();

        // Handle the warnings
        if (warnings.length() > 0) {
            setWarningMessage(warnings);
        }

        exec.setMessage("Generating PMML");
        // Init PMML output port
        PMMLPortObject pmmlPort = new PMMLPortObject(new PMMLPortObjectSpecCreator(inSpec).createSpec());
        pmmlPort.addModelTranslater(mvTable.getPMMLTranslator());

        return new PortObject[]{(BufferedDataTable)container.getTable(), pmmlPort};
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
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        m_settings.configure((DataTableSpec)inSpecs[0]);
        MissingCellReplacingDataTable mvTable = new MissingCellReplacingDataTable(
                                                        (DataTableSpec)inSpecs[0], m_settings);
        PMMLPortObjectSpecCreator pmmlC = new PMMLPortObjectSpecCreator((DataTableSpec)inSpecs[0]);
        return new PortObjectSpec[]{mvTable.getDataTableSpec(), pmmlC.createSpec()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveToSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.loadSettings(settings, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir, final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir, final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

}
