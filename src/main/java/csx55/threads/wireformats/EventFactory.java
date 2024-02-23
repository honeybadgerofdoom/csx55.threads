package csx55.threads.wireformats;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.BufferedInputStream;
import java.io.IOException;

public class EventFactory {

    private final static EventFactory EventFactoryInstance = new EventFactory();

    private EventFactory() { };

    public static EventFactory getInstance() {
        return EventFactoryInstance;
    }

    public Event getEvent(byte[] bytes) throws IOException {
        ByteArrayInputStream bArrayInputStream = new ByteArrayInputStream(bytes);
        DataInputStream din = new DataInputStream(new BufferedInputStream(bArrayInputStream));
        int messageType = din.readInt();
        switch(messageType) {
            case (Protocol.REGISTER_REQUEST):
                return new RegisterRequest(bytes);
            case (Protocol.REGISTER_RESPONSE):
                return new RegisterResponse(bytes);
            case (Protocol.DEREGISTER_REQUEST):
                return new DeregisterRequest(bytes);
            case (Protocol.DEREGISTER_RESPONSE):
                return new DeregisterResponse(bytes);
            case (Protocol.MESSAGING_NODES_LIST):
                return new PartnerNodeInfo(bytes);
            case (Protocol.TASK_INITIATE):
                return new TaskInitiate(bytes);
            case (Protocol.TRAFFIC_SUMMARY):
                return new TaskSummaryResponse(bytes);
            case (Protocol.POKE):
                return new PartnerPoke(bytes);
            case (Protocol.TASK_AVERAGE):
                return new RoundAverage(bytes);
            case (Protocol.TASK_DELIVERY):
                return new TaskDelivery(bytes);
            case (Protocol.AVERAGES_CALCULATED):
                return new AveragesCalculated(bytes);
            default:
                System.out.println("getEvent() found no matching route, messageType: " + messageType);
                return null;
        }

    }
    
}