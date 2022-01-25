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
 *   25 Jan 2022 (carlwitt): created
 */
package org.knime.core.node.workflow.loader;

import java.util.function.BiConsumer;
import java.util.function.Function;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;

/**
 *
 * @author carlwitt
 */
public class LoadUtil {


    @FunctionalInterface
    private interface LoaderCode<T> {
        T load() throws InvalidSettingsException;
    }

    /**
     * Generates error message "Unable to load <attributeName>: <invalid settings exception message>"
     *
     * TODO maybe pass the logger to the load result and log it from there?
     *
     * Outputs the message on debug level and adds it to the load result.
     *
     * Sets the loadResult to dirty.
     *
     * @param <T>
     * @param attributeName
     * @param fallback
     * @param r
     * @param loadResult
     * @param logTo
     * @return Fallback if the loader code throws an {@link InvalidSettingsException}. Otherwise the loaded value.
     */
    private static <T> T tryLoadWithDefaultGeneric(final Function<Throwable, String> errorMessageGen, final T fallback,
        final LoaderCode<T> r, final LoadResult loadResult, final BiConsumer<Object, Throwable> logTo) {
        try {
            return r.load();
        } catch (InvalidSettingsException e) {
            var error = errorMessageGen.apply(e);
            logTo.accept(error, e);
            loadResult.setDirtyAfterLoad();
            loadResult.addError(error);
        }
        return fallback;
    }

    // TODO I didn't pay attention during refactoring and always used tryLoadDebug, but some should emit warnings or errors
    // seems as if loadResult is always using setError (except for two cases) even if the log level is debug - probably
    // not consistent but should stay like this for now
    private <T> T tryLoadDebug(final Function<Throwable, String> errorMessageGen, final T fallback,
        final LoaderCode<T> r, final LoadResult loadResult) {
        return tryLoadWithDefaultGeneric(errorMessageGen, fallback, r, loadResult, getLogger()::debug);
    }

    private <T> T tryLoadDebug(final String attributeName, final T fallback, final LoaderCode<T> r,
        final LoadResult loadResult) {
        return tryLoadWithDefaultGeneric(e -> "Unable to load " + attributeName + ": " + e.getMessage(), fallback, r,
            loadResult, getLogger()::debug);
    }

}
