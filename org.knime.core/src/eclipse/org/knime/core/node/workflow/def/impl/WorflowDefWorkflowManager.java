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
package org.knime.core.node.workflow.def.impl;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.knime.core.node.NodeFactory.NodeType;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.workflow.def.AnnotationDef;
import org.knime.core.workflow.def.AuthorInformationDef;
import org.knime.core.workflow.def.ConnectionDef;
import org.knime.core.workflow.def.ConnectionUISettingsDef;
import org.knime.core.workflow.def.ExtrainfoNodeBoundsDef;
import org.knime.core.workflow.def.FactorySettingsDef;
import org.knime.core.workflow.def.FilestoreDef;
import org.knime.core.workflow.def.NodeAnnotationDef;
import org.knime.core.workflow.def.NodeDef;
import org.knime.core.workflow.def.NodeUISettingsDef;
import org.knime.core.workflow.def.PortDef;
import org.knime.core.workflow.def.StyleDef;
import org.knime.core.workflow.def.WorkflowCredentialsDef;
import org.knime.core.workflow.def.WorkflowDef;
import org.knime.core.workflow.def.WorkflowEditorSettingsDef;

/**
 *
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 * @since 4.4
 */
public class WorflowDefWorkflowManager implements WorkflowDef {

    private final WorkflowManager m_workflowManager;

