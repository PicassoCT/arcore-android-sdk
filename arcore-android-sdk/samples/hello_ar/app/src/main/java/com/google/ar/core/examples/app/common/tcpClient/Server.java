package com.google.ar.core.examples.app.common.tcpClient;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import com.google.ar.core.examples.app.common.helpers.SpringAR;

import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import android.content.Context;
import android.util.Log;


import com.google.ar.core.examples.app.common.helpers.Stopwatch;
import com.google.ar.core.examples.app.common.helpers.comonUtils;

public class Server {
    //Server Port
    //0.0.0.0 able to recieve all (any address)

    private String defaultHostIP = "192.168.178.20";
    InetAddress defaultHostIPAddress;

    InetAddress hostIpAddress = null;
    InetAddress ARDeviceAddress;


    byte[][] rcv_message;
    int recieveByteIndex = 0;

    //Message Zähler - 0 bedeutet die Verbindung wurde reinitialisiert
    public int messageCounter = 0;


    public DatagramReciever datagramReciever = null;

    //Callback to return recieved data to the corresponding thread
    IPackageRecivedCallback packageRecipient;
    Context context;
    Stopwatch watchDog;
    public SpringAR.comStates State;


    public String getCurrentStateMachineState() {
        return State.name();
    }

    public void closeSocket(DatagramSocket socket) {

        if (socket != null && socket.isConnected() ) {
            while (!socket.isConnected()) {
                socket.disconnect();
                try {
                    Thread.sleep(SpringAR.TIME_OUT_IN_BROADCAST);
                } catch (InterruptedException e) {
                    Log.d(SpringAR.protocollDebugLogPrefix, " Socket Closing interrupted");
                    e.printStackTrace();
                }
            }
        }

        if (socket != null && !socket.isClosed()) {
            socket.close();
            while (!socket.isClosed()) {
                try {
                    Thread.sleep(SpringAR.TIME_OUT_IN_BROADCAST);
                } catch (InterruptedException e) {
                    Log.d(SpringAR.protocollDebugLogPrefix, " Socket Closing interrupted");
                    e.printStackTrace();
                }
            }
        }


    }

