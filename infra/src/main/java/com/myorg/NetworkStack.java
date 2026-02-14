package com.myorg;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.*;
import software.constructs.Construct;
import java.util.Arrays;

public class NetworkStack extends Stack {
    private final IVpc vpc;

    public NetworkStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        this.vpc = Vpc.Builder.create(this, "BimVpc")
                .vpcName("bim-stream-vpc-" + AppConfig.ENVIRONMENT.toLowerCase())
                .ipAddresses(IpAddresses.cidr(AppConfig.VPC_CIDR))
                .maxAzs(2)
                .natGateways(1)
                .restrictDefaultSecurityGroup(false)
                .subnetConfiguration(Arrays.asList(
                        SubnetConfiguration.builder()
                                .name("Public")
                                .subnetType(SubnetType.PUBLIC)
                                .cidrMask(24)
                                .build(),
                        SubnetConfiguration.builder()
                                .name("Private")
                                .subnetType(SubnetType.PRIVATE_WITH_EGRESS)
                                .cidrMask(24)
                                .build(),
                        SubnetConfiguration.builder()
                                .name("Isolated")
                                .subnetType(SubnetType.PRIVATE_ISOLATED)
                                .cidrMask(24)
                                .build()
                ))
                .build();

        this.vpc.addGatewayEndpoint("S3Endpoint", GatewayVpcEndpointOptions.builder()
                .service(GatewayVpcEndpointAwsService.S3)
                .build());

        addServiceEndpoints(this.vpc);
    }

    private void addServiceEndpoints(IVpc vpc) {
        vpc.addInterfaceEndpoint("SecretsManagerEndpoint", InterfaceVpcEndpointOptions.builder()
                .service(InterfaceVpcEndpointAwsService.SECRETS_MANAGER).build());

        vpc.addInterfaceEndpoint("EcrApiEndpoint", InterfaceVpcEndpointOptions.builder()
                .service(InterfaceVpcEndpointAwsService.ECR).build());
        vpc.addInterfaceEndpoint("EcrDockerEndpoint", InterfaceVpcEndpointOptions.builder()
                .service(InterfaceVpcEndpointAwsService.ECR_DOCKER).build());

        vpc.addInterfaceEndpoint("SqsEndpoint", InterfaceVpcEndpointOptions.builder()
                .service(InterfaceVpcEndpointAwsService.SQS).build());

        vpc.addInterfaceEndpoint("CloudWatchLogsEndpoint", InterfaceVpcEndpointOptions.builder()
                .service(InterfaceVpcEndpointAwsService.CLOUDWATCH_LOGS).build());

        vpc.addInterfaceEndpoint("EcsEndpoint", InterfaceVpcEndpointOptions.builder()
                .service(InterfaceVpcEndpointAwsService.ECS).build());
        vpc.addInterfaceEndpoint("EcsAgentEndpoint", InterfaceVpcEndpointOptions.builder()
                .service(InterfaceVpcEndpointAwsService.ECS_AGENT).build());

        vpc.addInterfaceEndpoint("EcsTelemetryEndpoint", InterfaceVpcEndpointOptions.builder()
                .service(InterfaceVpcEndpointAwsService.ECS_TELEMETRY).build());
    }

    public IVpc getVpc() {
        return this.vpc;
    }
}