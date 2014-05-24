/*
 * ------------------------------------------------------------------------
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
 */
package org.knime.testing.internal.nodes.pmml;

import java.io.File;
import java.io.IOException;

import org.dmg.pmml.PMMLDocument;
import org.knime.core.data.xml.util.XmlDomComparer.Diff;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * This is the model implementation of PMMLDifferenceChecker.
 *
 *
 * @author Alexander Fillbrunn
 */
class PMMLDifferenceCheckerNodeModel extends NodeModel {

    /**
     * Constructor for the node model.
     */
    protected PMMLDifferenceCheckerNodeModel() {
        super(new PortType[]{PMMLPortObject.TYPE, PMMLPortObject.TYPE}, null);
    }

    private static final String CFG_CHK_DATADICT = "cfg_checkDataDictionaries";

    private static final String CFG_CHK_TRANSDICT = "cfg_checkTransformationDictionaries";

    private static final String CFG_CHK_HEADER = "cfg_checkHeader";

    private static final String CFG_CHK_MININGBUILDTASK = "cfg_checkMiningBuildTask";

    private static final String CFG_CHK_MODELVERIFICATION = "cfg_checkModelVerification";

    private static final String CFG_CHK_EXTENSIONS = "cfg_checkExtensions";

    private static final String CFG_CHK_SCHEMA = "cfg_checkSchema";

    /**
     * Creates a settings model for the check data dictionary setting.
     *
     * @return the settings model
     */
    static SettingsModelBoolean createCheckDataDictionariesSettingsModel() {
        return new SettingsModelBoolean(CFG_CHK_DATADICT, true);
    }

    /**
     * Creates a settings model for the check transformation dictionary setting.
     *
     * @return the settings model
     */
    static SettingsModelBoolean createCheckTransformationDictionarySettingsModel() {
        return new SettingsModelBoolean(CFG_CHK_TRANSDICT, true);
    }

    /**
     * Creates a settings model for the check header setting.
     *
     * @return the settings model
     */
    static SettingsModelBoolean createCheckHeaderSettingsModel() {
        return new SettingsModelBoolean(CFG_CHK_HEADER, true);
    }

    /**
     * Creates a settings model for the check mining build task setting.
     *
     * @return the settings model
     */
    static SettingsModelBoolean createCheckMiningBuildTaskSettingsModel() {
        return new SettingsModelBoolean(CFG_CHK_MININGBUILDTASK, true);
    }

    /**
     * Creates a settings model for the check model verification setting.
     *
     * @return the settings model
     */
    static SettingsModelBoolean createCheckModelVerificationSettingsModel() {
        return new SettingsModelBoolean(CFG_CHK_MODELVERIFICATION, true);
    }

    /**
     * Creates a settings model for the check extensions setting.
     *
     * @return the settings model
     */
    static SettingsModelBoolean createCheckExtensionsSettingsModel() {
        return new SettingsModelBoolean(CFG_CHK_EXTENSIONS, true);
    }

    /**
     * Creates a settings model for the check schema setting.
     *
     * @return the settings model
     */
    static SettingsModelBoolean createCheckSchemaSettingsModel() {
        return new SettingsModelBoolean(CFG_CHK_SCHEMA, true);
    }

    private SettingsModelBoolean m_checkDataDictionaries = createCheckDataDictionariesSettingsModel();

    private SettingsModelBoolean m_checkTransformationDictionaries = createCheckTransformationDictionarySettingsModel();

    private SettingsModelBoolean m_checkHeader = createCheckHeaderSettingsModel();

    private SettingsModelBoolean m_checkMiningBuildTask = createCheckMiningBuildTaskSettingsModel();

    private SettingsModelBoolean m_checkModelVerification = createCheckModelVerificationSettingsModel();

    private SettingsModelBoolean m_checkExtensions = createCheckExtensionsSettingsModel();

    private SettingsModelBoolean m_checkSchema = createCheckSchemaSettingsModel();

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inPorts, final ExecutionContext exec) throws Exception {
        PMMLDocument doc1 = PMMLDocument.Factory.parse(((PMMLPortObject)inPorts[0]).getPMMLValue().getDocument());
        PMMLDocument doc2 = PMMLDocument.Factory.parse(((PMMLPortObject)inPorts[1]).getPMMLValue().getDocument());

        PMMLDocumentComparer comp =
                new PMMLDocumentComparer(m_checkDataDictionaries.getBooleanValue(),
                        m_checkTransformationDictionaries.getBooleanValue(), m_checkHeader.getBooleanValue(),
                        m_checkMiningBuildTask.getBooleanValue(), m_checkModelVerification.getBooleanValue(),
                        m_checkExtensions.getBooleanValue(), m_checkSchema.getBooleanValue());

        Diff res = comp.areEqual(doc1, doc2);
        if (res != null) {
            throw new IllegalStateException("Mismatch at: " + crToString(res));
        }
        return new PortObject[0];
    }

    // Creates a string that gives the approximate mismatch location
    private String crToString(final Diff res) {

        StringBuilder builder = new StringBuilder();
        for (Node node : res.getReversePath()) {
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                String nodePath = node.getNodeName();
                NamedNodeMap attributes = node.getAttributes();
                if (attributes != null) {
                    Node id = attributes.getNamedItem("id");
                    if (id != null) {
                        nodePath += "[id=" + id.getNodeValue() + "]";
                    }
                }
                builder.insert(0, "/");
                builder.insert(0, nodePath);
            }
        }
        return builder.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_checkDataDictionaries.saveSettingsTo(settings);
        m_checkTransformationDictionaries.saveSettingsTo(settings);
        m_checkHeader.saveSettingsTo(settings);
        m_checkMiningBuildTask.saveSettingsTo(settings);
        m_checkModelVerification.saveSettingsTo(settings);
        m_checkExtensions.saveSettingsTo(settings);
        m_checkSchema.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_checkDataDictionaries.loadSettingsFrom(settings);
        m_checkTransformationDictionaries.loadSettingsFrom(settings);
        m_checkHeader.loadSettingsFrom(settings);
        m_checkMiningBuildTask.loadSettingsFrom(settings);
        m_checkModelVerification.loadSettingsFrom(settings);
        m_checkExtensions.loadSettingsFrom(settings);
        m_checkSchema.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_checkDataDictionaries.validateSettings(settings);
        m_checkTransformationDictionaries.validateSettings(settings);
        m_checkHeader.validateSettings(settings);
        m_checkMiningBuildTask.validateSettings(settings);
        m_checkModelVerification.validateSettings(settings);
        m_checkExtensions.validateSettings(settings);
        m_checkSchema.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir, final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir, final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

}
