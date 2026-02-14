package com.myorg;

import java.util.List;
import java.util.Map;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.CfnOutputProps;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.services.apigatewayv2.*;
import software.amazon.awscdk.services.apigatewayv2.HttpMethod;
import software.amazon.awscdk.aws_apigatewayv2_integrations.HttpLambdaIntegration;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.lambda.*;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.s3.IBucket;
import software.amazon.awscdk.services.secretsmanager.ISecret;
import software.constructs.Construct;

public class ApiStack extends Stack {

    public ApiStack(final Construct scope, final String id, final ApiStackProps props) {
        super(scope, id, props.getStackProps());

        IBucket inputBucket = props.getInputBucket();

        Function presignFn = Function.Builder.create(this, "PresignFn")
                .runtime(Runtime.PYTHON_3_12)
                .handler("handler.main")
                .code(Code.fromAsset("lambda/presign"))
                .timeout(Duration.seconds(5))
                .memorySize(256)
                .environment(Map.of(
                        "BUCKET_NAME", inputBucket.getBucketName(),
                        "UPLOAD_PREFIX", "uploads/",
                        "URL_EXPIRES_SECONDS", "60"
                ))
                .build();

        inputBucket.grantPut(presignFn, "uploads/*");

        HttpApi api = HttpApi.Builder.create(this, "BimHttpApi")
                .corsPreflight(CorsPreflightOptions.builder()
                        .allowOrigins(List.of("*"))
                        .allowMethods(List.of(
                                CorsHttpMethod.POST,
                                CorsHttpMethod.GET,
                                CorsHttpMethod.OPTIONS
                        ))
                        .allowHeaders(List.of("content-type"))
                        .build())
                .build();

        HttpLambdaIntegration integration = new HttpLambdaIntegration("PresignIntegration", presignFn);

        //Add route to API gateway and forward the request to the presign Lambda
        api.addRoutes(AddRoutesOptions.builder()
                .path("/uploads/presign")
                .methods(List.of(HttpMethod.POST))
                .integration(integration)
                .build());

        ISecret dbSecret = props.getDbSecret();

        SecurityGroup lookupSg = SecurityGroup.Builder.create(this, "LookupLambdaSg")
                .vpc(props.getVpc())
                .allowAllOutbound(true)
                .build();

        Function lookupFn = Function.Builder.create(this, "ImportLookupFn")
                .runtime(Runtime.PYTHON_3_12)
                .handler("handler.main")
                .code(Code.fromAsset("lambda/lookup"))
                .timeout(Duration.seconds(10))
                .memorySize(512)
                .vpc(props.getVpc())
                .securityGroups(List.of(lookupSg))
                .vpcSubnets(SubnetSelection.builder()
                        .subnetType(SubnetType.PRIVATE_ISOLATED)
                        .build())
                .environment(Map.of(
                        "DB_SECRET_ARN", dbSecret.getSecretArn()
                ))
                .build();

        dbSecret.grantRead(lookupFn);

        HttpLambdaIntegration lookupIntegration = new HttpLambdaIntegration("LookupIntegration", lookupFn);

        api.addRoutes(AddRoutesOptions.builder()
                .path("/imports/by-key")
                .methods(List.of(HttpMethod.GET))
                .integration(lookupIntegration)
                .build());

        new CfnOutput(this, "ApiUrl", CfnOutputProps.builder()
                .value(api.getApiEndpoint())
                .build());
    }
}
