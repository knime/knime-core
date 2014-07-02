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
 *   Jul 2, 2014 (wiswedel): created
 */
package org.knime.core.node.dialog;

import org.knime.core.node.NodeSettingsWO;

/**
 * Implemented by nodes whose execution not only depends on the node configuration (dialog) but also on additional
 * runtime parameters. These are, for instance dialog values set via a metanode or subnode dialog. Also nodes having
 * a view that sets view values are "value controlled".
 *
 * <p>Nodes implementing this interface and whose dialog component implements {@link ValueControlledDialogPane} will
 * have an additional method called when the dialog is opened. This allows them to display the currently used value
 * in the configuration dialog. E.g. a string quickform input has a default value of "foo" but the value as per
 * subnode configuration dialog is "bar" -- this will then be shown in the main config dialog of the QF node to
 * clarify why the current output is "bar" although it says "foo" in the dialog.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 * @since 2.10
 */
public interface ValueControlledNode {

    /** Save the currently used value to a node settings argument. This can be the full value or just a message saying
     * "Output is currently controlled by sub node dialog". The corresponding load method is
     * {@link ValueControlledDialogPane#loadCurrentValue(org.knime.core.node.NodeSettingsRO)}
     * @param content To save to, not null.
     */
    public void saveCurrentValue(final NodeSettingsWO content);

}
