package com.google.ar.core.examples.app.common.tcpClient;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import com.google.ar.core.examples.app.common.helpers.SpringAR;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import android.content.Context;
import android.util.Log;


import com.google.ar.core.examples.app.common.helpers.Stopwatch;
import com.google.ar.core.examples.app.common.helpers.comonUtils;

public class Server {
    //Server Port

    private String defaultHostIP = "192.168.178.20";
    InetAddress defaultHostIPAddress;
    private String deviceIP = comonUtils.getIPAddress(true);
    InetAddress deviceIPAddress;

    InetAddress hostIpAddress = null;
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

    //Constructor
    public Server(Context context, IPackageRecivedCallback packageRecipient) {
        State = SpringAR.comStates.STATE_resetCommunication;

        this.packageRecipient = packageRecipient;
        this.context = context;
        try {
            hostIpAddress = comonUtils.getBroadcastAddress(context);
        } catch (IOException e) {
            e.printStackTrace();
        }


        try {

            defaultHostIPAddress = InetAddress.getByName(defaultHostIP);
            deviceIP = comonUtils.getIPAddress(true);
            defaultHostIPAddress = InetAddress.getByName(deviceIP);

        } catch (UnknownHostException e) {
            e.printStackTrace();
        }


        rcv_message = new byte[2][SpringAR.MAX_UDP_DATAGRAM_RCV_LEN];
        this.datagramReciever = new DatagramReciever();
        watchDog = new Stopwatch();
        watchDog.start();
        datagramReciever.start();
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

        DatagramSocket socket = null;

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


        public void run() {


            Log.d("Server:Run", "Run started");
            String delMeOldMessage = new String();

            try {
                socket = new DatagramSocket(SpringAR.UDP_SERVER_PORT);
                socket.setSoTimeout(SpringAR.TIME_OF_FRAME_IN_MS);

                while (true) {

                    {//Recieving Datagramm
                        DatagramPacket rcv_packet = new DatagramPacket(rcv_message[writeBuffer], rcv_message.length);
                        boolean NewMessageArrived = true;
                        try {
                            socket.receive(rcv_packet);
                        } catch (SocketTimeoutException e) {
                            NewMessageArrived = false;
                        }
                        //Watchdog
                        resetWatchDogTimer(State);

                        //TODO Delete String conversion
                        dbg_message = new String(rcv_message[writeBuffer], "US-ASCII");// rcv_message[writeBuffer].length);
                      //  if (NewMessageArrived && !dbg_message.contentEquals(delMeOldMessage)) {
                            Log.d("Server:Run", "RCV: " + dbg_message);
                        //    delMeOldMessage = dbg_message;
                        //  }
                    }

                    connectionStateMachine(rcv_message);

                    { //Sending Datagram

                        if (newDatagramToSend && hostIpAddress != null) {
                            Log.d("Server:Run", "Run sending");
                            byte[] snd_message = datagramToSend.getBytes();

                            snd_packet = new DatagramPacket(snd_message, snd_message.length, getAdressByState(), SpringAR.UDP_SERVER_PORT);

                            try {
                                socket.send(snd_packet);
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }
                            Log.d("Server:Run", "Send: " + datagramToSend);
                            newDatagramToSend = false;
                        }
                    }
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }


        private void resetWatchDogTimer(SpringAR.comStates newState ) {
            if (watchDog.getElapsedTimeMili() > SpringAR.watchDogTimeOutInMs
                    && newState == oldState) {
                State = SpringAR.comStates.STATE_resetCommunication;
            }

            if (newState != oldState ) {//restart
                watchDog.start();
            }

            oldState = newState;
        }

        InetAddress getAdressByState() {
            if (State == SpringAR.comStates.STATE_broadCastHeader) {
                try {
                    return comonUtils.getBroadcastAddress(context);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return hostIpAddress;

        }

        // handles management traffic like configurstion files
        private void connectionStateMachine(byte[][] payload) throws IOException {
            //Reset triggered by Host
            if (comonUtils.indexOf(payload[1], SpringAR.recieveResetHeaderByte) != -1) {
                State = SpringAR.comStates.STATE_resetCommunication;
            }
            Log.d("ConnectionStateMachine", "State: " + State.name());
            switch (State) {

                case STATE_resetCommunication: {
                    messageCounter = 0;
                    hostIpAddress = null;
                    socket.setBroadcast(true);

                    if (setSendToSpringMessage(SpringAR.sendResetHeader))
                         State = SpringAR.comStates.STATE_broadCastHeader;

                    return;
                }

                case STATE_broadCastHeader: {
                    if (comonUtils.indexOf(payload[writeBuffer], SpringAR.recieveHostReplyHeaderByte) != -1) {

                        socket.setBroadcast(false);
                        String hostIpAdressAsString = new String(payload[writeBuffer]);
                        hostIpAdressAsString=  hostIpAdressAsString.replace(SpringAR.recieveHostReplyHeader,"");
                        hostIpAddress= InetAddress.getByName(hostIpAdressAsString);

                        State = SpringAR.comStates.STATE_sendCFG;
                        return;
                    }


                    setSendToSpringMessage(SpringAR.sendBroadcasteHeader);

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Log.d("Server:Broadcast", "Interrupted");
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
                    Log.e("Connection State Machine", "Invalid State");
            }
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


        public void kill() {
            socket.close();
        }


        public boolean setSendToSpringMessage(String toSend) {
            if (newDatagramToSend) return false;

            datagramToSend = toSend;
            newDatagramToSend = true;
            messageCounter++;
            return newDatagramToSend;
        }


    }
}