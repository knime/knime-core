package org.knime.base.node.preproc.filter.nominal;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.NominalValue;
import org.knime.core.data.container.DataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This is the model implementation of PossibleValueRowFilter. For a nominal
 * column one or more possible values can be selected. If the value in the
 * selected column of a row matches the included possible values the row is
 * added to the included rows at first out port, else to the excluded at second
 * outport.
 *
 *
 * @author KNIME GmbH
 */
public class NominalValueRowFilterNodeModel extends NodeModel {

    private String m_selectedColumn;

    private int m_selectedColIdx;

    private final Set<String> m_selectedAttr = new HashSet<String>();

    /**
     * One inport (data to be filtered) two out ports (included and excluded).
     */
    protected NominalValueRowFilterNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        // include data container
        DataContainer positive =
                exec.createDataContainer(inData[0].getDataTableSpec());
        double currentRow = 0;
        for (DataRow row : inData[0]) {
            // if row matches to included...
            if (matches(row)) {
                positive.addRowToTable(row);
            }
            exec.setProgress(currentRow / inData[0].getRowCount(),
                    "filtering row # " + currentRow);
            currentRow++;
            exec.checkCanceled();
        }
        positive.close();
        BufferedDataTable positiveTable =
                exec.createBufferedDataTable(positive.getTable(), exec);
        if (positiveTable.getRowCount() <= 0) {
            setWarningMessage("No rows matched!");
        }
        return new BufferedDataTable[]{positiveTable};
    }

    /*
     * Check if the value in the selected column is in the selected possible
     * values.
     */
    private boolean matches(final DataRow row) {
        DataCell dc = row.getCell(m_selectedColIdx);
        return m_selectedAttr.contains(dc.toString());
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
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        // check if possible values are available
        int nrValidCols = 0;
        for (DataColumnSpec colSpec : inSpecs[0]) {
            if (colSpec.getType().isCompatible(NominalValue.class)
                    && colSpec.getDomain().hasValues()) {
                nrValidCols++;
            }
        }
        // are there some valid columns (nominal and with possible values)
        if (nrValidCols == 0) {
            throw new InvalidSettingsException(
                    "No nominal columns with possible values found! "
                            + "Execute predecessor or check input table.");
        }
        // all values excluded?
        if (m_selectedColumn != null && m_selectedAttr.size() == 0) {
            setWarningMessage("All values are excluded!"
                    + " Input data will be mirrored at out-port 1 (excluded)");
        }
        if (m_selectedColumn != null && m_selectedColumn.length() > 0) {
            m_selectedColIdx = inSpecs[0].findColumnIndex(m_selectedColumn);
            // selected attribute not found in possible values
            if (m_selectedColIdx < 0) {
                throw new InvalidSettingsException("Column " + m_selectedColumn
                        + " not found in in spec!");
            }
            // all values included?
            if (inSpecs[0].getColumnSpec(m_selectedColIdx).getDomain()
                    .hasValues()) {
                if (inSpecs[0].getColumnSpec(m_selectedColIdx).getDomain()
                        .getValues().size() == m_selectedAttr.size()) {
                    setWarningMessage("All values are included! Input will be "
                            + "mirrored at out-port 0 (included)");
                }
            }
            // if attribute values isn't found in domain also throw exception
            boolean validAttrVal = false;
            for (DataCell dc : inSpecs[0].getColumnSpec(m_selectedColIdx)
                    .getDomain().getValues()) {
                if (m_selectedAttr.contains(dc.toString())) {
                    validAttrVal = true;
                    break;
                }
            }
            if (!validAttrVal && m_selectedAttr.size() > 0) {
                throw new InvalidSettingsException("Selected attribute value "
                        + m_selectedAttr + " not found!");
            }

            // return original spec,
            // only the rows are affected
        }
        return new DataTableSpec[]{inSpecs[0]};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addString(NominalValueRowFilterNodeDialog.CFG_SELECTED_COL,
                m_selectedColumn);
        settings.addStringArray(
                NominalValueRowFilterNodeDialog.CFG_SELECTED_ATTR,
                m_selectedAttr.toArray(new String[m_selectedAttr.size()]));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_selectedColumn = settings.getString(
                        NominalValueRowFilterNodeDialog.CFG_SELECTED_COL);
        String[] selected = settings.getStringArray(
                NominalValueRowFilterNodeDialog.CFG_SELECTED_ATTR);
        m_selectedAttr.clear();
        for (String s : selected) {
            m_selectedAttr.add(s);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_selectedColumn = settings.getString(
                NominalValueRowFilterNodeDialog.CFG_SELECTED_COL);
        String[] selected = settings.getStringArray(
                NominalValueRowFilterNodeDialog.CFG_SELECTED_ATTR);
        m_selectedAttr.clear();
        for (String s : selected) {
            m_selectedAttr.add(s);
        }
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
