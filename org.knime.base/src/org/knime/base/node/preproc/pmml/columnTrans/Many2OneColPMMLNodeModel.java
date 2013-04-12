/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 * ------------------------------------------------------------------------
 */
package org.knime.base.node.preproc.pmml.columnTrans;

import java.io.File;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.node.preproc.columnTrans.AbstractMany2OneCellFactory;
import org.knime.base.node.preproc.columnTrans.BinaryCellFactory;
import org.knime.base.node.preproc.columnTrans.Many2OneColNodeModel;
import org.knime.base.node.preproc.columnTrans.MinMaxCellFactory;
import org.knime.base.node.preproc.columnTrans.RegExpCellFactory;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.port.pmml.PMMLPortObjectSpecCreator;


/**
 * This is the model implementation of Many2OneColPMML.
 *
 * @author Alexander Fillbrunn, Universitaet Konstanz
 * @author Tobias Koetter, University of Konstanz
 * @since 2.8
 */
public class Many2OneColPMMLNodeModel extends NodeModel {

    /**
     * Possible methods to let a column match.
     *
     * @author Alexander Fillbrunn, Fabian Dill, University of Konstanz
     */
    public enum IncludeMethod {
        /** 1 matches, 0 not. */
        Binary,
        /** Maximum value of row matches. */
        Maximum,
        /** Minimum value of row matches. */
        Minimum,
        /** Regular Expression pattern matches. */
        RegExpPattern
    }


    /**The port were the  model expects the in data.*/
    public static final int DATA_IN_PORT = 0;

    /**The port which the model uses to return the data.*/
    public static final int DATA_OUT_PORT = 0;

    /**The name of the settings tag which holds the recognition values the user
     *  has entered in the dialog as <code>String</code>.*/
    public static final String RECOGNICTION_REGEX = "many2OneRecoValBox";

    /**The name of the settings tag which holds the names of the columns the
     * user has selected in the dialog as <code>String[]</code>.*/
    public static final String SELECTED_COLS = "many2OneCols2Condense";

    /**The name of the settings tag which holds the indices in the select field
     * of the columns the user has selected in the dialog as
     * <code>int[]</code>.*/
    public static final String SELECTED_COLS_IDX = "many2OneCols2CondenseIdx";

    /**The name of the settings tag which holds the new name of the condensed
     * column the user has entered in the dialog as <code>String</code>.*/
    public static final String CONDENSED_COL_NAME = "many2OneCondenseColName";

    /**The name of the settings tag which holds <code>true</code> if the user
     * wants have the selected condensing columns removed as
     * <code>boolean</code>.*/
    public static final String KEEP_COLS = "many2OneRemoveCols";

    /**The name of the settings tag which holds <code>true</code> if the user
     * wants that the node handles multiple occurrences as
     *  <code>boolean</code>.*/
    public static final String ALLOW_MULTIPLE = "many2OneAllowMultiple";

    /**
     * Config key for the method to determine whether the value of a column is
     * selected or not.
     */
    public static final String INCLUDE_METHOD = "includeMethod";

    /**
     * Creates a Settings Model for storing the name of the appended column.
     * @return the settings model
     */
    static SettingsModelString createAppendedColumnNameSettingsModel() {
        return new SettingsModelString(CONDENSED_COL_NAME, "Condensed Column");
    }

    /**
     * Creates a Settings Model for storing the columns that are included in the many-to-one conversion.
     * @return  the settings model
     */
    static SettingsModelFilterString createIncludedColumnsSettingsModel() {
        return new SettingsModelFilterString(SELECTED_COLS);
    }

    /**
     * Creates a Settings Model for storing the include method.
     * @return  the settings model
     */
    static SettingsModelString createIncludeMethodSettingsModel() {
        return new SettingsModelString(INCLUDE_METHOD, IncludeMethod.Binary.name());
    }

    /**
     * Creates a Settings Model for storing the pattern that is used .
     * @return  the settings model
     */
    static SettingsModelString createPatternSettingsModel() {
        return new SettingsModelString(RECOGNICTION_REGEX, "[^0]*");
    }

    /**
     * Creates a Settings Model for storing whether the merged columns should be
     * kept additionally to the appended column.
     * @return  the settings model
     */
    static SettingsModelBoolean createKeepColumnsSettingsModel() {
        return new SettingsModelBoolean(KEEP_COLS, true);
    }

    private final SettingsModelString m_appendedColumnName = createAppendedColumnNameSettingsModel();

    private final SettingsModelFilterString m_includedColumns = createIncludedColumnsSettingsModel();

    private final SettingsModelString m_includeMethod = createIncludeMethodSettingsModel();

    private final SettingsModelString m_pattern = createPatternSettingsModel();

    private final SettingsModelBoolean m_keepColumns = createKeepColumnsSettingsModel();

    private final boolean m_pmmlEnabled;

