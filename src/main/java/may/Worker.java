package may;

import software.amazon.awssdk.services.sqs.model.Message;
import java.io.IOException;
import java.util.List;

public class Worker {

    private String SQS_URL = "your-worker-queue-url";
   

    private final AWS aws;
    private boolean shouldTerminate;

    public Worker(String SQS_URL) {
        this.SQS_URL=SQS_URL;
        this.aws = AWS.getInstance();
        this.shouldTerminate=false;
    }

    public void run() {
        while (!shouldTerminate) {
            String massage=this.aws.receiveMessageFromQueue(SQS_URL);
            if(message)

            for (String wm : workerMessages) {
                

                if ("terminate".equalsIgnoreCase(taskBody.trim())) {
                    aws.deleteMessageFromQueue(SQS_URL, workerMessage.receiptHandle());
                    return;
                }

                try {
                    processTask(taskBody);
                    aws.deleteMessageFromQueue(SQS_URL, workerMessage.receiptHandle());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    public void terminate(){
        this.shouldTerminate=true;
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
        aws.sendMessageToQueue(SQS_URL, "Processed: " + task);
    }
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: Worker <sqs-url> <split-size>");
            System.exit(1);
        }

        String sqsUrl = args[0];
        int splitSize = Integer.parseInt(args[1]);

        Worker worker = new Worker(sqsUrl);
        worker.run();
    }

    
}

