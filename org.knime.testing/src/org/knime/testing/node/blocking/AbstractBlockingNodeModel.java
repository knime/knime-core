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
 */
package org.knime.testing.node.blocking;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.Pointer;
import org.knime.core.util.ThreadPool;
import org.knime.testing.node.blocking.BlockingRepository.LockedMethod;

/**
 *
 * @author wiswedel, University of Konstanz
 */
public abstract class AbstractBlockingNodeModel extends NodeModel {

    private final SettingsModelString m_lockIDModel;

    private Pointer<ExecutionContext> m_execContextPointer;

    /**
     * One data input, one data output.
     */
    AbstractBlockingNodeModel(final PortType input, final PortType output) {
        super(new PortType[]{input}, new PortType[]{output});
        m_lockIDModel = createLockIDModel();
        m_execContextPointer = Pointer.newInstance(null);
    }

    @Override
    protected final PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        synchronized (m_execContextPointer) {
            m_execContextPointer.set(exec);
            m_execContextPointer.notifyAll();
        }
        Lock lock = getLock(LockedMethod.EXECUTE).orElseGet(ReentrantLock::new);
        return ThreadPool.currentPool().runInvisible(() -> { // NOSONAR (block slightly too long)
            lock.lockInterruptibly();
            try {
                return new PortObject[]{executeImplementation(inData[0])};
            } finally {
                synchronized (m_execContextPointer) {
                    m_execContextPointer.set(null);
                    m_execContextPointer.notifyAll();
                }
                lock.unlock();
            }
        });
    }

    /**
     * @return the context provide during node execution, only non-null when currently in execution. Waits at most 2s
     *         for it to appear (otherwise returns an empty Optional)
     */
    public final Optional<ExecutionContext> fetchExecutionContext() throws InterruptedException {
        synchronized (m_execContextPointer) {
            // this really should be done in a loop etc (but eh...)
            var res = m_execContextPointer.get();
            if (res != null) {
                return Optional.of(res);
            }
            m_execContextPointer.wait(2000, 0);
            return Optional.ofNullable(m_execContextPointer.get());
        }
    }

    abstract PortObject executeImplementation(final PortObject input);

    @Override
    protected final PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        String id = m_lockIDModel.getStringValue();
        CheckUtils.checkSetting(StringUtils.isNotEmpty(id), "No lock id provided.");
        Lock lock = getLock(LockedMethod.CONFIGURE).orElseGet(ReentrantLock::new);
        lock.lock();
        try {
            return new PortObjectSpec[]{configureImplementation(inSpecs[0])};
        } finally {
            lock.unlock();
        }
    }

    abstract PortObjectSpec configureImplementation(final PortObjectSpec spec);

    @Override
    protected void reset() {
    }

    @Override
    public final void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_lockIDModel.loadSettingsFrom(settings);
    }

    @Override
    protected final void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_lockIDModel.validateSettings(settings);
    }

    @Override
    protected final void saveSettingsTo(final NodeSettingsWO settings) {
        m_lockIDModel.saveSettingsTo(settings);
    }

    @Override
    protected final void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
    }

    @Override
    protected final void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        Lock lock = getLock(LockedMethod.SAVE_INTERNALS).orElseGet(ReentrantLock::new);
        lock.lock();
        lock.unlock();
    }

    final Optional<ReentrantLock> getLock(final LockedMethod lockedMethod) {
        String id = CheckUtils.checkArgumentNotNull(m_lockIDModel.getStringValue(), "No lock id set");
        return BlockingRepository.get(id, lockedMethod);
    }

    /**
     * Factory method to create the lock id model.
     *
     * @return a new model used in dialog and model.
     */
    public static final SettingsModelString createLockIDModel() {
        return new SettingsModelString("lock_id", null);
    }

}
