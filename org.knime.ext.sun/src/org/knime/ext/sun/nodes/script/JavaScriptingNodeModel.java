/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
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
import org.knime.ext.sun.nodes.script.expression.Expression;

/**
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
public class JavaScriptingNodeModel extends NodeModel {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(JavaScriptingNodeModel.class);

    private final JavaScriptingSettings m_settings;

    /* the compiled version is stored because it is expensive to create it. Do
     * not rely on its existence!
     */
    private Expression m_compiledExpression = null;

   /* The input table spec at the time the above expression was compiled
    */
    private DataTableSpec m_inputSpec = null;

    private File m_tempFile;

    /** One input, one output. */
    public JavaScriptingNodeModel() {
        super(1, 1);
        m_settings = new JavaScriptingSettings();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        new JavaScriptingSettings().loadSettingsInModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // after we got a new expression delete the compiled version of it.
        m_compiledExpression = null;
        m_inputSpec = null;
        m_settings.loadSettingsInModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        DataTableSpec inSpec = inData[0].getDataTableSpec();
        ColumnRearranger c = createColumnRearranger(inSpec);
        BufferedDataTable o = exec.createColumnRearrangeTable(
                inData[0], c, exec);
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
        ColumnRearranger c = createColumnRearranger(inSpecs[0]);
        return new DataTableSpec[]{c.createSpec()};
    }

    private ColumnRearranger createColumnRearranger(final DataTableSpec spec)
            throws InvalidSettingsException {
        if (m_settings == null) {
            throw new InvalidSettingsException("No expression has been set.");
        }
        boolean isReplace = m_settings.isReplace();
        String colName = m_settings.getColName();
        try {
            if ((m_compiledExpression == null)
                    || (!m_inputSpec.equalStructure(spec))) {
                // if the spec changes, we need to re-compile the expression
                if (m_tempFile == null) {
                    m_tempFile = createTempFile();
                }
                m_compiledExpression =
                    Expression.compile(m_settings, spec, m_tempFile);
                m_inputSpec = spec;
            }
            assert m_inputSpec != null;
            DataColumnSpec newColSpec = getNewColSpec();
            ColumnCalculator cc = new ColumnCalculator(this, newColSpec);
            ColumnRearranger result = new ColumnRearranger(spec);
            if (isReplace) {
                result.replace(cc, colName);
            } else {
                result.append(cc);
            }
            return result;
        } catch (Exception e) {
            throw new InvalidSettingsException(e.getMessage(), e);
        }
    }
    
    /** Reads a variable from this node model. Calls for instance
     * {@link #peekScopeVariableDouble(String)}.
     * @param name The name of variable.
     * @param type Type of variable.
     * @return The value
     */
    Object readVariable(final String name, final Class<?> type) {
        if (Integer.class.equals(type)) {
            return peekScopeVariableInt(name);
        } else if (Double.class.equals(type)) {
            return peekScopeVariableDouble(name);
        } else if (String.class.equals(type)) {
            return peekScopeVariableString(name);
        } else {
            throw new RuntimeException("Invalid variable class: " + type);
        }
    }
    
    /**
     * @return the returnType
     */
    Class<?> getReturnType() {
        return m_settings.getReturnType();
    }

    /**
     * @return the compiledExpression
     */
    Expression getCompiledExpression() {
        return m_compiledExpression;
    }
    
    /**
     * @return the inputSpec
     */
    DataTableSpec getInputSpec() {
        return m_inputSpec;
    }
    
    /** Creates an returns a temp java file, for which no .class file exists
     * yet.
     * @return The temporary java file to use.
     * @throws IOException If that fails for whatever reason.
     */
    static File createTempFile() throws IOException {
        // what follows: create a temp file, check if the corresponding
        // class file exists and if so, generate the next temp file
        // (we can't use a temp file, for which a class file already exists).
        while (true) {
            File tempFile = File.createTempFile("Expression", ".java");
            File classFile = getAccompanyingClassFile(tempFile);
            if (classFile.exists()) {
                tempFile.delete();
            } else {
                tempFile.deleteOnExit();
                return tempFile;
            }
        }
    }

    /** Determine the class file name of the javaFile argument. Needed to
     * check if temp file is ok and on exit (to delete all traces of this node).
     * @param javaFile The file name of java file
     * @return The class file (may not exist (yet)).
     */
    static File getAccompanyingClassFile(final File javaFile) {
        if (javaFile == null || !javaFile.getName().endsWith(".java")) {
            throw new IllegalArgumentException("Can't determine class file for"
                    + " non-java file: " + javaFile);
        }
        File parent = javaFile.getParentFile();
        String prefixName = javaFile.getName().substring(
                0, javaFile.getName().length() - ".java".length());
        String classFileName = prefixName + ".class";
        return new File(parent, classFileName);
    }

    private DataColumnSpec getNewColSpec() throws InvalidSettingsException {
        Class<?> returnType = m_settings.getReturnType();
        String colName = m_settings.getColName();
        DataType cellReturnType;
        if (returnType.equals(Integer.class)) {
            cellReturnType = IntCell.TYPE;
        } else if (returnType.equals(Double.class)) {
            cellReturnType = DoubleCell.TYPE;
        } else if (returnType.equals(String.class)) {
            cellReturnType = StringCell.TYPE;
        } else {
            throw new InvalidSettingsException("Illegal return type: "
                    + returnType.getName());
        }
        return new DataColumnSpecCreator(colName, cellReturnType)
                .createSpec();
    }

    /** Attempts to delete temp files.
     * {@inheritDoc} */
    @Override
    protected void finalize() throws Throwable {
        try {
            if ((m_tempFile != null) && m_tempFile.exists()) {
                if (!m_tempFile.delete()) {
                    LOGGER.warn("Unable to delete temp file "
                            + m_tempFile.getAbsolutePath());
                }
                File classFile = getAccompanyingClassFile(m_tempFile);
                if (classFile.exists() && !classFile.delete()) {
                    LOGGER.warn("Unable to delete temp class file "
                            + classFile.getAbsolutePath());
                }
            }
        } finally {
            super.finalize();
        }
    }
}
