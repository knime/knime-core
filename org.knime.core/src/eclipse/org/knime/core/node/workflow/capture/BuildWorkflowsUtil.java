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
 *   Mar 12, 2020 (hornm): created
 */
package org.knime.core.node.workflow.capture;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.KNIMEException;
import org.knime.core.node.message.Message;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResultEntry.LoadResultEntryType;
import org.knime.core.node.workflow.WorkflowPersistor.WorkflowLoadResult;
import org.knime.core.node.workflow.WorkflowSaveHelper;
import org.knime.core.node.workflow.capture.WorkflowSegment.Input;
import org.knime.core.node.workflow.capture.WorkflowSegment.Output;
import org.knime.core.util.FileUtil;
import org.knime.core.util.FileUtil.ZipFileFilter;
import org.knime.core.util.LockFailedException;
import org.knime.core.util.Pair;
import org.knime.core.util.VMFileLocker;

/**
 * Utility methods for 'build-workflow' functionality.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @since 5.3
 * @noreference This class is not intended to be referenced by clients.
 */
public final class BuildWorkflowsUtil {

    // vertical distance between newly added input and output nodes
    private static final int NODE_DIST = 120;

    private static final int NODE_WIDTH = 124;

    private static final int NODE_HEIGHT = 83;

    private BuildWorkflowsUtil() {
        //utility class
    }

    /**
     * Calculates the position of a n nodes either placed at the beginning (inputs) or the end (outputs) of a workflow.
     *
     * @param wfBounds the bounding box of the workflow to place the inputs/outputs before/after
     * @param numNodes the number of nodes to place
     * @param isInput if the position is calculated for input or output nodes
     * @return a pair containing the x-position first, second a list of y-positions (= number of nodes)
     */
    public static Pair<Integer, int[]> getInputOutputNodePositions(final int[] wfBounds, final int numNodes,
        final boolean isInput) {
        int yBoundingBoxCenter = (int)Math.round((wfBounds[3] - wfBounds[1]) / 2.0 + wfBounds[1]);
        int yOffset = (int)Math.floor(yBoundingBoxCenter - ((numNodes - 1) * NODE_DIST) / 2.0 - NODE_HEIGHT / 2.0);
        int xPos = (int)Math.round((isInput ? (wfBounds[0] - NODE_DIST) : (wfBounds[2] + NODE_DIST)) - NODE_WIDTH / 2.0);
        int[] yPos = new int[numNodes];
        for (int i = 0; i < numNodes; i++) {
            yPos[i] = yOffset + i * NODE_DIST;
        }
        return Pair.create(xPos, yPos);
    }

    /**
     * Validate a custom workflow name and return an appropriate error message if invalid.
     *
     * @since 4.4
     * @param name The workflow name to validate
     * @param allowEmpty If the workflow name can be empty
     * @param doFormat If the message should be HTML-formatted (e.g. for display in dialog)
     * @return validation error message
     */
    public static Optional<String> validateCustomWorkflowName(final String name, final boolean allowEmpty,
        final boolean doFormat) {
        if (!allowEmpty && name.trim().isEmpty()) {
            return Optional.of("Custom workflow name is empty");
        }
        final Matcher matcher = FileUtil.ILLEGAL_FILENAME_CHARS_PATTERN.matcher(name);
        if (matcher.find()) {
            StringBuilder res = new StringBuilder();
            res.append("Custom workflow name must not contain either of ")
                .append(listChars(FileUtil.ILLEGAL_FILENAME_CHARS, doFormat));
            return Optional.of(res.toString());
        }
        return Optional.empty();
    }

