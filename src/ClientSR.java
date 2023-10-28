import java.io.IOException;

public class ClientSR extends SR{
    public ClientSR(int RECEIVE_PORT, int WINDOW_SIZE, int TIMEOUT, String name) throws IOException {
        super(RECEIVE_PORT, WINDOW_SIZE, TIMEOUT, name);
    }

    public static void main(String[] args) throws Exception {
        ClientSR srClient = new ClientSR(40,5,3,"client");
        srClient.setDestPort(50);
        srClient.sendData("D:\\sendTXT.txt",50);
    }
}
