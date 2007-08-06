/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 * -------------------------------------------------------------------
 * 
 */
package org.knime.ext.sun.nodes.script;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

import org.knime.ext.sun.nodes.script.expression.CompilationFailedException;
import org.knime.ext.sun.nodes.script.expression.Expression;

/**
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class JavaScriptingNodeModel extends NodeModel {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(JavaScriptingNodeModel.class);

    /** NodeSettings key for the expression. */
    protected static final String CFG_EXPRESSION = "expression";

    /** NodeSettings key which column is to be replaced or appended. */
    protected static final String CFG_COLUMN_NAME = "replaced_column";

    /** NodeSettings key is replace or append column? */
    protected static final String CFG_IS_REPLACE = "append_column";

    /** NodeSettings key for the return type of the expression. */
    protected static final String CFG_RETURN_TYPE = "return_type";

    private String m_expression;

    private Class<?> m_returnType;

    private String m_colName;

    private boolean m_isReplace;

    private File m_tempFile;

    public JavaScriptingNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addString(CFG_EXPRESSION, m_expression);
        settings.addString(CFG_COLUMN_NAME, m_colName);
        settings.addBoolean(CFG_IS_REPLACE, m_isReplace);
        String rType = m_returnType != null ? m_returnType.getName() : null;
        settings.addString(CFG_RETURN_TYPE, rType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        settings.getString(CFG_EXPRESSION);
        settings.getString(CFG_COLUMN_NAME);
        settings.getBoolean(CFG_IS_REPLACE);
        String returnType = settings.getString(CFG_RETURN_TYPE);
        getReturnType(returnType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_expression = settings.getString(CFG_EXPRESSION);
        m_colName = settings.getString(CFG_COLUMN_NAME);
        m_isReplace = settings.getBoolean(CFG_IS_REPLACE);
        String returnType = settings.getString(CFG_RETURN_TYPE);
        m_returnType = getReturnType(returnType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        DataTableSpec inSpec = inData[0].getDataTableSpec();
        ColumnRearranger c = createColumnRearranger(inSpec);
        BufferedDataTable o = exec.createColumnRearrangeTable(inData[0], c,
                exec);
        return new BufferedDataTable[]{o};
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
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        if (m_expression == null) {
            throw new InvalidSettingsException("No expression has been set.");
        }
        ColumnRearranger c;
        try {
            c = createColumnRearranger(inSpecs[0]);
        } catch (IOException ioe) {
            LOGGER.warn("Unable to create temp file.", ioe);
            setWarningMessage("Unable to create temp file.");
            throw new InvalidSettingsException("Error creating temp file", ioe);
        } catch (CompilationFailedException cfe) {
            LOGGER.debug("Unable to compile expression: ", cfe);
            throw new InvalidSettingsException(cfe.getMessage());
        } catch (InstantiationException ie) {
            LOGGER.debug("Unable to instantiate auto-generated class.", ie);
            throw new InvalidSettingsException(ie.getMessage());
        }
        return new DataTableSpec[]{c.createSpec()};
    }

    private ColumnRearranger createColumnRearranger(final DataTableSpec spec)
            throws InvalidSettingsException, IOException,
            CompilationFailedException, InstantiationException {
        DataColumnSpec newColSpec = getNewColSpec();
        checkTempFile();
        Expression exp = compile(m_expression, spec, m_returnType, m_tempFile);
        ColumnCalculator cc = new ColumnCalculator(exp, m_returnType, spec,
                newColSpec);
        ColumnRearranger result = new ColumnRearranger(spec);
        if (m_isReplace) {
            result.replace(cc, m_colName);
        } else {
            result.append(cc);
        }
        return result;
    }

    private void checkTempFile() throws IOException {
        if (m_tempFile == null) {
            m_tempFile = File.createTempFile("Expression", ".java");
            m_tempFile.deleteOnExit();
        }
    }

    private DataColumnSpec getNewColSpec() throws InvalidSettingsException {
        DataType cellReturnType;
        if (m_returnType.equals(Integer.class)) {
            cellReturnType = IntCell.TYPE;
        } else if (m_returnType.equals(Double.class)) {
            cellReturnType = DoubleCell.TYPE;
        } else if (m_returnType.equals(String.class)) {
            cellReturnType = StringCell.TYPE;
        } else {
            throw new InvalidSettingsException("Illegal return type: "
                    + m_returnType.getName());
        }
        return new DataColumnSpecCreator(m_colName, cellReturnType)
                .createSpec();
    }

    /**
     * Get the class associated with returnType.
     * 
     * @param returnType <code>Double.class.getName()</code>
     * @return the associated class
     * @throws InvalidSettingsException if the argument is invalid
     */
    static Class<?> getReturnType(final String returnType)
            throws InvalidSettingsException {
        if (Integer.class.getName().equals(returnType)) {
            return Integer.class;
        } else if (Double.class.getName().equals(returnType)) {
            return Double.class;
        } else if (String.class.getName().equals(returnType)) {
            return String.class;
        } else {
            throw new InvalidSettingsException("Not a valid return type: "
                    + returnType);
        }
    }

    /**
     * Tries to compile the given expression as entered in the dialog with the
     * current spec.
     * 
     * @param expression the expression from dialog or settings
     * @param spec the spec
     * @param rType the return type, e.g. <code>Integer.class</code>
     * @param tempFile the file to use
     * @return the java expression
     * @throws CompilationFailedException if that fails
     * @throws InvalidSettingsException if settings are missing
     */
    static Expression compile(final String expression,
            final DataTableSpec spec, final Class<?> rType, final File tempFile)
            throws CompilationFailedException, InvalidSettingsException {
        int offset = 0;
        HashMap<String, String> nameValueMap = new HashMap<String, String>();
        StringBuffer correctedExp = new StringBuffer();
        while (offset < expression.length()) {
            int start = expression.indexOf('$', offset);
            if (start < offset) {
                break;
            }
            int end = expression.indexOf('$', start + 1);
            boolean isOrdinaryColumn = true;
            if (end == start + 1) { // "$$" - row number or row key
                isOrdinaryColumn = false;
                end = expression.indexOf("$$", end + 1);
            }
            if (end < start) {
                throw new InvalidSettingsException("No closing $ for: \""
                        + expression.substring(start, Math.max(expression
                                .length(), start + 10)) + "\"");
            }
            // either "$" or "$$"
            int off = isOrdinaryColumn ? "$".length() : "$$".length();
            String colIdPound = expression.substring(start, end + off);
            String colId = colIdPound.substring(off, colIdPound.length() - off);
            correctedExp.append(expression.substring(offset, start));
            String colFieldName;
            Class<?> type;
            if (isOrdinaryColumn) {
                int colIndex = spec.findColumnIndex(colId);
                if (colIndex < 0) {
                    throw new InvalidSettingsException("No such column: "
                            + colId);
                }
                colFieldName = ColumnCalculator.createColField(colIndex);
                DataType colType = spec.getColumnSpec(colIndex).getType();
                correctedExp.append(colFieldName);
                if (colType.isCompatible(IntValue.class)) {
                    type = Integer.class;
                    correctedExp.append(".intValue()");
                } else if (colType.isCompatible(DoubleValue.class)) {
                    type = Double.class;
                    correctedExp.append(".doubleValue()");
                } else {
                    type = String.class;
                }
            } else {
                if (colId.equals(ColumnCalculator.ROWINDEX)) {
                    colFieldName = ColumnCalculator.ROWINDEX;
                    type = Integer.class;
                } else if (colId.equals(ColumnCalculator.ROWKEY)) {
                    colFieldName = ColumnCalculator.ROWKEY;
                    type = String.class;
                } else {
                    throw new InvalidSettingsException("Invalid name : "
                            + colId);
                }
                correctedExp.append(colFieldName);
            }
            offset = Math.min(end + off, expression.length());
            nameValueMap.put(colFieldName, type.getName());
        }
        correctedExp.append(expression.substring(Math.min(offset, expression
                .length())));
        return new Expression(correctedExp.toString(), nameValueMap, rType,
                tempFile);
    }

    /**
     * Attemtps to delete temp file.
     * 
     * @see Object#finalize()
     */
    @Override
    protected void finalize() throws Throwable {
        try {
            if ((m_tempFile != null) && m_tempFile.exists()
                    && !m_tempFile.delete()) {
                LOGGER.warn("Unable to delete temp file "
                        + m_tempFile.getAbsolutePath());
            }
        } finally {
            super.finalize();
        }
    }
}
