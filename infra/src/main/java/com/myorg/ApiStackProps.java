package com.myorg;

import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.IVpc;
import software.amazon.awscdk.services.ec2.ISecurityGroup;
import software.amazon.awscdk.services.s3.IBucket;
import software.amazon.awscdk.services.secretsmanager.ISecret;

public class ApiStackProps {
    private final StackProps stackProps;
    private final IBucket inputBucket;
    private final IVpc vpc;
    private final ISecret dbSecret;

    public ApiStackProps(StackProps stackProps, IBucket inputBucket, IVpc vpc, ISecret dbSecret) {
        this.stackProps = stackProps;
        this.inputBucket = inputBucket;
        this.vpc = vpc;
        this.dbSecret = dbSecret;
    }

    public StackProps getStackProps() {
        return stackProps;
    }

    public IBucket getInputBucket() {
        return inputBucket;
    }

    public IVpc getVpc() {
        return vpc;
    }

    public ISecret getDbSecret() {
        return dbSecret;
    }

}
