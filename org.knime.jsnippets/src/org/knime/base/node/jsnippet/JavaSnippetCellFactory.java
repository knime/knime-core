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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   15.12.2011 (hofer): created
 */
package org.knime.base.node.jsnippet;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.knime.base.node.jsnippet.JavaField.InCol;
import org.knime.base.node.jsnippet.JavaField.InVar;
import org.knime.base.node.jsnippet.JavaField.OutCol;
import org.knime.base.node.jsnippet.JavaField.OutVar;
import org.knime.base.node.jsnippet.JavaFieldList.OutColList;
import org.knime.base.node.jsnippet.JavaFieldList.OutVarList;
import org.knime.base.node.jsnippet.expression.Abort;
import org.knime.base.node.jsnippet.expression.AbstractJSnippet;
import org.knime.base.node.jsnippet.expression.Cell;
import org.knime.base.node.jsnippet.expression.TypeException;
import org.knime.base.node.jsnippet.type.TypeProvider;
import org.knime.base.node.jsnippet.type.data.DataValueToJava;
import org.knime.base.node.jsnippet.type.data.JavaToDataCell;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CellFactory;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.FlowVariable.Type;

/**
 * Cell factory for the java snippet node.
 *
 * @author Heiko Hofer
 */
public class JavaSnippetCellFactory implements CellFactory {
    private static final NodeLogger LOGGER = NodeLogger
        .getLogger(JavaSnippetCellFactory.class);

    private JavaSnippet m_snippet;
    private DataTableSpec m_spec;
    private AbstractJSnippet m_jsnippet;
    private FlowVariableRepository m_flowVars;
    private int m_rowIndex;
    private int m_rowCount;
    private List<String> m_columns;


