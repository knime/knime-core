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
 * -------------------------------------------------------------------
 *
 * History
 *    17.10.2008 (Tobias Koetter): created
 */

package org.knime.base.node.preproc.ungroup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.property.hilite.DefaultHiLiteMapper;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.property.hilite.HiLiteTranslator;
import org.knime.core.node.util.filter.NameFilterConfiguration.FilterResult;

/**
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class UngroupNodeModel extends NodeModel {

    /** The config key of the collections column names. */
    private static final String CFG_COL_NAMES = "columnNames";

    private final SettingsModelColumnFilter2 m_collCols = createCollectionColsModel();

    private final SettingsModelString m_columnName = createColumnModel();

    private final SettingsModelBoolean m_removeCollectionCol = createRemoveCollectionColModel();

    private final SettingsModelBoolean m_skipMissingVal = createSkipMissingValModel();

    private final SettingsModelBoolean m_enableHilite = createEnableHiliteModel();

    /**
     * Node returns a new hilite handler instance.
     */
    private HiLiteTranslator m_trans;

    private final HiLiteHandler m_hilite = new HiLiteHandler();

    /**
     * Constructor for class AppenderNodeModel.
     *
     */
    public UngroupNodeModel() {
        super(1, 1);
    }

    /**
     * @return the collection columns model
     */
    @SuppressWarnings("unchecked")
    static SettingsModelColumnFilter2 createCollectionColsModel() {
        return new SettingsModelColumnFilter2(CFG_COL_NAMES, CollectionDataValue.class);
    }

    /**
     * @return the enable hilite translation model
     */
    static SettingsModelBoolean createEnableHiliteModel() {
        return new SettingsModelBoolean("enableHilite", false);
    }

    /**
     * @return the ignore missing value model
     */
    static SettingsModelBoolean createSkipMissingValModel() {
        return new SettingsModelBoolean("skipMissingValues", false);
    }

    /**
     * @return the remove collection column model
     */
    static SettingsModelBoolean createRemoveCollectionColModel() {
        return new SettingsModelBoolean("removeCollectionCol", true);
    }

    /**
     * @return the column name settings model
     */
    private static SettingsModelString createColumnModel() {
        return new SettingsModelString("columnName", null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        final DataTableSpec spec = inSpecs[0];
        final DataTableSpec resultSpec = compatibleCreateResultSpec(spec);
        return new DataTableSpec[]{resultSpec};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {

        UngroupOperation ugO =
            new UngroupOperation(m_enableHilite.getBooleanValue(), m_skipMissingVal.getBooleanValue(),
                m_removeCollectionCol.getBooleanValue());

        if (inData == null || inData.length != 1) {
            throw new InvalidSettingsException("Invalid input data");
        }
        final BufferedDataTable table = inData[0];
        ugO.setTable(table);
        int[] colIdxs = compatibleGetSelectedColIds(table);
        if (colIdxs == null || colIdxs.length <= 0) {
            setWarningMessage("No ungroup column selected. Node returns input table.");
            return inData;
        }
        ugO.setColIndices(colIdxs);

        final DataTableSpec newSpec = compatibleCreateResultSpec(table.getDataTableSpec());
        ugO.setNewSpec(newSpec);

        ugO.setTrans(m_trans);

        BufferedDataTable[] result = new BufferedDataTable[]{ugO.compute(exec)};

        m_trans = ugO.getTrans();

        return result;
    }

    private int[] getSelectedColIdxs(final DataTableSpec spec, final String... colNames)
        throws InvalidSettingsException {
        final int[] idxs = new int[colNames.length];
        for (int i = 0, length = colNames.length; i < length; i++) {
            final String name = colNames[i];
            idxs[i] = spec.findColumnIndex(name);
            if (idxs[i] < 0) {
                throw new InvalidSettingsException("Column with name " + name + " not found in input table");
            }
        }
        return idxs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        if (m_trans != null) {
            m_trans.setMapper(null);
        }
        m_hilite.fireClearHiLiteEvent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setInHiLiteHandler(final int inIndex, final HiLiteHandler hiLiteHdl) {
        if (m_trans != null && m_trans.getFromHiLiteHandler() != hiLiteHdl) {
            m_trans.removeAllToHiliteHandlers();
            m_trans.setMapper(null);
            m_trans = null;
        }
        if (hiLiteHdl != null) {
            m_trans = new HiLiteTranslator(hiLiteHdl);
            m_trans.addToHiLiteHandler(m_hilite);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected HiLiteHandler getOutHiLiteHandler(final int outIndex) {
        return m_hilite;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_removeCollectionCol.validateSettings(settings);
        m_skipMissingVal.validateSettings(settings);
        m_enableHilite.validateSettings(settings);
        if (settings.containsKey(CFG_COL_NAMES)) {
            //this option has been introduced in KNIME 2.8
            m_collCols.validateSettings(settings);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_removeCollectionCol.loadSettingsFrom(settings);
        m_skipMissingVal.loadSettingsFrom(settings);
        m_enableHilite.loadSettingsFrom(settings);
        try {
            // this option has been introduced in KNIME 2.8
            m_collCols.loadSettingsFrom(settings);
            //set the old column name setting to null to indicate that the new settings should be used
            m_columnName.setStringValue(null);
        } catch (InvalidSettingsException e) {
            //load and use the old settings
            m_columnName.loadSettingsFrom(settings);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_removeCollectionCol.saveSettingsTo(settings);
        m_skipMissingVal.saveSettingsTo(settings);
        m_enableHilite.saveSettingsTo(settings);
        m_collCols.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        if (m_enableHilite.getBooleanValue()) {
            final NodeSettings config = new NodeSettings("hilite_mapping");
            final DefaultHiLiteMapper mapper = (DefaultHiLiteMapper)m_trans.getMapper();
            if (mapper != null) {
                //the mapper is null if the node produces an empty data table
                mapper.save(config);
            }
            config.saveToXML(new GZIPOutputStream(
                new FileOutputStream(new File(nodeInternDir, "hilite_mapping.xml.gz"))));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        if (m_enableHilite.getBooleanValue()) {
            final NodeSettingsRO config =
                NodeSettings.loadFromXML(new GZIPInputStream(new FileInputStream(new File(nodeInternDir,
                    "hilite_mapping.xml.gz"))));
            try {
                m_trans.setMapper(DefaultHiLiteMapper.load(config));
            } catch (final InvalidSettingsException ex) {
                throw new IOException(ex.getMessage());
            }
        }
    }

    /**
     * @param table
     * @return
     * @throws InvalidSettingsException
     */
    private int[] compatibleGetSelectedColIds(final BufferedDataTable table) throws InvalidSettingsException {
        final DataTableSpec spec = table.getDataTableSpec();
        final String[] columnNames;
        if (m_columnName.getStringValue() == null) {
            //the column filter has been introduced in KNIME 2.8
            final FilterResult filterResult = m_collCols.applyTo(spec);
            columnNames = filterResult.getIncludes();
        } else {
            columnNames = new String[]{m_columnName.getStringValue()};
        }
        return getSelectedColIdxs(spec, columnNames);
    }

    /**
     * @param spec
     * @return
     * @throws InvalidSettingsException
     */
    private DataTableSpec compatibleCreateResultSpec(final DataTableSpec spec) throws InvalidSettingsException {
        final DataTableSpec resultSpec;
        if (m_columnName.getStringValue() == null) {
            //the column filter has been introduced in KNIME 2.8
            final FilterResult filterResult = m_collCols.applyTo(spec);
            String[] colNames = filterResult.getIncludes();
            resultSpec = UngroupOperation.createTableSpec(spec, m_removeCollectionCol.getBooleanValue(), colNames);
        } else {
            resultSpec =
                UngroupOperation.createTableSpec(spec, m_removeCollectionCol.getBooleanValue(),
                    m_columnName.getStringValue());
        }
        return resultSpec;
    }
}
