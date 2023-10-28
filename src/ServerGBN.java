import java.io.IOException;

public class ServerGBN extends GBN{
    public ServerGBN(int RECEIVE_PORT, int WINDOW_SIZE, int TIMEOUT, String name) throws IOException {
        super(RECEIVE_PORT, WINDOW_SIZE, TIMEOUT, name);
    }

    public static void main(String[] args) throws Exception {
        ServerGBN gbnServer = new ServerGBN(50,5,2,"server");
        gbnServer.setDestPort(40);
        gbnServer.receiveData("D:\\receiveTXT.txt",50);
        gbnServer.sendData("D:\\sendTXT.txt",50);
    }
}
