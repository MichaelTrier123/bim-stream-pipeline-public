package com.myorg;

import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.iam.*;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;

public class IamStack extends Stack {

    private final IRole githubRole;

    public IamStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        OpenIdConnectProvider githubProvider = OpenIdConnectProvider.Builder.create(this, "GithubProvider")
                .url("https://token.actions.githubusercontent.com")
                .clientIds(List.of("sts.amazonaws.com"))
                .thumbprints(List.of("6938fd4d98bab03faadb97b34396831e3780aea1"))
                .build();

        FederatedPrincipal principal = new FederatedPrincipal(
                githubProvider.getOpenIdConnectProviderArn(),
                Map.of(
                        "StringLike", Map.of("token.actions.githubusercontent.com:sub",
                                "repo:MichaelTrier123/*"
                        ),
                        "StringEquals", Map.of("token.actions.githubusercontent.com:aud", "sts.amazonaws.com")
                ),
                "sts:AssumeRoleWithWebIdentity"
        );

        this.githubRole = Role.Builder.create(this, "GitHubActionsRole")
                .roleName("BIMStreamGitHubRoleFinal")
                .assumedBy(principal)
                .build();

        this.githubRole.addManagedPolicy(ManagedPolicy.fromAwsManagedPolicyName("AWSCloudFormationFullAccess"));

        this.githubRole.addToPrincipalPolicy(
                PolicyStatement.Builder.create()
                        .actions(List.of(
                                "cloudformation:DescribeStacks",
                                "cloudformation:GetTemplate",
                                "cloudformation:DescribeStackEvents",
                                "cloudformation:ListStackResources",
                                "sts:GetCallerIdentity",
                                "iam:PassRole",
                                "ssm:GetParameter",
                                "sts:AssumeRole",
                                "ec2:Describe*"
                        ))
                        .resources(List.of("*"))
                        .build()
        );
     //   githubRole.applyRemovalPolicy(RemovalPolicy.RETAIN);

    }

    public IRole getGithubRole() {
        return githubRole;
    }
}
