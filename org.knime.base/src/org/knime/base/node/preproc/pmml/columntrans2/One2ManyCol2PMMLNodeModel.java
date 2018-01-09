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
 * ---------------------------------------------------------------------
 *
 */
package org.knime.base.node.preproc.pmml.columntrans2;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.knime.base.node.preproc.columntrans2.One2ManyCellFactory;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.NominalValue;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.port.pmml.PMMLPortObjectSpecCreator;
import org.knime.core.node.port.pmml.preproc.DerivedFieldMapper;

/**
 * This node converts one column to many columns, such that each possible value
 * becomes an extra column with the value 1 if the row contains this value in
 * the original column and 0 otherwise.
 *
 * @author Fabian Dill, University of Konstanz
 */
public class One2ManyCol2PMMLNodeModel extends NodeModel {

    /** Config key for the columns 2 be transformed. */
    public static final String CFG_COLUMNS = "columns2Btransformed";

    /** Config key which stores if the source columns should be removed. */
    public static final String CFG_REMOVESOURCES = "removeSources";

    @SuppressWarnings("unchecked")
    private final SettingsModelColumnFilter2 m_includedColumns = new SettingsModelColumnFilter2(CFG_COLUMNS,
        NominalValue.class);


    // if several columns should be converted and the possible values
    // of them overlap the original column names is appended to
    // distinguish them
    private boolean m_appendOrgColName = false;

    private boolean m_removeSources = false;

    private final boolean m_pmmlOutEnabled;

    private final boolean m_pmmlInEnabled;

    /**
     * @param pmmlEnabled if PMML support should be enabled or not
     */
    public One2ManyCol2PMMLNodeModel(final boolean pmmlEnabled) {
        this(pmmlEnabled, pmmlEnabled);
    }

    /**
     * @param pmmlOutEnabled if PMML output should be enabled or not
     * @param pmmlInEnabled if optional PMML input should be enabled or not
     */
    public One2ManyCol2PMMLNodeModel(final boolean pmmlOutEnabled, final boolean pmmlInEnabled) {
        super(pmmlInEnabled ? new PortType[]{BufferedDataTable.TYPE, PMMLPortObject.TYPE_OPTIONAL}
                        : new PortType[]{BufferedDataTable.TYPE},
                pmmlOutEnabled ? new PortType[]{BufferedDataTable.TYPE, PMMLPortObject.TYPE}
                        : new PortType[]{BufferedDataTable.TYPE});
        m_pmmlOutEnabled = pmmlOutEnabled;
        m_pmmlInEnabled = pmmlInEnabled;
    }

    /**
     * Creates a new PMML-enabled node model.
     */
    public One2ManyCol2PMMLNodeModel() {
        this(true);
    }




    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_includedColumns.saveSettingsTo(settings);
        settings.addBoolean(CFG_REMOVESOURCES, m_removeSources);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_includedColumns.validateSettings(settings);
        // added in 2.11
        //settings.getBoolean(CFG_REMOVESOURCES);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_includedColumns.loadSettingsFrom(settings);
        // added in 2.11
        m_removeSources = settings.getBoolean(CFG_REMOVESOURCES, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inObjects,
            final ExecutionContext exec) throws Exception {
        BufferedDataTable inData = (BufferedDataTable)inObjects[0];
        DataTableSpec dts = inData.getDataTableSpec();
        checkColumnsSpecs(dts);

        String[] includes = m_includedColumns.applyTo(dts).getIncludes();

        One2ManyCellFactory cellFactory = new One2ManyCellFactory(
                dts, Arrays.asList(includes), m_appendOrgColName);
        BufferedDataTable outData =
                exec.createColumnRearrangeTable(inData, createRearranger(dts, cellFactory), exec);

        if (m_pmmlOutEnabled) {
            // the optional PMML in port (can be null)
            PMMLPortObject inPMMLPort = m_pmmlInEnabled ? (PMMLPortObject)inObjects[1] : null;
            PMMLOne2ManyTranslator trans = new PMMLOne2ManyTranslator(
                    cellFactory.getColumnMapping(), new DerivedFieldMapper(inPMMLPort));
            PMMLPortObjectSpecCreator creator = new PMMLPortObjectSpecCreator(
                    inPMMLPort, outData.getDataTableSpec());
            PMMLPortObject outPMMLPort = new PMMLPortObject(creator.createSpec(), inPMMLPort);
            outPMMLPort.addGlobalTransformations(trans.exportToTransDict());

            return new PortObject[] {outData, outPMMLPort};
        } else {
            return new PortObject[] {outData};
        }
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
        DataTableSpec inDataSpec = (DataTableSpec)inSpecs[0];
        m_appendOrgColName = false;
        String[] includes = m_includedColumns.applyTo(inDataSpec).getIncludes();

        if (includes.length <= 0) {
            setWarningMessage(
                    "No columns to transfrom selected. Will have no effect!");
        }
        // check if the values are present in the current spec
        if (includes.length > 0) {
            checkColumnsSpecs(inDataSpec);
        }
        CellFactory cellFactory = new One2ManyCellFactory(
                inDataSpec, Arrays.asList(includes),
                m_appendOrgColName);
        ColumnRearranger rearranger = createRearranger(inDataSpec, cellFactory);

        if (m_pmmlOutEnabled) {
            PMMLPortObjectSpec pmmlSpec = m_pmmlInEnabled ? (PMMLPortObjectSpec)inSpecs[1] : null;
            PMMLPortObjectSpecCreator pmmlSpecCreator
                = new PMMLPortObjectSpecCreator(pmmlSpec, inDataSpec);
            return new PortObjectSpec[]{rearranger.createSpec(),
                    pmmlSpecCreator.createSpec()};
        } else {
            return new PortObjectSpec[]{rearranger.createSpec()};
        }
    }

    private void checkColumnsSpecs(final DataTableSpec spec)
            throws InvalidSettingsException {
        Set<String> allPossibleValues = new HashSet<String>();
        for (final String colName : m_includedColumns.applyTo(spec).getIncludes()) {
            if (spec.findColumnIndex(colName) < 0) {
                throw new InvalidSettingsException("Column " + colName
                        + " not found in input table");
            }
            if (!spec.getColumnSpec(colName).getDomain().hasValues()) {
                throw new InvalidSettingsException("column: " + colName
                        + " has no possible values");
            }
            Set<String> possibleValues = new HashSet<String>();
            for (DataCell dc : spec.getColumnSpec(colName).getDomain()
                    .getValues()) {
                possibleValues.add(dc.toString());
            }
            Set<String> duplicateTest = new HashSet<String>();
            duplicateTest.addAll(possibleValues);
            duplicateTest.retainAll(allPossibleValues);
            if (duplicateTest.size() > 0) {
                // there are elements in both
                setWarningMessage("Duplicate possible values found."
                        + " Original column name will be appended");
                m_appendOrgColName = true;
            }
            allPossibleValues.addAll(possibleValues);
        }
    }


    private ColumnRearranger createRearranger(final DataTableSpec spec,
            final CellFactory cellFactory) {
        ColumnRearranger rearranger = new ColumnRearranger(spec);
        if (m_removeSources) {
            List<String> includes = Arrays.asList(m_includedColumns.applyTo(spec).getIncludes());
            for (DataColumnSpec s : spec) {
                if (includes.contains(s.getName())) {
                    rearranger.remove(s.getName());
                }
            }
        }
        rearranger.append(cellFactory);
        return rearranger;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) {
        //nothing to do
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) {
        //nothing to do
    }

}
