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
 *   Jun 8, 2014 (wiswedel): created
 */
package org.knime.core.node.workflow;

import org.knime.core.node.workflow.FileWorkflowPersistor.LoadVersion;



/** Implemented by persistors reading workflows or templates (meta- or subnode) from file location.
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 * @noreference Not to be used by clients.
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public interface TemplateNodeContainerPersistor extends FromFileNodeContainerPersistor {

    /** @return The version of the workflow or template being loaded. */
    public LoadVersion getLoadVersion();

    /** @return true if the persistor represent a workflow project, false for metanodes and other templates. */
    boolean isProject();

    /** @return the mustWarnOnDataLoadError */
    public boolean mustWarnOnDataLoadError();

    /** Mark as dirty when loading completed. */
    public void setDirtyAfterLoad();

    /** Set a name that overloads the name as persisted in the worklow. Used to overwrite the name in
     * metanode templates (name is then derived from the folder name).
     * @param nameOverwrite the nameOverwrite to set
     */
    public void setNameOverwrite(final String nameOverwrite);

    /** @param templateInfo The new template information. This is a newly created link to the template that is
     * currently loaded.*/
    public void setOverwriteTemplateInformation(final MetaNodeTemplateInformation templateInfo);

}
