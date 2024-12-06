package may;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Base64;

public class AWS {
    private final S3Client s3;
    protected final SqsClient sqs;
    protected final Ec2Client ec2;
    private final Region region = Region.US_WEST_2; // Replace with your preferred region
    private static AWS instance = null;

    private AWS() {
        this.s3 = S3Client.builder().region(region).build();
        this.sqs = SqsClient.builder().region(region).build();
        this.ec2 = Ec2Client.builder().region(region).build();
    }

    public static AWS getInstance() {
        if (instance == null) {
            instance = new AWS();
        }
        return instance;
    }

    //////////////////////////////// S3 Methods ////////////////////////////////

    public void createBucketIfNotExists(String bucketName) {
        try {
            s3.createBucket(CreateBucketRequest.builder()
                    .bucket(bucketName)
                    .createBucketConfiguration(CreateBucketConfiguration.builder()
                            .locationConstraint(region.id())
                            .build())
                    .build());
            System.out.println("Bucket created: " + bucketName);
        } catch (S3Exception e) {
            if (!e.getMessage().contains("BucketAlreadyOwnedByYou")) {
                System.err.println("Error creating bucket: " + e.getMessage());
            }
        }
    }

    public void uploadFileToS3(String bucketName, String key, File file) {
        try {
            s3.putObject(PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .build(),
                    file.toPath());
            System.out.println("File uploaded to S3: " + key);
        } catch (S3Exception e) {
            System.err.println("Error uploading file: " + e.getMessage());
        }
    }

    public void downloadFileFromS3(String bucketName, String key, File destination) {
        try {
            byte[] fileBytes = s3.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build()).asByteArray();
            try (OutputStream os = new FileOutputStream(destination)) {
                os.write(fileBytes);
            }
            System.out.println("File downloaded from S3: " + key);
        } catch (Exception e) {
            System.err.println("Error downloading file: " + e.getMessage());
        }
    }

    //////////////////////////////// SQS Methods ////////////////////////////////

    public String createQueue(String queueName) {
        try {
            CreateQueueResponse response = sqs.createQueue(CreateQueueRequest.builder()
                    .queueName(queueName)
                    .build());
            System.out.println("Queue created: " + queueName);
            return response.queueUrl();
        } catch (SqsException e) {
            System.err.println("Error creating queue: " + e.getMessage());
            return null;
        }
    }

    public void sendMessageToQueue(String queueUrl, String messageBody) {
        try {
            sqs.sendMessage(SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(messageBody)
                    .build());
            System.out.println("Message sent to queue: " + queueUrl);
        } catch (SqsException e) {
            System.err.println("Error sending message: " + e.getMessage());
        }
    }

    public String receiveMessageFromQueue(String queueUrl) {
        try {
            ReceiveMessageResponse response = sqs.receiveMessage(ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .maxNumberOfMessages(1)
                    .build());
            if (!response.messages().isEmpty()) {
                String messageBody = response.messages().get(0).body();
                String receiptHandle = response.messages().get(0).receiptHandle();
                // Delete the message after processing
                deleteMessageFromQueue(queueUrl, receiptHandle);
                return messageBody;
            }
        } catch (SqsException e) {
            System.err.println("Error receiving message: " + e.getMessage());
        }
        return null;
    }

    public void deleteMessageFromQueue(String queueUrl, String receiptHandle) {
        try {
            sqs.deleteMessage(DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(receiptHandle)
                    .build());
            System.out.println("Message deleted from queue: " + queueUrl);
        } catch (SqsException e) {
            System.err.println("Error deleting message: " + e.getMessage());
        }
    }

    //////////////////////////////// EC2 Methods ////////////////////////////////

    public String launchEC2Instance(String amiId, String script, String instanceType) {
        try {
            RunInstancesResponse response = ec2.runInstances(RunInstancesRequest.builder()
                    .imageId(amiId)
                    .instanceType(InstanceType.fromValue(instanceType))
                    .maxCount(1)
                    .minCount(1)
                    .userData(Base64.getEncoder().encodeToString(script.getBytes()))
                    .build());
            String instanceId = response.instances().get(0).instanceId();
            System.out.println("EC2 instance launched: " + instanceId);
            return instanceId;
        } catch (Ec2Exception e) {
            System.err.println("Error launching EC2 instance: " + e.getMessage());
            return null;
        }
    }

    public void terminateEC2Instance(String instanceId) {
        try {
            ec2.terminateInstances(TerminateInstancesRequest.builder()
                    .instanceIds(instanceId)
                    .build());
            System.out.println("EC2 instance terminated: " + instanceId);
        } catch (Ec2Exception e) {
            System.err.println("Error terminating EC2 instance: " + e.getMessage());
        }
    }
}
