package csx55.threads.util;

public class AgreementSpace {

    private final int agreementPolicy;
    private boolean iAmReady = false;
    private boolean allAreReady = false;

    public AgreementSpace(int agreementPolicy) {
        this.agreementPolicy = agreementPolicy;
    }

    public boolean amIReady() {
        return iAmReady;
    }

    public boolean areAllReady() {
        return allAreReady;
    }

    public void setIAmReady(boolean iAmReady) {
        this.iAmReady = iAmReady;
    }

    public void setAllAreReady(boolean allAreReady) {
        this.allAreReady = allAreReady;
    }

    public int getAgreementPolicy() {
        return agreementPolicy;
    }

}
