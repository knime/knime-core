
KNIME 1.2.0 All-in-One Developer version
============================================================

Quickstart guide:
- Extract the archive (if you read this here, you likely already did so).
- Launch eclipse.exe to start the Eclipse IDE, it will use the default workspace
  folder (called 'workspace') located in this directory to store the project files in.
- For further information see the manuals located in the 'doc' directory.


How to get additional features:
  KNIME allows to easily integrate new features. These often come under a
  different license and therefore are not included in this archive. Please
  see http://www.knime.org for any available extension. For
  example an R integration feature (http://www.r-project.org) and a 
  chemistry feature (using the chemistry development kit, CDK, http://cdk.sf.net/) 
  are available.

  A convenient way to download new KNIME features is via the KNIME 
  update site. In your current KNIME installation select 
  "File", "Update KNIME..." (or in the Developer Version via "Help", 
  "Software Updates",  "Find and Install..."). The Update Wizard opens. 
  If you expand the "KNIME" site, you see two main categories: 
  KNIME Base Package and KNIME Extensions. (In the All-in-One version
  of KNIME you also have the KNIME Development Wizard under the
  "KNIME Development" site available.)
  You may now select the features you want to install. If you select a 
  feature and receive an error about a configuration problem, click the 
  "Select Required" button. This automatically selects all required features.
  You need to restart KNIME after installing new extensions in order to get
  them activated.

  Another way to install new features is to download the extension in an archive
  file from our website (www.knime.org). Unpack the file into your KNIME 
  installation directory (so that all files of the archive's plugin directory 
  are stored in KNIME's plugin dir and the files of the feature directory are 
  stored in KNIME's feature dir). Restart KNIME to register the newly added 
  features.