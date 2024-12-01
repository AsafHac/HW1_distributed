package may;

import software.amazon.awssdk.services.sqs.model.Message;
import java.util.List;

public class Manager {

    private static final String TASK_QUEUE_URL = "your-task-queue-url";
    private static final String RESULT_QUEUE_URL = "your-result-queue-url";
    private static final String WORKER_QUEUE_URL = "your-worker-queue-url";

    private final AWS aws;

    public Manager() {
        this.aws = new AWS();
    }

    public void run() {
        while (true) {
            List<Message> taskMessages = aws.receiveMessages(TASK_QUEUE_URL, 10);

            for (Message taskMessage : taskMessages) {
                String taskBody = taskMessage.body();

                if ("terminate".equalsIgnoreCase(taskBody.trim())) {
                    terminateWorkers();
                    aws.deleteMessageFromQueue(TASK_QUEUE_URL, taskMessage.receiptHandle());
                    return;
                }

                aws.sendMessageToQueue(WORKER_QUEUE_URL, taskBody);
                aws.deleteMessageFromQueue(TASK_QUEUE_URL, taskMessage.receiptHandle());
            }
        }
    }

    private void terminateWorkers() {
        aws.sendMessageToQueue(WORKER_QUEUE_URL, "terminate");
    }

    public static void main(String[] args) {
        Manager manager = new Manager();
        manager.run();
    }
}

