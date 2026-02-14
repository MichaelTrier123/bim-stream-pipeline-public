package com.myorg;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.iam.*;
import software.amazon.awscdk.services.secretsmanager.ISecret;
import software.amazon.awscdk.services.secretsmanager.Secret;
import software.constructs.Construct;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ComputeStack extends Stack {
    public ComputeStack(final Construct scope, final String id, final ComputeProps props) {
        super(scope, id, props.getStackProps());

        Cluster cluster = Cluster.Builder.create(this, "BimCluster")
                .vpc(props.getVpc())
                .build();

        cluster.addCapacity("DefaultAutoScalingGroupCapacity", AddCapacityOptions.builder()
                .instanceType(InstanceType.of(InstanceClass.T3, InstanceSize.SMALL))
                .desiredCapacity(1)
                .vpcSubnets(SubnetSelection.builder()
                        .subnetGroupName("Private")
                        .build())
                .build());

        Role taskRole = Role.Builder.create(this, "TaskRole")
                .assumedBy(new ServicePrincipal("ecs-tasks.amazonaws.com"))
                .build();

        ISecret elasticApmSecret = Secret.fromSecretNameV2(
                this,
                "ElasticApmSecret",
                "elastic/apm/dev"
        );

        BimWorkerTask taskDef = new BimWorkerTask(this, "BimWorkerTask", props, taskRole, elasticApmSecret);

        props.getDbSecret().grantRead(taskRole);


        props.getInputBucket().grantReadWrite(taskRole);
        props.getConversionQueue().grantConsumeMessages(taskRole);


        Objects.requireNonNull(taskDef.getExecutionRole()).addManagedPolicy(
                ManagedPolicy.fromAwsManagedPolicyName("service-role/AmazonECSTaskExecutionRolePolicy")
        );
        props.getWorkerRepo().grantPull(taskDef.getExecutionRole());
        props.getProcessorRepo().grantPull(taskDef.getExecutionRole());

        Ec2Service service = Ec2Service.Builder.create(this, "BimService")
                .cluster(cluster)
                .taskDefinition(taskDef)
                .minHealthyPercent(0)
                .maxHealthyPercent(100)
                .desiredCount(1)
                .vpcSubnets(SubnetSelection.builder()
                        .subnetGroupName("Private")
                        .build())
                .build();

        props.getGithubRole().addToPrincipalPolicy(PolicyStatement.Builder.create()
                .actions(Collections.singletonList("ecs:UpdateService"))
                .resources(Collections.singletonList(
                        "arn:aws:ecs:" + getRegion() + ":" + getAccount() + ":service/BimCluster/*"
                ))
                .build());
    }



}