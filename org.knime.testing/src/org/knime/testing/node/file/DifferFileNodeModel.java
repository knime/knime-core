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
 *   29.04.2011 (hofer): created
 */
package org.knime.testing.node.file;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;

import org.knime.core.data.DataTableSpec;
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
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.testing.node.differNode.TestEvaluationException;

/**
 * This is the model for the Differ File node.
 *
 * @author Heiko Hofer
 */
public class DifferFileNodeModel extends NodeModel {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(DifferFileNodeModel.class);

	private static int BUFFER_SIZE = 8192;
	private final DifferFileNodeSettings m_settings;

	/**
	 * Creates a new model with no input port and one output port.
	 */
	public DifferFileNodeModel() {
		super(new PortType[]{FlowVariablePortObject.TYPE}, new PortType[0]);
		m_settings = new DifferFileNodeSettings();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final PortObjectSpec[] inSpecs)
			throws InvalidSettingsException {
		if (null == m_settings.getTestFileFlowVar()
				&& null == m_settings.getTestFileFlowVar()) {
			// auto-configuration
        	Map<String, FlowVariable> flows = getAvailableFlowVariables();
        	String first = null;
        	String second = null;
        	for (FlowVariable flow : flows.values()) {
        		if (flow.getType().equals(FlowVariable.Type.STRING)) {
        			if (null == first) {
        				first = flow.getName();
        			} else {
        				if (null == second) {
        					second = flow.getName();
        					break;
        				}
        			}
        		}
        	}
            if (null != first) {
            	second = null == second ? first : second;
            	// auto-guessing
                m_settings.setTestFileFlowVar(first);
                m_settings.setReferenceFileFlowVar(second);
                setWarningMessage("Auto guessing: using variables \""
                        + first + "\" and \""
                        + second + "\".");
            } else {
                throw new InvalidSettingsException("No string compatible"
                		+ "flow variable.");
            }
		}
		validateFile(m_settings.getTestFileFlowVar(), "\"Test File\"");
		validateFile(m_settings.getReferenceFileFlowVar(),
				"\"Reference File\"");

		return new DataTableSpec[0];
	}

	private void validateFile(final String flowVarName, final String label)
			throws InvalidSettingsException {
		FlowVariable flowVar = getAvailableFlowVariables().get(flowVarName);
		if (null == flowVar) {
			throw new InvalidSettingsException("Flow variable " + flowVarName
					+ " does not exist.");
		}
		String loc = flowVar.getStringValue();
		if (loc == null || loc.length() == 0) {
			throw new InvalidSettingsException("No " + label
					+ " file selected");
		}

		if (loc.startsWith("file:/") || !loc.matches("^[a-zA-Z]+:/.*")) {
			File file = null;
			if (loc.startsWith("file:/")) {
				URL url;
				try {
					url = new URL(loc);
				} catch (MalformedURLException ex) {
					throw new InvalidSettingsException("Invalid URL: " + loc,
							ex);
				}
				try {
					// can handle file:///c:/Documents%20and%20Settings/...
					file = new File(url.toURI());
				} catch (Exception e) {
					// can handle file:///c:/Documents and Settings/...
					file = new File(url.getPath());
				}
			} else {
				file = new File(loc);
			}

			if (!file.exists()) {
				throw new InvalidSettingsException("File '"
						+ file.getAbsolutePath() + "' does not exist");
			}
			if (!file.isFile()) {
				throw new InvalidSettingsException("'" + file.getAbsolutePath()
						+ "' is a directory");
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected PortObject[] execute(final PortObject[] inData,
			final ExecutionContext exec) throws Exception {
		FlowVariable flowVarA = getAvailableFlowVariables().get(
				m_settings.getTestFileFlowVar());
		if (null == flowVarA) {
			throw new InvalidSettingsException("Flow variable "
					+ m_settings.getTestFileFlowVar()
					+ " does not exist.");
		}
		FlowVariable flowVarB = getAvailableFlowVariables().get(
				m_settings.getReferenceFileFlowVar());
		if (null == flowVarB) {
			throw new InvalidSettingsException("Flow variable "
					+ m_settings.getReferenceFileFlowVar()
					+ " does not exist.");
		}
		InputStream inA = null;
		InputStream inB = null;
		try {
			inA = openInputStream(flowVarA.getStringValue());
			inB = openInputStream(flowVarB.getStringValue());
			LOGGER.info("Comparing file " + flowVarA.getStringValue()
			        + " with file " + flowVarB.getStringValue());
			compareFiles(inA, inB, BUFFER_SIZE);
			return new BufferedDataTable[0];
		} finally {
			if (inA != null) {
				try {
					inA.close();
				} catch (IOException ioe) {
					// ignore
				}
			}
			if (inB != null) {
				try {
					inB.close();
				} catch (IOException ioe) {
					// ignore
				}
			}
		}
	}

	private InputStream openInputStream(final String loc) throws IOException,
			InvalidSettingsException {
		if (loc == null || loc.length() == 0) {
			throw new InvalidSettingsException("No location provided");
		}
        try {
            URL url = new URL(loc);
            return url.openStream();
        } catch (Exception e) {
            // see if they specified a file without giving the protocol
            File file = new File(loc);
            if (!file.exists()) {
				throw new InvalidSettingsException("No such file: " + loc);
			}
			return new FileInputStream(file);
        }
	}

    private void compareFiles(final InputStream inA, final InputStream inB, final int bufferSize)
    throws IOException, TestEvaluationException {
        final BufferedInputStream buffInA =
        	new BufferedInputStream(inA, bufferSize);
        byte[] buffA = new byte[bufferSize];
        final BufferedInputStream buffInB =
        	new BufferedInputStream(inB, bufferSize);
        byte[] buffB = new byte[bufferSize];

        boolean atEnd = false;
        while (!atEnd) {
            int read1 = buffInA.read(buffA);
            int read2 = buffInB.read(buffB);
            if (read1 == read2) {
                atEnd = read1 == -1;
            } else {
            	// files have different length
            	break;
            }
            if (!Arrays.equals(buffA, buffB)) {
            	// files are not equal
                break;
            }
        }
        if (!atEnd) {
        	throw new TestEvaluationException("Files are not equal.");
        }
        buffInA.close();
        buffInB.close();
    }

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadInternals(final File nodeInternDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		// no internals
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveInternals(final File nodeInternDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
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
		new DifferFileNodeSettings().loadSettingsModel(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_settings.loadSettingsModel(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void reset() {
		// no internals
	}
}
