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
 *   21.09.2011 (hofer): created
 */
package org.knime.base.node.preproc.colconvert.categorytonumber;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.dmg.pmml40.DerivedFieldDocument.DerivedField;
import org.dmg.pmml40.MapValuesDocument.MapValues;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;

/**
 * Node model of the Category2Number (Apply) node.
 *
 * @author Heiko Hofer
 */
public class CategoryToNumberApplyNodeModel extends NodeModel {
    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(CategoryToNumberApplyNodeModel.class);

    private final CategoryToNumberApplyNodeSettings m_settings;

    /**
     * Create new instance.
     */
    public CategoryToNumberApplyNodeModel() {
        super(new PortType[] {PMMLPortObject.TYPE, BufferedDataTable.TYPE},
                new PortType[] {PMMLPortObject.TYPE, BufferedDataTable.TYPE});
        m_settings = new CategoryToNumberApplyNodeSettings();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        PMMLPortObjectSpec modelSpec = (PMMLPortObjectSpec) inSpecs[0];

        return new PortObjectSpec[] {modelSpec, null};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws Exception {
        PMMLPortObject model = (PMMLPortObject) inData[0];
        BufferedDataTable table = (BufferedDataTable) inData[1];

        ColumnRearranger rearranger = createRearranger(table.getDataTableSpec(),
                model);
        if (null == rearranger) {
            throw new IllegalArgumentException(
                    "No map values configuration found.");
        }
        BufferedDataTable outTable =
            exec.createColumnRearrangeTable(table, rearranger, exec);

        return new PortObject[] {model, outTable};
    }

    /**
     * Creates a rearranger that processes the derived fields with MapValues
     * in the given model.
     */
    private ColumnRearranger createRearranger(final DataTableSpec spec,
            final PMMLPortObject model) {
        // Retrieve columns with string data in the spec
        Set<String> stringCols = new LinkedHashSet<String>();
        Set<String> otherCols = new LinkedHashSet<String>();
        for (DataColumnSpec colSpec : spec) {
            if (colSpec.getType().isCompatible(StringValue.class)) {
                stringCols.add(colSpec.getName());
            } else {
                otherCols.add(colSpec.getName());
            }
        }
        if (stringCols.isEmpty()) {
            if (null == model) { // during configure
                setWarningMessage("No columns to process.");
            } else { // during execute
                setWarningMessage("No columns to process, returning input.");
            }
        }
        // The map values in the model if present
        Map<String, DerivedField> mapValues = null != model
                ? getMapValues(model)
                : new HashMap<String, DerivedField>();
        // Create rearranger
        ColumnRearranger rearranger = new ColumnRearranger(spec);
        for (String col : mapValues.keySet()) {
            DerivedField derivedField = mapValues.remove(col);
            MapValues map = derivedField.getMapValues();

            // if we are during execute and the source column of
            // this PMML MapValues model is found but has wrong type.
            if (null != model && otherCols.contains(col)) {
                String outColumn =
                    null == derivedField.getDisplayName()
                    || derivedField.getDisplayName().trim().isEmpty()
                    ? derivedField.getName()
                    : derivedField.getDisplayName();
                LOGGER.warn("Cannot create column \""
                        + outColumn + "\" since the input column \""
                        + col + "\" is not of type StringValue.");
                continue;
            }
            // if we are during execute and the source column is not found for
            // this PMML MapValues model.
            if (null != model && !stringCols.contains(col)) {
                String outColumn =
                    null == derivedField.getDisplayName()
                    || derivedField.getDisplayName().trim().isEmpty()
                    ? derivedField.getName()
                    : derivedField.getDisplayName();
                LOGGER.warn("Cannot create column \""
                        + outColumn + "\" since the column \""
                        + col + "\" is not in the input.");
                continue;
            }
            CategoryToNumberApplyCellFactory factory =
                new CategoryToNumberApplyCellFactory(spec,
                        col, m_settings, map);
            if (m_settings.getAppendColumns()) {
                rearranger.append(factory);
            } else {
                rearranger.replace(factory, col);
            }
        }

        return rearranger;
    }

    /**
     * @param model the PMML model
     * @return the field in the first FieldColumnPair of the MapValues mapped
     * to the MapValues Model
     */
    private Map<String, DerivedField> getMapValues(final PMMLPortObject model) {
        Map<String, DerivedField> mapValues =
            new HashMap<String, DerivedField>();
        DerivedField[] derivedFields = model.getDerivedFields();
        for (DerivedField derivedField : derivedFields) {
            MapValues map = derivedField.getMapValues();
            if (null != map) {
                // This is the field name the mapValues is based on
                String name = map.getFieldColumnPairArray()[0].getField();
                mapValues.put(name, derivedField);
            }
        }
        return mapValues;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // no internals
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // no internals
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
        new CategoryToNumberApplyNodeSettings().loadSettingsForModel(settings);
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
        // no internals
    }

}
