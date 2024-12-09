#!groovy
def BN = (BRANCH_NAME == 'master' || BRANCH_NAME.startsWith('releases/')) ? BRANCH_NAME : 'releases/2025-07'

library "knime-pipeline@$BN"

properties([
    pipelineTriggers([upstream(
        'knime-shared/' + env.BRANCH_NAME.replaceAll('/', '%2F') +
        ', knime-core-table/' + env.BRANCH_NAME.replaceAll('/', '%2F')
    )]),
    parameters(workflowTests.getConfigurationsAsParameters()),
    buildDiscarder(logRotator(numToKeepStr: '5')),
    disableConcurrentBuilds()
])

try {
    parallel (
        'Tycho Build': {
            knimetools.defaultTychoBuild('org.knime.update.core')
        },
        'Integrated Workflowtests': {
                workflowTests.runIntegratedWorkflowTests(profile: 'test')
         },
     )

    workflowTests.runTests(
        dependencies: [
            repositories: [
                'knime-aws',
                'knime-buildworkflows',
                'knime-chemistry',
                'knime-cef',
                'knime-cloud',
                'knime-cluster',
                'knime-conda',
                'knime-core',
                'knime-core-columnar',
                'knime-database',
                'knime-datageneration',
                'knime-distance',
                'knime-ensembles',
                'knime-filehandling',
                'knime-gateway',
                'knime-jep',
                'knime-js-base',
                'knime-json',
                'knime-kerberos',
                'knime-productivity-oss',
                'knime-python-legacy',
                'knime-python',
                'knime-r',
                'knime-reporting',
                'knime-reporting2',
                'knime-rest',
                'knime-streaming',
                'knime-textprocessing',
                'knime-virtual',
                'knime-workbench',
                'knime-xml',
                'knime-credentials-base'
            ],
            ius: [ 
                'com.knime.enterprise.client.filehandling',
                'org.knime.chem.types'
            ]
        ]
    )

    stage('Sonarqube analysis') {
        env.lastStage = env.STAGE_NAME
        workflowTests.runSonar()
    }
} catch (ex) {
    currentBuild.result = 'FAILURE'
    throw ex
} finally {
    notifications.notifyBuild(currentBuild.result);
}
/* vim: set shiftwidth=4 expandtab smarttab: */
