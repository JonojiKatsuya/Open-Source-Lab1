import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SR {

    //分支B2修改_01

    int flag = 1;       //模拟丢失ACK时用到
    protected int WINDOW_SIZE;    //窗口长度
    protected int TIMEOUT;    //超时时间，单位为秒
    protected String hostName;    //主机名称
    protected int nextSeq = 1;    //下一个发送的分组
    protected int base = 1;    //当前窗口起始位置
    protected InetAddress destAddress;    //发送的目标地址
    protected int destPort;    //发送分组的目标端口
    private Set<Integer> senderReceivedACKSet = new HashSet<>();    //作为发送方时收到的ACK
    private Map<Integer, byte[]> receiverReceivedMap = new HashMap<>();    //作为接收方时收到的分组，用来作为缓存
    protected DatagramSocket sendSocket;    //发送分组使用的socket
    protected DatagramSocket receiveSocket;    //接收分组使用的socket
    protected boolean isSendCarryData = false;    //标志是否发送文件
    protected boolean isRevCarriedData = false;    //标志是否接受文件
    protected byte[] dataList = new byte[0];    //储存文件数据
    protected List<Integer> dataLengthList = new ArrayList<>();    //每个数据报文的中数据长度
    protected int MaxDataPacketSize = 0;    //每个数据报文最大的储存的文件大小
    protected int DATA_NUMBER_Send = -1;    //发送分组个数
    protected String outputFileName = new String();    //输出文件名
    protected Map<Integer, Timer> TimerMap = new HashMap<>();     //分组的计时器

    public SR(int RECEIVE_PORT, int WINDOW_SIZE, int TIMEOUT, String name) throws IOException {
        this.WINDOW_SIZE = WINDOW_SIZE;
        this.TIMEOUT = TIMEOUT;
        this.hostName = name;

        sendSocket = new DatagramSocket();
        receiveSocket = new DatagramSocket(RECEIVE_PORT);
        destAddress = InetAddress.getLocalHost();
    }


    //发送文件
    public void sendData(String filename, int MaxDataPacketSize) throws IOException {
        File file = new File(filename);
        this.MaxDataPacketSize = MaxDataPacketSize;
        if (file.length() == 0) {
            System.out.println("文件为空！");
            return;
        }
        try {
            DATA_NUMBER_Send = 0;
            FileInputStream fis = new FileInputStream(file);
            byte[] bytes = new byte[MaxDataPacketSize];
            int length = 0;
            while ((length = fis.read(bytes, 0, bytes.length)) != -1) {
                DATA_NUMBER_Send++;
                dataList = addBytes(dataList, bytes);
                dataLengthList.add(length);
            }
            isSendCarryData = true;
            System.out.println(hostName + ":文件被拆分为" + DATA_NUMBER_Send + "个包");
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        send();
    }

    public void send() throws IOException {
        while (true) {
            // 发送分组循环
            while (nextSeq < base + WINDOW_SIZE && nextSeq <= DATA_NUMBER_Send) {
                // 模拟数据丢失
                if (nextSeq % 5 == 0) {
                    System.out.println(hostName + "模拟丢失报文：Seq = " + nextSeq);
                    Timer timer = new Timer(nextSeq, TIMEOUT, this);
                    TimerMap.put(nextSeq, timer);
                    timer.start();
                    nextSeq++;
                    continue;
                }

                byte[] data = new byte[MaxDataPacketSize];
                int length = 0;
                if (isSendCarryData) {
                    length = dataLengthList.get(nextSeq - 1);
                    int curByte = 0;
                    for (int i = 0; i < nextSeq - 1; i++) {
                        curByte += dataLengthList.get(i);
                    }
                    System.arraycopy(dataList, curByte, data, 0, length);
                }

                String sendDataLabel = hostName + ": Sending to port " + destPort + ", Seq = " + nextSeq
                        + " isDataCarried =" + isSendCarryData + " length = " + length + " DATA_NUMBER_Send = " + DATA_NUMBER_Send + "@@@@@";

                byte[] datagram = addBytes(sendDataLabel.getBytes(), data);

                DatagramPacket datagramPacket = new DatagramPacket(datagram, datagram.length, destAddress, destPort);
                sendSocket.send(datagramPacket);
                System.out.println(hostName + "发送到" + destPort + "端口， Seq = " + nextSeq);

                //等待该分组ACK的计时器
                Timer timer = new Timer(nextSeq, TIMEOUT, this);
                TimerMap.put(nextSeq, timer);
                timer.start();

                nextSeq = nextSeq + 1;
            }

            // 用尽窗口后开始接收ACK
            byte[] bytes = new byte[4096];
            DatagramPacket datagramPacket = new DatagramPacket(bytes, bytes.length);
            sendSocket.setSoTimeout(1000 * TIMEOUT);
            try {
                sendSocket.receive(datagramPacket);
                // 转换成String
                String fromServer = new String(datagramPacket.getData(), 0, datagramPacket.getLength());
                // 解析出ACK编号
                int ack = Integer.parseInt(fromServer.substring(fromServer.indexOf("ACK: ") + "ACK: ".length()).trim());
                senderReceivedACKSet.add(ack);    // 加入已收到set
                System.out.println(hostName + "收到了 ACK: " + ack);

                //停止并移去该计时器
                TimerMap.get(ack).interrupt();
                TimerMap.remove(ack);

                if (base == ack) {
                    // 向右滑动
                    while (senderReceivedACKSet.contains(base)) {
                        base++;
                    }
                    //System.out.println(hostName + " 当前窗口 [" + base + "," + (base + WINDOW_SIZE - 1) + "]");
                }

            } catch (SocketTimeoutException ex) {
                continue;
            }


            // 发送完了，此时base会滑到右边多一格
            if (base == DATA_NUMBER_Send + 1) {
                System.out.println(hostName + "发送完毕，发送方收到了全部的ACK");
                return;
            }
        }
    }

    public void receiveData(String fileName) throws IOException {
        isRevCarriedData = true;
        if (!fileName.trim().isEmpty()) {
            outputFileName = fileName;
            receive();
        } else {
            System.out.println("文件名为空！");
        }
    }

    public void receive() throws IOException {

        File output = null;
        FileOutputStream fos = null;
        if (isRevCarriedData) {
            output = new File(outputFileName);
            fos = new FileOutputStream(output);
        }

        int rcvBase = 1;

        while (true) {
            if (rcvBase == DATA_NUMBER_Send + 1) {
                System.out.println(hostName + "接受完毕，发送方收到了全部的数据");
                return;
            }
            byte[] receivedData = new byte[4096];
            DatagramPacket receivePacket = new DatagramPacket(receivedData, receivedData.length);
            receiveSocket.setSoTimeout(1000 * TIMEOUT);
            try {
                receiveSocket.receive(receivePacket);
            } catch (SocketTimeoutException ex) {
                System.out.println(hostName + " 正在等待分组的到来");
                continue;
            }

            // 收到的数据
            String receivedLabel = new String(receivedData, 0, receivedData.length);
            String label = receivedLabel.split("@@@@@")[0];
            int labelSize = (label + "@@@@@").getBytes().length;

            String pattern = "\\w*: Sending to port \\d+, Seq = (\\d+) isDataCarried =(true|false) length = (\\d+) DATA_NUMBER_Send = (\\d+)";
            Matcher matcher = Pattern.compile(pattern).matcher(label);

            if (!matcher.find()) {
                System.out.println(hostName + " 收到错误数据" + label);
                // 仍发送之前的ACK
                sendACK(base - 1, receivePacket.getAddress(), receivePacket.getPort());
                continue;
            }

            int seq = Integer.parseInt(matcher.group(1));
            isRevCarriedData = Boolean.parseBoolean(matcher.group(2));
            int dataLength = Integer.parseInt(matcher.group(3));
            DATA_NUMBER_Send = Integer.parseInt(matcher.group(4));

            if (seq >= rcvBase && seq <= rcvBase + WINDOW_SIZE - 1) {
                //收到了接受窗口里存在的分组
                receiverReceivedMap.put(seq, receivedData);
                System.out.println(hostName + "收到一个接收方窗口内的分组，Seq = " + seq + "已确认");

                //模拟ACK丢失
                if (seq % 7 == 0 && seq / 7 == flag) {
                    flag++;
                } else {
                    sendACK(seq, receivePacket.getAddress(), receivePacket.getPort());
                }

                if (seq == rcvBase) {
                    // 收到这个分组后可以开始滑动
                    while (receiverReceivedMap.containsKey(rcvBase)) {
                        fos.write(receiverReceivedMap.get(rcvBase), labelSize, dataLength);
                        receiverReceivedMap.remove(rcvBase);
                        rcvBase++;
                    }
                }
            } else {
                // 这个分组序列号不在窗口内，应该舍弃
                System.out.println(hostName + "收到一个不在窗口内的分组，Seq = " + seq + "因此丢弃此分组");
                if (seq < rcvBase) {
                    sendACK(seq, receivePacket.getAddress(), receivePacket.getPort());
                }
            }
        }
    }

    //超时重发
    public void timeOut(int Seq) throws IOException {

        System.out.println(hostName + " 接受ACK超时，重发Seq：" + Seq);

        int length = 0;
        byte[] data = new byte[MaxDataPacketSize];

        if (isSendCarryData) {
            length = dataLengthList.get(Seq - 1);
            int curByte = 0;
            for (int i = 0; i < Seq - 1; i++) {
                curByte += dataLengthList.get(i);
            }
            System.arraycopy(dataList, curByte, data, 0, length);
        }

        String resendData = hostName + ": Sending to port " + destPort + ", Seq = " + Seq
                + " isDataCarried =" + isSendCarryData + " length = " + length + " DATA_NUMBER_Send = " + DATA_NUMBER_Send + "@@@@@";

        byte[] datagram = addBytes(resendData.getBytes(), data);
        DatagramPacket datagramPacket = new DatagramPacket(datagram, datagram.length, destAddress, destPort);
        sendSocket.send(datagramPacket);
        Timer timer = new Timer(Seq, TIMEOUT, this);
        TimerMap.put(Seq, timer);
        timer.start();
    }

    //向发送方回应ACK
    protected void sendACK(int seq, InetAddress toAddr, int toPort) throws IOException {
        String response = hostName + " responses ACK: " + seq;
        byte[] responseData = response.getBytes();
        // 获得来源IP地址和端口，确定发给谁
        DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length, toAddr, toPort);
        receiveSocket.send(responsePacket);
    }

    public String getHostName() {
        return hostName;
    }

    public void setDestAddress(InetAddress destAddress) {
        this.destAddress = destAddress;
    }

    public int getDestPort() {
        return destPort;
    }

    public void setDestPort(int destPort) {
        this.destPort = destPort;
    }

    public static byte[] addBytes(byte[] data1, byte[] data2) {
        byte[] data3 = new byte[data1.length + data2.length];
        System.arraycopy(data1, 0, data3, 0, data1.length);
        System.arraycopy(data2, 0, data3, data1.length, data2.length);
        return data3;
    }
}

