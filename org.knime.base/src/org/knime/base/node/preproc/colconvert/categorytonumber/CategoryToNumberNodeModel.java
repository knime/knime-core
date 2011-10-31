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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   25.08.2011 (hofer): created
 */
package org.knime.base.node.preproc.colconvert.categorytonumber;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.ColumnRearranger;
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
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.port.pmml.PMMLPortObjectSpecCreator;
import org.knime.core.node.port.pmml.preproc.DerivedFieldMapper;

/**
 * The {@link NodeModel} of the Category2Number node.
 *
 * @author Heiko Hofer
 */
public class CategoryToNumberNodeModel extends NodeModel {
    private final CategoryToNumberNodeSettings m_settings;
    private final List<CategoryToNumberCellFactory> m_factories;

    /** Create a new instance. */
    CategoryToNumberNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE,
                    new PortType(PMMLPortObject.class, true)},
                new PortType[]{BufferedDataTable.TYPE,
                    new PortType(PMMLPortObject.class, true)});
        m_settings = new CategoryToNumberNodeSettings();
        m_factories = new ArrayList<CategoryToNumberCellFactory>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        DataTableSpec inSpec = (DataTableSpec)inSpecs[0];
        List<String> inputCols = new ArrayList<String>();
        for (DataColumnSpec column : inSpec) {
            if (column.getType().isCompatible(StringValue.class)) {
                inputCols.add(column.getName());
            }
        }

        // Auto configuration when included columns are not set
        if (null == m_settings.getIncludedColumns()) {
            // when there is no column with nominal data
            if (inputCols.isEmpty()) {
                throw new InvalidSettingsException("No column in "
                        + "the input compatible to \"StringValue\".");
            } else {
                // auto-configure
                m_settings.setIncludedColumns(inputCols.toArray(
                        new String[inputCols.size()]));
            }
        } else {
            if (!m_settings.getIncludeAll()) {
                if (m_settings.getIncludedColumns().length == 0) {
                    setWarningMessage("No columns selected.");
                }
                List<String> included = new ArrayList<String>();
                included.addAll(Arrays.asList(m_settings
                        .getIncludedColumns()));
                included.removeAll(inputCols);
                if (!included.isEmpty()) {
                    // output first missing column
                    throw new InvalidSettingsException(
                            "Missing column: " + included.get(0).toString());
                }
            } else {
                if (inputCols.isEmpty()) {
                    throw new InvalidSettingsException("No column in "
                            + "the input compatible to \"StringValue\".");
                } else {
                    // automatically add all columns
                    m_settings.setIncludedColumns(inputCols.toArray(
                            new String[inputCols.size()]));
                }
            }
        }

        ColumnRearranger rearranger = createRearranger(inSpec);

        PMMLPortObjectSpec pmmlSpec = (PMMLPortObjectSpec)inSpecs[1];
        PMMLPortObjectSpecCreator pmmlSpecCreator
            = new PMMLPortObjectSpecCreator(pmmlSpec, inSpec);
        pmmlSpecCreator.addPreprocColNames(inputCols);
        return new PortObjectSpec[]{rearranger.createSpec(),
                pmmlSpecCreator.createSpec()};

    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inObjects,
            final ExecutionContext exec) throws Exception {

        if (m_settings.getIncludedColumns().length == 0) {
            // nothing to convert, let's return the input table.
            setWarningMessage("No columns selected,"
                    + " returning input.");
        }
        BufferedDataTable inData = (BufferedDataTable)inObjects[0];
        DataTableSpec inSpec = (DataTableSpec)inObjects[0].getSpec();

        ColumnRearranger rearranger = createRearranger(inSpec);
        BufferedDataTable outTable =
            exec.createColumnRearrangeTable(inData, rearranger, exec);

        // the optional PMML in port (can be null)
        PMMLPortObject inPMMLPort = (PMMLPortObject)inObjects[1];

        PMMLPortObjectSpecCreator creator = new PMMLPortObjectSpecCreator(
                inPMMLPort, rearranger.createSpec());

        PMMLPortObject outPMMLPort = new PMMLPortObject(
               creator.createSpec(), inPMMLPort);
        for (CategoryToNumberCellFactory factory : m_factories) {
            PMMLMapValuesTranslator trans = new PMMLMapValuesTranslator(
                    factory.getConfig(), new DerivedFieldMapper(inPMMLPort));
            outPMMLPort.addGlobalTransformations(trans.exportToTransDict());
        }
        return new PortObject[] {outTable, outPMMLPort};
    }

    private ColumnRearranger createRearranger(final DataTableSpec spec) {
        m_factories.clear();
        ColumnRearranger rearranger = new ColumnRearranger(spec);
        for (String colName : m_settings.getIncludedColumns()) {
            CategoryToNumberCellFactory cellFactory =
                new CategoryToNumberCellFactory(spec, colName, m_settings);
            if (m_settings.getAppendColumns()) {
                rearranger.append(cellFactory);
            } else {
                rearranger.replace(cellFactory, colName);
            }
            m_factories.add(cellFactory);
        }

        return rearranger;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // The Category2Number node does not have internals
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // The Category2Number node does not have internals
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        new CategoryToNumberNodeSettings().loadSettingsForModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_settings.loadSettingsForModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // The Category2Number node does not have internals
    }

}
