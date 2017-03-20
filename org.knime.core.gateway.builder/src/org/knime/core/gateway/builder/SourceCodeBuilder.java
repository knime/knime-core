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
 */
package org.knime.core.gateway.builder;

import static java.lang.String.format;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.knime.core.gateway.codegen.EntityGenerator;
import org.knime.core.gateway.codegen.ServiceGenerator;
import org.knime.core.gateway.codegen.SourceCodeGenerator;
import org.knime.core.gateway.codegen.def.EntityDef;
import org.knime.core.gateway.codegen.def.ObjectDef;
import org.knime.core.gateway.codegen.def.ServiceDef;
import org.knime.core.gateway.codegen.def.ServiceDefUtilGenerator;
import org.knime.core.gateway.codegen.spec.ObjectSpec;
import org.knime.core.gateway.codegen.spec.ObjectSpecUtilGenerator;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

/**
 * A project builder that generates java source code from velocity templates and json-files for definition and
 * configuration.
 *
 * In order to activate this builder for a project, add the following lines to the <code>.project</code> file:
 *
 * <pre>
  &ltbuildCommand&gt
     &ltname&gtorg.knime.core.gateway.builder.SourceCodeBuilder&lt/name&gt
     &ltarguments&gt
     &lt/arguments&gt
  &lt/buildCommand&gt
 * </pre>
 *
 * The builder expects the following structure and conventions:
 * <ul>
 * <li>a <b>src-gen</b> root directory</li>
 * <li>a <b>src-gen/conf.json</b> global configuration file, e.g.
 *
 * <pre>
   {
    "project-dependencies" : [],
    "default-output-dir" : "src/generated",
    "default-package" : "org.knime.core.gateway"
   }
 * </pre>
 *
 * <i>project-dependencies</i>: are the exact names of dependent project to read object specifications and api
 * definitions from (must be at the same level as the projet the builder is run on.<br>
 * <i>default-output-dir</i>: the default output directory to write the generated files to<br>
 * <i>default-package</i>: the default package<br>
 * See also {@link BuilderConfig}.</li>
 * <li>an <i>optional</i> <b>src-gen/api</b> directory containing the api definition files (e.g. for services and
 * entities). They can be organised in further sub-directories. The json-files are a kind of interface definition
 * language (IDL) and describes services and entities. See {@link ServiceDef} and {@link EntityDef} for further
 * details.</li>
 * <li>a <b>src-gen/spec</b> directory containing the object specifications (.json) and templates for source code
 * generation. For each velocity-template (*.vm) must exactly be one json-specification file of the exact same name
 * (except the file extension). E.g. 'ExampleTemplate.vm' must be accompanied by 'ExampleTemplate.json'. The json-spec
 * file looks like this:
 *
 * <pre>
   {
    "id" : "api",
    "type" : "entity",
    "pattern" : "##name##",
    "package-prefix" : "org.knime.core.gateway.v0",
    "package-suffix" : "",
    "output-dir" : "src/generated",
    "import-specs" : [],
    "field-import-specs" : []
   }
 * </pre>
 *
 * For more detail of the fields please refer to {@link ObjectSpec}.
 * </ul>
 *
 * Current shortcomings, issues and other remarks:
 * <ul>
 * <li>for every project build the source files (json and templates) are newly read in</li>
 * <li>either the project itself or at least on dependent project MUST contain the object definition json-files (src-gen/api)</li>
 * <li>remark on the project dependencies (as defined in the src-gen/conf.json file) and imports: the dependencies defined
 * there serve the only purpose to get the imports right, i.e. imports on classes that have been automatically generated by other
 * projects. Can maybe be simplified by using eclipse mechanisms to organize the imports automatically.</li>
 * <li>all dependent projects need be checked out and reside in the same folder as the current project</li>
 * <li>...</li>
 * </ul>
 *
 *
 * TODO generalize the builder a bit such that it somehow works with {@link SourceCodeGenerator}s and associated
 * {@link ObjectDef}s, templates and config files in general
 *
 * @author Martin Horn, University of Konstanz
 */
public class SourceCodeBuilder extends IncrementalProjectBuilder {

    private static final Logger LOGGER = Logger.getLogger(SourceCodeBuilder.class);

    private static final String ROOT_DIR_NAME = "src-gen";

    private static final String API_DIR_NAME = "api";

    private static final String SPEC_DIR_NAME = "spec";

    private static final String CONFIG_FILE_NAME = "conf.json";

    private static final ObjectMapper OBJECT_MAPPER =
        new ObjectMapper().setSerializationInclusion(Include.NON_NULL).registerModule(new Jdk8Module());