    public DatagramSocket createSocket(InetAddress ipAddress, int port) {
        try {
            DatagramSocket socket = new DatagramSocket(null);
            InetSocketAddress address = new InetSocketAddress(ipAddress, port);
            socket.bind(address);

            return socket;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public DatagramSocket getBroadcastListenerSocket() throws IOException {

        InetSocketAddress address = new InetSocketAddress(InetAddress.getByName("0.0.0.0"), 9000);
        DatagramSocket socket = new DatagramSocket(null);
        socket.setSoTimeout(30);
        socket.bind(address);
        return socket;
    }

    public DatagramSocket setSocketBroadcast(DatagramSocket oldSocket) {
        if (oldSocket != null && oldSocket.isBound()) {
            closeSocket(oldSocket);
        }
        DatagramSocket socket = null;
        try {
            ARDeviceAddress = InetAddress.getByName(comonUtils.getIPAddress(true));

            // senderSocket = createSocket(ARDeviceAddress, SpringAR.UDP_SERVER_PORT);
            socket = createSocket(InetAddress.getByName("0.0.0.0"), SpringAR.UDP_SERVER_PORT);
            socket.setBroadcast(true);
            socket.setSoTimeout(SpringAR.TIME_OF_FRAME_IN_MS);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return socket;
    }

    public DatagramSocket setSocketAddress(DatagramSocket oldSocket, InetAddress ipAddress, int port, InetAddress targetAddress) {

        if (oldSocket != null ) {
            closeSocket(oldSocket);
        }
        DatagramSocket socket = null;
        try {
            socket = createSocket(ipAddress, port);
            socket.setBroadcast(false);
            socket.setSoTimeout(SpringAR.TIME_OF_FRAME_IN_MS);
            if (targetAddress != null)
              socket.connect(targetAddress, port);

        } catch (SocketException e) {
            e.printStackTrace();
        }
        return socket;
    }


    //Constructor
    public Server(Context context, IPackageRecivedCallback packageRecipient) {
        State = SpringAR.comStates.STATE_resetCommunication;
        this.packageRecipient = packageRecipient;
        this.context = context;

        try {
            hostIpAddress = comonUtils.getBroadcastAddress(context);
            defaultHostIPAddress = InetAddress.getByName(defaultHostIP);


        } catch (IOException e) {
            e.printStackTrace();
        }

        rcv_message = new byte[2][SpringAR.MAX_UDP_DATAGRAM_RCV_LEN];
        this.datagramReciever = new DatagramReciever();

        datagramReciever.start();
        watchDog = new Stopwatch();
        watchDog.start();
    }

    //Restart the Server on Resume
    protected void onResume() {
        State = SpringAR.comStates.STATE_broadCastHeader;
        datagramReciever = new DatagramReciever();
        datagramReciever.start();
    }

    protected void onPause() {
        datagramReciever.kill();
    }


    public class DatagramReciever extends Thread {

        private String datagramToSend = "";
        private boolean newDatagramToSend = false;
        private DatagramPacket snd_packet;

        DatagramSocket senderSocket = null;
        DatagramSocket listenerSocket = null;
        private DatagramSocket broadCastListenerSocket;

        //Buffer gettters and setters
        private int writeBuffer = 0;
        private SpringAR.comStates oldState;

        int getReadBuffer() {
            if (writeBuffer == 1) return 0;
            return 1;
        }

        void switchBuffer() {
            recieveByteIndex = 0;
            writeBuffer = getReadBuffer();
        }

        public String dbg_message = "";
        //Management Communication Headers

        public void kill() {
            closeSocket(senderSocket);
            closeSocket(listenerSocket);
            closeSocket(broadCastListenerSocket);
        }

        /*
        private void testLoop() throws IOException {
        int modeSelector= 0;
            DatagramSocket sendSocket = null;
            DatagramSocket recieveSocket = null;


        while (true) {
            SpringAR.testCases toTest = SpringAR.testCases.bindsend;
            closeSocket(sendSocket);
            closeSocket(recieveSocket);

            modeSelector = (modeSelector+1)%4;
                switch (modeSelector){
                    case 0:  toTest = SpringAR.testCases.broadcastsend;break;
                    case 1: toTest = SpringAR.testCases.bindsend;break;
                    case 2: toTest = SpringAR.testCases.broadcastrecieve;break;
                    case 3: toTest = SpringAR.testCases.bindrecieve;break;

                }



            InetSocketAddress address;

            switch (toTest) {

                case broadcastrecieve:
                    address = new InetSocketAddress(InetAddress.getByName("0.0.0.0"), 9000);
                    recieveSocket = new DatagramSocket(null);
                    recieveSocket.setSoTimeout(30);
                    recieveSocket.bind(address);

                    break;
                case broadcastsend:
                    address = new InetSocketAddress(InetAddress.getByName("192.168.178.178"), 9000);
                    sendSocket = new DatagramSocket(null);
                    sendSocket.setSoTimeout(30);
                    sendSocket.setBroadcast(true);
                    sendSocket.bind(address);
                    break;

                case bindsend:
                    address = new InetSocketAddress(InetAddress.getByName("192.168.178.178"), 9000);
                    sendSocket = new DatagramSocket(null);
                    sendSocket.setSoTimeout(30);
                    sendSocket.bind(address);
                    InetSocketAddress remoteAddress = new InetSocketAddress(InetAddress.getByName("192.168.178.20"), 9000);
                    sendSocket.connect(remoteAddress);
                    sendSocket.setBroadcast(false);
                    break;

                case bindrecieve:
                    address = new InetSocketAddress(InetAddress.getByName("192.168.178.178"), 9000);
                    recieveSocket = new DatagramSocket(null);
                    recieveSocket.setSoTimeout(30);
                    recieveSocket.bind(address);
                    InetSocketAddress remoteAddressr = new InetSocketAddress(InetAddress.getByName("192.168.178.20"), 9000);
                    sendSocket.connect(remoteAddressr);
                    recieveSocket.setBroadcast(false);
                    break;
            }
            boolean boolTestCaseDone = false;
            while (!boolTestCaseDone) {
                Log.d(SpringAR.protocollDebugLogPrefix, "TestLoop in testcase : "+ toTest.name()+ " :");
                String dbg_message;
                if (recieveSocket != null) {
                    try {
                        DatagramPacket rcv_packet = new DatagramPacket(rcv_message[writeBuffer], rcv_message[writeBuffer].length);
                        recieveSocket.receive(rcv_packet);
                        dbg_message = new String(rcv_message[writeBuffer], 0, rcv_packet.getLength(), "US-ASCII");
                        Log.d(SpringAR.dataDebugLogPrefix, "" + rcv_packet.getAddress().getHostAddress() + ": " + dbg_message.trim() + " via ");
                        boolTestCaseDone=true;
                    } catch (SocketTimeoutException e) {

                    }
                }

                if (sendSocket != null ) {
                    try {
                        DatagramPacket snd_packet = null;
                        switch (toTest) {
                            case broadcastsend:
                             snd_packet = new DatagramPacket(("Testmessage send by " + toTest.name() + " socket").getBytes(),
                                                              ("Testmessage send by " + toTest.name() + " socket").length(),
                                                                InetAddress.getByName("255.255.255.255"),
                                                                SpringAR.UDP_SERVER_PORT
                                                                             );
                             break;
                            case bindsend:
                                snd_packet = new DatagramPacket(("Testmessage send by " + toTest.name() + " socket").getBytes(),
                                        ("Testmessage send by " + toTest.name() + " socket").length(),
                                        InetAddress.getByName("192.168.178.20"),
                                        SpringAR.UDP_SERVER_PORT
                                );
                                break;
                        }
                        sendSocket.send(snd_packet);
                        boolTestCaseDone=true;
                    } catch (IOException e1) {
                    }
                }
                try {
                    Thread.sleep(SpringAR.TIME_OUT_IN_BROADCAST);
                } catch (InterruptedException e) {
                    Log.d(SpringAR.protocollDebugLogPrefix, " Broadcast Interrupted");
                }

            }
        }
        }
*/

        public void run() {

            //Log.d(SpringAR.protocollDebugLogPrefix, "Server Run started");
            //  try {
            //      testLoop();
            //} catch (IOException e) {
            //   e.printStackTrace();
            //}

            try {

                initializeBroadcastConnection();

                while (true) {
                    //Log.d(SpringAR.protocollDebugLogPrefix, "Server Run Entered recieve");
                    //Recieving Datagramm
                    DatagramPacket rcv_packet = new DatagramPacket(rcv_message[writeBuffer], rcv_message[writeBuffer].length);
                    boolean NewMessageArrived = true;
                    try {
                        listenerSocket.receive(rcv_packet);
                    } catch (SocketTimeoutException e) {
                        //Log.d(SpringAR.dataDebugLogPrefix, "Timeout before recieving");
                        NewMessageArrived = false;
                    }
                    //Watchdog
                    handleWatchDogTimer(State);

                    //TODO Delete String conversion
                    if (NewMessageArrived) {
                        dbg_message = new String(rcv_message[writeBuffer], 0, rcv_packet.getLength(), "US-ASCII");
                        Log.d(SpringAR.dataDebugLogPrefix, "" + rcv_packet.getAddress().getHostAddress() + ": " + dbg_message.trim() + " of " + rcv_packet.getLength() + "length ");
                    }
                    //Log.d(SpringAR.protocollDebugLogPrefix, "Server Run Entered State machine");

                    connectionStateMachine(rcv_message);

                    //Log.d(SpringAR.protocollDebugLogPrefix, "Server Run Entered Sending");
                    //Sending Datagram
                    if (newDatagramToSend && hostIpAddress != null) {
                        //Log.d(SpringAR.protocollDebugLogPrefix, "Server sending: " + datagramToSend);
                        byte[] snd_message = datagramToSend.getBytes();

                        try {
                            snd_packet = packSendPackageByState(snd_message);
                            assert (snd_packet != null);
                            senderSocket.send(snd_packet);
                            newDatagramToSend = false;
                        } catch (IOException e1) {
                            e1.printStackTrace();
                            //causes     Caused by: android.system.ErrnoException: sendto failed: EINVAL (Invalid argument)
                            //Log.d(SpringAR.protocollDebugLogPrefix, "Server Error Sending in " + State.name());
                            assert (false);
                        }

                    }

                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        private void initializeBroadcastConnection() throws IOException {
            ARDeviceAddress = InetAddress.getByName(comonUtils.getIPAddress(true));
            senderSocket = setSocketAddress(null, ARDeviceAddress, SpringAR.UDP_SERVER_PORT, null);
            broadCastListenerSocket = getBroadcastListenerSocket();
            listenerSocket = broadCastListenerSocket;
        }


        // handles management traffic like configurstion files
        private void connectionStateMachine(byte[][] payload) throws IOException {
            //Reset triggered by Host
            if (comonUtils.indexOf(payload[writeBuffer], SpringAR.recieveResetHeaderByte) != -1) {
                State = SpringAR.comStates.STATE_resetCommunication;
            }

            Log.d(SpringAR.protocollDebugLogPrefix, "ConnectionStateMachine: " + State.name());
            switch (State) {

                case STATE_resetCommunication: {
                    messageCounter = 0;
                    listenerSocket = broadCastListenerSocket;
                    hostIpAddress = comonUtils.getBroadcastAddress(context);
                    senderSocket = setSocketBroadcast(senderSocket);
                    setSendToSpringMessage(SpringAR.sendResetHeader);
                    State = SpringAR.comStates.STATE_broadCastHeader;

                    return;
                }

                case STATE_broadCastHeader: {
                    if (comonUtils.indexOf(payload[writeBuffer], SpringAR.recieveHostReplyHeaderByte) != -1) {
                        Log.d(SpringAR.protocollDebugLogPrefix, " Host Reply Header recieved");
                        //Extract the hostIp
                        String hostIpAdressAsString = new String(payload[writeBuffer]);
                        hostIpAdressAsString = hostIpAdressAsString.replace(SpringAR.recieveHostReplyHeader, "");
                        hostIpAddress = InetAddress.getByName(hostIpAdressAsString);

                        //Set Connection from broadcast to target
                        ARDeviceAddress = InetAddress.getByName(comonUtils.getIPAddress(true));
                        Log.d(SpringAR.protocollDebugLogPrefix, " New Device Adress " + ARDeviceAddress);
                        senderSocket = setSocketAddress(senderSocket, ARDeviceAddress, SpringAR.UDP_SERVER_PORT, hostIpAddress);
                        listenerSocket = senderSocket;
                        State = SpringAR.comStates.STATE_sendCFG;
                        return;
                    }


                    setSendToSpringMessage(SpringAR.sendBroadcasteHeader);

                    try {
                        Thread.sleep(SpringAR.TIME_OUT_IN_BROADCAST);
                    } catch (InterruptedException e) {
                        Log.d(SpringAR.protocollDebugLogPrefix, " Broadcast Interrupted");
                    }
                    return;
                }

                case STATE_sendCFG: {
                    if (-1 != comonUtils.indexOf(payload[writeBuffer], SpringAR.recieveCFGHeaderByte)) {

                        State = SpringAR.comStates.STATE_sendRecieveData;
                        return;
                    }

                    setSendToSpringMessage(SpringAR.formConfigurationMessage());
                    return;
                }

                case STATE_sendRecieveData: {
                    if (-1 != comonUtils.indexOf(payload[writeBuffer], SpringAR.recieveDataHeaderByte)) {
                        writeRecievedDataToBuffer(rcv_message[getReadBuffer()], rcv_message[getReadBuffer()].length);
                    }
                }
                default:
                    Log.d(SpringAR.protocollDebugLogPrefix, "Connection State Machine invalid state");

            }
        }

        private void handleWatchDogTimer(SpringAR.comStates newState) {
            if (true) return;

            if (watchDog.getElapsedTimeMili() > SpringAR.watchDogTimeOutInMs
                    && newState == oldState) {
                State = SpringAR.comStates.STATE_resetCommunication;
            }

            if (newState != oldState) {//restart
                watchDog.start();
            }

            oldState = newState;
        }

        private DatagramPacket packSendPackageByState(byte[] snd_message) throws IOException {
            if (State == SpringAR.comStates.STATE_broadCastHeader || State == SpringAR.comStates.STATE_resetCommunication) {
                return new DatagramPacket(snd_message, snd_message.length, comonUtils.getBroadcastAddress(context), SpringAR.UDP_SERVER_PORT);
                //return new DatagramPacket(snd_message, snd_message.length, comonUtils.getScopedBroadcastAdress(context), SpringAR.UDP_SERVER_PORT);
            } else {
                Log.d(SpringAR.protocollDebugLogPrefix, "Package Connect to " + hostIpAddress + " at " + SpringAR.UDP_SERVER_PORT);
                return new DatagramPacket(snd_message, snd_message.length, hostIpAddress, SpringAR.UDP_SERVER_PORT);
            }
        }

        public boolean setSendToSpringMessage(String toSend) {
            if (newDatagramToSend) return false;

            datagramToSend = toSend;
            newDatagramToSend = true;
            messageCounter++;
            return newDatagramToSend;
        }


        private void writeRecievedDataToBuffer(byte[] bytes, int length) {
            boolean lastMessage = -1 != comonUtils.indexOf(bytes, SpringAR.recieveDataEndHeaderByte);

            int startIndex = (lastMessage ?
                    comonUtils.indexOf(bytes, SpringAR.recieveDataEndHeaderByte) + SpringAR.recieveDataEndHeader.length() :
                    comonUtils.indexOf(bytes, SpringAR.seperator.getBytes(), comonUtils.indexOf(bytes, SpringAR.recieveDataHeaderByte))
            );

            //Copy the data over
            for (int writeIndex = 0; writeIndex < length - startIndex; writeIndex++) {
                rcv_message[writeBuffer][recieveByteIndex + writeIndex] = bytes[writeIndex];
            }

            //update the write Index
            recieveByteIndex = recieveByteIndex + length;

            if (lastMessage) {
                //do the callback necessary
                switchBuffer();
                packageRecipient.callback(rcv_message[getReadBuffer()], rcv_message[getReadBuffer()].length);
            }
        }


    }
}