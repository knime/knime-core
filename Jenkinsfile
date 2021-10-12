#!groovy
def BN = (BRANCH_NAME == 'master' || BRANCH_NAME.startsWith('releases/')) ? BRANCH_NAME : 'releases/2021-12'

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
                'knime-buildworkflows',
                'knime-chemistry',
                'knime-cloud',
                'knime-cluster',
                'knime-core',
                'knime-core-arrow',
                'knime-core-columnar',
                'knime-database',
                'knime-datageneration',
                'knime-distance',
                'knime-ensembles',
                'knime-filehandling',
                'knime-jep',
                'knime-js-base',
                'knime-json',
                'knime-kerberos',
                'knime-python',
                'knime-r',
                'knime-streaming',
                'knime-textprocessing',
                'knime-virtual'
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
