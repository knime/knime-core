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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   18.09.2007 (thiel): created
 */
package org.knime.base.node.util.exttool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeModel;
import org.knime.core.node.port.PortType;

/**
 * Implements a {@link NodeModel} for nodes that launch external commands. It
 * provides methods to store the output of the external tool and to save and
 * load this output. It comes with two views ({@link ExtToolStderrNodeView} and
 * {@link ExtToolStdoutNodeView}) which will display the text then. It
 * maintains two output buffers for each stream (output and error output). One
 * for a successful execution (which will be cleared when the node is reset)
 * (methods {@link #getExternalOutput()} and {@link #getExternalErrorOutput()}),
 * and one buffer keeping the output of a failing execution (for debugging
 * purposes), (methods {@link #getFailedExternalOutput()} and
 * {@link #getFailedExternalErrorOutput()}). The output of a failing run will
 * be shown in gray by the views, and will be preserved until the node is
 * re-executed.
 *
 * @author Kilian Thiel, University of Konstanz
 */
public abstract class ExtToolOutputNodeModel extends NodeModel implements
        Observer {

    // StdOut, StdErr Buffers
    private LinkedList<String> m_extOutput;

    private LinkedList<String> m_failedExtOutput;

    private LinkedList<String> m_extErrout;

    private LinkedList<String> m_failedExtErrout;

    /**
     * Constructor for a node with data and model ports.
     *
     * @param inPortTypes types of the input ports
     * @param outPortTypes types of the output ports
     */
    public ExtToolOutputNodeModel(final PortType[] inPortTypes,
            final PortType[] outPortTypes) {
        super(inPortTypes, outPortTypes);

        m_extOutput = new LinkedList<String>();
        m_extErrout = new LinkedList<String>();
        m_failedExtOutput = null;
        m_failedExtErrout = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        synchronized (m_extOutput) {
            m_extOutput.clear();
        }
        synchronized (m_extErrout) {
            m_extErrout.clear();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // load back in the standard output
        synchronized (m_extOutput) {
            assert m_extOutput != null;

            File stdOutIn = new File(nodeInternDir, "execOutput");
            if ((!stdOutIn.exists()) || stdOutIn.isDirectory()) {
                m_extOutput.clear();
            } else {

                FileReader reader = new FileReader(stdOutIn);
                BufferedReader breader = new BufferedReader(reader);
                String line = breader.readLine();
                while (line != null) {
                    m_extOutput.addLast(line);
                    line = breader.readLine();
                }

            }
        }

        // load back in the error output
        synchronized (m_extErrout) {

            assert m_extErrout != null;

            File stdErrIn = new File(nodeInternDir, "execErrout");
            if ((!stdErrIn.exists()) || stdErrIn.isDirectory()) {
                m_extErrout.clear();
            } else {
                FileReader reader = new FileReader(stdErrIn);
                BufferedReader breader = new BufferedReader(reader);
                String line = breader.readLine();
                while (line != null) {
                    m_extErrout.addLast(line);
                    line = breader.readLine();
                }
            }

        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        File out = new File(nodeInternDir, "execOutput");
        FileWriter writer = new FileWriter(out);
        synchronized (m_extOutput) {
            for (String line : m_extOutput) {
                writer.write(line);
                writer.append('\n');
            }
        }
        writer.close();

        File err = new File(nodeInternDir, "execErrout");
        FileWriter errWriter = new FileWriter(err);
        synchronized (m_extErrout) {
            for (String line : m_extErrout) {
                errWriter.write(line);
                errWriter.append('\n');
            }
        }
        errWriter.close();
    }

    /**
     * @return a list of strings containing the lines generated by the external
     *         executable during execution for the standard output. The list
     *         could be empty, but never null.
     */
    public final List<String> getExternalOutput() {
        /*
         * to avoid collisions between threads, we create a copy of the list
         */
        ArrayList<String> listCopy;
        synchronized (m_extOutput) {
            listCopy = new ArrayList<String>(m_extOutput.size());
            listCopy.addAll(m_extOutput);
        }
        return Collections.unmodifiableList(listCopy);
    }

    /**
     * @return a list of strings containing the lines generated by the previous
     *         failing execution. It is null if the previous execution
     *         succeeded. It will not be cleared by a reset. It will not be
     *         preserved during save.
     */
    public final List<String> getFailedExternalOutput() {
        if (m_failedExtOutput == null) {
            return null;
        } else {
            return Collections.unmodifiableList(m_failedExtOutput);
        }
    }

    /**
     * @return a list of strings containing the lines generated by the external
     *         executable during execution for the standard error output. The
     *         list could be empty, but never null.
     */
    public final List<String> getExternalErrorOutput() {
        /*
         * to avoid collisions between threads, we create a copy of the list
         */
        ArrayList<String> listCopy;
        synchronized (m_extErrout) {
            listCopy = new ArrayList<String>(m_extErrout.size());
            listCopy.addAll(m_extErrout);
        }
        return Collections.unmodifiableList(listCopy);
    }

    /**
     * @return a list of strings containing the lines generated by the previous
     *         failing execution. It is null if the previous execution
     *         succeeded. It will not be cleared by a reset. It will not be
     *         preserved during save.
     */
    public final List<String> getFailedExternalErrorOutput() {
        if (m_failedExtErrout == null) {
            return null;
        } else {
            return Collections.unmodifiableList(m_failedExtErrout);
        }
    }

    /**
     * @param extOutput the extOutput buffer list to set.
     */
    protected final void setExternalOutput(final LinkedList<String> extOutput) {
        m_extOutput = extOutput;
    }

    /**
     * @param failedExtOutput the failedExtOutput buffer list to set.
     */
    protected final void setFailedExternalOutput(
            final LinkedList<String> failedExtOutput) {
        if (failedExtOutput == null) {
            throw new NullPointerException(
                    "List of failed external output must"
                            + " not be null. Set an emtpy list instead");
        }
        m_failedExtOutput = failedExtOutput;
    }

    /**
     * @param extErrout the extErrout buffer list to set. Must not be null.
     */
    protected final void setExternalErrorOutput(
            final LinkedList<String> extErrout) {
        if (extErrout == null) {
            throw new NullPointerException("List of external error output must"
                    + " not be null. Set an emtpy list instead");
        }
        m_extErrout = extErrout;
    }

    /**
     * @param failedExtErrout the failedExtErrout buffer list to set.
     */
    protected final void setFailedExternalErrorOutput(
            final LinkedList<String> failedExtErrout) {
        if (failedExtErrout == null) {
            throw new NullPointerException("List of failed external error "
                    + "output must not be null. Set an emtpy list instead");
        }
        m_failedExtErrout = failedExtErrout;
    }

    /**
     * {@inheritDoc}
     */
    public void update(final Observable o, final Object arg) {
        if (arg instanceof ViewUpdateNotice) {
            notifyViews(arg);
        }
    }
}
