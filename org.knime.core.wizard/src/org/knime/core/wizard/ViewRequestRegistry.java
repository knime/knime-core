/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright by KNIME AG, Zurich, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.com
 * email: contact@knime.com
 * ---------------------------------------------------------------------
 *
 * History
 *   Created on 7 May 2018 by albrecht
 */
package org.knime.core.wizard;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.wizard.DefaultViewRequestJob;
import org.knime.core.node.wizard.WizardViewResponse;

/**
 * Simple singleton registry utility class for view initiated requests.
 *
 * @author Christian Albrecht, KNIME GmbH, Konstanz, Germany
 * @since 3.6
 */
public class ViewRequestRegistry {

    private static ViewRequestRegistry m_instance;

    private final Map<String, DefaultViewRequestJob<? extends WizardViewResponse>> m_requestMap;

    /**
     * Singleton accessor, returns the instance for this class.
     *
     * @return the instance for this class
     */
    public static ViewRequestRegistry getInstance() {
        if (m_instance == null) {
            m_instance = new ViewRequestRegistry();
        }
        return m_instance;
    }

    /**
     * @return true if the registry was previously initialized, false otherwise
     */
    public static boolean isRunning() {
        return m_instance != null;
    }

    private ViewRequestRegistry() {
        m_requestMap = new HashMap<String, DefaultViewRequestJob<? extends WizardViewResponse>>();
    }

    /**
     * Adds a job to the registry if the job with the current id does not exist yet. Otherwise updates an already
     * registered job under that same id.
     *
     * @param requestJob the job to add or update, not null
     * @return the previous job associated with the same id, or <tt>null</tt> if there was no mapping for the id.
     */
    public synchronized DefaultViewRequestJob<? extends WizardViewResponse>
        addOrUpdateJob(final DefaultViewRequestJob<?> requestJob) {
        CheckUtils.checkNotNull(requestJob);
        CheckUtils.checkNotNull(requestJob.getId());
        return m_requestMap.put(requestJob.getId(), requestJob);
    }

    /**
     * Returns a view request job for a given id.
     * @param jobID the id to get the request job for
     * @return the registered view request job, or null if no job is registered with the given id
     */
    public synchronized DefaultViewRequestJob<? extends WizardViewResponse> getJob(final String jobID) {
        return m_requestMap.get(jobID);
    }

    /**
     * Removes a view request job from this registry.
     * @param jobID the job id of the job to remove
     * @return the removed job, or null if no job was registered with the given id
     */
    public synchronized DefaultViewRequestJob<? extends WizardViewResponse> removeJob(final String jobID) {
        return m_requestMap.remove(jobID);
    }

    /**
     * Creates a {@link Stream} for all currently registered view request jobs.
     * @return a stream from all registered jobs
     */
    public synchronized Stream<DefaultViewRequestJob<? extends WizardViewResponse>> streamJobs() {
        return m_requestMap.values().stream();
    }

    /**
     * @return the number of all currently registered view request jobs
     */
    public synchronized int getNumberOfRegisteredJobs() {
        return m_requestMap.size();
    }

    /**
     * Tests if a view request job with a given id exists in the registry.
     * @param jobID the id of the job to test
     * @return true if a job is registered with the given id, false otherwise
     */
    public synchronized boolean isJobRegistered(final String jobID) {
        return m_requestMap.containsKey(jobID);
    }

    /**
     * Call this method when the registry is not needed anymore. Cancels all running jobs and cleans up allocated
     * resources.
     */
    public void teardown() {
        m_requestMap.values().forEach(job -> job.cancel());
        m_requestMap.clear();
        m_instance = null;
    }

}
