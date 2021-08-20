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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.workflow.def.CoreToDefUtil;
import org.knime.core.util.LoadVersion;
import org.knime.core.util.workflowalizer.AuthorInformation;
import org.knime.core.workflow.def.AnnotationDataDef;
import org.knime.core.workflow.def.AuthorInformationDef;
import org.knime.core.workflow.def.ConnectionDef;
import org.knime.core.workflow.def.NodeDef;
import org.knime.core.workflow.def.NodeRefDef;
import org.knime.core.workflow.def.StyleDef;
import org.knime.core.workflow.def.WorkflowCredentialsDef;
import org.knime.core.workflow.def.WorkflowDef;
import org.knime.core.workflow.def.WorkflowMetadataDef;
import org.knime.core.workflow.def.WorkflowProjectDef;
import org.knime.core.workflow.def.WorkflowUISettingsDef;
import org.knime.core.workflow.def.impl.DefaultAnnotationDataDef;
import org.knime.core.workflow.def.impl.DefaultAuthorInformationDef;
import org.knime.core.workflow.def.impl.DefaultConnectionDef;
import org.knime.core.workflow.def.impl.DefaultNodeRefDef;
import org.knime.core.workflow.def.impl.DefaultStyleDef;
import org.knime.core.workflow.def.impl.DefaultWorkflowMetadataDef;
import org.knime.core.workflow.def.impl.DefaultWorkflowProjectDef;
import org.knime.core.workflow.def.impl.DefaultWorkflowUISettingsDef;

/**
 *
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 * @author hornm
 * @since 4.4
 */
public class DefWorkflowManagerWrapper implements WorkflowDef {

    protected final WorkflowManager m_wfm;

    /**
     * @param wfm the workflow manager to use as a base
     */
    public DefWorkflowManagerWrapper(final WorkflowManager wfm) {
        this.m_wfm = wfm;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ConnectionDef> getConnections() {
        final var result = new ArrayList<ConnectionDef>();
        final var connections = m_wfm.getConnectionContainers();
        for (final var connection : connections) {
            final var uiInfo = Optional.ofNullable(connection.getUIInfo())//
                 .map(CoreToDefUtil::toConnectionUISettingsDef)//
                .orElse(null);

            result.add(DefaultConnectionDef.builder()//
                .setUiSettings(uiInfo)
                .setSourcePort(connection.getSourcePort())//
                .setSourceID(connection.getSource().getIndex())//
                .setDestPort(connection.getDestPort())//
                .setDestID(connection.getDest().getIndex())//
                .build());
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WorkflowUISettingsDef getWorkflowEditorSettings() {
        final var wfEditorSettings = m_wfm.getEditorUIInformation();
        return DefaultWorkflowUISettingsDef.builder()//
            .setSnapToGrid(wfEditorSettings.getSnapToGrid())//
            .setShowGrid(wfEditorSettings.getShowGrid())//
            .setCurvedConnections(wfEditorSettings.getHasCurvedConnections())//
            .setZoomLevel(BigDecimal.valueOf(wfEditorSettings.getZoomLevel()))//
            .setGridX(wfEditorSettings.getGridX())//
            .setGridY(wfEditorSettings.getGridY())//
            .setConnectionWidth(wfEditorSettings.getConnectionLineWidth())//
            .build();
    }

    @Override
    public WorkflowMetadataDef getMetadata() {
        return DefaultWorkflowMetadataDef.builder()//
                .setName(m_wfm.getNameField())
                .setCreatedByNightly(KNIMEConstants.isNightlyBuild())//
                .setCreatedBy(KNIMEConstants.VERSION)//
                .setAuthorInformation(getAuthorInformation(m_wfm.getAuthorInformation()))
                .build();
    }


    private static AuthorInformationDef getAuthorInformation(final AuthorInformation authorInfo) {

        final var authDate = Optional//
            .ofNullable(authorInfo.getAuthoredDate())//
            .map(d -> OffsetDateTime.ofInstant(d.toInstant(), ZoneId.systemDefault()))//
            .orElse(null);

        final var lastEditDate = authorInfo.getLastEditDate()//
            .map(d -> OffsetDateTime.ofInstant(d.toInstant(), ZoneId.systemDefault()))//
            .orElse(null);

        return DefaultAuthorInformationDef.builder()//
            .setLastEditedWhen(lastEditDate)//
            .setAuthoredWhen(authDate)//
            .setAuthoredBy(authorInfo.getAuthor())//
            .setLastEditedBy(authorInfo.getLastEditor().orElse(null))//
            .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WorkflowCredentialsDef getWorkflowCredentials() {
        return () -> m_wfm.getCredentialsStore().toString(); //// ????????
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, NodeDef> getNodes() {
        return m_wfm.getNodeContainers().stream()//
            .collect(Collectors.toMap(nc -> nc.getID().toString(), this::getNodeDef));
    }

    private NodeDef getNodeDef(final NodeContainer nc) {
        if (nc instanceof WorkflowManager) {
            return new DefMetanodeWrapper((WorkflowManager)nc);
        } else if (nc instanceof NativeNodeContainer) {
            return new DefNativeNodeWrapper((NativeNodeContainer)nc);
        } else if (nc instanceof SubNodeContainer) {
            return new DefComponentWrapper((SubNodeContainer)nc);
        } else {
            throw new IllegalStateException();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<NodeRefDef> getNodeRefs() {
        return m_wfm.getNodeContainers().stream().map(NodeContainer::getID)
            .map(id -> DefaultNodeRefDef.builder().setReference(id.toString()).setId(id.getIndex()).build())
            .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<AnnotationDataDef> getAnnotations() {
        final var result = new ArrayList<AnnotationDataDef>();
        for (final var annotation : m_wfm.getWorkflowAnnotations()) {

            final var styles = new ArrayList<StyleDef>();
            for (final var style : annotation.getStyleRanges()) {
                styles.add(DefaultStyleDef.builder()//
                    .setStart(style.getStart())//
                    .setLength(style.getLength())//
                    .setFontstyle(style.getFontStyle())//
                    .setFontsize(style.getFontSize())//
                    .setFontname(style.getFontName())//
                    .setFgcolor(style.getFgColor())//
                    .build());
            }

            AnnotationDataDef annotationData = DefaultAnnotationDataDef.builder()//
                .setLocation(CoreToDefUtil.createCoordinate(annotation.getX(), annotation.getY()))//
                .setWidth(annotation.getWidth())//
                .setHeight(annotation.getHeight())//
                .setDefFontSize(annotation.getDefaultFontSize())//
                .setBorderSize(annotation.getBorderSize())//
                .setBorderColor(annotation.getBorderColor())//
                .setBgcolor(annotation.getBgColor())//
                .setText(annotation.getText())//
                .setStyles(styles)//
                .setAnnotationVersion(annotation.getVersion())//
                .setAlignment(annotation.getAlignment().toString()).build();
            result.add(annotationData);

        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean isProject() {
        return m_wfm.isProject();
    }

    /**
     *
     * Use new {@link FileWorkflowPersistor#parseVersion(String)} to restore a {@link LoadVersion} object from this
     * String.
     *
     * @return description of the workflow project represented by the wrapped workflow manager in POJO format
     */
    public WorkflowProjectDef asProjectDef() {
        return DefaultWorkflowProjectDef.builder()//
                .setLoadVersion(m_wfm.getLoadVersion().getVersionString())//
                .setWorkflow(this)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getState() {
        return m_wfm.getNodeContainerState().toString();
    }


}
