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
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.workflow.def.CoreToDefUtil;
import org.knime.core.workflow.def.AnnotationDataDef;
import org.knime.core.workflow.def.AuthorInformationDef;
import org.knime.core.workflow.def.ConnectionDef;
import org.knime.core.workflow.def.ConnectionUISettingsDef;
import org.knime.core.workflow.def.NodeDef;
import org.knime.core.workflow.def.NodeRefDef;
import org.knime.core.workflow.def.NodeUIInfoDef;
import org.knime.core.workflow.def.PortDef;
import org.knime.core.workflow.def.StyleDef;
import org.knime.core.workflow.def.TemplateInfoDef;
import org.knime.core.workflow.def.WorkflowCredentialsDef;
import org.knime.core.workflow.def.WorkflowDef;
import org.knime.core.workflow.def.WorkflowUISettingsDef;
import org.knime.core.workflow.def.impl.DefaultNodeRefDef;

/**
 *
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 * @author hornm
 * @since 4.4
 */
public class DefWorkflowManagerWrapper extends DefNodeContainerWrapper implements WorkflowDef {

    private final WorkflowManager m_wfm;

    /**
     * @param wfm the workflow manager to use as a base
     */
    public DefWorkflowManagerWrapper(final WorkflowManager wfm) {
        super(wfm);
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
            result.add(new ConnectionDef() {

                @Override
                public ConnectionUISettingsDef getUiSettings() {
                    ConnectionUIInformation uiInfo = connection.getUIInfo();
                    if (uiInfo != null) {
                        return () -> connection.getUIInfo().getAllBendpoints().length;
                    } else {
                        return null;
                    }
                }

                @Override
                public Integer getSourcePort() {
                    return connection.getSourcePort();
                }

                @Override
                public Integer getSourceID() {
                    return connection.getSource().getIndex();
                }

                @Override
                public Integer getDestPort() {
                    return connection.getDestPort();
                }

                @Override
                public Integer getDestID() {
                    return connection.getDest().getIndex();
                }
            });
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WorkflowUISettingsDef getWorkflowEditorSettings() {
        final var wfEditorSettings = m_wfm.getEditorUIInformation();
        return new WorkflowUISettingsDef() {

            @Override
            public Boolean isSnapToGrid() {
                return wfEditorSettings.getSnapToGrid();
            }

            @Override
            public Boolean isShowGrid() {
                return wfEditorSettings.getShowGrid();
            }

            @Override
            public Boolean isCurvedConnections() {
                return wfEditorSettings.getHasCurvedConnections();
            }

            @Override
            public BigDecimal getZoomLevel() {
                return BigDecimal.valueOf(wfEditorSettings.getZoomLevel());
            }

            @Override
            public Integer getGridY() {
                return wfEditorSettings.getGridY();
            }

            @Override
            public Integer getGridX() {
                return wfEditorSettings.getGridX();
            }

            @Override
            public Integer getConnectionWidth() {
                return wfEditorSettings.getConnectionLineWidth();
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCreatedBy() {
        return KNIMEConstants.VERSION;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean isCreatedByNightly() {
        return KNIMEConstants.isNightlyBuild();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AuthorInformationDef getAuthorInformation() {
        final var authorInfo = m_wfm.getAuthorInformation();
        return new AuthorInformationDef() {

            @Override
            public OffsetDateTime getLastEditedWhen() {
                return authorInfo.getLastEditDate()
                    .map(d -> OffsetDateTime.ofInstant(d.toInstant(), ZoneId.systemDefault())).orElse(null);
            }

            @Override
            public String getLastEditedBy() {
                return authorInfo.getLastEditor().orElse(null);
            }

            @Override
            public OffsetDateTime getAuthoredWhen() {
                final var authDate = authorInfo.getAuthoredDate();
                if (authDate == null) {
                    return null;
                } else {
                    return OffsetDateTime.ofInstant(authDate.toInstant(), ZoneId.systemDefault());
                }
            }

            @Override
            public String getAuthoredBy() {
                return authorInfo.getAuthor();
            }
        };
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
        return m_wfm.getNodeContainers().stream().collect(Collectors.toMap(nc -> nc.getID().toString(), nc -> {
            if (nc instanceof WorkflowManager) {
                return new DefWorkflowManagerWrapper((WorkflowManager)nc);
            } else if (nc instanceof NativeNodeContainer) {
                return new DefNativeNodeContainerWrapper((NativeNodeContainer)nc);
            } else if (nc instanceof SubNodeContainer) {
                return new DefSubNodeContainerWrapper((SubNodeContainer)nc);
            } else {
                throw new IllegalStateException();
            }
        }));
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
            result.add(new AnnotationDataDef() {

                @Override
                public Integer getYCoordinate() {
                    return annotation.getY();
                }

                @Override
                public Integer getXCoordinate() {
                    return annotation.getX();
                }

                @Override
                public Integer getWidth() {
                    return annotation.getWidth();
                }

                @Override
                public String getText() {
                    return annotation.getText();
                }

                @Override
                public List<StyleDef> getStyles() {
                    final var result = new ArrayList<StyleDef>();
                    for (final var style : annotation.getStyleRanges()) {
                        result.add(new StyleDef() {

                            @Override
                            public Integer getStart() {
                                return style.getStart();
                            }

                            @Override
                            public Integer getLength() {
                                return style.getLength();
                            }

                            @Override
                            public Integer getFontstyle() {
                                return style.getFontStyle();
                            }

                            @Override
                            public Integer getFontsize() {
                                return style.getFontSize();
                            }

                            @Override
                            public String getFontname() {
                                return style.getFontName();
                            }

                            @Override
                            public Integer getFgcolor() {
                                return style.getFgColor();
                            }
                        });
                    }
                    return result;
                }

                @Override
                public Integer getHeight() {
                    return annotation.getHeight();
                }

                @Override
                public Integer getDefFontSize() {
                    return annotation.getDefaultFontSize();
                }

                @Override
                public Integer getBorderSize() {
                    return annotation.getBorderSize();
                }

                @Override
                public Integer getBorderColor() {
                    return annotation.getBorderColor();
                }

                @Override
                public Integer getBgcolor() {
                    return annotation.getBgColor();
                }

                @Override
                public Integer getAnnotationVersion() {
                    return annotation.getVersion();
                }

                @Override
                public String getAlignment() {
                    return annotation.getAlignment().toString();
                }
            });
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return m_wfm.getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean isProject() {
        return m_wfm.isProject();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TemplateInfoDef getTemplateInfo() {
        return CoreToDefUtil.toTemplateInfoDef(m_wfm.getTemplateInformation());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PortDef> getInPorts() {
        return IntStream.range(0, m_wfm.getNrInPorts()).mapToObj(m_wfm::getInPort).map(CoreToDefUtil::toPortDef)
            .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PortDef> getOutPorts() {
        return IntStream.range(0, m_wfm.getNrOutPorts()).mapToObj(m_wfm::getOutPort).map(CoreToDefUtil::toPortDef)
            .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeUIInfoDef getInPortsBarUIInfo() {
        return CoreToDefUtil.toNodeUIInfoDef(m_wfm.getInPortsBarUIInfo());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeUIInfoDef getOutPortsBarUIInfo() {
        return CoreToDefUtil.toNodeUIInfoDef(m_wfm.getOutPortsBarUIInfo());
    }

}
