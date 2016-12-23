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
 *   Nov 11, 2016 (hornm): created
 */
package org.knime.core.thrift;

import java.util.HashMap;
import java.util.Map;

import org.knime.core.gateway.entities.EntityBuilderFactory;
import org.knime.core.gateway.v0.workflow.entity.GatewayEntity;
import org.knime.core.gateway.v0.workflow.entity.builder.AnnotationEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.BoundsEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.ConnectionEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.EntityIDBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.GatewayEntityBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.JobManagerEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.MetaPortEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.NodeAnnotationEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.NodeEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.NodeInPortEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.NodeMessageEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.NodeOutPortEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.PortTypeEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.RepoCategoryEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.RepoNodeTemplateEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.TestEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.WorkflowAnnotationEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.WorkflowEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.XYEntBuilder;
import org.knime.core.thrift.workflow.entity.TAnnotationEnt.TAnnotationEntBuilder;
import org.knime.core.thrift.workflow.entity.TBoundsEnt.TBoundsEntBuilder;
import org.knime.core.thrift.workflow.entity.TConnectionEnt.TConnectionEntBuilder;
import org.knime.core.thrift.workflow.entity.TEntityID.TEntityIDBuilder;
import org.knime.core.thrift.workflow.entity.TJobManagerEnt.TJobManagerEntBuilder;
import org.knime.core.thrift.workflow.entity.TMetaPortEnt.TMetaPortEntBuilder;
import org.knime.core.thrift.workflow.entity.TNodeAnnotationEnt.TNodeAnnotationEntBuilder;
import org.knime.core.thrift.workflow.entity.TNodeEnt.TNodeEntBuilder;
import org.knime.core.thrift.workflow.entity.TNodeInPortEnt.TNodeInPortEntBuilder;
import org.knime.core.thrift.workflow.entity.TNodeMessageEnt.TNodeMessageEntBuilder;
import org.knime.core.thrift.workflow.entity.TNodeOutPortEnt.TNodeOutPortEntBuilder;
import org.knime.core.thrift.workflow.entity.TPortTypeEnt.TPortTypeEntBuilder;
import org.knime.core.thrift.workflow.entity.TRepoCategoryEnt.TRepoCategoryEntBuilder;
import org.knime.core.thrift.workflow.entity.TRepoNodeTemplateEnt.TRepoNodeTemplateEntBuilder;
import org.knime.core.thrift.workflow.entity.TTestEnt.TTestEntBuilder;
import org.knime.core.thrift.workflow.entity.TWorkflowAnnotationEnt.TWorkflowAnnotationEntBuilder;
import org.knime.core.thrift.workflow.entity.TWorkflowEnt.TWorkflowEntBuilder;
import org.knime.core.thrift.workflow.entity.TXYEnt.TXYEntBuilder;

/**
 *
 * @author Martin Horn, University of Konstanz
 */
public class TEntityBuilderFactory implements EntityBuilderFactory {

    private static Map<Class<? extends GatewayEntityBuilder>, Class<? extends ThriftEntityBuilder>> ENTITY_BUILDER_MAP =
        new HashMap<>();

    {
        ENTITY_BUILDER_MAP.put(TestEntBuilder.class, TTestEntBuilder.class);
        ENTITY_BUILDER_MAP.put(XYEntBuilder.class, TXYEntBuilder.class);
        ENTITY_BUILDER_MAP.put(WorkflowEntBuilder.class, TWorkflowEntBuilder.class);
        ENTITY_BUILDER_MAP.put(NodeMessageEntBuilder.class, TNodeMessageEntBuilder.class);
        ENTITY_BUILDER_MAP.put(JobManagerEntBuilder.class, TJobManagerEntBuilder.class);
        ENTITY_BUILDER_MAP.put(NodeEntBuilder.class, TNodeEntBuilder.class);
        ENTITY_BUILDER_MAP.put(NodeInPortEntBuilder.class, TNodeInPortEntBuilder.class);
        ENTITY_BUILDER_MAP.put(NodeOutPortEntBuilder.class, TNodeOutPortEntBuilder.class);
        ENTITY_BUILDER_MAP.put(EntityIDBuilder.class, TEntityIDBuilder.class);
        ENTITY_BUILDER_MAP.put(BoundsEntBuilder.class, TBoundsEntBuilder.class);
        ENTITY_BUILDER_MAP.put(ConnectionEntBuilder.class, TConnectionEntBuilder.class);
        ENTITY_BUILDER_MAP.put(AnnotationEntBuilder.class, TAnnotationEntBuilder.class);
        ENTITY_BUILDER_MAP.put(NodeAnnotationEntBuilder.class, TNodeAnnotationEntBuilder.class);
        ENTITY_BUILDER_MAP.put(WorkflowAnnotationEntBuilder.class, TWorkflowAnnotationEntBuilder.class);
        ENTITY_BUILDER_MAP.put(PortTypeEntBuilder.class, TPortTypeEntBuilder.class);
        ENTITY_BUILDER_MAP.put(MetaPortEntBuilder.class, TMetaPortEntBuilder.class);
        ENTITY_BUILDER_MAP.put(RepoCategoryEntBuilder.class, TRepoCategoryEntBuilder.class);
        ENTITY_BUILDER_MAP.put(RepoNodeTemplateEntBuilder.class, TRepoNodeTemplateEntBuilder.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <E extends GatewayEntity, B extends GatewayEntityBuilder<E>> B
        createEntityBuilder(final Class<B> builderInterface) {
        try {
            return (B)ENTITY_BUILDER_MAP.get(builderInterface).newInstance().wrap();
        } catch (InstantiationException | IllegalAccessException ex) {
            // TODO better exception handling
            throw new RuntimeException();
        }
    }

    public static interface ThriftEntityBuilder<E extends GatewayEntity> {
        GatewayEntityBuilder<E> wrap();
    }
}
