package com.github.vatbub.awsEc2InstanceRebooter;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.*;
import com.github.vatbub.common.core.logging.FOKLogger;

import java.util.ArrayList;
import java.util.List;

public class AWSEC2Rebooter {
    private static AmazonEC2 buildAWSClient(Regions awsRegion, String awsKey, String awsSecret) {
        FOKLogger.info(AWSEC2Rebooter.class.getName(), "Initializing the connection to aws ec2...");
        AWSCredentials credentials = new BasicAWSCredentials(awsKey, awsSecret);
        return AmazonEC2ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(credentials)).withRegion(awsRegion).build();
    }

    /**
     * Reboots the specified instance.
     *
     * @param instanceID The id of the instance to reboot
     * @param awsRegion  The region where the instance is located in
     * @param awsKey     The aws access key
     * @param awsSecret  The aws secret
     */
    public static void rebootInstance(String instanceID, Regions awsRegion, String awsKey, String awsSecret) {
        try {
            stopInstance(instanceID, awsRegion, awsKey, awsSecret);
        } catch (AmazonEC2Exception e) {
            FOKLogger.severe(AWSEC2Rebooter.class.getName(), "Could not stop instance " + instanceID + ": " + e.getMessage());
        }

        try {
            startInstance(instanceID, awsRegion, awsKey, awsSecret);
        } catch (AmazonEC2Exception e) {
            FOKLogger.severe(AWSEC2Rebooter.class.getName(), "Could not start instance " + instanceID + ": " + e.getMessage());
        }
    }

    /**
     * Stops the specified instance.
     *
     * @param instanceID The id of the instance to stop
     * @param awsRegion  The region where the instance is located in
     * @param awsKey     The aws access key
     * @param awsSecret  The aws secret
     */
    public static void stopInstance(String instanceID, Regions awsRegion, String awsKey, String awsSecret) {
        AmazonEC2 client = buildAWSClient(awsRegion, awsKey, awsSecret);

        FOKLogger.info(AWSEC2Rebooter.class.getName(), "Sending the shutdown request to AWS EC2...");
        List<String> instanceIDCopy = new ArrayList<>(1);
        instanceIDCopy.add(instanceID);
        StopInstancesRequest stopInstancesRequest = new StopInstancesRequest(instanceIDCopy);
        StopInstancesResult stopInstancesResult = client.stopInstances(stopInstancesRequest);
        for (InstanceStateChange item : stopInstancesResult.getStoppingInstances()) {
            FOKLogger.info(AWSEC2Rebooter.class.getName(), "Stopping instance: " + item.getInstanceId() + ", instance state changed from " + item.getPreviousState() + " to " + item.getCurrentState());

            FOKLogger.info(AWSEC2Rebooter.class.getName(), "Waiting for the instance to shut down...");

            long lastPrintTime = System.currentTimeMillis();
            DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest();
            List<String> instanceId = new ArrayList<>(1);
            instanceId.add(instanceID);
            describeInstancesRequest.setInstanceIds(instanceId);
            DescribeInstancesResult describeInstancesResult;
            Instance newInstance = null;
            int retries = 0;

            do {
                // we're waiting

                if (System.currentTimeMillis() - lastPrintTime >= Math.pow(2, retries) * 100) {
                    retries = retries + 1;
                    describeInstancesResult = client.describeInstances(describeInstancesRequest);
                    newInstance = describeInstancesResult.getReservations().get(0).getInstances().get(0);
                    lastPrintTime = System.currentTimeMillis();
                    if (newInstance.getState().getCode() != 80) {
                        FOKLogger.info(AWSEC2Rebooter.class.getName(), "Still waiting for the instance to shut down, current instance state is " + newInstance.getState().getName());
                    } else {
                        FOKLogger.info(AWSEC2Rebooter.class.getName(), "Instance is " + newInstance.getState().getName());
                    }
                }
            } while (newInstance == null || newInstance.getState().getCode() != 80);
        }
    }

    /**
     * Starts the specified instance.
     *
     * @param instanceID The id of the instance to start
     * @param awsRegion  The region where the instance is located in
     * @param awsKey     The aws access key
     * @param awsSecret  The aws secret
     */
    public static void startInstance(String instanceID, Regions awsRegion, String awsKey, String awsSecret) {
        AmazonEC2 client = buildAWSClient(awsRegion, awsKey, awsSecret);

        FOKLogger.info(AWSEC2Rebooter.class.getName(), "Sending the start request to AWS EC2...");
        List<String> instanceIDCopy = new ArrayList<>(1);
        instanceIDCopy.add(instanceID);
        StartInstancesRequest startInstancesRequest = new StartInstancesRequest(instanceIDCopy);
        StartInstancesResult startInstancesResult = client.startInstances(startInstancesRequest);
        for (InstanceStateChange item : startInstancesResult.getStartingInstances()) {
            FOKLogger.info(AWSEC2Rebooter.class.getName(), "Started instance: " + item.getInstanceId() + ", instance state changed from " + item.getPreviousState() + " to " + item.getCurrentState());

            FOKLogger.info(AWSEC2Rebooter.class.getName(), "Waiting for the instance to boot...");

            long lastPrintTime = System.currentTimeMillis();
            DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest();
            List<String> instanceId = new ArrayList<>(1);
            instanceId.add(instanceID);
            describeInstancesRequest.setInstanceIds(instanceId);
            DescribeInstancesResult describeInstancesResult;
            Instance newInstance = null;
            int retries = 0;

            do {
                // we're waiting

                if (System.currentTimeMillis() - lastPrintTime >= Math.pow(2, retries) * 100) {
                    retries = retries + 1;
                    describeInstancesResult = client.describeInstances(describeInstancesRequest);
                    newInstance = describeInstancesResult.getReservations().get(0).getInstances().get(0);
                    lastPrintTime = System.currentTimeMillis();
                    if (newInstance.getState().getCode() != 16) {
                        FOKLogger.info(AWSEC2Rebooter.class.getName(), "Still waiting for the instance to boot, current instance state is " + newInstance.getState().getName());
                    } else {
                        FOKLogger.info(AWSEC2Rebooter.class.getName(), "Instance is " + newInstance.getState().getName());
                    }
                }
            } while (newInstance == null || newInstance.getState().getCode() != 16);
        }
    }
}