    /**
     * Given a String, list each character of that string in a human-readable, formatted string.
     *
     * @since 4.4
     * @param chars A string containing the characters to be listed.
     * @return A HTML-formatted string listing the given characters.
     */
    private static String listChars(final String chars, final boolean doFormat) {
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < chars.length(); i++) {
            var curChar = chars.substring(i, i + 1);
            if (doFormat) {
                curChar = escapeChevrons(curChar);
                res.append("<code>");
            }
            res.append(curChar);
            if (doFormat) {
                res.append("</code>");
            }
            if (i < chars.length() - 2) {
                res.append(", ");
            }
            if (i == chars.length() - 2) {
                res.append(" or ");
            }
        }
        return res.toString();
    }

    private static String escapeChevrons(String curChar) {
        // Printing `<` or `>` in JLabels that already use HTML formatting is problematic.
        if (curChar.equals("<")) {
            curChar = "&lt;";
        }
        if (curChar.equals(">")) {
            curChar = "&gt;";
        }
        return curChar;
    }

    /**
     * Checks a {@link WorkflowLoadResult} and possibly turns it into a warning message or an exception.
     *
     * @param lr the load result to check
     *
     * @return a warning message if there are warnings, or an empty optional.
     * @throws KNIMEException thrown if there are loading errors
     */
    public static Optional<String> checkLoadResult(final WorkflowLoadResult lr) throws KNIMEException {
        switch (lr.getType()) {
            case Warning:
                return Optional.of(lr.getFilteredError("", LoadResultEntryType.Warning));
            case Error:
                throw KNIMEException.of( //
                    Message.builder() //
                        .withSummary("Error(s) while loading the workflow.") //
                        .addTextIssue(lr.getFilteredError("", LoadResultEntryType.Error)) //
                        .build().orElseThrow());
            case Ok:
            case DataLoadError: // NOSONAR ignore data load errors
            default:
                return Optional.empty();

        }
    }

    /**
     * Helper method to load the workflow from a {@link WorkflowSegment}.
     *
     * @param ws the segment to load the workflow from
     * @param warningConsumer called if there was a warning while loading
     * @return the load workflow manager
     * @throws KNIMEException if there were loading errors
     */
    public static WorkflowManager loadWorkflow(final WorkflowSegment ws, final Consumer<String> warningConsumer)
        throws KNIMEException {
        AtomicReference<KNIMEException> exception = new AtomicReference<>();
        WorkflowManager wfm = ws.loadWorkflow(lr -> { // NOSONAR
            try {
                var warningOptional = checkLoadResult(lr);
                warningOptional //
                    .map("Problem(s) while loading the workflow: \n"::concat) //
                    .ifPresent(warningConsumer::accept);
            } catch (KNIMEException e) {
                exception.set(e);
            }
        });

        if (exception.get() != null) {
            ws.disposeWorkflow();
            throw exception.get();
        }
        return wfm;
    }

    /**
     * TODO
     *
     * @param wfmStream
     * @param workflowName
     * @param inputs
     * @param outputs
     * @return
     */
    public static WorkflowSegment createWorkflowSegment(final byte[] wfmStream, final String workflowName,
        final List<Input> inputs, final List<Output> outputs) {
        return new WorkflowSegment(wfmStream, workflowName, inputs, outputs, Set.of());
    }

    /**
     * TODO
     *
     * @param wfm
     * @return
     * @throws IOException
     */
    public static byte[] wfmToStream(final WorkflowManager wfm) throws IOException {
        var tmpDir = newTempDirWithName(wfm.getName());
        try {
            return wfmToStream(wfm, tmpDir);
        } finally {
            FileUtil.deleteRecursively(tmpDir.getParentFile());
        }
    }

    /**
     * @return a new empty temporary directory called according to the <code>name</code> (workflow name)
     * @throws IOException Failing to creating the folder
     */
    // added as part of AP-21997
    static File newTempDirWithName(final String name) throws IOException {
        final String sanitizedName = FileUtil.ILLEGAL_FILENAME_CHARS_PATTERN.matcher(name).replaceAll("_");
        return new File(FileUtil.createTempDir("workflow_segment"), sanitizedName);
    }

    static byte[] wfmToStream(final WorkflowManager wfm, final File tmpDir) throws IOException {
        try (var bos = new ByteArrayOutputStream(); ZipOutputStream out = new ZipOutputStream(bos);) {
            var saveHelper = new WorkflowSaveHelper(false, false);
            wfm.save(tmpDir, saveHelper, new ExecutionMonitor());
            FileUtil.zipDir(out, Collections.singleton(tmpDir), new ZipFileFilter() {
                @Override
                public boolean include(final File f) {
                    return !f.getName().equals(VMFileLocker.LOCK_FILE);
                }
            }, null);
            bos.flush();
            return bos.toByteArray();
        } catch (LockFailedException | CanceledExecutionException | IOException e) {
            throw new IOException("Failed saving workflow port object", e);
        }
    }


}
