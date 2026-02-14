package com.myorg;

import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.IVpc;
import software.amazon.awscdk.services.ecr.IRepository;
import software.amazon.awscdk.services.iam.IRole;
import software.amazon.awscdk.services.rds.IDatabaseInstance;
import software.amazon.awscdk.services.s3.IBucket;
import software.amazon.awscdk.services.secretsmanager.ISecret;
import software.amazon.awscdk.services.sqs.IQueue;

public class ComputeProps {
    private final StackProps stackProps;
    private final IVpc vpc;
    private final IRepository workerRepo;
    private final ISecret dbSecret;
    private final IRole githubRole;
    private final IRepository processorRepo;
    private final IDatabaseInstance database;
    private final IBucket inputBucket;
    private final IQueue conversionQueue;

    public ComputeProps(StackProps stackProps,
                        IVpc vpc,
                        IRepository workerRepo,
                        ISecret dbSecret,
                        IRepository processorRepo,
                        IDatabaseInstance database,
                        IBucket inputBucket,
                        IQueue conversionQueue,
                        IRole githubRole) {
        this.stackProps = stackProps;
        this.vpc = vpc;
        this.workerRepo = workerRepo;
        this.dbSecret = dbSecret;
        this.processorRepo = processorRepo;
        this.database = database;
        this.inputBucket = inputBucket;
        this.conversionQueue = conversionQueue;
        this.githubRole = githubRole;
    }

    public StackProps getStackProps() { return stackProps; }
    public IVpc getVpc() { return vpc; }
    public IRepository getWorkerRepo() { return workerRepo; }
    public IRepository getProcessorRepo() { return processorRepo; }
    public IDatabaseInstance getDatabase() { return database; }
    public IBucket getInputBucket() { return inputBucket; }
    public IQueue getConversionQueue() { return conversionQueue; }

    public ISecret getDbSecret() {
        return dbSecret;
    }
    public IRole getGithubRole() { return githubRole; }
}