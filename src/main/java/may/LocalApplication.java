package may;

import java.io.*;
import java.util.*;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.regions.Region;


public class LocalApplication {

    private static String LOCAL_APP_SQS_URL = null;
    private static final String MANAGER_SQS_URL = "your-manager-sqs-queue-url";    
    private static final String S3_BUCKET_NAME = "Assignment_1";
    private static final String MANAGER_AMI_ID = "ami-0c02fb55956c7d316"; 
    
        private final AWS aws;
    
        public LocalApplication() {
            this.aws = AWS.getInstance();
        }
    
        public void run(String inputFileName, String outputFileName, int n, boolean terminate) throws IOException {
            aws.createBucketIfNotExists(S3_BUCKET_NAME);
            LOCAL_APP_SQS_URL = createQueue("LocalApp queue");
            List<String> tasks = readInputFile(inputFileName);
    
            // Check or start the Manager instance
            checkOrStartManagerNode();
    
            // Upload input file to S3
            File input_file = new File(inputFileName);
            String key = inputFileName + "_" + System.currentTimeMillis();
            aws.uploadFileToS3(S3_BUCKET_NAME, key, input_file);
            String s3FileUrl = "https://" + S3_BUCKET_NAME + ".s3." + Region.US_WEST_2.id() + ".amazonaws.com/" + key;
    
            // Send SQS message with the S3 file URL
            aws.sendMessageToQueue(MANAGER_SQS_URL, s3FileUrl);
    
            // Wait for results and download the result file from S3
            String resultFileUrl = waitForResults();
            File summary = new File(outputFileName);
            aws.downloadFileFromS3(S3_BUCKET_NAME, resultFileUrl, summary);
    
            // Handle terminate mode
            if (terminate) {
                aws.sendMessageToQueue(MANAGER_SQS_URL, "terminate");
            }
        }

        public String createQueue(String queueName) {
            CreateQueueResponse response = aws.sqs.createQueue(CreateQueueRequest.builder()
                                            .queueName(queueName)
                                            .build());
            System.out.println("Queue created: " + queueName);
            return response.queueUrl(); // Return the URL of the created queue
        }

        private List<String> readInputFile(String inputFileName) {
            List<String> tasks = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new FileReader(inputFileName))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    tasks.add(line.trim()); // Trim whitespace around each line
                }
                System.out.println("Successfully read tasks from input file: " + inputFileName);
            } catch (IOException e) {
                System.err.println("Error reading input file: " + e.getMessage());
            }
            return tasks;
        }
        
    
        private void checkOrStartManagerNode() {
            if (!isManagerActive()) {
                System.out.println("Manager node is not active. Starting manager...");
                startManagerInstance();
            } else {
                System.out.println("Manager node is active.");
            }
        }
    
        private boolean isManagerActive() {
            Filter roleFilter = Filter.builder()
                    .name("tag:Role") 
                    .values("Manager") 
                    .build();
        
            // Describe EC2 instances with the specified filter
            DescribeInstancesRequest describeInstancesRequest = DescribeInstancesRequest.builder()
                    .filters(roleFilter) 
                    .build();
        
            DescribeInstancesResponse response = aws.ec2.describeInstances(describeInstancesRequest);
        
            // Check if any manager instance is running
            for (Reservation reservation : response.reservations()) {
                for (Instance instance : reservation.instances()) {
                    // Check if the instance is in a running state
                    if (instance.state().name() == InstanceStateName.RUNNING) {
                        System.out.println("Found active Manager instance: " + instance.instanceId());
                        return true;
                    }
                }
            }
            return false;
        }
        
    
        private void startManagerInstance() {
            // Launch a new EC2 instance with the Manager AMI and tags
            String instanceId = aws.launchEC2Instance(MANAGER_AMI_ID, "t2.micro", "Some initialization script"); // Replace with actual values for AMI and instance type
        
            if (instanceId != null) {
                System.out.println("Started new Manager instance with ID: " + instanceId);
        
                // Wait for the instance to become active
                waitForInstanceToBeRunning(instanceId);
                System.out.println("Manager instance is now running.");
            }
        }

        public void waitForInstanceToBeRunning(String instanceId) {
            DescribeInstancesRequest describeInstancesRequest = DescribeInstancesRequest.builder()
                    .instanceIds(instanceId)
                    .build();
        
            boolean isRunning = false;
        
            while (!isRunning) {
                try {
                    DescribeInstancesResponse response = aws.ec2.describeInstances(describeInstancesRequest);
                    Instance instance = response.reservations().get(0).instances().get(0);
        
                    if (instance.state().name() == InstanceStateName.RUNNING) {
                        isRunning = true;
                        System.out.println("Instance " + instanceId + " is now running.");
                    } else {
                        System.out.println("Waiting for instance " + instanceId + " to be running...");
                        Thread.sleep(5000); // Wait for 5 seconds before checking again
                    }
                } catch (Ec2Exception | InterruptedException e) {
                    System.err.println("Error while waiting for instance to become running: " + e.getMessage());
                    break; // Exit the loop if there is an error
                }
            }
        }
        
    
        private String waitForResults() {
            while (true) {
                // Receive a single message from the queue
                String messageBody = aws.receiveMessageFromQueue(LOCAL_APP_SQS_URL);
                
                // If a message is received, return the message body
                if (messageBody != null) {
                    return messageBody;
                }
                
                // If no message is received, wait for 5 seconds before polling again
                try {
                    Thread.sleep(5000); // Wait for 5 seconds before polling again
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        
    
        public static void main(String[] args) throws IOException {
            if (args.length < 3) {
                System.err.println("Usage: java -jar LocalApplication.jar inputFileName outputFileName n [terminate]");
                return;
            }
    
            String inputFileName = args[0];
            String outputFileName = args[1];
        int n = Integer.parseInt(args[2]);
        boolean terminate = args.length == 4 && args[3].equalsIgnoreCase("terminate");

        LocalApplication app = new LocalApplication();
        app.run(inputFileName, outputFileName, n, terminate);
    }
}
