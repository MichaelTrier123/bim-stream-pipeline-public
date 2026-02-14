package com.myorg;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.ecr.TagMutability;
import software.amazon.awscdk.services.rds.*;
import software.amazon.awscdk.services.s3.*;
import software.amazon.awscdk.services.s3.notifications.SqsDestination;
import software.amazon.awscdk.services.secretsmanager.ISecret;
import software.amazon.awscdk.services.sqs.DeadLetterQueue;
import software.amazon.awscdk.services.sqs.IQueue;
import software.amazon.awscdk.services.sqs.Queue;
import software.constructs.Construct;
import software.amazon.awscdk.services.s3.CorsRule;
import software.amazon.awscdk.services.s3.HttpMethods;

import java.util.Collections;
import java.util.List;

public class DataStack extends Stack {
    private final Bucket inputBucket;
    private final IQueue conversionQueue;
    private final DatabaseInstance database;
    private final ISecret dbSecret;
    private final Repository workerRepo;
    private final Repository processorRepo;

    public DataStack(final Construct scope, final String id, final DataStackProps props) {
        super(scope, id, props.getStackProps());
        this.inputBucket = Bucket.Builder.create(this, "BIMStreamInputBucket")
                .bucketName(AppConfig.withEnv("bim-stream-input"))
                .encryption(BucketEncryption.S3_MANAGED)
                .versioned(true)
                .blockPublicAccess(BlockPublicAccess.BLOCK_ALL)
                .removalPolicy(AppConfig.GLOBAL_REMOVAL_POLICY)
                .build();

        // SQS & NOTIFICATIONS
        Queue conversionDLQ = Queue.Builder.create(this, "IFCConversionDLQ")
                .retentionPeriod(Duration.days(14))
                .build();

        this.conversionQueue = Queue.Builder.create(this, "IFCConversionQueue")
                .visibilityTimeout(Duration.seconds(300))
                .deadLetterQueue(DeadLetterQueue.builder()
                        .maxReceiveCount(3)
                        .queue(conversionDLQ)
                        .build())
                .build();

        this.inputBucket.addEventNotification(EventType.OBJECT_CREATED, new SqsDestination(this.conversionQueue));

        this.inputBucket.addCorsRule(CorsRule.builder()
                .allowedMethods(List.of(HttpMethods.PUT, HttpMethods.HEAD, HttpMethods.POST, HttpMethods.GET))
                .allowedOrigins(List.of("*"))
                .allowedHeaders(List.of("*"))
                .exposedHeaders(List.of("ETag"))
                .build());

        this.workerRepo = Repository.Builder.create(this, "WorkerRepo")
                .repositoryName(AppConfig.withEnv("bim-stream-worker"))
                .imageScanOnPush(true)
                .imageTagMutability(TagMutability.MUTABLE)
                .removalPolicy(AppConfig.GLOBAL_REMOVAL_POLICY)
                .emptyOnDelete(AppConfig.AUTO_DELETE_IMAGES)
                .build();

        this.processorRepo = Repository.Builder.create(this, "IfcProcessorRepo")
                .repositoryName(AppConfig.withEnv("ifc-processor"))
                .imageScanOnPush(true)
                .imageTagMutability(TagMutability.MUTABLE)
                .removalPolicy(AppConfig.GLOBAL_REMOVAL_POLICY)
                .emptyOnDelete(AppConfig.AUTO_DELETE_IMAGES)
                .build();

        SecurityGroup rdsSecurityGroup = SecurityGroup.Builder.create(this, "RDSSecurityGroup")
                .vpc(props.getVpc())
                .description("Allow internal VPC access to SQL Server")
                .allowAllOutbound(true)
                .build();

        rdsSecurityGroup.addIngressRule(Peer.ipv4(props.getVpc().getVpcCidrBlock()), Port.tcp(5432));

        this.database = DatabaseInstance.Builder.create(this, "BIMStreamPostgresDB")
                .engine(DatabaseInstanceEngine.postgres(
                        PostgresInstanceEngineProps.builder()
                                .version(PostgresEngineVersion.VER_16)
                                .build()))
                .instanceType(InstanceType.of(InstanceClass.BURSTABLE3, InstanceSize.SMALL))
                .vpc(props.getVpc())
                .credentials(Credentials.fromGeneratedSecret("postgres"))
                .databaseName("bim_stream")
                .vpcSubnets(SubnetSelection.builder().subnetType(SubnetType.PRIVATE_ISOLATED).build())
                .securityGroups(Collections.singletonList(rdsSecurityGroup))
                .allocatedStorage(AppConfig.DB_ALLOCATED_STORAGE)
                .removalPolicy(AppConfig.GLOBAL_REMOVAL_POLICY)
                .build();
        this.dbSecret = this.database.getSecret();
    }

    public IBucket getInputBucket() { return inputBucket; }
    public IQueue getConversionQueue() { return conversionQueue; }
    public IDatabaseInstance getDatabase() { return database; }
    public Repository getWorkerRepo() { return workerRepo; }
    public Repository getProcessorRepo() { return processorRepo; }
    public ISecret getDbSecret() { return dbSecret; }

}