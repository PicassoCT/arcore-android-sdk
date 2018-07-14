package com.google.ar.core.examples.app.common.tcpClient;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import android.util.Log;

public class AltServer {
    int MAX_UDP_DATAGRAM_RCV_LEN = 1024;
    int MAX_UDP_DATAGRAM_SND_LEN = 1024;
    int UDP_SERVER_PORT = 8090;
    int TIME_OF_FRAME_IN_MS = 33;

    private MyDatagramReceiver myDatagramReceiver = null;
    //Call the Server on Resume
    protected void onResume() {
        myDatagramReceiver = new MyDatagramReceiver();
        myDatagramReceiver.start();
    }
    protected void onPause() {
        myDatagramReceiver.kill();
    }

    private class MyDatagramReceiver extends Thread {
        private boolean bKeepRunning = true;
        private String lastMessage = "";
        private String datagramToSend = "";
        private boolean newDatagramToSend= false;
        float TIME_OF_FRAME = 3000;
        DatagramSocket socket = null;

        public void run() {
            String message;
            byte[] rcv_message = new byte[MAX_UDP_DATAGRAM_RCV_LEN];
            byte[] snd_message = new byte[MAX_UDP_DATAGRAM_SND_LEN];

            DatagramPacket rcv_packet = new DatagramPacket(rcv_message, rcv_message.length);
            DatagramPacket snd_packet = new DatagramPacket(snd_message, snd_message.length);
            try {
                DatagramSocket socket = new DatagramSocket(UDP_SERVER_PORT);
                while(bKeepRunning) {

                    // Receiving
                    socket.setSoTimeout(TIME_OF_FRAME_IN_MS);
                    socket.receive(rcv_packet);
                    message = new String(rcv_message, 0, rcv_packet.getLength());
                    Log.d("altServer", message);
                    //start process image thread


                    //callback to update Buffer
                    //TODO runOnUiThread(updateTextMessage);
                    if (newDatagramToSend) {
                        snd_message = datagramToSend.getBytes();
                        snd_packet = new DatagramPacket(snd_message , snd_message.length, snd_packet.getAddress(), snd_packet.getPort());
                        socket.send(snd_packet);
                        newDatagramToSend = false;
                    }
                    //Sending



                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
            if (socket != null) {
                socket.close();
            }
        }
        public void kill() {
            bKeepRunning = false;
        }
        public void setDatagram(String toSend) {
            datagramToSend = toSend;
            newDatagramToSend  = true;
        }
    }

// private Runnable updateTextMessage = new Runnable() {
    // public void run() {
    // if (myDatagramReceiver == null) return;
    // textMessage.setText(myDatagramReceiver.getLastMessage());
    // }
// };

}
