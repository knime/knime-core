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
 *   05.07.2018 (thor): created
 */
package org.knime.core.data.container.storage;

import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.knime.core.node.KNIMEComponentInformation;

/**
 * Class that represents (hard-coded) format information for known formats provided by KNIME. (Currently for ORC and
 * Parquet.)
 *
 * @since 3.6
 */
public final class TableStoreFormatInformation implements KNIMEComponentInformation {
    private final String m_bundleName;

    private final String m_featureName;

    private final String m_humanReadableFeatureName;

    private TableStoreFormatInformation(final String bundleName, final String featureName,
        final String humanReadableFeatureName) {
        m_bundleName = bundleName;
        m_featureName = featureName;
        m_humanReadableFeatureName = humanReadableFeatureName;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()//
            .append(m_bundleName)//
            .append(m_featureName)//
            .append(m_humanReadableFeatureName)//
            .toHashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        TableStoreFormatInformation other = (TableStoreFormatInformation)obj;
        return new EqualsBuilder()//
                .append(m_bundleName, other.m_bundleName)//
                .append(m_featureName, other.m_featureName)//
                .append(m_humanReadableFeatureName, other.m_humanReadableFeatureName)//
                .isEquals();
    }

    @Override
    public String toString() {
        return m_humanReadableFeatureName;
    }

    @Override
    public Optional<String> getBundleSymbolicName() {
        return Optional.of(m_bundleName);
    }

    @Override
    public Optional<String> getFeatureSymbolicName() {
        return Optional.of(m_featureName);
    }

    @Override
    public String getComponentName() {
        return m_humanReadableFeatureName;
    }

    /** For known class names return the bundle information, otherwise an empty optional. */
    static final Optional<TableStoreFormatInformation> of(final String formatFullyQualifiedName) {
        if (Objects.equals(formatFullyQualifiedName, "org.knime.parquet.ParquetTableStoreFormat")) {
            return Optional.of(new TableStoreFormatInformation("org.knime.parquet",
                "org.knime.features.parquet.feature.group", "Parquet Table Store Format"));
        } else if (Objects.equals(formatFullyQualifiedName, "org.knime.orc.OrcTableStoreFormat")) {
            return Optional.of(new TableStoreFormatInformation("org.knime.orc", "org.knime.features.orc.feature.group",
                "ORC Table Store Format"));
        } else {
            return Optional.empty();
        }
    }

    /** Represents missing {@link org.knime.core.data.TableBackend}. Technically speaking a table backend has nothing
     * to do with a table store format but the error handling code path is so similar that we (mis-)use this class here.
     * @param bundle ...
     * @param feature ...
     * @param shortname usually the value of {@link org.knime.core.data.TableBackend#getShortName()}.
     * @return the info.
     * @since 4.3
     */
    public static final TableStoreFormatInformation forTableBackend(final String bundle, final String feature,
        final String shortname) {
        return new TableStoreFormatInformation(bundle, feature, shortname);
    }

}