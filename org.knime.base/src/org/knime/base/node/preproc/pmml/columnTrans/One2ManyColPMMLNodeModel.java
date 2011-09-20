/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 * ---------------------------------------------------------------------
 *
 */
package org.knime.base.node.preproc.pmml.columnTrans;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.knime.base.node.preproc.columnTrans.One2ManyCellFactory;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
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
public class One2ManyColPMMLNodeModel extends NodeModel {

    /** Config key for the columns 2 be transformed. */
    public static final String CFG_COLUMNS = "columns2Btransformed";

//    private String[] m_includedColumns = new String[0];

    private final SettingsModelFilterString m_includedColumns
        = new SettingsModelFilterString(CFG_COLUMNS);


    // if several columns should be converted and the possible values
    // of them overlap the original column names is appended to
    // distinguish them
    private boolean m_appendOrgColName = false;

    private final boolean m_pmmlEnabled;

    /**
     * @param pmmlEnabled if PMML support should be enabled or not
     */
    public One2ManyColPMMLNodeModel(final boolean pmmlEnabled) {
        super(pmmlEnabled ? new PortType[]{BufferedDataTable.TYPE,
                new PortType(PMMLPortObject.class, true)}
                : new PortType[]{BufferedDataTable.TYPE},
                pmmlEnabled ? new PortType[]{BufferedDataTable.TYPE,
                        new PortType(PMMLPortObject.class, true)}
                        : new PortType[]{BufferedDataTable.TYPE});
        m_pmmlEnabled = pmmlEnabled;
    }

    /**
     * Creates a new PMML-enabled node model.
     */
    public One2ManyColPMMLNodeModel() {
        this(true);
    }




    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_includedColumns.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_includedColumns.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_includedColumns.loadSettingsFrom(settings);
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

        One2ManyCellFactory cellFactory = new One2ManyCellFactory(
                dts, m_includedColumns.getIncludeList(),
                m_appendOrgColName);
        BufferedDataTable outData =
                exec.createColumnRearrangeTable(inData,
                        createRearranger(dts, cellFactory), exec);

        if (m_pmmlEnabled) {
            // the optional PMML in port (can be null)
            PMMLPortObject inPMMLPort = (PMMLPortObject)inObjects[1];
            PMMLOne2ManyTranslator trans = new PMMLOne2ManyTranslator(
                    cellFactory.getColumnMapping(),
                    new DerivedFieldMapper(inPMMLPort));
            PMMLPortObjectSpecCreator creator = new PMMLPortObjectSpecCreator(
                    inPMMLPort, outData.getDataTableSpec());
            PMMLPortObject outPMMLPort = new PMMLPortObject(
                   creator.createSpec(), inPMMLPort);
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
        if (m_includedColumns.getIncludeList() == null
                || m_includedColumns.getIncludeList().size() <= 0) {
            setWarningMessage(
                    "No columns to transfrom selected. Will have no effect!");
        }
        // check if the values are present in the current spec
        if (m_includedColumns.getIncludeList() != null
                && m_includedColumns.getIncludeList().size() > 0) {
            checkColumnsSpecs(inDataSpec);
        }
        CellFactory cellFactory = new One2ManyCellFactory(
                inDataSpec, m_includedColumns.getIncludeList(),
                m_appendOrgColName);
        ColumnRearranger rearranger = createRearranger(inDataSpec, cellFactory);
        
        if (m_pmmlEnabled) {
            PMMLPortObjectSpec pmmlSpec = (PMMLPortObjectSpec)inSpecs[1];
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
        for (final String colName : m_includedColumns.getIncludeList()) {
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
