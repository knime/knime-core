/**
 * Job Manager implementation that mimics an "noop" execution. The
 * {@link org.knime.testing.streaming.testexecutor.StreamingTestNodeExecutionJob} inserts nodes that get executed as
 * part of a component execution (changes in AP-20402 - component output always executes all contained nodes). These
 * copied nodes must not really execute but just mimic that they got executed. Instead, the
 * {@link org.knime.testing.streaming.testexecutor.StreamingTestNodeExecutionJob} will use the copied nodes 'directly'.
 *
 * @author Bernd Wiswedel
 */
package org.knime.testing.streaming.testexecutor.noopexecutor;