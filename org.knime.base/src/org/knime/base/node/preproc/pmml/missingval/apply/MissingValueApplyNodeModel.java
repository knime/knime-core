package org.knime.base.node.preproc.pmml.missingval.apply;

import java.io.File;
import java.io.IOException;

import org.dmg.pmml.PMMLDocument;
import org.knime.base.node.preproc.pmml.missingval.MissingCellReplacingDataTable;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.util.AutocloseableSupplier;
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
import org.w3c.dom.Document;

/**
 * This is the model implementation of CompiledModelReader.
 *
 *
 * @author Alexander Fillbrunn
 */
public class MissingValueApplyNodeModel extends NodeModel {

    private static final int PMML_PORT_IDX = 0;

    private static final int DATA_PORT_IDX = 1;

    /**
     * Constructor for the node model.
     */
    protected MissingValueApplyNodeModel() {
        super(new PortType[]{PMMLPortObject.TYPE, BufferedDataTable.TYPE}, new PortType[]{BufferedDataTable.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {

        BufferedDataTable inTable = (BufferedDataTable)inData[DATA_PORT_IDX];
        DataTableSpec inSpec = inTable.getDataTableSpec();

        PMMLPortObject pmmlIn = (PMMLPortObject)inData[PMML_PORT_IDX];
        MissingCellReplacingDataTable mvTable = null;

        try (AutocloseableSupplier<Document> supplier = pmmlIn.getPMMLValue().getDocumentSupplier()) {
            mvTable = new MissingCellReplacingDataTable(inSpec, PMMLDocument.Factory.parse(supplier.get()));
        }

        // Calculate the statistics
        mvTable.init(inTable, exec.createSubExecutionContext(0.5));

        long rowCounter = 0;
        final long numOfRows = inTable.size();
        DataContainer container = exec.createDataContainer(mvTable.getDataTableSpec());

        for (DataRow row : mvTable) {
            exec.checkCanceled();
            if(row != null) {
                exec.setProgress(++rowCounter / (double)numOfRows,
                    "Processed row " + rowCounter + "/" + numOfRows + " (\"" + row.getKey() + "\")");
                container.addRowToTable(row);
            } else {
                exec.setProgress(++rowCounter / (double)numOfRows,
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

        return new PortObject[]{(BufferedDataTable)container.getTable()};
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
        return new PortObjectSpec[]{null};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
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
