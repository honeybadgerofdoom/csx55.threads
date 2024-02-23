package csx55.threads.wireformats;

public class Protocol {

    // message types
    public static final int REGISTER_REQUEST = 1;
    public static final int REGISTER_RESPONSE = 2;
    public static final int DEREGISTER_REQUEST = 3;
    public static final int DEREGISTER_RESPONSE = 4;
    public static final int MESSAGING_NODES_LIST = 5;
    public static final int TASK_INITIATE = 6;
    public static final int TRAFFIC_SUMMARY = 9;
    public static final int POKE = 13;
    public static final int TASK_AVERAGE = 14;
    public static final int TASK_DELIVERY = 15;
    public static final int NODE_AGREEMENT = 18;

    // status codes
    public static final byte SUCCESS = 1;
    public static final byte FAILURE = 0;

    public static final int AGR_AVERAGE = 0;
    public static final int AGR_TASK_MANAGERS = 1;
    public static final int AGR_ROUNDS = 1;

}