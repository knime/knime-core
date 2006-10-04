
KNIME - the Konstanz Information Miner, version 1.1.0,
(including WEKA and the WEKA integration plugin - ).
============================================================

Quickstart guide:
- Extract the archive (if you read this here, you likely already did so).
- Under Windows launch knime.exe, for Linux use knime.sh (either double
  click in a file browser or execute it in a shell)
- KNIME will open and you can start assembling a workflow (KNIME will use 
  the folder 'workspace' contained in this directory to put the project
  files).


How to use additional plugins:
  KNIME allows to easily integrate new plugins. These often come under a
  different license and therefore are not included in this archive. Please
  see http://www.knime.org for any available third-party plugin. For
  example an R plugin (http://www.r-project.org) and a chemistry plugin
  (using the chemistry development kit, CDK, http://cdk.sf.net/) are
  available.

  To include a new plugin, extract the plugin archive into your KNIME
  installation directory. For instance, if you download the
  chemistry plugin and extract the zip file, it will create a folder
  hierarchy similiar to:

   - plugins
       |
       - org.knime.chem_1.1.0
            |
            - ...
       - ...
   - features
       |
       - org.knime.chem_1.1.0
            |
            - ...
  Copy the folders contained in the plugin archive to their destination
  or directly extract the archive/zip-file to the knime-directory.

  Restart KNIME to register the newly added plugins.