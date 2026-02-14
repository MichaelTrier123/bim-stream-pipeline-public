package com.myorg;

import software.amazon.awscdk.RemovalPolicy;

public class AppConfig {
    public static final String ENVIRONMENT = "dev";

    public static final String VPC_CIDR = "10.0.0.0/16";

    public static String withEnv(String name) {
        return name + "-" + ENVIRONMENT;
    }

    public static final RemovalPolicy GLOBAL_REMOVAL_POLICY = RemovalPolicy.DESTROY;
    public static final boolean AUTO_DELETE_IMAGES = true;

    public static final int DB_ALLOCATED_STORAGE = 20;
}