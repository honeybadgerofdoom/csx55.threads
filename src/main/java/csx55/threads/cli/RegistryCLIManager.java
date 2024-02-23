package csx55.threads.cli;

import csx55.threads.Registry;

import java.util.Scanner;

public class RegistryCLIManager implements Runnable, CLIManager {

    private Registry node;

    public RegistryCLIManager(Registry node) {
        this.node = node;
    }

    public void run() {
        String input = "";
        Scanner sc = new Scanner(System.in);
        while (!(input = sc.nextLine()).equals("quit")) {
            processInput(input);
        }
        sc.close();
        System.exit(0);
    }

    public void processInput(String input) {
        String[] parsedInput = input.split(" ");
        String command = parsedInput[0];
        switch (command) {
            case "list-messaging-nodes":
                this.node.listMessagingNodes();
                break;
            case "setup-overlay":
                try {
                    int numberOfThreads = Integer.parseInt(parsedInput[1]);
                    this.node.setupOverlay(numberOfThreads);
                } catch (ArrayIndexOutOfBoundsException e) {
                    System.out.println("ERROR you must provide a NumberOfLinks (int) argument. " + e);
                }
                break;
            case "start":
                try {
                    int numberOfRounds = Integer.parseInt(parsedInput[1]);
                    this.node.initiateMessagePassing(numberOfRounds);
                } catch (ArrayIndexOutOfBoundsException e) {
                    System.out.println("ERROR you must provide a NumberOfLinks (int) argument. " + e);
                }
                break;
            default:
                System.out.println("Invalid command to the Registry: " + command);
        }
    }

}
