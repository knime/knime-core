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
 * ---------------------------------------------------------------------
 *
 * History
 *   14.03.2016 (thor): created
 */
package org.knime.workbench.editor2;

import static org.knime.core.node.util.ConvenienceMethods.distinctByKey;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.InstallOperation;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.ui.LoadMetadataRepositoryJob;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.knime.core.data.container.storage.TableStoreFormatInformation;
import org.knime.core.node.KNIMEComponentInformation;
import org.knime.core.node.NodeAndBundleInformationPersistor;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.SWTUtilities;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * Job that checks all enabled update sites for extensions that provided missing nodes. It will open an installation
 * dialog when the features have been found.
 *
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 */
final class InstallMissingNodesJob extends Job {

    private final List<NodeAndBundleInformationPersistor> m_missingNodes;
    private final List<TableStoreFormatInformation> m_missingTableFormats;

    /**
     * Creates a new job.
     *
     * @param missingNodes a list of information about missing nodes
     * @param missingTableFormats list of missing table formats (fully qualified names), not null.
     */
    InstallMissingNodesJob(final List<NodeAndBundleInformationPersistor> missingNodes,
        final List<TableStoreFormatInformation> missingTableFormats) {
        super("Find extensions for missing components");
        m_missingNodes = missingNodes;
        m_missingTableFormats = missingTableFormats;
    }

    @Override
    protected IStatus run(final IProgressMonitor monitor) {
        List<KNIMEComponentInformation> missingComponents = new ArrayList<>();
        m_missingNodes.stream().filter(distinctByKey(i -> i.getFactoryClass())).forEach(missingComponents::add);
        m_missingTableFormats.stream().distinct().collect(Collectors.toList()).forEach(missingComponents::add);
        Set<IInstallableUnit> featuresToInstall = new HashSet<>();
        IStatus status = findExtensions(monitor, missingComponents, featuresToInstall);
        if (!status.isOK()) {
            return status;
        } else if (featuresToInstall.isEmpty()) {
            Display.getDefault().asyncExec(() -> {
                MessageDialog.openWarning(SWTUtilities.getActiveShell(), "No suitable extension found",
                    "Could not find any extension(s) that provides the missing component(s).");
            });
            return Status.OK_STATUS;
        } else {
            if (!missingComponents.isEmpty()) {
                Display.getDefault().syncExec(() -> {
                    MessageDialog.openWarning(SWTUtilities.getActiveShell(), "Not all extensions found",
                        "No extensions for the following components were found: " + missingComponents.stream()
                            .map(i -> i.getComponentName()).collect(Collectors.joining(", ")));
                });
            }
            startInstallJob(featuresToInstall);
            return Status.OK_STATUS;
        }
    }

    private static void startInstallJob(final Set<IInstallableUnit> featuresToInstall) {
        final ProvisioningUI provUI = ProvisioningUI.getDefaultUI();
        Job.getJobManager().cancel(LoadMetadataRepositoryJob.LOAD_FAMILY);
        final LoadMetadataRepositoryJob loadJob = new LoadMetadataRepositoryJob(provUI);
        loadJob.setProperty(LoadMetadataRepositoryJob.ACCUMULATE_LOAD_ERRORS, Boolean.toString(true));

        loadJob.addJobChangeListener(new JobChangeAdapter() {
            @Override
            public void done(final IJobChangeEvent event) {
                if (PlatformUI.isWorkbenchRunning() && event.getResult().isOK()) {
                    Display.getDefault().asyncExec(() -> {
                        if (Display.getDefault().isDisposed()) {
                            NodeLogger.getLogger("Display disposed, aborting install action");
                            return; // fixes AP-8376, AP-8380, AP-7184
                        }
                        provUI.getPolicy().setRepositoriesVisible(false);
                        provUI.openInstallWizard(featuresToInstall,
                            new InstallOperation(provUI.getSession(), featuresToInstall), loadJob);
                        provUI.getPolicy().setRepositoriesVisible(true);
                    });
                }
            }
        });
        loadJob.setUser(true);
        loadJob.schedule();
    }

    private IStatus findExtensions(final IProgressMonitor monitor,
        final List<KNIMEComponentInformation> missingComponents, final Set<IInstallableUnit> featuresToInstall) {
        ProvisioningSession session = ProvisioningUI.getDefaultUI().getSession();
        Bundle myself = FrameworkUtil.getBundle(getClass());
        try {
            IMetadataRepositoryManager metadataManager = (IMetadataRepositoryManager)session.getProvisioningAgent()
                .getService(IMetadataRepositoryManager.SERVICE_NAME);

            for (URI uri : metadataManager.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL)) {
                IMetadataRepository repo = metadataManager.loadRepository(uri, monitor);

                if (!missingComponents.isEmpty()) {
                    for (Iterator<KNIMEComponentInformation> it = missingComponents.iterator(); it.hasNext();) {
                        KNIMEComponentInformation info = it.next();
                        if (searchInRepository(repo, info, monitor, featuresToInstall)) {
                            it.remove();
                        }
                    }
                }
            }
            return Status.OK_STATUS;
        } catch (ProvisionException ex) {
            NodeLogger.getLogger(getClass()).error("Could not create provisioning agent: " + ex.getMessage(), ex);
            return new Status(IStatus.ERROR, myself.getSymbolicName(),
                "Could not query updates site for missing extensions", ex);
        }
    }

    private static boolean searchInRepository(final IMetadataRepository repository,
        final KNIMEComponentInformation componentInfo, final IProgressMonitor monitor,
        final Set<IInstallableUnit> featuresToInstall) throws ProvisionException, OperationCanceledException {
        if (componentInfo.getFeatureSymbolicName().isPresent()) {
            IQuery<IInstallableUnit> query =
                QueryUtil.createLatestQuery(QueryUtil.createIUQuery(componentInfo.getFeatureSymbolicName().get()));
            IQueryResult<IInstallableUnit> result = repository.query(query, monitor);

            // the result is empty after the iterator has been used (Eclipse bug?)
            boolean empty = result.isEmpty();
            result.forEach(i -> featuresToInstall.add(i));
            return !empty;
        } else if (componentInfo.getBundleSymbolicName().isPresent()) {
            IQuery<IInstallableUnit> bundleQuery =
                QueryUtil.createLatestQuery(QueryUtil.createIUQuery(componentInfo.getBundleSymbolicName().get()));
            IQueryResult<IInstallableUnit> bundleResult = repository.query(bundleQuery, monitor);

            if (bundleResult.isEmpty()) {
                return false;
            }

            // try to find feature that contains the bundle
            IQuery<IInstallableUnit> featureQuery = QueryUtil.createLatestQuery(QueryUtil.createQuery(
                "everything.select(y | y.properties ~= filter(\"(org.eclipse.equinox.p2.type.group=true)\") "
                    + "&& everything.select(x | x.id == $0).exists(r | y.requirements.exists(v | r ~= v)))",
                bundleResult.iterator().next().getId()));
            IQueryResult<IInstallableUnit> featureResult = repository.query(featureQuery, monitor);

            // the result is empty after the iterator has been used (Eclipse bug?)
            if (featureResult.isEmpty()) {
                bundleResult.forEach(i -> featuresToInstall.add(i));
            } else {
                featureResult.forEach(i -> featuresToInstall.add(i));
            }
            return true;
        } else {
            return false;
        }
    }
}
