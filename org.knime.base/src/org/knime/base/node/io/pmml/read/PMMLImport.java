/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 * ------------------------------------------------------------------------
 */
package org.knime.base.node.io.pmml.read;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.dmg.pmml40.PMMLDocument;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.pmml.PMMLDataDictionaryTranslator;
import org.knime.core.node.port.pmml.PMMLMiningSchemaTranslator;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.port.pmml.PMMLPortObjectSpecCreator;
import org.knime.core.pmml.PMMLUtils;
import org.w3c.dom.Document;

/**
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 */
public class PMMLImport {
    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(PMMLImport.class);

    private PMMLPortObject m_portObject;

    /**
     * Reads and validates the passed file and creates the
     * {@link PMMLPortObjectSpec} from the content of the file.
     *
     * @param file containing the PMML model
     * @param update try to update the PMML to version 4.0 if an older version
     *      is imported
     * @throws IOException if something goes wrong reading the file
     * @throws XmlException if an invalid PMML file is passed
     * @throws IllegalArgumentException if the input file is invalid or has
     *      invalid content
     */
    public PMMLImport(final File file, final boolean update)
            throws IOException, XmlException, IllegalArgumentException {
        if (file == null) {
            throw new IllegalArgumentException("File must not be null!");
        }
        if (!file.exists()) {
            throw new IllegalArgumentException("File " + file.getName()
                    + " does not exists.");
        }

        PMMLDocument pmmlDoc = null;
        XmlObject xmlDoc = XmlObject.Factory.parse(file);
        if (xmlDoc instanceof PMMLDocument) {
            pmmlDoc = (PMMLDocument)xmlDoc;
        } else {
            /* Try to recover when reading a PMML 3.x document that
             * was produced by KNIME by just replacing the PMML version and
             * namespace or when the recover flag is set. */
            if (update || PMMLUtils.isOldKNIMEPMML(xmlDoc)) {
                try {
                    String updatedPMML
                            = PMMLUtils.getUpdatedVersionAndNamespace(xmlDoc);
                    /* Parse the modified document and assign it to a
                     * PMMLDocument.*/
                    pmmlDoc = PMMLDocument.Factory.parse(updatedPMML);
                } catch (Exception e) {
                    throw new RuntimeException(
                            "Parsing of PMML v 3.x document failed.", e);
                }
                if (!update) {
                    LOGGER.info("KNIME produced PMML 3.x updated to PMML 4.0.");
                } else {
                    LOGGER.info("Older PMML version updated to PMML 4.0.");
                }
            }
        }

        if (pmmlDoc == null || !pmmlDoc.validate()) {
            throw new IllegalArgumentException("File \"" + file
                    + "\" is not a valid PMML 4.0 file.");
        }
        init(pmmlDoc);
    }

    /**
     * Reads and validates the passed file and creates the
     * {@link PMMLPortObjectSpec} from the content of the file.
     *
     * @param file containing the PMML model
     * @throws IOException if something goes wrong reading the file
     * @throws XmlException if an invalid PMML file is passed
     * @throws IllegalArgumentException if the input file is invalid or has
     *      invalid content
     */
    public PMMLImport(final File file) throws IOException, XmlException,
            IllegalArgumentException {
        this(file, false);
    }

    /**
     * @param pmmlDoc
     */
    private void init(final PMMLDocument pmmlDoc) {
        PMMLDataDictionaryTranslator dictTrans
                = new PMMLDataDictionaryTranslator();
        dictTrans.initializeFrom(pmmlDoc);
        DataTableSpec tableSpec = dictTrans.getDataTableSpec();
        List<String> activeDerivedFields = dictTrans.getActiveDerivedFields();
        PMMLPortObjectSpecCreator specCreator
                = new PMMLPortObjectSpecCreator(tableSpec);

        PMMLMiningSchemaTranslator miningTrans
                = new PMMLMiningSchemaTranslator();
        miningTrans.initializeFrom(pmmlDoc);

        List<String> activeFields = new ArrayList<String>();
        List<String> miningFields = miningTrans.getActiveFields();
        /* If we have a model all active fields of the data dictionary
         * are passed through the mining schema. */
        activeFields.addAll(miningFields);
        activeFields.addAll(activeDerivedFields);
        specCreator.setLearningColsNames(activeFields);
        specCreator.setTargetColsNames(miningTrans.getTargetFields());

        PMMLPortObjectSpec portObjectSpec = specCreator.createSpec();
        m_portObject = new PMMLPortObject(portObjectSpec, pmmlDoc);
    }

    /**
     * Reads and validates the passed file and creates the
     * {@link PMMLPortObjectSpec} from the content of the file.
     *
     * @param doc the document containing the PMML model
     * @throws IOException if something goes wrong reading the file
     * @throws XmlException if an invalid PMML file is passed
     * @throws IllegalArgumentException if the input file is invalid or has
     *      invalid content
     */
    public PMMLImport(final Document doc) throws IOException, XmlException,
            IllegalArgumentException {
        if (doc == null) {
            throw new IllegalArgumentException("Document must not be null!");
        }

        PMMLDocument pmmlDoc = PMMLDocument.Factory.parse(doc);
        if (!pmmlDoc.validate()) {
            throw new IllegalArgumentException("Document \"" + doc
                    + "\" is not a valid PMML 4.0 file.");
        }
        init(pmmlDoc);
    }


    /**
     *
     * @return the parsed port object spec (data dictionary and mining schema)
     */
    public PMMLPortObjectSpec getPortObjectSpec() {
        return m_portObject.getSpec();
    }

    /**
     *
     * @return the parsed PMML model
     */
    public PMMLPortObject getPortObject() {
        return m_portObject;
    }



}
