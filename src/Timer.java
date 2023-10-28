
public class Timer extends Thread {
    private int TIMEOUT;
    private int seq;
    SR ClientSR;

    public Timer(int seq ,int TIMEOUT, SR ClientSR) {
        this.seq = seq;
        this.TIMEOUT = TIMEOUT;
        this.ClientSR = ClientSR;
    }

    public void run() {
        try {
            Thread.sleep(TIMEOUT * 1000);
            System.out.println(ClientSR.hostName + " 等待ACK:Seq=" + seq + "超时");
            this.ClientSR.timeOut(seq);
        } catch (InterruptedException var3) {
        } catch (Exception var4) {
        }
    }
}
