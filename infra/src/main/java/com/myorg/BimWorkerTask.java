package com.myorg;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.iam.IRole;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.secretsmanager.ISecret;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;

public class BimWorkerTask extends Ec2TaskDefinition {
    public BimWorkerTask(Construct scope, String id, ComputeProps props, IRole taskRole, ISecret elasticApmSecret) {
        super(scope, id, Ec2TaskDefinitionProps.builder()
                .family("BIMStreamWorker")
                .taskRole(taskRole)
                .networkMode(NetworkMode.AWS_VPC)
                .build());


        LogGroup logGroup = LogGroup.Builder.create(this, "BimWorkerLogs")
                .logGroupName("/ecs/bim-stream/" + AppConfig.ENVIRONMENT.toLowerCase())
                .retention(RetentionDays.ONE_WEEK)
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        LogDriver logDriverJava = LogDriver.awsLogs(AwsLogDriverProps.builder()
                .logGroup(logGroup)
                .streamPrefix("java")
                .build());

        LogDriver logDriverPython = LogDriver.awsLogs(AwsLogDriverProps.builder()
                .logGroup(logGroup)
                .streamPrefix("python")
                .build());

        this.addVolume(Volume.builder().name("shared-data").build());

        // Java Worker Container
        ContainerDefinition worker = this.addContainer("WorkerContainer", ContainerDefinitionOptions.builder()
                .image(ContainerImage.fromEcrRepository(props.getWorkerRepo(), "latest"))
                .memoryLimitMiB(450)
                .cpu(256)
                .logging(logDriverJava)
                .healthCheck(HealthCheck.builder()
                        .command(List.of("CMD-SHELL", "curl -fsS http://localhost:8080/actuator/health || exit 1"))
                        .interval(Duration.seconds(30))
                        .timeout(Duration.seconds(5))
                        .retries(3)
                        .startPeriod(Duration.seconds(60))
                        .build())
                .environment(Map.of(
                        "SPRING_PROFILES_ACTIVE", "aws",
                        "RDS_SECRET_NAME", props.getDbSecret().getSecretName(),
                        "S3_INPUT_BUCKET", props.getInputBucket().getBucketName(),
                        "SQS_QUEUE_URL", props.getConversionQueue().getQueueUrl(),
                        "JAVA_OPTS", "-javaagent:/elastic-apm-agent.jar",
                        "ELASTIC_APM_SERVICE_NAME", "bim-worker-java",
                        "ELASTIC_APM_ENVIRONMENT", "dev"

                ))
                .secrets(Map.of(
                        "ELASTIC_APM_SECRET_TOKEN",
                        Secret.fromSecretsManager(elasticApmSecret, "secretToken"),
                        "ELASTIC_APM_SERVER_URL",
                        Secret.fromSecretsManager(elasticApmSecret, "serverUrl")
                ))
                .build());

        // Python Processor Sidecar
        ContainerDefinition python = this.addContainer("IfcProcessorContainer", ContainerDefinitionOptions.builder()
                .image(ContainerImage.fromEcrRepository(props.getProcessorRepo(), "latest"))
                .memoryLimitMiB(512)
                .cpu(256)
                .logging(logDriverPython)
                .healthCheck(HealthCheck.builder()
                        .command(List.of("CMD-SHELL", "curl -fsS http://localhost:5000/health || exit 1"))
                        .interval(Duration.seconds(30))
                        .timeout(Duration.seconds(5))
                        .retries(3)
                        .startPeriod(Duration.seconds(30))
                        .build())
                .environment(Map.of(
                        "ELASTIC_APM_SERVICE_NAME", "ifc-processor-python",
                        "ELASTIC_APM_ENVIRONMENT", "dev"
                ))
                .secrets(Map.of(
                        "ELASTIC_APM_SECRET_TOKEN",
                        Secret.fromSecretsManager(elasticApmSecret, "secretToken"),
                        "ELASTIC_APM_SERVER_URL",
                        Secret.fromSecretsManager(elasticApmSecret, "serverUrl")
                ))
                .build());

        worker.addContainerDependencies(ContainerDependency.builder()
                .container(python)
                .condition(ContainerDependencyCondition.HEALTHY)
                .build());

        // Mount Shared Volume
        MountPoint sharedMount = MountPoint.builder().containerPath("/data").sourceVolume("shared-data").readOnly(false).build();
        worker.addMountPoints(sharedMount);
        python.addMountPoints(sharedMount);


    }
}