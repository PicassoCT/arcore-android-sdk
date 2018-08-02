package com.google.ar.core.examples.app.common.tcpClient;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;

import com.google.ar.core.examples.app.common.helpers.SpringAR;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.net.DhcpInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;


import com.google.ar.core.examples.app.common.helpers.Stopwatch;
import com.google.ar.core.examples.app.common.helpers.comonUtils;

public class Server {
     //Server Port

    private String defaultHostIP = "192.168.178.20" ;
    InetAddress   defaultHostIPAddress;
    private String deviceIP = comonUtils.getIPAddress(true);
    InetAddress   deviceIPAddress;

    InetAddress hostIpAddress = null;
    byte[][] rcv_message;

    //Message Zähler - 0 bedeutet die Verbindung wurde reinitialisiert
    public int messageCounter = 0;

    //Basically Watchdogvariable- every arriving Datagram resets the timer
    public boolean stillConnected = true;

    public DatagramReciever datagramReciever = null;

    //Callback to return recieved data to the corresponding thread
    IPackageRecivedCallback packageRecipient;
    Context context;
    Stopwatch watchDog;

    //Constructor
    public Server(Context context, IPackageRecivedCallback packageRecipient) {
        stillConnected = false;
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
        datagramReciever = new DatagramReciever();
        datagramReciever.start();
    }

    protected void onPause() {
        datagramReciever.kill();
    }

    public class DatagramReciever extends Thread {
        boolean stillConnected = false;
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
        final byte[] searchDataHeaderByte = SpringAR.searchDataHeader.getBytes();
        final byte[] recieveHostReplyHeaderByte = SpringAR.recieveHostReplyHeader.getBytes(); //Ipadress
        final byte[] searchResetHeaderByte = SpringAR.searchResetHeaderString.getBytes(); //Ipadress

        final byte[] broadcastHeaderByte = (SpringAR.broadcasteHeader + comonUtils.getIPAddress(true)).getBytes();

        private boolean isDataMessage(byte[] payload) {
            return (-1 != comonUtils.indexOf(payload, searchDataHeaderByte));
        }


        public void run() {
            String message;


            Log.d("Server:Run", "Run started");
            String delMeOldMessage = new String();

            try {
                socket = new DatagramSocket(SpringAR.UDP_SERVER_PORT);

                while (true) {
                    broadcastDevice();
                    //Initialisation

                    while (stillConnected) {

                        DatagramPacket rcv_packet = new DatagramPacket(rcv_message[writeBuffer], rcv_message.length);

                        {
                            // Receiving
                            boolean NewMessageArrived = true;
                            socket.setSoTimeout(SpringAR.TIME_OF_FRAME_IN_MS);
                            try {
                                socket.receive(rcv_packet);
                            } catch (SocketTimeoutException e) {
                                NewMessageArrived = false;
                            }
                            resetWatchDogTimer(NewMessageArrived);

                            //TODO Delete String conversion
                            message = new String(rcv_message[writeBuffer], "US-ASCII");// rcv_message[writeBuffer].length);
                            if (!message.contentEquals(delMeOldMessage)) {
                                Log.d("Server:Run", "RCV: " + message);
                                delMeOldMessage = message;
                            }

                        }

                        {
                            // callback
                            if (isDataMessage(rcv_message[getReadBuffer()])) {
                                stillConnected = true;
                                packageRecipient.callback(rcv_message[getReadBuffer()], rcv_message[getReadBuffer()].length);
                            } else {
                                connectionStateMachine(rcv_message[getReadBuffer()]);
                            }
                        }

                        switchBuffer();
                        { //callback to update Buffer

                            if (newDatagramToSend && hostIpAddress != null) {
                                Log.d("Server:Run", "Run sending");
                                byte[] snd_message = datagramToSend.getBytes();

                                snd_packet = new DatagramPacket(snd_message, snd_message.length, hostIpAddress, SpringAR.UDP_SERVER_PORT);
                                socket.send(snd_packet);
                                Log.d("Server:Run", "SND: " + datagramToSend);
                                newDatagramToSend = false;
                            }
                        }


                        //Sending
                    }

                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
            if (socket != null) {
                socket.close();
            }
        }


        private void resetWatchDogTimer(boolean newMessageArrived) {
            if (watchDog.getElapsedTimeMili() > 2000 && !newMessageArrived) {
                stillConnected = false;
            }

            if (newMessageArrived) {//restart
                watchDog.start();
            }

        }

        // handles management traffic like configurstion files
        private void connectionStateMachine(byte[] payload) {
            if (comonUtils.indexOf(payload, searchResetHeaderByte) != -1) {
                messageCounter = 0;
                stillConnected = false;
                hostIpAddress= null;

            }

            if (comonUtils.indexOf(payload, recieveHostReplyHeaderByte) != -1) {
                messageCounter = 0;
                stillConnected = false;

                try {
                    hostIpAddress = InetAddress.getByName(payload.toString().replace(SpringAR.recieveHostReplyHeader , ""));
                    snd_packet.setAddress(hostIpAddress);
                    deviceIPAddress = findIPnearby(hostIpAddress.toString());

                    comonUtils.setStaticIP(context, deviceIPAddress);

                    //send Configuration Message
                     setSendToSpringMessage(SpringAR.formConfigurationMessage());
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            }
        }

        public InetAddress findIPnearby(String ip){

        }

        public void broadcastDevice() {

            try {
                //activate broadcast
                socket.setBroadcast(true);
                snd_packet = new DatagramPacket( (SpringAR.broadcasteHeader + comonUtils.getIPAddress(true)).getBytes() ,
                                                broadcastHeaderByte.length,
                                                 comonUtils.getBroadcastAddress(context),
                                                SpringAR.UDP_SERVER_PORT);

                Log.d("Server:Run", "Broadcast "+SpringAR.broadcasteHeader + comonUtils.getIPAddress(true));
                socket.send(snd_packet);

                DatagramPacket rcv_packet = new DatagramPacket(rcv_message[1], rcv_message[1].length);

                // Receiving
                try {
                    socket.setSoTimeout(SpringAR.TIME_OF_FRAME_IN_MS);
                    socket.receive(rcv_packet);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                socket.setBroadcast(false);

                connectionStateMachine(rcv_message[1]);

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Log.d("Server:Broadcast", "Interrupted");
                }
                socket.setBroadcast(false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }




        public void kill() {
            stillConnected = false;
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