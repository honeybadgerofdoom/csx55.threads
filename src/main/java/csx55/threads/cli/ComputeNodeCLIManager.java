package csx55.threads.cli;

import csx55.threads.ComputeNode;

import java.util.Scanner;

public class ComputeNodeCLIManager implements Runnable, CLIManager {

    private ComputeNode node;

    public ComputeNodeCLIManager(ComputeNode node) {
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
        switch(command) {
            case ("exit-overlay"):
                this.node.deregisterSelf();
                break;
            case ("poke"):
                this.node.pokePartner();
                break;
            case "task-manager-stats":
                this.node.printTaskManagerSum();
                break;
            default:
                System.out.println("Invalid CLI Input: " + input);
        }
    }

}
