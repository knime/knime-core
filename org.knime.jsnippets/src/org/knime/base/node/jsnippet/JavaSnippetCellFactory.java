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
 * ------------------------------------------------------------------------
 *
 * History
 *   15.12.2011 (hofer): created
 */
package org.knime.base.node.jsnippet;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import org.knime.base.node.jsnippet.expression.Abort;
import org.knime.base.node.jsnippet.expression.AbstractJSnippet;
import org.knime.base.node.jsnippet.expression.Cell;
import org.knime.base.node.jsnippet.expression.TypeException;
import org.knime.base.node.jsnippet.type.ConverterUtil;
import org.knime.base.node.jsnippet.util.FlowVariableRepository;
import org.knime.base.node.jsnippet.util.JavaFieldList.InColList;
import org.knime.base.node.jsnippet.util.JavaFieldList.OutColList;
import org.knime.base.node.jsnippet.util.JavaFieldList.OutVarList;
import org.knime.base.node.jsnippet.util.field.InCol;
import org.knime.base.node.jsnippet.util.field.InVar;
import org.knime.base.node.jsnippet.util.field.OutCol;
import org.knime.base.node.jsnippet.util.field.OutVar;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.convert.datacell.JavaToDataCellConverter;
import org.knime.core.data.convert.datacell.JavaToDataCellConverterFactory;
import org.knime.core.data.convert.java.DataCellToJavaConverter;
import org.knime.core.data.convert.java.DataCellToJavaConverterFactory;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.FlowVariable.Type;

/**
 * Cell factory for the java snippet node.
 *
 * @author Heiko Hofer
 */
public class JavaSnippetCellFactory extends AbstractCellFactory {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(JavaSnippetCellFactory.class);

    private JavaSnippet m_snippet;

    private DataTableSpec m_spec;

    private AbstractJSnippet m_jsnippet;

    private FlowVariableRepository m_flowVars;

    private int m_rowIndex;

    private int m_rowCount;

    private List<String> m_columns;

    private ExecutionContext m_context;

    private final LinkedHashMap<String, Cell> m_cellsMap;

    private ArrayList<DataCellToJavaConverter<?, ?>> m_inConverters = new ArrayList<>();

    private ArrayList<JavaToDataCellConverter<?>> m_outConverters = new ArrayList<>();

