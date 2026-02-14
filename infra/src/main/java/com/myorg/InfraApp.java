package com.myorg;

import software.amazon.awscdk.App;
import software.amazon.awscdk.StackProps;


public class InfraApp {
    public static void main(final String[] args) {
        App app = new App();

        StackProps standardProps = StackProps.builder().build();
        IamStack iam = new IamStack(app, "BimIam", standardProps);
        NetworkStack network = new NetworkStack(app, "BimNetwork", standardProps);

        DataStackProps dataProps = new DataStackProps(network.getVpc(), standardProps);
        DataStack data = new DataStack(app, "BimData", dataProps);
        ApiStackProps apiProps = new ApiStackProps(
                standardProps,
                data.getInputBucket(),
                network.getVpc(),
                data.getDbSecret()
        );
        new ApiStack(app, "BimApi", apiProps);
        data.getWorkerRepo().grantPullPush(iam.getGithubRole());
        data.getProcessorRepo().grantPullPush(iam.getGithubRole());
        data.getInputBucket().grantReadWrite(iam.getGithubRole());
        ComputeProps computeProps = new ComputeProps(
                standardProps,
                network.getVpc(),
                data.getWorkerRepo(),
                data.getDbSecret(),
                data.getProcessorRepo(),
                data.getDatabase(),
                data.getInputBucket(),
                data.getConversionQueue(),
                iam.getGithubRole()
        );
        new ComputeStack(app, "BimCompute", computeProps);

        app.synth();
    }
}

