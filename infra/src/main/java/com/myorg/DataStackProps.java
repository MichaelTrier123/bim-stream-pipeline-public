package com.myorg;

import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.IVpc;

public class DataStackProps {
    private final IVpc vpc;
    private final StackProps stackProps;

    public DataStackProps(IVpc vpc, StackProps stackProps) {
        this.vpc = vpc;
        this.stackProps = stackProps;
    }

    public IVpc getVpc() {
        return vpc;
    }

    public StackProps getStackProps() {
        return stackProps;
    }
}