    /**
     * Create a new cell factory.
     * @param snippet the snippet
     * @param spec the spec of the data table at the input
     * @param flowVariableRepository the flow variables at the input
     * @param rowCount the number of rows of the table at the input
     */
    public JavaSnippetCellFactory(final JavaSnippet snippet,
            final DataTableSpec spec,
            final FlowVariableRepository flowVariableRepository,
            final int rowCount) {
        m_snippet = snippet;
        m_spec = spec;
        m_flowVars = flowVariableRepository;
        m_rowIndex = 0;
        m_rowCount = rowCount;
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public DataCell[] getCells(final DataRow row) {
         try {
            if (null == m_jsnippet) {
                m_jsnippet = m_snippet.createSnippetInstance();
                // populate the fields in the m_jsnippet that are constant
                // across the rows.
                Field[] fs =
                    m_jsnippet.getClass().getSuperclass().getDeclaredFields();
                for (Field field : fs) {
                    if (field.getName().equals("m_flowVars")) {
                        field.setAccessible(true);
                        field.set(m_jsnippet, m_flowVars);
                    }
                    if (field.getName().equals(JavaSnippet.ROWCOUNT)) {
                        field.setAccessible(true);
                        field.set(m_jsnippet, m_rowCount);
                    }
                }
            }
            // populate data structure with the input cells
            Map<String, Cell> cellsMap = createCellsMap(row);
            if (null == m_columns) {
                m_columns = new ArrayList<String>();
                m_columns.addAll(cellsMap.keySet());
            }
            Field[] fs =
                m_jsnippet.getClass().getSuperclass().getDeclaredFields();
            for (Field field : fs) {
                if (field.getName().equals("m_cellsMap")) {
                    field.setAccessible(true);
                    field.set(m_jsnippet, cellsMap);
                }
                if (field.getName().equals("m_cells")) {
                    field.setAccessible(true);
                    List<Cell> cells = new ArrayList<Cell>();
                    cells.addAll(cellsMap.values());
                    field.set(m_jsnippet, cells);
                }
                if (field.getName().equals("m_columns")) {
                    field.setAccessible(true);
                    field.set(m_jsnippet, m_columns);
                }
                if (field.getName().equals("m_inSpec")) {
                    field.setAccessible(true);
                    field.set(m_jsnippet, m_spec);
                }
                if (field.getName().equals(JavaSnippet.ROWID)) {
                    field.setAccessible(true);
                    field.set(m_jsnippet, row.getKey().getString());
                }
                if (field.getName().equals(JavaSnippet.ROWINDEX)) {
                    field.setAccessible(true);
                    field.set(m_jsnippet, m_rowIndex);
                }
            }

            // populate the system input column fields with data
            for (InCol inCol : m_snippet.getSystemFields().getInColFields()) {
                Field field = m_jsnippet.getClass().getField(
                        inCol.getJavaName());
                Cell cell = cellsMap.get(inCol.getKnimeName());
                Object v = cell.getValueOfType(inCol.getJavaType());
                if (inCol.getKnimeType().isCollectionType() && v != null) {
                    field.set(m_jsnippet,
                            Arrays.copyOf((Object[]) v, ((Object[]) v).length,
                                    inCol.getJavaType()));
                } else {
                    field.set(m_jsnippet, v);
                }
            }
            // reset the system output fields to null (see also bug 3781)
            for (OutCol outCol : m_snippet.getSystemFields().getOutColFields()) {
                Field field = m_jsnippet.getClass().getField(
                        outCol.getJavaName());
                field.set(m_jsnippet, null);
            }
            // populate the system input flow variable fields with data
            for (InVar inCol : m_snippet.getSystemFields().getInVarFields()) {
                Field field = m_jsnippet.getClass().getField(
                        inCol.getJavaName());
                Object v = m_flowVars.getValueOfType(inCol.getKnimeName(),
                        inCol.getJavaType());
                field.set(m_jsnippet, v);
            }
         } catch (Exception e) {
             // all reflection exceptions which will never happen, but in case
             // re-throw exception
             throw new RuntimeException(e);
         }

        try {
            // evaluate user script
            m_jsnippet.snippet();
        } catch (Throwable thr) {
            if (thr instanceof Abort) {
                StringBuilder builder = new StringBuilder(
                        "Calculation aborted: ");
                String message = thr.getMessage();
                builder.append(message == null ? "<no details>" : message);
                throw new RuntimeException(builder.toString(), thr);
            } else {
                LOGGER.warn("Evaluation of java snippet failed for row \""
                        + row.getKey() + "\": " + thr.getMessage(), thr);
                OutVarList outVars =
                    m_snippet.getSystemFields().getOutVarFields();
                if (outVars.size() > 0) {
                    // Abort if flow variables are defined
                    throw new RuntimeException("An error occured in an "
                            + "expression with output flow variables.", thr);
                }
                OutColList outFields =
                    m_snippet.getSystemFields().getOutColFields();
                DataCell[] out = new DataCell[outFields.size()];
                for (int i = 0; i < out.length; i++) {
                    // Return missing values for output fields
                    out[i] = DataType.getMissingCell();
                }
                m_rowIndex++;
                return out;
            }
        }

        try {
            // update m_flowVars with output flow variable fields.
            for (OutVar var : m_snippet.getSystemFields().getOutVarFields()) {
                Field field = m_jsnippet.getClass().getField(var.getJavaName());
                Object value = field.get(m_jsnippet);
                if (null != value) {
                    Type type = var.getKnimeType();
                    FlowVariable flowVar = null;
                    if (type.equals(Type.INTEGER)) {
                        flowVar = new FlowVariable(
                                var.getKnimeName(), (Integer)value);
                    } else if (type.equals(Type.DOUBLE)) {
                        flowVar = new FlowVariable(
                                var.getKnimeName(), (Double)value);
                    } else { // case type.equals(Type.String)
                        flowVar = new FlowVariable(
                                var.getKnimeName(), (String)value);
                    }
                    m_flowVars.put(flowVar);
                } else {
                    throw new RuntimeException("Flow varibale \""
                            + var.getKnimeName() + "\" has no value.");
                }

            }
            // get output column fields
            OutColList outFields =
                m_snippet.getSystemFields().getOutColFields();
            DataCell[] out = new DataCell[outFields.size()];
            for (int i = 0; i < out.length; i++) {
                OutCol outField = outFields.get(i);

                Field field = m_jsnippet.getClass().getField(
                        outField.getJavaName());
                Object value = field.get(m_jsnippet);
                if (null == value) {
                    out[i] = DataType.getMissingCell();
                } else {
                    DataType elemType =
                        outField.getKnimeType().isCollectionType()
                        ? outField.getKnimeType().getCollectionElementType()
                        : outField.getKnimeType();
                    JavaToDataCell conv =
                        TypeProvider.getDefault().getJavaToDataCell(
                                elemType,
                                outField.getKnimeType().isCollectionType());
                    out[i] = conv.createDataCell(value);
                }
            }
            m_rowIndex++;
            return out;
        } catch (Exception e) {
            // all but one are reflection exceptions which will never happen,
            // but in case re-throw exception
            throw new RuntimeException(e);
        }



    }


    /**
     * @param row
     * @return
     */
    private Map<String, Cell> createCellsMap(final DataRow row) {
        Map<String, Cell> cells = new LinkedHashMap<String, Cell>(
                row.getNumCells());
        for (int i = 0; i < row.getNumCells(); i++) {
            String name = m_spec.getColumnSpec(i).getName();
            cells.put(name, new DataCellProxy(row, i));
        }
        return cells;
    }





    /**
     * {@inheritDoc}
     */
    @Override
    public DataColumnSpec[] getColumnSpecs() {
        OutColList outFields =
            m_snippet.getSystemFields().getOutColFields();
        DataColumnSpec[] cols = new DataColumnSpec[outFields.size()];
        for (int i = 0; i < cols.length; i++) {
            OutCol field = outFields.get(i);
            cols[i] = new DataColumnSpecCreator(field.getKnimeName(),
                    field.getKnimeType()).createSpec();
        }
        return cols;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setProgress(final int curRowNr, final int rowCount,
            final RowKey lastKey,
            final ExecutionMonitor exec) {
        exec.setProgress(curRowNr / (double)rowCount, "Processed row "
                + curRowNr + " (\"" + lastKey + "\")");
    }

    private static class DataCellProxy implements Cell {
        private DataRow m_row;
        private int m_index;


        /**
         * Represents a cell in the given row as a java snippet cell.
         *
         * @param row the underlying row
         * @param i the index of the cell to represent
         */
        public DataCellProxy(final DataRow row, final int i) {
            super();
            this.m_row = row;
            this.m_index = i;
        }

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("rawtypes")
        @Override
        public Object getValueAs(final Class t) throws TypeException {
            return getValueOfType(t);
        }

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("rawtypes")
        @Override
        public Object getValueOfType(final Class c) throws TypeException {
            DataCell cell = m_row.getCell(m_index);
            if (cell.isMissing()) {
                return null;
            }
            TypeProvider p = TypeProvider.getDefault();
            DataType type = cell.getType();
            DataType elemType = type.isCollectionType()
                ? type.getCollectionElementType() : type;
            DataValueToJava conv = p.getDataValueToJava(elemType,
                    type.isCollectionType());
            return conv.getValue(cell, c);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isMissing() {
            DataCell cell = m_row.getCell(m_index);
            return cell.isMissing();
        }


    }
}
