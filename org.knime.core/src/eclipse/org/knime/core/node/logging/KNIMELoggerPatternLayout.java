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
 *   05.06.2015 (koetter): created
 */
package org.knime.core.node.logging;

import java.io.File;
import java.util.Objects;
import java.util.UUID;

import org.apache.log4j.MDC;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.helpers.FormattingInfo;
import org.apache.log4j.helpers.PatternConverter;
import org.apache.log4j.helpers.PatternParser;
import org.apache.log4j.spi.LoggingEvent;
import org.knime.core.node.NodeLoggerPatternLayout;
import org.knime.core.util.CoreConstants;

/**
 * {@link PatternLayout} implementation that recognises KNIME specific pattern e.g. I for node id, N for node name and
 * W for the workflow directory.
 *
 * @author Tobias Koetter, KNIME.com
 * @since 5.3
 */
public abstract class KNIMELoggerPatternLayout extends PatternLayout {

    // methods moved from NodeLoggerPatternLayout to gain access to package-scope logging methods

    static class LogPatternParser extends PatternParser {
        /**
         * @param aPattern the pattern to parse
         */
        public LogPatternParser(final String aPattern) {
            super(aPattern);
        }

        @Override
        protected void finalizeConverter(final char c) {
            switch (c) {
                case NodeLoggerPatternLayout.NODE_ID -> {
                    currentLiteral.setLength(0);
                    addConverter(new NodeIDLogPatternConverter(formattingInfo, extractPrecisionOption()));
                }
                case NodeLoggerPatternLayout.NODE_NAME -> {
                    currentLiteral.setLength(0);
                    addConverter(new NodeNameLogPatternConverter(formattingInfo));
                }
                case NodeLoggerPatternLayout.WORKFLOW_DIR -> {
                    currentLiteral.setLength(0);
                    addConverter(new WorkflowDirLogPatternConverter(formattingInfo, extractPrecisionOption()));
                }
                case NodeLoggerPatternLayout.QUALIFIER -> {
                    currentLiteral.setLength(0);
                    addConverter(new QualifierPatternConverter(formattingInfo, extractPrecisionOption()));
                }
                case NodeLoggerPatternLayout.JOB_ID -> {
                    currentLiteral.setLength(0);
                    addConverter(new JobIDLogPatternConverter(formattingInfo));
                }
                case NodeLoggerPatternLayout.CORRELATION_ID -> {
                    currentLiteral.setLength(0);
                    addConverter(new CorrelationIDLogPatternConverter(formattingInfo));
                }
                default -> super.finalizeConverter(c);
            }
        }
    }

    static class NodeIDLogPatternConverter extends PatternConverter {

        private final int m_precision;

        /**
         * @param formattingInfo
         * @param precision
         */
        NodeIDLogPatternConverter(final FormattingInfo formattingInfo, final int precision) {
            super(formattingInfo);
            m_precision = precision;
        }

        @Override
        protected String convert(final LoggingEvent event) {
            Object msg = event.getMessage();
            if (msg instanceof KNIMELogMessage kmsg) {
                final var nodeID = kmsg.nodeID();
                if (nodeID != null) {
                    if (m_precision <= 0) {
                        return nodeID.toString();
                    } else if (m_precision == 1) {
                        return Integer.toString(nodeID.getIndex());
                    } else {
                        final var buf = new StringBuilder();
                        buf.append(nodeID.getIndex());
                        var prefix = nodeID.getPrefix();
                        var counter = 1;
                        while (prefix != null && !prefix.isRoot() && counter < m_precision) {
                            buf.insert(0, ":");
                            buf.insert(0, prefix.getIndex());
                            prefix = prefix.getPrefix();
                            counter++;
                        }
                        return buf.toString();
                    }
                }
            }
            return null;
        }
    }

    static class NodeNameLogPatternConverter extends PatternConverter {
        /**
         * @param formattingInfo
         */
        protected NodeNameLogPatternConverter(final FormattingInfo formattingInfo) {
            super(formattingInfo);
        }

        @Override
        protected String convert(final LoggingEvent event) {
            Object msg = event.getMessage();
            if (msg instanceof KNIMELogMessage kmsg) {
                final String nodeName = kmsg.nodeName();
                if (nodeName != null) {
                    return nodeName;
                }
            }
            return null;
        }
    }

    static class QualifierPatternConverter extends NodeNameLogPatternConverter {
        private int m_precision;

        /**
         * @param formattingInfo
         */
        QualifierPatternConverter(final FormattingInfo formattingInfo, final int precision) {
            super(formattingInfo);
            m_precision = precision;
        }

        @Override
        protected String convert(final LoggingEvent event) {
            final String msg = super.convert(event);
            if (msg != null) {
                return msg;
            }
            final String n = event.getLoggerName();
            //copied from PatternParser.NamedPatternConverter
            if (m_precision <= 0) {
                return n;
            } else {
                int len = n.length();
                // We substract 1 from 'len' when assigning to 'end' to avoid out of
                // bounds exception in return r.substring(end+1, len). This can happen if
                // precision is 1 and the category name ends with a dot.
                int end = len - 1;
                for (int i = m_precision; i > 0; i--) {
                    end = n.lastIndexOf('.', end - 1);
                    if (end == -1) {
                        return n;
                    }
                }
                return n.substring(end + 1, len);
            }
        }
    }

    static class WorkflowDirLogPatternConverter extends PatternConverter {
        private int m_precision;
        /**
         * @param formattingInfo
         * @param precision the number of folders to log
         */
        WorkflowDirLogPatternConverter(final FormattingInfo formattingInfo, final int precision) {
            super(formattingInfo);
            m_precision = precision;
        }

        @Override
        protected String convert(final LoggingEvent event) {
            final Object msg = event.getMessage();
            if (msg instanceof KNIMELogMessage kmsg) {
                final File wfDir = kmsg.workflowDir();
                if (wfDir != null) {
                    final String wfPath = wfDir.getAbsolutePath();
                    if (m_precision <= 0) {
                        return wfPath;
                    } else {
                        final int len = wfPath.length();
                        // We substract 1 from 'len' when assigning to 'end' to avoid out of
                        // bounds exception in return r.substring(end+1, len). This can happen if
                        // precision is 1 and the path ends with a separator.
                        int end = len - 1;
                        for (int i = m_precision; i > 0; i--) {
                            end = wfPath.lastIndexOf(File.separatorChar, end - 1);
                            if (end == -1) {
                                return wfPath;
                            }
                        }
                        return wfPath.substring(end + 1, len);
                    }
                }
            }
            return null;
        }
    }

    static class JobIDLogPatternConverter extends PatternConverter {

        /**
         * @param formattingInfo
         */
        JobIDLogPatternConverter(final FormattingInfo formattingInfo) {
            super(formattingInfo);
        }

        @Override
        protected String convert(final LoggingEvent event) {
            Object msg = event.getMessage();
            if (msg instanceof KNIMELogMessage kmsg) {
                final UUID jobID = kmsg.jobID();
                if (jobID != null) {
                    return jobID.toString();
                }
            }
            return Objects.toString(MDC.get(CoreConstants.JOB_ID_MDC), null);
        }
    }

    static class CorrelationIDLogPatternConverter extends PatternConverter {
        CorrelationIDLogPatternConverter(final FormattingInfo formattingInfo) {
            super(formattingInfo);
        }

        @Override
        protected String convert(final LoggingEvent event) {
            return Objects.toString(MDC.get(CoreConstants.CORRELATION_ID_MDC), null);
        }
    }

    @Override
    protected PatternParser createPatternParser(final String pattern) {
        return new LogPatternParser(pattern);
    }
}