    @Override
    protected IProject[] build(final int kind, final Map<String, String> args, final IProgressMonitor monitor)
        throws CoreException {

        LOGGER.info("Source code builder started ...");

        IProject project = getProject();

        switch (kind) {

            case FULL_BUILD:

                IPath workspaceRoot = project.getLocation().makeAbsolute().removeLastSegments(1);

                //read config file and those for all dependencies and put them all into one set (TODO handle circular dependencies
                //and allow a dependency-depth of more than just one level)
                IPath projectRoot = project.getLocation().makeAbsolute();
                BuilderConfig configFile = readConfigFile(projectRoot);
                if (configFile == null) {
                    //probably not a source code generation project
                    return null;
                }
                Map<IPath, BuilderConfig> configFiles = new HashMap<>();
                configFile.getProjectDependencies().stream().forEach(s -> {
                    IPath depProjectRoot = workspaceRoot.append(s);
                    BuilderConfig depConfigFile = readConfigFile(depProjectRoot);
                    if (depConfigFile != null) {
                        configFiles.put(depProjectRoot, depConfigFile);
                    }
                });

                //read 'this' definitions and generate the ServiceDefUtil file
                List<ServiceDef> thisServiceDefs = new ArrayList<ServiceDef>();
                List<EntityDef> thisEntityDefs = new ArrayList<EntityDef>();
                readAPIDefs(projectRoot, thisServiceDefs, thisEntityDefs);

                //generate the ServiceDefUtil class the allows programmatic access to all services defs
                if (thisServiceDefs.size() > 0) {
                    new ServiceDefUtilGenerator(thisServiceDefs,
                        projectRoot.append(configFile.getDefaultOutputDir()).toOSString(),
                        configFile.getDefaultPackage()).generate();
                }

                // read idl source files (json) into data structures
                List<ServiceDef> serviceDefs = new ArrayList<ServiceDef>();
                List<EntityDef> entityDefs = new ArrayList<EntityDef>();
                configFiles.keySet().forEach(p -> readAPIDefs(p, serviceDefs, entityDefs));
                serviceDefs.addAll(thisServiceDefs);
                entityDefs.addAll(thisEntityDefs);

                //read 'this' specs
                Map<Path, ObjectSpec> thisSpecs = new HashMap<>();
                try {
                    readAll(thisSpecs, ObjectSpec.class,
                        Paths.get(projectRoot.append(ROOT_DIR_NAME).append(SPEC_DIR_NAME).toOSString()));
                } catch (IOException ex) {
                    throw new RuntimeException("Problems reading the object specs", ex);
                }

                //generate the ObjectSpecUtil file that allows one to programmatically access the available object specs
                new ObjectSpecUtilGenerator(thisSpecs.values(),
                    projectRoot.append(configFile.getDefaultOutputDir()).toOSString(), configFile.getDefaultPackage())
                        .generate();

                //read all 'dependent' specs
                Map<Path, ObjectSpec> specsByPath = new HashMap<>();
                configFiles.keySet().forEach(p -> {
                    try {
                        readAll(specsByPath, ObjectSpec.class,
                            Paths.get(p.append(ROOT_DIR_NAME).append(SPEC_DIR_NAME).toOSString()));
                    } catch (IOException ex) {
                        throw new RuntimeException("Problems reading the object specs", ex);
                    }
                });
                specsByPath.putAll(thisSpecs);
                Map<String, ObjectSpec> specsById = new HashMap<>();
                specsByPath.values().forEach(s -> {
                    specsById.put(s.getId(), s);
                });

                //generate source files
                //TODO possibly delete files prior to generation
                for (Entry<Path, ObjectSpec> entry : thisSpecs.entrySet()) {
                    //possible TODO here (not urgent) - use the abstract SourceFileGenerator class here
                    //and configure these in a generic way

                    String outputFolder = projectRoot.append(entry.getValue().getOutputDir()).toOSString();
                    if (!new File(outputFolder).exists()) {
                        throw new IllegalStateException("Output folder doesn't exist: " + outputFolder);
                    }

                    //get field import specs
                    List<ObjectSpec> fieldImportSpecs = entry.getValue().getFieldImportSpecs().stream().map(s -> {
                        ObjectSpec os = specsById.get(s);
                        if (os == null) {
                            //spec id couldn't be found -> very likely a missing dependency
                            throw new IllegalStateException("Object spec with id '" + s
                                + "' couldn't be find. Please check the dependencies in the 'conf.json'.");
                        }
                        return os;
                    }).filter(os -> os.getType().equals(ObjectSpec.ObjecType.ENTITY.id())).collect(Collectors.toList());

                    if (entry.getValue().getType().equals(ObjectSpec.ObjecType.SERVICE.id())) {
                        //the assumption here is that the spec file (.json) has the same name as the template (.vm)
                        ServiceGenerator serviceGen = new ServiceGenerator(serviceDefs, outputFolder,
                            entry.getKey().toString().replace(".json", ".vm"), entry.getValue());

                        serviceGen.setEntityFieldsImports(fieldImportSpecs);

                        //get import specs
                        List<ObjectSpec> importSpecs =
                            entry.getValue().getImportSpecs().stream().map(s -> specsById.get(s))
                                .filter(s -> s.getType().equals(ObjectSpec.ObjecType.SERVICE.id()))
                                .collect(Collectors.toList());
                        serviceGen.setServiceImport(importSpecs);

                        serviceGen.generate();
                    }
                    if (entry.getValue().getType().equals(ObjectSpec.ObjecType.ENTITY.id())) {
                        //the assumption here is that the spec file (.json) has the same name as the template (.vm)
                        EntityGenerator entityGen = new EntityGenerator(entityDefs, outputFolder,
                            entry.getKey().toString().replace(".json", ".vm"), entry.getValue());
                        entityGen.setEntityFieldsImports(fieldImportSpecs);

                        //get import specs
                        List<ObjectSpec> importSpecs =
                            entry.getValue().getImportSpecs().stream().map(s -> specsById.get(s))
                                .filter(s -> s.getType().equals(ObjectSpec.ObjecType.ENTITY.id()))
                                .collect(Collectors.toList());
                        entityGen.setEntityImport(importSpecs);

                        entityGen.generate();
                    }
                }

                break;

            case INCREMENTAL_BUILD:
                break;

            case AUTO_BUILD:
                break;
        }

        project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());

