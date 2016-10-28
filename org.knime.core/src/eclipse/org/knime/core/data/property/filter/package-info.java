/**
 * Represents filtering on a table in (usually JavaScript) views. Filtering is done on data and represented by means
 * of a {@link org.knime.core.data.property.filter.FilterHandler}. Despite filtering being an interactive process in a
 * view the filter information itself is immutable and requires re-execution of the node that defines the filtering
 * (e.g. a filter node).
 *
 * @author Bernd Wiswedel, KNIME.com
 * @author Christian Albrecht, KNIME.com
 */
package org.knime.core.data.property.filter;