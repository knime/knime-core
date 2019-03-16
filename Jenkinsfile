#!groovy
def BN = BRANCH_NAME == "master" || BRANCH_NAME.startsWith("releases/") ? BRANCH_NAME : "master"

library "knime-pipeline@$BN"

properties([
  parameters([
    stringParam(
      name: 'KNIME_TP_P2',
      defaultValue: '${P2_REPO}/knime-tp/' + env.BRANCH_NAME.replaceAll("/", "%252F") + '/repository/',
      description: 'KNIME Target Platform P2 update site url.'
    ),
    stringParam(
      name: 'KNIME_SHARED_P2',
      defaultValue: '${P2_REPO}/knime-shared/' + env.BRANCH_NAME.replaceAll("/", "%252F") + '/repository/',
      description: 'org.knime.update.shared site url.'
    )
  ]),

  pipelineTriggers([
    triggers: [
      [
        $class: 'jenkins.triggers.ReverseBuildTrigger',
        upstreamProjects: "knime-shared/" + env.BRANCH_NAME.replaceAll("/", "%2F"), threshold: hudson.model.Result.SUCCESS
      ]
    ]
  ]),

  buildDiscarder(logRotator(numToKeepStr: '5')),
])

node {
  docker.withServer('tcp://proxy1:2375') {
    docker.image(slaves.DEFAULT_JAVA).inside {
      stage('Checkout Sources') {
        checkout scm
      }

      stage('Maven/Tycho Build') {
        withMavenJarsignerCredentials {
          sh '''
            export TEMP="${WORKSPACE}/tmp"
            rm -rf "${TEMP}"
            mkdir "${TEMP}"
            mvn --settings /var/cache/m2/settings.xml clean install
            rm -rf "${TEMP}"
          '''
	}
      }

      stage('Stage Build Artifacts') {
        sh '''
          #!/bin/bash -eux

          if [[ ! -d "/var/cache/build_artifacts/${JOB_NAME}/" ]]; then
            mkdir -p "/var/cache/build_artifacts/${JOB_NAME}/"
          else
            rm -Rf /var/cache/build_artifacts/${JOB_NAME}/*
          fi

          cp -a ${WORKSPACE}/org.knime.update.core/target/repository/ /var/cache/build_artifacts/${JOB_NAME}
        '''
      }
    }
  }
}

/* vim: set ts=4: */
