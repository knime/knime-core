
KNIME SDK v2.0.0
============================================================

Quickstart guide:
- Extract the archive (if you read this here, you likely already did so).
- Launch eclipse to start the Eclipse IDE, select a workspace directory
  in the workspace prompt and continue from there.
  
Potential problem:
  Eclipse 3.3.2 (i.e. this version) has a known problem detecting 
  the system's xulrunner library (used to renderer html pages such as, 
  for instance the welcome page). This can lead to a crash of the application.
  This problem can be worked around by specifying the path to the xulrunner
  library on your system (usually located in /usr/lib/xulrunner-x.y.z or
  /usr/lib64/xulrunner-x.y.z). Add the following line to the eclipse.ini file
  (as last line)
    -Dorg.eclipse.swt.browser.XULRunnerPath=/usr/lib/xulrunner-x.y.z   

How to get additional features:
  KNIME allows for an easy integration of new features. These often 
  come under a different license and therefore are not included in 
  this archive. For example an R integration feature 
  (http://www.r-project.org) and a  chemistry feature (using the chemistry
  development kit, CDK, http://cdk.sf.net/) are available. These features
  are installed via the KNIME update site. In your current Eclipse installation
  select "Help", "Software Updates", "Find an Install". The Update Wizard 
  opens; select the features you want to install now from the KNIME category.
  If you select a feature and receive an error about a configuration problem, 
  click the "Select Required" button. This automatically selects all required 
  features. You need to restart Eclipse after installing new extensions in order 
  to get them activated.
  
  Note, the update site is also available as zip archive for a later offline
  installation. Refer to the knime.org website for details.
