/*
 * ------------------------------------------------------------------------
 *
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
 *   Dec 4, 2017 (clemens): created
 */
package org.knime.core.node.streamable;

import java.io.Serializable;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.JComponent;

import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.PortTypeRegistry;

/**
 * A streamable container port object. Encapsulates an object having a generic type that is shared between the producing
 * and the consuming node and is updated by the producer and processed by the consumer multiple times.
 *
 * @author Clemens von Schwerin, University of Ulm
 * @since 3.5
 */
public class SharedContainerPortObject<T extends Serializable> implements PortObject {

    private ReentrantLock m_lock;

    private Condition m_sharedObjectSet;

    private Condition m_waitOnProcessingCondition;

    private Condition m_waitOnUpdateCondition;

    private T m_sharedObject;

    private boolean m_iterative;

    private boolean m_updated;

    /**
     *  Define port type of objects of this class when used as PortObjects.
     */
    public final static PortType TYPE = PortTypeRegistry.getInstance().getPortType(SharedContainerPortObject.class);

    /**
     * A port type representing an optional shared object input (as used, for instance
     * in the Concatenate node).
     */
    public final static PortType TYPE_OPTIONAL = PortTypeRegistry.getInstance().getPortType(SharedContainerPortObject.class, true);

    /**
     * Constructor.
     */
    public SharedContainerPortObject() {
        m_lock = new ReentrantLock();
        m_sharedObjectSet = m_lock.newCondition();
        m_waitOnProcessingCondition = m_lock.newCondition();
        m_waitOnUpdateCondition = m_lock.newCondition();
        m_updated = false;
    }

    /**
     * Set the object to share between nodes.
     * @param obj the shared object
     */
    public void set(final T obj) {
        m_sharedObject = obj;
        m_lock.lock();
        try {
            m_sharedObjectSet.signalAll();
        } finally {
            m_lock.unlock();
        }
    }

    /**
     * Get the encapsulated object. Wait until its set (not-null) and unlocked (the producer / consumer may work on it).
     *
     * @return the encapsulated object
     */
    synchronized T getAndLock() {
        m_lock.lock();
        try {
            while(m_sharedObject == null) {
                m_sharedObjectSet.await();
            }
            return m_sharedObject;
        } catch(InterruptedException ex) {
            throw new IllegalStateException("Interrupted while waiting on valid object to be set.");
        }
    }

    /**
     * Signal that a working step (updating / processing) on the encapsulated object is done and the next thread waiting
     * on retrieving the encapsulated object may get a hold of it.
     */
    synchronized void unlock() {
        m_lock.unlock();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSummary() {
        return "Sharing a portobject of class " + m_sharedObject.getClass();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PortObjectSpec getSpec() {
        return new SharedContainerPortObjectSpec(m_sharedObject.getClass().getName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JComponent[] getViews() {
        // no views
        return new JComponent[0];
    }

    /**
     * Enforce an iterative update / process order.
     * @param enforce yes enforce, no do not enforce
     */
    public void enforceIterative(final boolean enforce) {
        m_iterative = enforce;
    }

    /**
     * Set the updated flag to true and notify all threads waiting for model updates.
     */
    public void setUpdated() {
        if(m_updated && m_iterative) {
            try {
                m_waitOnProcessingCondition.await();
            } catch (InterruptedException ex) {
                // TODO Auto-generated catch block
            }
        }
        m_updated = true;
        m_waitOnUpdateCondition.signalAll();
    }

    /**
     * Set the updated flag to false and notify all threads waiting for model processing.
     */
    public void setProcessed() {
        if(!m_updated && m_iterative) {
            try {
                m_waitOnUpdateCondition.await();
            } catch (InterruptedException ex) {
                // TODO Auto-generated catch block
            }
        }
        m_updated = false;
        m_waitOnProcessingCondition.signalAll();
    }

}
