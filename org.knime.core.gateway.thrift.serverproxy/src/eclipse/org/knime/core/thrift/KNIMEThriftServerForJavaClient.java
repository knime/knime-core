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
 *   Dec 15, 2016 (hornm): created
 */
package org.knime.core.thrift;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.apache.thrift.protocol.TJSONProtocol;
import org.apache.thrift.transport.TTransport;
import org.knime.core.thrift.workflow.service.TTestServiceFromThrift;
import org.knime.core.thrift.workflow.service.TWorkflowServiceFromThrift;

import com.facebook.nifty.codec.DefaultThriftFrameCodecFactory;
import com.facebook.nifty.core.NettyServerConfig;
import com.facebook.nifty.core.NettyServerConfigBuilder;
import com.facebook.nifty.core.NiftyTimer;
import com.facebook.nifty.core.ThriftServerDef;
import com.facebook.nifty.duplex.TDuplexProtocolFactory;
import com.facebook.nifty.processor.NiftyProcessor;
import com.facebook.nifty.processor.NiftyProcessorFactory;
import com.facebook.swift.codec.ThriftCodecManager;
import com.facebook.swift.service.ThriftServer;
import com.facebook.swift.service.ThriftServerConfig;
import com.facebook.swift.service.ThriftServiceProcessor;

import io.airlift.units.Duration;

/**
 * Uses nifty as server (thrift on a netty server - https://github.com/facebook/nifty).
 *
 * @author Martin Horn, University of Konstanz
 */
public class KNIMEThriftServerForJavaClient implements KNIMEThriftServer {

    private ThriftServer m_server;

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        m_server.close();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start() {
        ThriftServerConfig config = new ThriftServerConfig().setPort(2000);
        ThriftServiceProcessor thriftServiceProcessor =
                new ThriftServiceProcessor(new ThriftCodecManager(), Collections.EMPTY_LIST, new TTestServiceFromThrift(), new TWorkflowServiceFromThrift());

        NiftyProcessorFactory processorFactory = new NiftyProcessorFactory()
        {
            @Override
            public NiftyProcessor getProcessor(final TTransport transport)
            {
                return thriftServiceProcessor;
            }
        };

//        ThriftFrameCodecFactory frameFactory = new ThriftFrameCodecFactory() {
//
//            @Override
//            public ChannelHandler create(final int maxFrameSize, final TProtocolFactory defaultProtocolFactory) {
//                int twogb = 1 << 30;
//                return new DefaultThriftFrameCodec(twogb, defaultProtocolFactory);
//            }
//        };

        ThriftServerDef thriftServerDef = ThriftServerDef.newBuilder()
                .name("thrift")
                .listen(config.getPort())
                .limitFrameSizeTo((int) config.getMaxFrameSize().toBytes())
                .clientIdleTimeout(config.getIdleConnectionTimeout())
                .withProcessorFactory(processorFactory)
                .limitConnectionsTo(config.getConnectionLimit())
                .limitQueuedResponsesPerConnection(config.getMaxQueuedResponsesPerConnection())
                .thriftFrameCodecFactory(new DefaultThriftFrameCodecFactory())
                .protocol(TDuplexProtocolFactory.fromSingleFactory(new TJSONProtocol.Factory()))
                .withSecurityFactory(ThriftServer.DEFAULT_SECURITY_FACTORY.niftySecurityFactory)
                .using(config.getOrBuildWorkerExecutor(ThriftServer.DEFAULT_WORKER_EXECUTORS))
                .taskTimeout(new Duration(5, TimeUnit.MINUTES))
                .queueTimeout(config.getQueueTimeout())
                .withSSLConfiguration(ThriftServer.DEFAULT_SSL_SERVER_CONFIGURATION.sslServerConfiguration)
                .withTransportAttachObserver(ThriftServer.DEFAULT_TRANSPORT_ATTACH_OBSERVER.transportAttachObserver)
                .build();

                NettyServerConfigBuilder nettyServerConfigBuilder = NettyServerConfig.newBuilder();

                nettyServerConfigBuilder.getServerSocketChannelConfig().setBacklog(config.getAcceptBacklog());
                nettyServerConfigBuilder.setBossThreadCount(config.getAcceptorThreadCount());
                nettyServerConfigBuilder.setWorkerThreadCount(config.getIoThreadCount());
                nettyServerConfigBuilder.setTimer(new NiftyTimer("thrift"));
                if (config.getTrafficClass() != 0) {
                    nettyServerConfigBuilder.getSocketChannelConfig().setTrafficClass(config.getTrafficClass());
                }

                NettyServerConfig nettyServerConfig = nettyServerConfigBuilder.build();


                m_server = new ThriftServer(nettyServerConfig, thriftServerDef).start();

    }

}
