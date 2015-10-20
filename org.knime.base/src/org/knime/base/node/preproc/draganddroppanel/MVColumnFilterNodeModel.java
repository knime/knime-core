/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ------------------------------------------------------------------------
 *
 * History
 *   Aug 7, 2010 (wiswedel): created
 */
package org.knime.base.node.preproc.draganddroppanel;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.util.Pair;

/**
 * This is the model implementation of CellReplacer. Replaces cells in a column according to dictionary table (2nd
 * input)
 *
 * @author Bernd Wiswedel
 */
public class MVColumnFilterNodeModel extends NodeModel {

    private long[] percentages;

    private DNDSelectionConfiguration conf;


    /**
     *
     */
    protected MVColumnFilterNodeModel() {
        super(1, 1);
        conf = new DNDSelectionConfiguration(new ConfigurationDialogFactory());
        new ArrayList<Pair<List<String>,PaneConfigurationDialog>>();
        new ArrayList<Integer>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {

        conf.saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        conf.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {


        conf.loadValidatedSettingsFrom(settings);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec) throws Exception {
        int[] missingCount = new int[inData[0].getDataTableSpec().getNumColumns()];
        boolean stop = true;
        double rowCount = inData[0].size();
        long processedRows = 0;
        for (DataRow row : inData[0]) {
            exec.setProgress(processedRows++/rowCount);

            for (int i = 0; i < row.getNumCells(); i++) {
                if (percentages[i] > 0) {
                    stop = false;
                    if (row.getCell(i).isMissing()) {
                        missingCount[i]++;
                    }
                }
            }
            if (stop) {
                break;
            } else {
                stop = true;
            }
        }

        ColumnRearranger r = new ColumnRearranger(inData[0].getDataTableSpec());
        int alreadyRemoved = 0;
        for (int i = 0; i < percentages.length; i++) {
            if (percentages[i] > 0) {
                if (((double)missingCount[i] / inData[0].size())*100 >= percentages[i]) {
                r.remove(i - alreadyRemoved++);
                }
            }
        }

        return new BufferedDataTable[]{exec.createColumnRearrangeTable(inData[0], r, exec)};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {

        percentages = new long[inSpecs[0].getNumColumns()];

        List<Pair<String, PaneConfigurationDialog>> indata = conf.configure(inSpecs[0]);
        for (int i = 0; i < indata.size(); i++) {

            String colName = indata.get(i).getFirst();
            if (inSpecs[0].containsName(colName)) {
                percentages[inSpecs[0].findColumnIndex(colName)] =
                    ((PercentageDialog)indata.get(i).getSecond()).getPercentage();
            }

        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // TODO Auto-generated method stub

    }

}