    /**
     * @param workflowManager the workflow manager to use as a base
     */
    public WorflowDefWorkflowManager(final WorkflowManager workflowManager) {
        this.m_workflowManager = workflowManager;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getVersion() {
        return m_workflowManager.getLoadVersion().getVersionString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, ConnectionDef> getConnections() {
        final var result = new LinkedHashMap<String, ConnectionDef>();
        final var connections = m_workflowManager.getConnectionContainers();
        for (final var connection : connections) {
            result.put(connection.getID().toString(), new ConnectionDef() {

                @Override
                public ConnectionUISettingsDef getUiSettings() {
                    return () -> connection.getUIInfo().getAllBendpoints().length;
                }

                @Override
                public Integer getSourcePort() {
                    return connection.getSourcePort();
                }

                @Override
                public String getSourceID() {
                    return connection.getSource().toString();
                }

                @Override
                public Integer getDestPort() {
                    return connection.getDestPort();
                }

                @Override
                public String getDestID() {
                    return connection.getDest().toString();
                }
            });
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WorkflowEditorSettingsDef getWorkflowEditorSettings() {
        final var wfEditorSettings = m_workflowManager.getEditorUIInformation();
        return new WorkflowEditorSettingsDef() {

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
        // TODO think about whether this should be the case
        throw new UnsupportedOperationException("The workflow manager does not have access the creation version.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean isCreatedByNightly() {
        // TODO think about whether this should be the case
        throw new UnsupportedOperationException(
            "The workflow manager does not have access to whether it was created in a nighly build.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AuthorInformationDef getAuthorInformation() {
        final var authorInfo = m_workflowManager.getAuthorInformation();
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
    public String getCustomDescription() {
        return m_workflowManager.getCustomDescription();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WorkflowCredentialsDef getWorkflowCredentials() {
        return () -> m_workflowManager.getCredentialsStore().toString(); //// ????????
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, NodeDef> getNodes() {
        final var result = new LinkedHashMap<String, NodeDef>();
        for (final var node : m_workflowManager.getNodeContainers()) {
            result.put(node.getID().toString(), new NodeDef() {

                @Override
                public Boolean isNodeIsMeta() {
                    return NodeType.Meta == node.getType();
                }

                @Override
                public Boolean isIsInactive() {
                    if (node instanceof SingleNodeContainer) {
                        return ((SingleNodeContainer)node).isInactive();
                    }
                    return null;
                }

                @Override
                public Boolean hasContent() {
                    return null; // TODO: where to find this info?
                }

                @Override
                public NodeUISettingsDef getUiSettings() {
                    return () -> new ExtrainfoNodeBoundsDef() {
                        final int[] m_bounds = node.getUIInformation().getBounds();

                        @Override
                        public Integer getArraySize() {
                            return m_bounds.length;
                        }

                        @Override
                        public Integer get3() {
                            return m_bounds[3];
                        }

                        @Override
                        public Integer get2() {
                            return m_bounds[2];
                        }

                        @Override
                        public Integer get1() {
                            return m_bounds[1];
                        }

                        @Override
                        public Integer get0() {
                            return m_bounds[0];
                        }
                    };
                }

                @Override
                public List<FilestoreDef> getFilestores() {
                    // TODO Auto-generated method stub
                    return null;
                }

                @Override
                public FactorySettingsDef getInternalNodeSubsettings() {
                    // TODO Auto-generated method stub
                    return null;
                }

                @Override
                public String getNodeFile() {
                    // TODO Auto-generated method stub
                    return null;
                }

                @Override
                public FactorySettingsDef getModel() {
                    // TODO Auto-generated method stub
                    return null;
                }

                @Override
                public NodeAnnotationDef getNodeAnnotation() {
                    // TODO Auto-generated method stub
                    return null;
                }

                @Override
                public String getCustomDescription() {
                    // TODO Auto-generated method stub
                    return null;
                }

                @Override
                public String getNodeName() {
                    // TODO Auto-generated method stub
                    return null;
                }

                @Override
                public String getNodeBundleName() {
                    // TODO Auto-generated method stub
                    return null;
                }

                @Override
                public String getNodeBundleSymbolicName() {
                    // TODO Auto-generated method stub
                    return null;
                }

                @Override
                public String getNodeBundleVendor() {
                    // TODO Auto-generated method stub
                    return null;
                }

                @Override
                public String getNodeBundleVersion() {
                    // TODO Auto-generated method stub
                    return null;
                }

                @Override
                public String getNodeFeatureName() {
                    // TODO Auto-generated method stub
                    return null;
                }

                @Override
                public String getNodeFeatureSymbolicName() {
                    // TODO Auto-generated method stub
                    return null;
                }

                @Override
                public String getNodeFeatureVendor() {
                    // TODO Auto-generated method stub
                    return null;
                }

                @Override
                public String getNodeFeatureVersion() {
                    // TODO Auto-generated method stub
                    return null;
                }

                @Override
                public FactorySettingsDef getFactorySettings() {
                    // TODO Auto-generated method stub
                    return null;
                }

                @Override
                public Map<String, PortDef> getPorts() {
                    // TODO Auto-generated method stub
                    return null;
                }

                @Override
                public String getName() {
                    // TODO Auto-generated method stub
                    return null;
                }

                @Override
                public String getFactory() {
                    // TODO Auto-generated method stub
                    return null;
                }

                @Override
                public String getState() {
                    // TODO Auto-generated method stub
                    return null;
                }

                @Override
                public String getNodeType() {
                    // TODO Auto-generated method stub
                    return null;
                }

                @Override
                public String getNodeSettingsFile() {
                    // TODO Auto-generated method stub
                    return null;
                }

                @Override
                public String getUiClassname() {
                    // TODO Auto-generated method stub
                    return null;
                }

                @Override
                public Integer getId() {
                    // TODO Auto-generated method stub
                    return null;
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
        return m_workflowManager.getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, AnnotationDef> getAnnotations() {
        final var result = new LinkedHashMap<String, AnnotationDef>();
        for (final var annotation : m_workflowManager.getWorkflowAnnotations()) {
            result.put(annotation.getID().toString(), new AnnotationDef() {

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
                public Map<String, StyleDef> getStyles() {
                    final var result = new LinkedHashMap<String, StyleDef>();
                    for (final var style : annotation.getStyleRanges()) {
                        result.put(style.toString(), new StyleDef() {

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
    public String getId() {
        return m_workflowManager.getID().toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getState() {
        return m_workflowManager.getNodeContainerState().toString();
    }

}
