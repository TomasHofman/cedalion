package ci_jobs

import util.JobSharedUtils

class Builder {

    String jobName
    String repoName
    String branch
    String repoUrl
    String mavenGoals = '' // defaults to clean install in hera
    String javaHome=''

    void validate_properties() {
        if (jobName == null) {
            jobName = 'ci-' + repoName
        }
        if (repoUrl == null) {
            repoUrl = 'https://github.com/jboss-set/' + repoName
        }
    }

    def buildBashJob(factory) {
        validate_properties()
        factory.with {
            pipelineJob(jobName) {
                // for some reason readFileFromWorkspace doesn't work from within baseJob call
                definition {
                    cps {
                        script(readFileFromWorkspace('pipelines/bash-pipeline'))
                        sandbox()
                    }
                }
                baseJob(delegate)
                parameters {
                    JobSharedUtils.gitParameters(delegate, repoUrl, branch)
                    stringParam {
                        name("PATH_TO_SCRIPT")
                        defaultValue("./tests/tests-suite.sh")
                    }
                    stringParam {
                        name("BUILD_PODMAN_IMAGE")
                        defaultValue("localhost/bashomatons")
                    }
                }
            }
        }
    }

    def buildMvnJob(factory) {
        validate_properties()
        factory.with {
            pipelineJob(jobName) {
                // for some reason readFileFromWorkspace doesn't work from within baseJob call
                definition {
                    cps {
                        script(readFileFromWorkspace('pipelines/mvn-pipeline'))
                        sandbox()
                    }
                }
                baseJob(delegate)
                parameters {
                    JobSharedUtils.gitParameters(delegate, repoUrl, 'master')
                    JobSharedUtils.toolsDirParameters(delegate,'/opt', '/opt')
                    JobSharedUtils.podmanImageParameter(delegate)
                    JobSharedUtils.mavenParameters(params: delegate, javaHome: javaHome)
                    // override MAVEN_OPTS to add -Dnorpm
                    stringParam {
                        name ("MAVEN_OPTS")
                        defaultValue("-Dmaven.wagon.http.ssl.insecure=true -Dhttps.protocols=TLSv1.2  -Dnorpm")
                    }
                    stringParam {
                        name ("MAVEN_GOALS")
                        defaultValue(mavenGoals)
                    }
                    stringParam {
                        name("JAVA_HOME")
                        defaultValue("/opt/oracle/jdk11-latest")
                    }
                }
            }
        }
    }

    void baseJob(delegate) {
        delegate.with {
            JobSharedUtils.defaultBuildDiscarder(delegate)
            triggers {
                scm(JobSharedUtils.DEFAULT_SCHEDULE)
                cron('@daily')
            }
        }
    }
}
