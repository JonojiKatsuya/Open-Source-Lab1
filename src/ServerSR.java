import java.io.IOException;

public class ServerSR extends SR{
    public ServerSR(int RECEIVE_PORT, int WINDOW_SIZE, int TIMEOUT, String name) throws IOException {
        super(RECEIVE_PORT, WINDOW_SIZE, TIMEOUT, name);
    }

    public static void main(String[] args) throws Exception {
        ServerSR srServer = new ServerSR(50,5,3,"server");
        srServer.setDestPort(40);
        srServer.receiveData("D:\\receiveTXT.txt");
    }
}
