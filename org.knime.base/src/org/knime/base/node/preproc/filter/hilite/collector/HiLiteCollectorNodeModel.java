/* 
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * --------------------------------------------------------------------- *
 * 
 * History
 *   16.04.2008 (gabriel): created
 */
package org.knime.base.node.preproc.filter.hilite.collector;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.property.hilite.HiLiteHandler;

/**
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class HiLiteCollectorNodeModel extends NodeModel {
    
    private final Map<RowKey, Map<Integer, String>> m_annotationMap = 
        new LinkedHashMap<RowKey, Map<Integer, String>>();
    
    private Integer m_lastIndex = null;
        
    private static final String KEY_ANNOTATIONS = "annotations.xml.gz";
    
    /**
     * Create new hilite collector model with one data in- and one data 
     * out-port.
     */
    HiLiteCollectorNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE}, 
                new PortType[]{BufferedDataTable.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        return new DataTableSpec[]{null};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, 
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        this.reset();
        File f = new File(nodeInternDir, KEY_ANNOTATIONS);
        NodeSettingsRO sett = NodeSettings.loadFromXML(new FileInputStream(f));
        RowKey[] rowKeys = sett.getRowKeyArray("row_keys", (RowKey[]) null);
        if (rowKeys != null) {
            for (RowKey key : rowKeys) {
                try {
                    NodeSettingsRO subSett = sett.getNodeSettings(
                            key.toString());
                    Map<Integer, String> map = 
                        new LinkedHashMap<Integer, String>();
                    for (String i : subSett.keySet()) {
                        try {
                            int idx = Integer.parseInt(i);
                            m_lastIndex = (m_lastIndex == null 
                                    ? idx : Math.max(m_lastIndex, idx));
                            map.put(idx, subSett.getString(i));
                        } catch (InvalidSettingsException ise) {
                            // ignored
                        }
                    }
                    m_annotationMap.put(key, map);
                } catch (InvalidSettingsException ise) {
                    // ignored
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_annotationMap.clear();
        m_lastIndex = null;
    }

    /**
     * Appends new annotation.
     * @param anno the annotation to append to the current hilit keys
     * @param newColumn true, a new column is created at the end of the table
     *                  holding the current annotation
     */
    final void appendAnnotation(final String anno, final boolean newColumn) {
        HiLiteHandler hdl = getInHiLiteHandler(0);
        if (hdl == null || hdl.getHiLitKeys().isEmpty()) {
            return;
        }
        if (m_lastIndex == null) {
            m_lastIndex = 0;
        } else if (newColumn) {
            m_lastIndex++;
        }
        for (RowKey key : hdl.getHiLitKeys()) {
            Map<Integer, String> list = m_annotationMap.get(key); 
            if (list == null) {
                list = new LinkedHashMap<Integer, String>();
                list.put(m_lastIndex, anno);
            } else {
                String str = list.get(m_lastIndex);
                if (str == null) {
                    list.put(m_lastIndex, anno);
                } else if (!str.contains(anno)) {
                    list.put(m_lastIndex, str + "," + anno);
                }
            }
            m_annotationMap.put(key, list);
        }
        notifyViews(null);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData, 
            final ExecutionContext exec) throws Exception {
        if (m_annotationMap.isEmpty()) {
            return inData;
        }
        DataTableSpec inSpec = (DataTableSpec) inData[0].getSpec();
        final DataColumnSpec[] cspecs = createSpecs(inSpec);
        ColumnRearranger cr = new ColumnRearranger(inSpec);
        cr.append(new CellFactory() {
            /**
             * {@inheritDoc}
             */
            @Override
            public DataCell[] getCells(final DataRow row) {
                if (m_annotationMap.isEmpty()) {
                    return new DataCell[0];
                }
                DataCell[] cells = new DataCell[m_lastIndex + 1];
                for (int i = 0; i < cells.length; i++) {
                    Map<Integer, String> map = 
                        m_annotationMap.get(row.getKey());
                    if (map == null) {
                        cells[i] = DataType.getMissingCell();
                    } else { 
                        String str = map.get(i);
                        if (str == null) {
                            cells[i] = DataType.getMissingCell();
                        } else {
                            cells[i] = new StringCell(str);
                        }
                    }
                }
                return cells;
            }
            @Override
            public DataColumnSpec[] getColumnSpecs() {
                return cspecs;
            }
            /**
             * {@inheritDoc}
             */
            @Override
            public void setProgress(final int curRowNr, final int rowCount, 
                    final RowKey lastKey, final ExecutionMonitor em) {
                em.setProgress((double) curRowNr / rowCount);
            }
            
        });
        return new BufferedDataTable[]{exec.createColumnRearrangeTable(
                (BufferedDataTable) inData[0], cr, exec)};
    }
    
    private DataColumnSpec[] createSpecs(final DataTableSpec spec) {
        final DataColumnSpec[] cspecs = new DataColumnSpec[m_lastIndex + 1];
        int index = 0;
        for (int i = 0; i < cspecs.length; i++) {
            String name = "Anno #" + index;
            while (spec != null && spec.containsName(name)) {
                name = "Anno #" + (++index);
            }
            cspecs[i] = new DataColumnSpecCreator(
                    name, StringCell.TYPE).createSpec();
            index++;
        }
        return cspecs;
    }
        
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, 
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        NodeSettings sett = new NodeSettings(KEY_ANNOTATIONS);
        RowKey[] cells = m_annotationMap.keySet().toArray(
                new RowKey[m_annotationMap.size()]);
        sett.addRowKeyArray("row_keys", cells);
        for (RowKey cell : cells) {
            NodeSettingsWO subSett = sett.addNodeSettings(cell.toString());
            for (Map.Entry<Integer, String> e
                    : m_annotationMap.get(cell).entrySet()) {
                subSett.addString(e.getKey().toString(), e.getValue());
            }
        }
        File f = new File(nodeInternDir, KEY_ANNOTATIONS);
        sett.saveToXML(new FileOutputStream(f));
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
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {

    }
    
    /**
     * @return table with hilit rows first and then all rows with annotations 
     */
    DataTable getHiLiteAnnotationsTable() {
        DataContainer buf;
        if (m_annotationMap.isEmpty()) {
            buf = new DataContainer(new DataTableSpec());
        } else {
            buf = new DataContainer(new DataTableSpec(createSpecs(null)));   
        }
        HiLiteHandler hdl = getInHiLiteHandler(0);
        if (hdl != null) {
            for (RowKey key : hdl.getHiLitKeys()) {
                DataCell[] cells = new DataCell[
                                            buf.getTableSpec().getNumColumns()];
                for (int i = 0; i < cells.length; i++) {
                    Map<Integer, String> map = m_annotationMap.get(key);
                    if (map == null) {
                        cells[i] = DataType.getMissingCell();
                    } else {
                        String str = m_annotationMap.get(key).get(i);
                        if (str == null) {
                            cells[i] = DataType.getMissingCell();
                        } else {
                            cells[i] = new StringCell(str);
                        }
                    }
                }
                buf.addRowToTable(new DefaultRow(key, cells));
            }
            for (RowKey key : m_annotationMap.keySet()) {
                if (!hdl.isHiLit(key)) {
                    DataCell[] cells = new DataCell[
                                            buf.getTableSpec().getNumColumns()];
                    for (int i = 0; i < cells.length; i++) {
                        String str = m_annotationMap.get(key).get(i);
                        if (str == null) {
                            cells[i] = DataType.getMissingCell();
                        } else {
                            cells[i] = new StringCell(str);
                        }
                    }
                    buf.addRowToTable(new DefaultRow(key, cells));
                }
            }
        }
        buf.close();
        return buf.getTable();
    }

}
