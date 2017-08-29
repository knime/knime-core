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
 *   Mar 20, 2017 (hornm): created
 */
package org.knime.gateway.codegen.def;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Properties;

import org.apache.commons.collections.ExtendedProperties;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.knime.gateway.codegen.MyResourceLoader;
import org.knime.gateway.codegen.SourceCodeGenerator;

/**
 * Generates the 'ServiceDefUtil' from service definitions.
 *
 * @author Martin Horn, University of Konstanz
 */
public class ServiceDefUtilGenerator extends SourceCodeGenerator {

    private final static String DEF_UTIL_TEMPLATE = "ServiceDefUtil";

    private Collection<ServiceDef> m_serviceDefs;
    private String m_outputDir;
    private String m_defaultPackage;

    /**
     * @param serviceDefs
     * @param outputDir
     * @param defaultPackage
     *
     */
    public ServiceDefUtilGenerator(final Collection<ServiceDef> serviceDefs, final String outputDir, final String defaultPackage) {
        m_serviceDefs = serviceDefs;
        m_outputDir = outputDir;
        m_defaultPackage = defaultPackage;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void generate() {

        try {
            VelocityEngine velocity = new VelocityEngine();
            VelocityContext context = new VelocityContext();

            Properties p = new Properties();
            //set the root as template directory (template files need to be provided as absolute paths)
            p.setProperty("resource.loader", "myloader");
            p.setProperty("myloader.resource.loader.class", MyResourceLoader.class.getName());
            velocity.setExtendedProperties(ExtendedProperties.convertProperties(p));
            velocity.init();

            context.put("serviceDefs", m_serviceDefs);
            context.put("package", m_defaultPackage);
            Template template = null;
            try {
                template = velocity.getTemplate(DEF_UTIL_TEMPLATE + ".vm");
            } catch (ResourceNotFoundException rnfe) {
                throw new RuntimeException("Example : error : cannot find template", rnfe);
            } catch (ParseErrorException pee) {
                throw new RuntimeException("Example : Syntax error in template", pee);
            }

            /*
             *  Now have the template engine process your template using the
             *  data placed into the context.  Think of it as a  'merge'
             *  of the template and the data to produce the output stream.
             */
            String[] packages = m_defaultPackage.split("\\.");
            Path outputPath = Paths.get(m_outputDir, packages);
            Files.createDirectories(outputPath);

            try (BufferedWriter writer = Files.newBufferedWriter(outputPath.resolve(DEF_UTIL_TEMPLATE + ".java"))) {
                if (template != null) {
                    template.merge(context, writer);
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }

    }

}
