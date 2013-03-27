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
 *   Jun 14, 2012 (wiswedel): created
 */
package org.knime.core.node.streamable.simple;

import java.lang.reflect.Array;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.streamable.MergeOperator;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.StreamableFunction;
import org.knime.core.node.streamable.StreamableOperatorInternals;

/**
 * Abstract class of node that computes a simple (1:1) function but needs to
 * have some final processing afterwards (mostly only setting an error message).
 *
 * @param <T> The sub type of the internals used by the implementation
 * @since 2.6
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public abstract class SimpleStreamableFunctionWithInternalsNodeModel<T extends StreamableOperatorInternals> extends
        SimpleStreamableFunctionNodeModel {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(SimpleStreamableFunctionWithInternalsNodeModel.class);

    private final Class<T> m_class;

    /**
     * New node model (one in, one out).
     *
     * @param cl The class of the {@link StreamableOperatorInternals} (used for
     *            instantiation of arrays and new instances).
     * */
    public SimpleStreamableFunctionWithInternalsNodeModel(final Class<T> cl) {
        m_class = cl;

    }

    /**
     * Creates new empty instance of the internals. Default implementation uses
     * reflection and calls no-arg constructor.
     *
     * @return A new instance of the internals (not null!)
     */
    protected T createStreamingOperatorInternals() {
        try {
            return m_class.newInstance();
        } catch (Exception e) {
            final String msg =
                    "Internals class \"" + m_class.getSimpleName()
                            + "\" does not appear to have public default constructor";
            LOGGER.coding(msg, e);
            throw new IllegalStateException(msg, e);
        }
    }

    /**
     * Called the merge operator to merge internals created by different
     * streamble operators (possibly on remote machines).
     *
     * @param operatorInternals The internals to merge.
     * @return A new merged internals object.
     */
    protected abstract T mergeStreamingOperatorInternals(final T[] operatorInternals);

    /**
     * Finalizes execution with a merged internals object. Clients can access
     * its fields and update view content or set warning messages.
     *
     * @param operatorInternals The merged internals object.
     */
    protected abstract void finishStreamableExecution(final T operatorInternals);

    /** {@inheritDoc} */
    @Override
    protected final ColumnRearranger createColumnRearranger(final DataTableSpec spec) throws InvalidSettingsException {
        final T emptyInternals = createStreamingOperatorInternals();
        if (emptyInternals == null) {
            throw new NullPointerException("createStreamingOperatorInternals " + "in class "
                    + getClass().getSimpleName() + " must not return null");
        }
        return createColumnRearranger(spec, emptyInternals);
    }

    /** {@inheritDoc} */
    @Override
    public StreamableFunction createStreamableOperator(final PartitionInfo partitionInfo, final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        DataTableSpec in = (DataTableSpec)inSpecs[0];
        final T emptyInternals = createStreamingOperatorInternals();
        if (emptyInternals == null) {
            throw new NullPointerException("createStreamingOperatorInternals" + " in class "
                    + getClass().getSimpleName() + " must not return null");
        }
        return createColumnRearranger(in, emptyInternals).createStreamableFunction(emptyInternals);
    }

    /** {@inheritDoc} */
    @Override
    public void finishStreamableExecution(final StreamableOperatorInternals internals, final ExecutionContext exec,
            final PortOutput[] output) throws Exception {
        finishStreamableExecution(m_class.cast(internals));
    }

    /**
     * Extends the behavior of
     * {@link SimpleStreamableFunctionNodeModel#createColumnRearranger(DataTableSpec)}
     * by an empty internals object that is filled while processing the data.
     *
     * @param spec ...
     * @param emptyInternals The empty internals. Should be passed on to the
     *            cell factory (and filled in the
     *            {@link org.knime.core.data.container.AbstractCellFactory#afterProcessing()}
     *            method).
     * @return ...
     * @throws InvalidSettingsException ...
     *
     */
    protected abstract ColumnRearranger createColumnRearranger(final DataTableSpec spec, final T emptyInternals)
            throws InvalidSettingsException;

    /** {@inheritDoc} */
    @Override
    public MergeOperator createMergeOperator() {
        return new MergeOperator() {

            @Override
            public StreamableOperatorInternals mergeFinal(final StreamableOperatorInternals[] internals) {
                @SuppressWarnings("unchecked")
                T[] castedInternals = (T[])Array.newInstance(m_class, internals.length);
                for (int i = 0; i < internals.length; i++) {
                    StreamableOperatorInternals o = internals[i];
                    if (o == null) {
                        throw new NullPointerException("internals at position " + i + " is null");
                    } else if (!m_class.isInstance(o)) {
                        throw new IllegalStateException(String.format("Internals at position %d is not of expected "
                                + "class \"%s\", it's a \"%s\"", i, m_class.getSimpleName(), o.getClass()
                                .getSimpleName()));
                    }
                    castedInternals[i] = m_class.cast(o);
                }
                return mergeStreamingOperatorInternals(castedInternals);
            }

        };
    }

}
