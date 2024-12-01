package may;

import software.amazon.awssdk.services.sqs.model.Message;
import java.io.IOException;
import java.util.List;

public class Worker {

    private static final String WORKER_QUEUE_URL = "your-worker-queue-url";
    private static final String RESULT_QUEUE_URL = "your-result-queue-url";

    private final AWS aws;

    public Worker() {
        this.aws = new AWS();
    }

    public void run() {
        while (true) {
            List<Message> workerMessages = aws.receiveMessages(WORKER_QUEUE_URL, 10);

            for (Message workerMessage : workerMessages) {
                String taskBody = workerMessage.body();

                if ("terminate".equalsIgnoreCase(taskBody.trim())) {
                    aws.deleteMessageFromQueue(WORKER_QUEUE_URL, workerMessage.receiptHandle());
                    return;
                }

                try {
                    processTask(taskBody);
                    aws.deleteMessageFromQueue(WORKER_QUEUE_URL, workerMessage.receiptHandle());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void processTask(String task) throws IOException {
        String[] parts = task.split("\\s+", 2);
        if (parts.length != 2) {
            System.err.println("Invalid task format: " + task);
            return;
        }

        String command = parts[0];
        String pdfUrl = parts[1];

        PDFProcessor.processPDF(command, pdfUrl);
        aws.sendMessageToQueue(RESULT_QUEUE_URL, "Processed: " + task);
    }

    public static void main(String[] args) {
        Worker worker = new Worker();
        worker.run();
    }
}

