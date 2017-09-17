
The org.knime.parquet bundle currently contains all required libraries to get parquet to work to the extent
needed for a proper KNIME integration. This currently does not include the ability to write hdfs or any other
advanced parquet features. This may change as the integration is enhanced.

Attempts to have all dependencies modeled via proper OSGi references failed. Below is the block that we 
(temporarily) put into '/org.knime.features.externals/maven2osgi/pom.xml' but then removed due to a long
chain of extra bundles pulled into the external dependecy list and problems during KNIME startup (OSGi resolver
implementation (Apache Felix) took hours to start). Also, it seems wrong to have bundles such as (and outdated)
hadoop-core and codehaus.jackson part of the publicly available update site/target platform.

                                <artifact>
                                    <id>org.apache.parquet:parquet-avro:1.8.1</id>
                                    <source>true</source>
                                </artifact>                                
                                <artifact>
                                    <id>org.apache.parquet:parquet-format:2.3.1</id>
                                    <source>true</source>
                                </artifact>                                
                                <!--
                                <artifact>
                                    <id>org.apache.avro:avro:1.7.7</id>
                                    <source>true</source>
                                    <override>true</override>
                                    <instructions>
                                        <Import-Package>!sun.misc,*;resolution:=optional</Import-Package>
                                    </instructions>
                                        <Bundle-SymbolicName>org.apache.avro</Bundle-SymbolicName>
                                </artifact>                                
                                -->
                                <artifact>
                                    <id>org.apache.hadoop:hadoop-core:1.2.1</id>
                                    <source>true</source>
                                </artifact>                                
                                <artifact>
                                    <id>commons-configuration:commons-configuration:1.10</id>
                                    <source>true</source>
                                </artifact>                                
                                <artifact>
                                    <id>org.codehaus.jackson:jackson-core-asl:1.9.13</id>
                                    <source>true</source>
                                    <override>true</override>
                                    <instructions>
                                        <Bundle-SymbolicName>org.codehaus.jackson.jackson-core-asl</Bundle-SymbolicName>
                                    </instructions>
                                </artifact>                                
