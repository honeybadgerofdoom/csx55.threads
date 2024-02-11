package csx55.overlay.util;

import csx55.overlay.wireformats.LinkInfo;
import csx55.overlay.wireformats.MessagingNodesList;

import java.util.*;

public class OverlayCreator {

    private final List<String> nodes;

    private final int size;
    private int[][] matrix;
    private Map<String, List<ConnectedNode>> overlay;

    public OverlayCreator(List<String> nodes) {
        this.nodes = nodes;
        this.size = nodes.size();
    }

    public void printMatrix() {
        System.out.println(getMatrixString());
    }

    public Map<String, List<ConnectedNode>> getOverlay() {
        return this.overlay;
    }

    public void createOverlay() {
        setupMatrix();
        connectMatrixNeighbors();
        matrixToOverlay();
    }

    public Map<String, MessagingNodesList> overlayToMessagingNodesListMap() {
        Map<String, MessagingNodesList> messagingNodeMap = new HashMap<>();
        for (String key : overlay.keySet()) {
            List<ConnectedNode> connectedNodeList = overlay.get(key);
            List<String> partnerNodes = new ArrayList<>();
            for (ConnectedNode connectedNode : connectedNodeList) {
                partnerNodes.add(connectedNode.getName());
            }
            MessagingNodesList messagingNodesList = new MessagingNodesList(partnerNodes);
            messagingNodeMap.put(key, messagingNodesList);
        }
        return messagingNodeMap;
    }

    public void printOverlay() {
        String overlayString = "{\n";
        for (String key : this.overlay.keySet()) {
            List<ConnectedNode> connectedNodesList = this.overlay.get(key);
            overlayString += "\t" + key + ": " + getConnectedNodeListString(connectedNodesList) + "\n";
        }
        overlayString += "}";
        System.out.println(overlayString);
    }

    private void matrixToOverlay() {
        this.overlay = new HashMap<>();
        for (int i = 0; i < this.size; i++) {
            String node = this.nodes.get(i);
            List<ConnectedNode> connectedNodes = new ArrayList<>();
            for (int j = i; j < this.size; j++) {
                if (i == j) continue;
                int weight = this.matrix[i][j];
                if (weight == 0) continue;
                String partner = this.nodes.get(j);
                ConnectedNode connectedNode = new ConnectedNode(partner, weight);
                connectedNodes.add(connectedNode);
            }
            this.overlay.put(node, connectedNodes);
        }
    }

    private String getMatrixString() {
        String matrixString = "AdjacencyMatrix\n-------------------\n";
        for (int i = 0; i < this.size; i++) {
            String row = "[";
            for (int j = 0; j < this.size; j++) {
                String suffix = "";
                if (j < this.size - 1) {
                    suffix = " ";
                }
                row += this.matrix[i][j] + suffix;
            }
            row += "]";
            if (i < this.size - 1) {
                row += "\n";
            }
            matrixString += row;
        }
        return matrixString;
    }

    private void setupMatrix() {
        this.matrix = new int[this.size][this.size];
        for (int i = 0; i < this.size; i++) {
            for (int j = 0; j < this.size; j++) {
                this.matrix[i][j] = 0;
            }
        }
    }

    private void connectMatrixNeighbors() {
        for (int i = 0; i < this.size; i++) {
            for (int j = 0; j < this.size; j++) {
                if (j <= i) continue; // Ignore bottom-diagonal of matrix...we already set those values
                int newWeight = 1;
                if (i == 0 && j == this.size - 1) {
                    this.matrix[0][this.size - 1] = newWeight;
                    this.matrix[this.size - 1][0] = newWeight;
                }
                else if (j == i + 1) {
                    this.matrix[i][j] = newWeight;
                    this.matrix[j][i] = newWeight;
                }
            }
        }
    }

    public static String getConnectedNodeListString(List<ConnectedNode> list) {
        String listString = "[";
        for (int i = 0; i < list.size(); i++) {
            String suffix = ", ";
            if (i == list.size() - 1) {
                suffix = "";
            }
            listString += list.get(i).toString() + suffix;
        }
        listString += "]";
        return listString;
    }

    private class ConnectedNode {

        private final String name;
        private final int weight;

        public ConnectedNode(String name, int weight) {
            this.name = name;
            this.weight = weight;
        }

        public String getName() {
            return name;
        }

        public int getWeight() {
            return weight;
        }

        @Override
        public String toString() {
            return name + " (" + weight + ")";
        }

    }
    
}