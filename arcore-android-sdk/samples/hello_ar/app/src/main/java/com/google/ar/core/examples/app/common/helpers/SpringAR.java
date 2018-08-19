package com.google.ar.core.examples.app.common.helpers;

import android.content.res.Resources;
import android.os.Build;

//Static Ressource class
public  final  class SpringAR {

    public static long watchDogTimeOutInMs = 30 * 1000;
    public static long snackBarUpdateTimeInMs =  1000;

    public  enum comStates {
        STATE_broadCastHeader ,
        STATE_sendCFG,
        STATE_resetCommunication,
        STATE_sendRecieveData



    };


    public static final String protocollDebugLogPrefix = "DBG_PROTOCOLL";
    public static final String dataDebugLogPrefix = "DBG_DATA";

    //Protocol element strings
    public static final String seperator = ";";

    public static final String sendResetHeader = "SPRINGAR;RESET=DEVICE;";
    public static final byte[] sendResetHeaderByte = sendResetHeader.getBytes();

    public static final String sendBroadcasteHeader = "SPRINGAR;BROADCAST;ARDEVICE;";
    public static final byte[] sendBroadcasteHeaderByte = sendBroadcasteHeader.getBytes();

    public static final String sendCFGHeader = "SPRINGAR;CFG;";
    public static final byte[] sendCFGHeaderByte = sendCFGHeader.getBytes();

    public static final String sendCAMHeader = "SPRINGAR;DATA;";
    public static final byte[] sendCAMHeaderByte = sendCAMHeader.getBytes();

    public static final String recieveHostReplyHeader = "SPRINGAR;REPLY;HOSTIP=";
    public static final byte[] recieveHostReplyHeaderByte = recieveHostReplyHeader.getBytes();

    public static final String recieveCFGHeader = "SPRINGAR;CFG;RECIEVED;";
    public static final byte[] recieveCFGHeaderByte = recieveCFGHeader.getBytes();

    public static final String recieveResetHeader = "SPRINGAR;RESET=HOST;";
    public static final byte[] recieveResetHeaderByte = recieveResetHeader.getBytes();

    public static final String recieveDataHeader = "SPRINGAR;DATA=";
    public static final byte[] recieveDataHeaderByte = SpringAR.recieveDataHeader.getBytes();

    public static final String recieveDataEndHeader = "SPRINGAR;DATA=LAST";
    public static final byte[] recieveDataEndHeaderByte = SpringAR.recieveDataEndHeader.getBytes();



    public static final int UDP_SERVER_PORT = 9000;
    public static final  int TIME_OF_FRAME_IN_MS = 30;
    public static final int TIME_OUT_IN_BROADCAST = 1000;
    public static int MAX_UDP_DATAGRAM_RCV_LEN = 8 // PNG signature bytes
            + 25 // IHDR chunk
            + 12 // IDAT chunk (assuming only one IDAT chunk)
            + Resources.getSystem().getDisplayMetrics().heightPixels //pixels
                * (1 // filter byte for each row
                        + (Resources.getSystem().getDisplayMetrics().widthPixels // pixels
                * 3 // Red, blue, green color samples
                        * 2 // 16 bits per color sample
                        ))
                        + 6 // zlib compression overhead
                        + 2 // deflate overhead
                        + 12; // IEND chunk;


    public  static String formConfigurationMessage() {

        return  SpringAR.sendCFGHeader +
                "MODEL=" + Build.MODEL + SpringAR.seperator +//devicename
                "DISPLAYWIDTH=" + Resources.getSystem().getDisplayMetrics().widthPixels + SpringAR.seperator +// screen width
                "DISPLAYHEIGTH="+ Resources.getSystem().getDisplayMetrics().heightPixels + SpringAR.seperator +// screen heigth
                "DISPLAYDIVIDE="+ 50 + SpringAR.seperator +// divider
                "IPADDRESS=" +  comonUtils.getIPAddress(true) + SpringAR.seperator;

    }

}

