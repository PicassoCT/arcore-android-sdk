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

    //Message Zähler - 0 bedeutet die Verbindung wurde reinitialisiert
    public int messageCounter = 0;

    public DatagramReciever datagramReciever = null;

    //Callback to return recieved data to the corresponding thread
    IPackageRecivedCallback packageRecipient;
    Context context;
    Stopwatch watchDog;
    SpringAR.comStates State;

    //Constructor
    public Server(Context context, IPackageRecivedCallback packageRecipient) {
        State = SpringAR.comStates.STATE_broadCastHeader;

        this.packageRecipient = packageRecipient;
        this.context = context;

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

        int getReadBuffer() {
            if (writeBuffer == 1) return 0;
            return 1;
        }

        void switchBuffer() {
            writeBuffer = getReadBuffer();
        }


        //Management Communication Headers



        public void run() {
            String message;


            Log.d("Server:Run", "Run started");
            String delMeOldMessage = new String();

            try {
                socket = new DatagramSocket(SpringAR.UDP_SERVER_PORT);

                while (true) {

                    {
                        DatagramPacket rcv_packet = new DatagramPacket(rcv_message[writeBuffer], rcv_message.length);

                        socket.setSoTimeout(SpringAR.TIME_OF_FRAME_IN_MS);
                        boolean NewMessageArrived = true;
                        try {
                            socket.receive(rcv_packet);
                        } catch (SocketTimeoutException e) {
                            NewMessageArrived = false;
                        }
                        resetWatchDogTimer(NewMessageArrived);

                        //TODO Delete String conversion
                        message = new String(rcv_message[writeBuffer], "US-ASCII");// rcv_message[writeBuffer].length);
                        if (NewMessageArrived && !message.contentEquals(delMeOldMessage)) {
                            Log.d("Server:Run", "RCV: " + message);
                            delMeOldMessage = message;
                        }
                    }

                connectionStateMachine(rcv_message);

                { //callback to update Buffer

                    if (newDatagramToSend && hostIpAddress != null) {
                        Log.d("Server:Run", "Run sending");
                        byte[] snd_message = datagramToSend.getBytes();

                        snd_packet = new DatagramPacket(snd_message, snd_message.length, hostIpAddress, SpringAR.UDP_SERVER_PORT);
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
            }catch (IOException e1) {
                e1.printStackTrace();
            }
        }



        private void resetWatchDogTimer(boolean newMessageArrived) {
            if (watchDog.getElapsedTimeMili() > 2000 && !newMessageArrived) {
                State = SpringAR.comStates.STATE_resetCommunication;
            }

            if (newMessageArrived) {//restart
                watchDog.start();
            }

        }

        // handles management traffic like configurstion files
        private void connectionStateMachine(byte[][] payload) throws IOException {
            //Reset triggered by Host
            if (comonUtils.indexOf(payload[1], SpringAR.recieveResetHeaderByte ) != -1) {State = SpringAR.comStates.STATE_resetCommunication;}
            Log.d("ConnectionStateMachine","State: "+ State.name());
            switch (State) {
                case STATE_resetCommunication: {
                    messageCounter = 0;
                    hostIpAddress = null;

                    State = SpringAR.comStates.STATE_broadCastHeader;
                    return;
                }

                case STATE_broadCastHeader: {
                    if (comonUtils.indexOf(payload[1], SpringAR.recieveHostReplyHeaderByte) != -1) {
                        socket.setBroadcast(false);
                        State = SpringAR.comStates.STATE_sendCFG;
                        return;
                    }

                    socket.setBroadcast(true);
                    hostIpAddress = comonUtils.getBroadcastAddress(context);
                    setSendToSpringMessage(SpringAR.sendBroadcasteHeader);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Log.d("Server:Broadcast", "Interrupted");
                    }
                    return;
                }

                case STATE_sendCFG: {
                    if (-1 != comonUtils.indexOf(payload[writeBuffer], SpringAR.recieveDataHeaderByte)) {
                        State = SpringAR.comStates.STATE_sendRecieveData;
                        return;
                     }

                    setSendToSpringMessage(SpringAR.formConfigurationMessage());
                    return;
                }

                case STATE_sendRecieveData: {
                    if (-1 != comonUtils.indexOf(payload[writeBuffer], SpringAR.recieveDataHeaderByte)) {
                        packageRecipient.callback(rcv_message[getReadBuffer()], rcv_message[getReadBuffer()].length);
                        switchBuffer();
                    }
                }
                default:
                    Log.e("Connection State Machine", "Invalid State");
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