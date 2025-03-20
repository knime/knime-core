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
 *   13 Mar 2025 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.util.urlresolve;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.knime.core.node.util.CheckUtils;

/**
 * Utility class to work with URL query parameters.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 * @since 5.5
 */
public final class URLQueryParamUtil {

    private URLQueryParamUtil() {
        // utility class
    }

    /**
     * Replaces the value of the given query parameter in the non-{@code null} query params with the given value under
     * the new query parameter name (can be the same).
     * @param <T>
     *
     * @param queryParams query params to replace the parameter value in
     * @param queryParam name of the parameter to replace
     * @param queryParamValue new value
     * @param nameExtractor extractor for query param name
     * @param valueExtractor extractor for query param value
     * @param constructorFn function to create new query param objects
     * @return the URI with the replaced parameter value and optional new name
     */
    public static <T> List<T> replaceParameterValue(final List<T> queryParams, final String queryParam,
        final String queryParamValue, final Function<T, String> nameExtractor, final Function<T, String> valueExtractor,
        final BiFunction<String, String, T> constructorFn) {
        return replaceParameterValue(queryParams, queryParam, queryParam, queryParamValue, nameExtractor,
            valueExtractor, constructorFn);
    }

    /**
     * Replaces the value of the given query parameter in the non-{@code null} query params with the given value under
     * the new query parameter name (can be the same).
     *
     * @param queryParams query params to replace the parameter value in
     * @param fromParamName name of the parameter to replace
     * @param toParamName new name of the parameter to place the value under
     * @param paramValue new value
     * @param nameExtractor extractor for query param name
     * @param valueExtractor extractor for query param value
     * @param constructorFn function to create new query param objects
     * @return the URI with the replaced parameter value and optional new name
     */
    public static <T> List<T> replaceParameterValue(final List<T> queryParams, final String fromParamName,
        final String toParamName, final String paramValue, final Function<T, String> nameExtractor,
        final Function<T, String> valueExtractor, final BiFunction<String, String, T> constructorFn) {
        CheckUtils.checkArgumentNotNull(queryParams);
        CheckUtils.checkArgumentNotNull(fromParamName);
        CheckUtils.checkArgumentNotNull(toParamName);

        final var params = new ArrayList<>(queryParams);
        if (params.stream().anyMatch(nvp -> valueExtractor.apply(nvp).contains(","))) {
            throw new IllegalArgumentException("Cannot handle multi-valued query parameters. "
                + "Commas will be URL encoded, which means the parameter is interpreted as a single value "
                + "parameter.");
        }
        final var offset = params.stream().map(nameExtractor).toList().indexOf(fromParamName);
        if (offset != -1) {
            params.remove(offset);
        }
        final var insertAt = offset == -1 ? params.size() : offset;
        if (paramValue != null) {
            params.add(insertAt, constructorFn.apply(toParamName, paramValue));
        }
        return params;
    }
}
