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
 *   18.05.2021 (loescher): created
 */
package org.knime.core.node.workflow;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.knime.core.node.KNIMEConstants;
import org.knime.core.util.LoadVersion;
import org.knime.core.util.workflowalizer.AuthorInformation;
import org.knime.shared.workflow.def.AnnotationDataDef;
import org.knime.shared.workflow.def.AuthorInformationDef;
import org.knime.shared.workflow.def.BaseNodeDef;
import org.knime.shared.workflow.def.ConnectionDef;
import org.knime.shared.workflow.def.CreatorDef;
import org.knime.shared.workflow.def.StandaloneDef;
import org.knime.shared.workflow.def.WorkflowDef;
import org.knime.shared.workflow.def.WorkflowUISettingsDef;
import org.knime.shared.workflow.def.impl.AuthorInformationDefBuilder;
import org.knime.shared.workflow.def.impl.CreatorDefBuilder;
import org.knime.shared.workflow.def.impl.StandaloneDefBuilder;
import org.knime.shared.workflow.def.impl.WorkflowUISettingsDefBuilder;
import org.knime.shared.workflow.storage.util.PasswordRedactor;

/**
 *
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 * @author hornm
 * @since 4.4
 */
public class WorkflowManagerToDefAdapter implements WorkflowDef {

    protected final WorkflowManager m_wfm;
    private PasswordRedactor m_passwordHandler;

    /**
     * @param wfm the workflow manager to use as a base
     */
    public WorkflowManagerToDefAdapter(final WorkflowManager wfm, final PasswordRedactor passwordHandler) {
        this.m_wfm = wfm;
        m_passwordHandler = passwordHandler;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<List<ConnectionDef>> getConnections() {
        if(m_wfm.getConnectionContainers().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(m_wfm.getConnectionContainers().stream()//
            .map(CoreToDefUtil::connectionContainerToConnectionDef)//
            .collect(Collectors.toList()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<WorkflowUISettingsDef> getWorkflowEditorSettings() {
        if(m_wfm.getEditorUIInformation() == null) {
            return Optional.empty();
        }
        final var wfEditorSettings = m_wfm.getEditorUIInformation();
        return Optional.of(new WorkflowUISettingsDefBuilder()//
            .setSnapToGrid(wfEditorSettings.getSnapToGrid())//
            .setShowGrid(wfEditorSettings.getShowGrid())//
            .setCurvedConnections(wfEditorSettings.getHasCurvedConnections())//
            .setZoomLevel(wfEditorSettings.getZoomLevel())//
            .setGridX(wfEditorSettings.getGridX())//
            .setGridY(wfEditorSettings.getGridY())//
            .setConnectionLineWidth(wfEditorSettings.getConnectionLineWidth())//
            .build());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Map<String, BaseNodeDef>> getNodes() {
        if(m_wfm.getNodeContainers().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(m_wfm.getNodeContainers().stream()//
            .collect(Collectors.toMap(//
                nc -> Integer.toString(nc.getID().getIndex()), //
                this::getNodeDef)));
    }

    private BaseNodeDef getNodeDef(final NodeContainer nc) {
        if (nc instanceof WorkflowManager) {
            return new MetanodeToDefAdapter((WorkflowManager)nc, m_passwordHandler);
        } else if (nc instanceof NativeNodeContainer) {
            return new NativeNodeContainerToDefAdapter((NativeNodeContainer)nc, m_passwordHandler);
        } else if (nc instanceof SubNodeContainer) {
            return new SubnodeContainerToDefAdapter((SubNodeContainer)nc, m_passwordHandler);
        } else {
            throw new IllegalStateException();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Map<String, AnnotationDataDef>> getAnnotations() {
        if (m_wfm.getWorkflowAnnotations().isEmpty()) {
            return Optional.empty();
        }
        final var result = new HashMap<String, AnnotationDataDef>();
        for (final var annotation : m_wfm.getWorkflowAnnotations()) {
            result.put(String.valueOf(annotation.getID().getIndex()), CoreToDefUtil.toAnnotationDataDef(annotation));
        }
        return Optional.of(result);
    }

    /**
     * Use new {@link FileWorkflowPersistor#parseVersion(String)} to restore a {@link LoadVersion} object from this
     * String.
     *
     * @return description of the workflow project represented by the wrapped workflow manager in POJO format
     */
    public StandaloneDef asProjectDef() {
        CreatorDef creator = new CreatorDefBuilder()//
                .setNightly(KNIMEConstants.isNightlyBuild())//
                .setSavedWithVersion(KNIMEConstants.VERSION)//
                .build();

        return new StandaloneDefBuilder()//
                .setCreator(creator)//
                .setContents(this)
                .build();
    }

    @Override
    public Optional<String> getName() {
        return Optional.of(m_wfm.getName());
    }

    @Override
    public Optional<AuthorInformationDef> getAuthorInformation() {
        var authorInfo = Optional.ofNullable(m_wfm.getAuthorInformation());

        final var authDate = authorInfo//
                .map(AuthorInformation::getAuthoredDate)//
            .map(d -> OffsetDateTime.ofInstant(d.toInstant(), ZoneId.systemDefault()))//
            .orElse(null);

        final OffsetDateTime lastEditDate = authorInfo//
            .flatMap(AuthorInformation::getLastEditDate)//
            .map(d -> OffsetDateTime.ofInstant(d.toInstant(), ZoneId.systemDefault()))//
            .orElse(null);

        return Optional.of(new AuthorInformationDefBuilder()//
            .setLastEditedWhen(lastEditDate)//
            .setAuthoredWhen(authDate)//
            .setAuthoredBy(authorInfo.map(AuthorInformation::getAuthor).orElse(null))//
            .setLastEditedBy(authorInfo.flatMap(AuthorInformation::getLastEditor).orElse(null))//
            .build());
    }

}
