package com.google.ar.core.examples.app.common.tcpClient;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;


import com.google.ar.core.examples.app.common.helpers.Stopwatch;
import com.google.ar.core.examples.app.common.helpers.comonUtils;

public class Server {
    //Size of the Transfered Datagrams
    int MAX_UDP_DATAGRAM_RCV_LEN = 1024;

    //Server Port
    int UDP_SERVER_PORT = 8090;
    private String defaultHostIP = "192.168.178.179";

    InetAddress ipAdress = null;
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
        stillConnected = true;
        this.packageRecipient = packageRecipient;
        this.context = context;

        try {
            ipAdress = InetAddress.getByName(defaultHostIP);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        MAX_UDP_DATAGRAM_RCV_LEN = 8 // PNG signature bytes
                + 25 // IHDR chunk
                + 12 // IDAT chunk (assuming only one IDAT chunk)
                + Resources.getSystem().getDisplayMetrics().heightPixels //pixels
                * (1 // filter byte for each row
                + (Resources.getSystem().getDisplayMetrics().widthPixels // pixels
                * 3 // Red, blue, green color samples
                * 2 // 16 bits per color sample
        )
        )
                + 6 // zlib compression overhead
                + 2 // deflate overhead
                + 12; // IEND chunk;

        rcv_message = new byte[2][MAX_UDP_DATAGRAM_RCV_LEN];
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
        int TIME_OF_FRAME_IN_MS = 30;
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
        final byte[] searchDataHeaderByte = "SPRINGARSND;DATA;".getBytes();
        final String searchResetHeaderString = "SPRINGARSND;RESET;IPADDRRESS=";
        final byte[] searchResetHeaderByte = searchResetHeaderString.getBytes(); //Ipadress
        final String broadcastHeaderString = "SPRINGAR;BROADCAST;IPADDRRESS=" + comonUtils.getIPAddress(true);
        final byte[] broadcastHeaderByte = broadcastHeaderString.getBytes();

        private boolean isDataMessage(byte[] payload) {
            return (-1 != comonUtils.indexOf(payload, searchDataHeaderByte));
        }


        public void run() {
            String message;


            Log.d("Server:Run", "Run started");
            String delMeOldMessage = new String();

            try {
                socket = new DatagramSocket(UDP_SERVER_PORT);

                while (true) {
                    broadcastDevice();
                    //Initialisation

                    while (stillConnected) {

                        DatagramPacket rcv_packet = new DatagramPacket(rcv_message[writeBuffer], rcv_message.length);

                        {
                            // Receiving
                            boolean NewMessageArrived = true;
                            socket.setSoTimeout(TIME_OF_FRAME_IN_MS);
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
                                packageRecipient.callback(rcv_message[getReadBuffer()], rcv_message[getReadBuffer()].length);
                            } else {
                                handleManagedTraffic(rcv_message[getReadBuffer()]);
                            }
                        }

                        switchBuffer();
                        { //callback to update Buffer

                            if (newDatagramToSend && ipAdress != null) {
                                Log.d("Server:Run", "Run sending");
                                byte[] snd_message = datagramToSend.getBytes();

                                snd_packet = new DatagramPacket(snd_message, snd_message.length, ipAdress, UDP_SERVER_PORT);
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
        private void handleManagedTraffic(byte[] payload) {
            if (comonUtils.indexOf(payload, searchResetHeaderByte) != -1) {
                messageCounter = 0;
                try {
                    ipAdress = InetAddress.getByName(payload.toString().replace(searchResetHeaderString, ""));
                    snd_packet.setAddress(ipAdress);
                    stillConnected = true;
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }

            }
        }

        public void broadcastDevice() {
            Log.d("Server:Run", "Broadcast");
            try {
                //activate broadcast
                socket.setBroadcast(true);
                snd_packet = new DatagramPacket(broadcastHeaderByte, broadcastHeaderByte.length, InetAddress.getByName("255.255.255.255"), UDP_SERVER_PORT);
                socket.send(snd_packet);

                DatagramPacket rcv_packet = new DatagramPacket(rcv_message[1], rcv_message[1].length);

                // Receiving
                try {
                    socket.setSoTimeout(TIME_OF_FRAME_IN_MS);
                    socket.receive(rcv_packet);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                handleManagedTraffic(rcv_message[1]);

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