    /**
     * Constructor for the node model.
     *
     * @param pmmlEnabled true if there should be a pmml in and outport.
     */
    public Many2OneColPMMLNodeModel(final boolean pmmlEnabled) {

        super(pmmlEnabled ? new PortType[]{BufferedDataTable.TYPE, new PortType(PMMLPortObject.class, true)}
                          : new PortType[]{BufferedDataTable.TYPE},
              pmmlEnabled ? new PortType[]{BufferedDataTable.TYPE, new PortType(PMMLPortObject.class, true)}
                          : new PortType[]{BufferedDataTable.TYPE});

        m_pmmlEnabled = pmmlEnabled;

        m_includeMethod.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                m_pattern.setEnabled(
                        m_includeMethod.getStringValue().equals(
                                IncludeMethod.RegExpPattern.name()));
            }
        });
        // initially disable/enable
        m_pattern.setEnabled(m_includeMethod.getStringValue()
                .equals(Many2OneColNodeModel.IncludeMethod.RegExpPattern.name()));
    }

    /**
     * Standard constructor.
     */
    public Many2OneColPMMLNodeModel() {
        this(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inObjects,
            final ExecutionContext exec)
            throws CanceledExecutionException, Exception {
        BufferedDataTable inData = (BufferedDataTable)inObjects[0];

        AbstractMany2OneCellFactory cellFactory = getCellFactory(inData.getDataTableSpec());
        BufferedDataTable outData = exec.createColumnRearrangeTable(
                                                inData,
                                                createRearranger(inData.getDataTableSpec(), cellFactory),
                                                exec);


        if (m_pmmlEnabled) {
            // the optional PMML in port (can be null)
            PMMLPortObject inPMMLPort = (PMMLPortObject)inObjects[1];

            /*
             PMMLOne2ManyTranslator trans = new PMMLOne2ManyTranslator(
                    cellFactory.getColumnMapping(),
                    new DerivedFieldMapper(inPMMLPort));
            */
            int[] sourceColIndices = cellFactory.getIncludedColIndices();
            String[] sourceColNames = new String[sourceColIndices.length];

            for (int i = 0; i < sourceColIndices.length; i++) {
                sourceColNames[i] = inData.getDataTableSpec().getColumnSpec(sourceColIndices[i]).getName();
            }

            PMMLMany2OneTranslator trans = new PMMLMany2OneTranslator(
                    cellFactory.getAppendedColumnName(), sourceColNames);

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
        //nothing to reset
    }

    private AbstractMany2OneCellFactory getCellFactory(final DataTableSpec spec) {
        AbstractMany2OneCellFactory cellFactory;
        IncludeMethod method = IncludeMethod.valueOf(m_includeMethod
                .getStringValue());
        int[] includedColIndices = new int[m_includedColumns.
                                           getIncludeList().size()];
        int index = 0;
        for (String colName : m_includedColumns.getIncludeList()) {
            includedColIndices[index++] = spec.findColumnIndex(colName);
        }
        String newColName = DataTableSpec.getUniqueColumnName(spec,
                m_appendedColumnName.getStringValue());
        m_appendedColumnName.setStringValue(newColName);
        if (method.equals(IncludeMethod.RegExpPattern)) {
            cellFactory = new RegExpCellFactory(spec, newColName, includedColIndices, m_pattern.getStringValue());
        } else if (method.equals(IncludeMethod.Minimum)) {
            cellFactory = new MinMaxCellFactory(spec, newColName, includedColIndices, false);
        } else if (method.equals(IncludeMethod.Binary)) {
            cellFactory = new BinaryCellFactory(spec, newColName, includedColIndices);
        } else {
            cellFactory = new MinMaxCellFactory(spec, newColName, includedColIndices, true);
        }
        return cellFactory;
    }

    private ColumnRearranger createRearranger(final DataTableSpec spec, final AbstractMany2OneCellFactory cellFactory) {
        ColumnRearranger rearranger = new ColumnRearranger(spec);
        if (!m_keepColumns.getBooleanValue()) {
            rearranger.remove(cellFactory.getIncludedColIndices());
        }
        rearranger.append(cellFactory);
        return rearranger;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        DataTableSpec inDataSpec = (DataTableSpec)inSpecs[0];
        if (m_includedColumns.getIncludeList().size() <= 0) {
            setWarningMessage("No column selected. Node will have no effect!");
        }
        // if it is not a reg exp it must be double compatible
        if (!m_includeMethod.getStringValue().equals(IncludeMethod.RegExpPattern.name())) {
            for (String colName : m_includedColumns.getIncludeList()) {
                if (!inDataSpec.getColumnSpec(colName).getType().isCompatible(DoubleValue.class)) {
                    throw new InvalidSettingsException(
                            "For selected include method '"
                            + m_includeMethod.getStringValue()
                            + "' only double compatible values are allowed."
                            + " Column '" + colName + "' is not.");
                }
            }
        }
        ColumnRearranger rearranger = createRearranger(inDataSpec, getCellFactory(inDataSpec));
        if (m_pmmlEnabled) {
            PMMLPortObjectSpec pmmlSpec = (PMMLPortObjectSpec)inSpecs[1];
            PMMLPortObjectSpecCreator pmmlSpecCreator = new PMMLPortObjectSpecCreator(pmmlSpec, inDataSpec);
            return new PortObjectSpec[]{rearranger.createSpec(), pmmlSpecCreator.createSpec()};
        } else {
            return new DataTableSpec[] {rearranger.createSpec()};
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_appendedColumnName.saveSettingsTo(settings);
        m_includedColumns.saveSettingsTo(settings);
        m_includeMethod.saveSettingsTo(settings);
        m_pattern.saveSettingsTo(settings);
        m_keepColumns.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_appendedColumnName.loadSettingsFrom(settings);
        m_includedColumns.loadSettingsFrom(settings);
        m_includeMethod.loadSettingsFrom(settings);
        m_pattern.loadSettingsFrom(settings);
        m_keepColumns.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_appendedColumnName.validateSettings(settings);
        m_includedColumns.validateSettings(settings);
        m_includeMethod.validateSettings(settings);
        m_pattern.validateSettings(settings);
        m_keepColumns.validateSettings(settings);
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