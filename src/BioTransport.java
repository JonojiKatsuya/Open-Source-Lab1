import java.io.IOException;



public class BioTransport {

    public static void main(String[] args) throws IOException {

        //C4分支修改_01

        ClientGBN gbnClient = new ClientGBN(30,5,2,"client");
        gbnClient.setDestPort(60);
        ServerGBN gbnServer = new ServerGBN(60,5,2,"server");
        gbnServer.setDestPort(30);
        new Thread(() -> {
            try {
                gbnClient.sendData("D:\\sendTXT.txt",50);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
        new Thread(() -> {
            try {
                gbnServer.receiveData("D:\\receiveTXT.txt",50);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
        new Thread(() -> {
            try {
                gbnServer.sendData("D:\\sendTXT1.txt",50);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
        new Thread(() -> {
            try {
                gbnClient.receiveData("D:\\receiveTXT1.txt",50);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
