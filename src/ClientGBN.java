import java.io.IOException;

public class ClientGBN extends GBN{
    public ClientGBN(int RECEIVE_PORT, int WINDOW_SIZE, int TIMEOUT, String name) throws IOException {
        super(RECEIVE_PORT, WINDOW_SIZE, TIMEOUT, name);
    }

    public static void main(String[] args) throws Exception {
        ClientGBN gbnClient = new ClientGBN(40,5,2,"client");
        gbnClient.setDestPort(50);
        gbnClient.sendData("D:\\sendTXT.txt",50);
        gbnClient.receiveData("D:\\receiveTXT.txt",50);
    }
}
