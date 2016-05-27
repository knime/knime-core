/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *   Apr 12, 2016 (hornm): created
 */
package org.knime.workbench.workflowcoach.prefs;

import java.util.List;
import java.util.Optional;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.knime.workbench.workflowcoach.data.UpdatableNodeTripleProvider;
import org.osgi.framework.FrameworkUtil;

/**
 * Job that performs the download of statistics for the node recommendations, e.g. from KNIME servers etc.
 *
 * @author Martin Horn, University of Konstanz
 */
public class UpdateJob extends Job {
    private UpdateListener m_listener;

    private List<UpdatableNodeTripleProvider> m_providers;

    /**
     * Creates and immediately schedules the update job.
     *
     * @param listener listener to be informed about the progress of the job and when it's finished
     * @param providers the node triple providers to be updated
     */
    public static void schedule(final UpdateListener listener,
        final List<UpdatableNodeTripleProvider> providers) {
        if(providers.isEmpty()) {
            return;
        }
        UpdateJob j = new UpdateJob(listener, providers);
        j.setUser(false);
        j.schedule();
    }

    /**
     * Creates the update job.
     *
     * @param listener listener to be informed about the progress of the job and when it's finished
     */
    private UpdateJob(final UpdateListener listener, final List<UpdatableNodeTripleProvider> providers) {
        super("Downloading statistics for node recommendations.");
        m_providers = providers;
        m_listener = listener;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected IStatus run(final IProgressMonitor monitor) {
        Exception exception = null;
        for (UpdatableNodeTripleProvider ntp : m_providers) {
            try {
                ntp.upate();
            } catch (Exception e) {
                if (exception == null) {
                    exception = e;
                }
            }
        }

        if (exception == null) {
            m_listener.updateFinished(Optional.empty());
            return Status.OK_STATUS;
        } else {
            m_listener.updateFinished(Optional.of(exception));
            //don't return IStatus.ERROR -> otherwise an annoying error message will be opened
            return new Status(IStatus.OK, FrameworkUtil.getBundle(getClass()).getSymbolicName(),
                "Error while updating the statistics for the node recommendations (Workflow Coach).", exception);
        }
    }

    public interface UpdateListener {
        /**
         * Called when the update process is finished.
         *
         * @param e an optional exception if the update process finished without success. If not given, the update was
         *            successful.
         *
         */
        void updateFinished(Optional<Exception> e);
    }
}
