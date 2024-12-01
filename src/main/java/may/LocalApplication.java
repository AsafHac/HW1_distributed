package may;

import java.io.*;
import java.util.*;
import software.amazon.awssdk.services.sqs.model.Message;

public class LocalApplication {

    private static final String TASK_QUEUE_URL = "your-task-queue-url";
    private static final String RESULT_QUEUE_URL = "your-result-queue-url";
    private static final String S3_BUCKET_NAME = "your-s3-bucket-name";
    private static final int CHUNK_SIZE = 5;

    private final AWS aws;

    public LocalApplication() {
        this.aws = new AWS();
    }

    public void run(String inputFileName, String outputFileName, int n, boolean terminate) throws IOException {
        List<String> tasks = readInputFile(inputFileName);
        sendTasksToManager(tasks, n);

        if (terminate) {
            sendTerminateSignal();
        }

        collectResults(outputFileName);
    }

    private List<String> readInputFile(String inputFileName) throws IOException {
        List<String> tasks = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                tasks.add(line);
            }
        }
        return tasks;
    }

    private void sendTasksToManager(List<String> tasks, int n) {
        int taskCount = 0;
        List<String> chunk = new ArrayList<>();

        for (String task : tasks) {
            chunk.add(task);
            taskCount++;

            if (taskCount % CHUNK_SIZE == 0 || taskCount == tasks.size()) {
                String chunkMessage = String.join("\n", chunk);
                aws.sendMessageToQueue(TASK_QUEUE_URL, chunkMessage);
                chunk.clear();
            }
        }
    }

    private void sendTerminateSignal() {
        aws.sendMessageToQueue(TASK_QUEUE_URL, "terminate");
    }

    private void collectResults(String outputFileName) throws IOException {
        List<String> results = new ArrayList<>();

        while (true) {
            List<Message> messages = aws.receiveMessages(RESULT_QUEUE_URL, 10);

            if (messages.isEmpty()) {
                break;
            }

            for (Message message : messages) {
                results.add(message.body());
                aws.deleteMessageFromQueue(RESULT_QUEUE_URL, message.receiptHandle());
            }
        }

        saveResultsToFile(outputFileName, results);
    }

    private void saveResultsToFile(String outputFileName, List<String> results) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFileName))) {
            for (String result : results) {
                writer.write(result);
                writer.newLine();
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