        LOGGER.info("Source code generated and project refreshed.");
        return null;
    }

    /**
     * @param p the absolute path to the project root
     * @return the builder configuration or <code>null</code> if it couldn't be found
     */
    private static BuilderConfig readConfigFile(final IPath p) {
        // check for the root directory and read configuration file
        String rootDir = p.append(ROOT_DIR_NAME).toOSString();
        if (!new File(rootDir).exists()) {
            //source code generation root dir found - just return and do nothing
            LOGGER.warn(format(
                "The folder '%1$s' doesn't exist in project '%2$s'. Not assumed to be a source code generation project.",
                ROOT_DIR_NAME, p.toString()));
            return null;
        }
        String configFile = p.append(ROOT_DIR_NAME).append(CONFIG_FILE_NAME).toOSString();
        if (!new File(configFile).exists()) {
            throw new IllegalStateException(
                format("The source code builder config file '%1$s' doesn't exist!", configFile));
        }

        BuilderConfig builderConf;
        try {
            builderConf = readFromJSON(Paths.get(configFile), BuilderConfig.class);
            return builderConf;
        } catch (IOException ex) {
            throw new RuntimeException("Builder config file cannot be read.", ex);
        }
    }

    /**
     * @param p the absolute path to the api directory - if the directory doesn't exist, it will just be skipped
     * @param serviceDefs the service def list to be filled
     * @param entityDefs the entity def list ot be filled
     */
    private static void readAPIDefs(final IPath p, final List<ServiceDef> serviceDefs,
        final List<EntityDef> entityDefs) {
        File apiDir = new File(p.append(ROOT_DIR_NAME).append(API_DIR_NAME).toOSString());
        if (!apiDir.exists()) {
            LOGGER.warn(format("The api directory '%1$s' for project '%2$s' doesn't exist. Skipped.", API_DIR_NAME,
                p.toOSString()));
            return;
        }
        try {
            serviceDefs.addAll(ObjectDef.readAll(ServiceDef.class, apiDir.toPath()));
            entityDefs.addAll(ObjectDef.readAll(EntityDef.class, apiDir.toPath()));
        } catch (IOException e) {
            throw new RuntimeException("API definition files cannot be parsed", e);
        }
    }

    /**
     * @param path the file path
     * @param cl the type to read in (jackson-annotated)
     * @param map a map to be filled with the file path and the object spec read in
     */
    private static <T> void readAll(final Map<Path, T> map, final Class<T> cl, final Path path) throws IOException {
        PathMatcher jsonMatcher = FileSystems.getDefault().getPathMatcher("glob:**/*.json");
        try (Stream<Path> allFilesStream = Files.walk(path)) {
            Iterator<Path> pathIterator = allFilesStream.filter(jsonMatcher::matches).iterator();
            while (pathIterator.hasNext()) {
                Path jsonFileFPath = pathIterator.next();
                T struct;
                try {
                    struct = readFromJSON(jsonFileFPath, cl);
                } catch (JsonProcessingException e) {
                    System.err.println("Error in " + jsonFileFPath);
                    throw e;
                }
                if (cl.isInstance(struct)) {
                    map.put(jsonFileFPath, cl.cast(struct));
                }
            }
        }
    }

    private static <C> C readFromJSON(final Path jsonFilePath, final Class<C> clazz) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(jsonFilePath)) {
            return OBJECT_MAPPER.readValue(reader, clazz);
        }
    }

}