    /**
     * Create a new cell factory.
     *
     * @param snippet the snippet
     * @param spec the spec of the data table at the input
     * @param flowVariableRepository the flow variables at the input
     * @param rowCount the number of rows of the table at the input
     * @param context the execution context
     */
    public JavaSnippetCellFactory(final JavaSnippet snippet, final DataTableSpec spec,
        final FlowVariableRepository flowVariableRepository, final int rowCount, final ExecutionContext context) {
        m_snippet = snippet;
        m_spec = spec;
        m_flowVars = flowVariableRepository;
        m_rowIndex = 0;
        m_rowCount = rowCount;
        m_context = context;

        /* One time snippet instance preparation */
        try {
            if (null == m_jsnippet) {
                m_jsnippet = m_snippet.createSnippetInstance();
                // populate the fields in the m_jsnippet that are constant
                // across the rows.
                Field[] fs = m_jsnippet.getClass().getSuperclass().getDeclaredFields();
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
            m_cellsMap = new LinkedHashMap<>(spec.getNumColumns());
            m_columns = new ArrayList<>(m_cellsMap.keySet());

            Field field = m_jsnippet.getClass().getSuperclass().getDeclaredField("m_cellsMap");
            field.setAccessible(true);
            /* The map is private and never modified by AbstractJSnippet,
             * Making it unmodifiable ensures that stays that way. */
            field.set(m_jsnippet, Collections.unmodifiableMap(m_cellsMap));

            field = m_jsnippet.getClass().getSuperclass().getDeclaredField("m_columns");
            field.setAccessible(true);
            field.set(m_jsnippet, m_columns);

            field = m_jsnippet.getClass().getSuperclass().getDeclaredField("m_inSpec");
            field.setAccessible(true);
            field.set(m_jsnippet, m_spec);

        } catch (Exception e) {
            // all reflection exceptions which will never happen, but in case
            // re-throw exception
            throw new RuntimeException(e);
        }

        for (final InCol inCol : m_snippet.getSystemFields().getInColFields()) {
            // Get the converter factory for this column
            final Optional<DataCellToJavaConverterFactory<?, ?>> factory =
                ConverterUtil.getDataCellToJavaConverterFactory(inCol.getConverterFactoryId());
            if (!factory.isPresent()) {
                throw new RuntimeException("Missing converter factory with ID: " + inCol.getConverterFactoryId());
            }

            m_inConverters.add(factory.get().create());
        }

        for (final OutCol outField : m_snippet.getSystemFields().getOutColFields()) {
            final String id = outField.getConverterFactoryId();

            final Optional<JavaToDataCellConverterFactory<?>> factory =
                ConverterUtil.getJavaToDataCellConverterFactory(id);
            if (!factory.isPresent()) {
                throw new RuntimeException("Missing converter factory with ID: " + id);
            }
            m_outConverters.add(((JavaToDataCellConverterFactory<?>)factory.get()).create(m_context));
        }
    }

    @Override
    public DataCell[] getCells(final DataRow row) {
        try {
            fillCellsMap(row);
            {
                final Field field = m_jsnippet.getClass().getSuperclass().getDeclaredField("m_cells");
                field.setAccessible(true);
                field.set(m_jsnippet, new ArrayList<>(m_cellsMap.values()));
            }
            {
                final Field field = m_jsnippet.getClass().getSuperclass().getDeclaredField(JavaSnippet.ROWID);
                field.setAccessible(true);
                field.set(m_jsnippet, row.getKey().getString());
            }
            {
                final Field field = m_jsnippet.getClass().getSuperclass().getDeclaredField(JavaSnippet.ROWINDEX);
                field.setAccessible(true);
                field.set(m_jsnippet, m_rowIndex);
            }

            // populate the system input column fields with data
            final InColList inFields = m_snippet.getSystemFields().getInColFields();
            for (int i = 0; i < inFields.size(); ++i) {
                final InCol inCol = inFields.get(i);
                final Field field = m_jsnippet.getClass().getField(inCol.getJavaName());

                final DataCell cell = row.getCell(m_spec.findColumnIndex(inCol.getKnimeName()));
                if (cell.isMissing()) {
                    field.set(m_jsnippet, null);
                    continue;
                }

                // Get the converter factory for this column
                final Object converted = m_inConverters.get(i).convertUnsafe(cell);
                field.set(m_jsnippet, converted);
            }

            // reset the system output fields to null (see also bug 3781)
            for (final OutCol outCol : m_snippet.getSystemFields().getOutColFields()) {
                final Field field = m_jsnippet.getClass().getField(outCol.getJavaName());
                field.set(m_jsnippet, null);
            }

            // populate the system input flow variable fields with data
            for (final InVar inCol : m_snippet.getSystemFields().getInVarFields()) {
                final Field field = m_jsnippet.getClass().getField(inCol.getJavaName());
                final Object v = m_flowVars.getValueOfType(inCol.getKnimeName(), inCol.getJavaType());
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
        } catch (final Throwable thr) {
            if (thr instanceof Abort) {
                final String message = thr.getMessage();
                throw new RuntimeException(
                    String.format("Calculation aborted: %s", message == null ? "<no details>" : message), thr);
            } else {
                final StringBuilder msg = new StringBuilder();
                msg.append(String.format("Evaluation of java snippet failed for row \"%s\".", row.getKey()));

                final Integer lineNumber = findLineNumberInStackTrace(thr);
                if (lineNumber != null) {
                    msg.append(String.format("The exception is caused by line %d of the snippet. ", lineNumber));
                }

                if (thr.getMessage() != null) {
                    msg.append(String.format("Exception message:", thr.getMessage()));
                }

                LOGGER.warn(msg.toString(), thr);
                final OutVarList outVars = m_snippet.getSystemFields().getOutVarFields();
                if (outVars.size() > 0) {
                    // Abort if flow variables are defined
                    throw new RuntimeException("An error occured in an expression with output flow variables.", thr);
                }

                final OutColList outFields = m_snippet.getSystemFields().getOutColFields();
                final DataCell[] out = new DataCell[outFields.size()];
                // Return missing values for output fields
                Arrays.fill(out, DataType.getMissingCell());

                m_rowIndex++;
                return out;
            }
        }

        try {
            // update m_flowVars with output flow variable fields.
            for (final OutVar var : m_snippet.getSystemFields().getOutVarFields()) {
                final Field field = m_jsnippet.getClass().getField(var.getJavaName());
                final Object value = field.get(m_jsnippet);
                if (null != value) {
                    Type type = var.getFlowVarType();
                    FlowVariable flowVar = null;
                    if (type.equals(Type.INTEGER)) {
                        flowVar = new FlowVariable(var.getKnimeName(), (Integer)value);
                    } else if (type.equals(Type.DOUBLE)) {
                        flowVar = new FlowVariable(var.getKnimeName(), (Double)value);
                    } else { // case type.equals(Type.String)
                        flowVar = new FlowVariable(var.getKnimeName(), (String)value);
                    }
                    m_flowVars.put(flowVar);
                } else {
                    throw new RuntimeException("Flow variable \"" + var.getKnimeName() + "\" has no value.");
                }
            }

            // get output column fields
            final OutColList outFields = m_snippet.getSystemFields().getOutColFields();
            final DataCell[] out = new DataCell[outFields.size()];
            for (int i = 0; i < out.length; i++) {
                final OutCol outField = outFields.get(i);
                final Field field = m_jsnippet.getClass().getField(outField.getJavaName());

                final Object value = field.get(m_jsnippet);
                out[i] = (null == value) ? DataType.getMissingCell() : m_outConverters.get(i).convertUnsafe(value);

                // Cleanup Closeable and AutoCloeable inputs
                if (value instanceof AutoCloseable) {
                    // From the doc: Calling close more than once *can* have visible side effects!
                    ((AutoCloseable)value).close();
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
     * Find line number of last JSnippet stack trace element.
     *
     * @param thr Throwable containing the stack trace
     * @return The line number or <code>null</code> if no element in the stack trace originated from the JSnippet class.
     */
    private static Integer findLineNumberInStackTrace(final Throwable thr) {
        Integer lineNumber = null;
        for (final StackTraceElement ste : thr.getStackTrace()) {
            if (ste.getClassName().equals("JSnippet")) {
                lineNumber = ste.getLineNumber();
            }
        }

        return lineNumber;
    }

    @Override
    public void afterProcessing() {
        m_snippet.close();
    }

    /**
     * Fill m_cellsMap with {@link DataCellProxy} of the given rows.
     *
     * @param row Example row to create the map for.
     */
    private void fillCellsMap(final DataRow row) {
        m_cellsMap.clear();
        for (int i = 0; i < row.getNumCells(); i++) {
            String name = m_spec.getColumnSpec(i).getName();
            m_cellsMap.put(name, new DataCellProxy(row, i));
        }
    }

    @Override
    public DataColumnSpec[] getColumnSpecs() {
        OutColList outFields = m_snippet.getSystemFields().getOutColFields();
        DataColumnSpec[] cols = new DataColumnSpec[outFields.size()];
        for (int i = 0; i < cols.length; i++) {
            OutCol field = outFields.get(i);
            cols[i] = new DataColumnSpecCreator(field.getKnimeName(), field.getDataType()).createSpec();
        }
        return cols;
    }

    @Deprecated
    @Override
    public void setProgress(final int curRowNr, final int rowCount, final RowKey lastKey, final ExecutionMonitor exec) {
        exec.setProgress(curRowNr / (double)rowCount, () -> "Processed row " + curRowNr + " (\"" + lastKey + "\")");
    }

    @Override
    public void setProgress(final long curRowNr, final long rowCount, final RowKey lastKey,
        final ExecutionMonitor exec) {
        exec.setProgress(curRowNr / (double)rowCount, () -> "Processed row " + curRowNr + " (\"" + lastKey + "\")");
    }

    /**
     * Class that wraps a DataRow for access from inside the Java Snippet.
     *
     * This allows cell access while keeping the DataCell API safe from having to provide workflow level backwards
     * compatibility.
     *
     * DataCellProxy stores a reference to a row and a column index and can be updated to point to a new row. Accessing
     * the value stored inside the cell happens on demand.
     */
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

        @SuppressWarnings("rawtypes")
        @Override
        public Object getValueAs(final Class t) throws TypeException {
            return getValueOfType(t);
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        @Override
        public Object getValueOfType(final Class c) throws TypeException {
            DataCell cell = m_row.getCell(m_index);
            if (cell.isMissing()) {
                return null;
            }
            DataType type = cell.getType();

            final Optional<?> factory = ConverterUtil.getConverterFactory(type, c);
            if (!factory.isPresent()) {
                throw new RuntimeException(
                    "Could not find a converter factory for: " + type.getName() + " -> " + c.getName());
            }
            try {
                return ((DataCellToJavaConverterFactory<DataCell, Object>)factory.get()).create().convert(cell);
            } catch (Exception e) {
                throw new TypeException(e);
            }
        }

        @Override
        public boolean isMissing() {
            DataCell cell = m_row.getCell(m_index);
            return cell.isMissing();
        }

    }
}
