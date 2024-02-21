package csx55.threads.util;

public class TableHelper {

    private int numDashes;
    private int numCols;

    public TableHelper(int numDashes, int numCols) {
        this.numDashes = numDashes;
        this.numCols = numCols;
    }

    public String getTableLine() {
        String horizontalTablePiece = "";
        for (int i = 0; i < numDashes; i++) {
            horizontalTablePiece += "-";
        }
        String tableCorner = "+";
        String tableLine = tableCorner;
        for (int i = 0; i < numCols; i++) {
            tableLine += horizontalTablePiece + tableCorner;
        }
        return tableLine;
    }

    public String formatTable(String header, String row) {
        String tableLine = getTableLine();
        return tableLine + "\n" + header + "\n" + tableLine + "\n" + row + "\n" + tableLine;
    }

}
