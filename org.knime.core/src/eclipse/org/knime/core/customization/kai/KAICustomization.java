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
 *   Jun 5, 2024 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.customization.kai;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents customization settings for K-AI.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param suggestExtensions whether K-AI should suggest extensions in its Q&A mode
 * @param hub allows to customize which hubs K-AI is allowed to connect to
 * @param disable whether to disable all K-AI related features
 * @since 5.3
 * @noreference This class is not intended to be referenced by clients.
 */
public record KAICustomization(
    @JsonProperty("suggestExtensions") boolean suggestExtensions, KAIHubCustomization hub,
    boolean disable) {

    /**
     * Default customization.
     */
    public static final KAICustomization DEFAULT = new KAICustomization(true, KAIHubCustomization.DEFAULT, false);

    /**
     * @param suggestExtensions whether K-AI should suggest extensions or not
     * @param hub customization of the hubs K-AI is allowed to use as backend
     * @param disable whether to disable all K-AI related features
     * @since 5.4
     */
    public KAICustomization(final boolean suggestExtensions, final KAIHubCustomization hub,
    final boolean disable) {
        this.suggestExtensions = suggestExtensions;
        this.hub = Objects.requireNonNullElse(hub, KAIHubCustomization.DEFAULT);
        this.disable = disable;
    }
}
