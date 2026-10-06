package com.google.ar.core.examples.app.common.rendering;

import android.graphics.Bitmap;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;

/**
 * Receives Recoil's LAN-only FrameStream UDP protocol and decodes QOI/RGBA frames.
 * The receiver deliberately keeps only the newest in-progress frame.
 */
public final class FrameStreamReceiver extends Thread {
    public interface Listener {
        void onFrame(Bitmap bitmap);
    }

    private static final String TAG = FrameStreamReceiver.class.getSimpleName();
    private static final int PORT = 9001;
    private static final int MAGIC = 0x4D415253; // "MARS"
    private static final int HEADER_SIZE = 28;
    private static final int MAX_FRAME_BYTES = 16 * 1024 * 1024;

    private final Listener listener;
    private volatile boolean running = true;
    private DatagramSocket socket;

    private int currentFrameId = -1;
    private int currentWidth;
    private int currentHeight;
    private int expectedChunks;
    private int expectedBytes;
    private byte[][] chunks;
    private boolean[] received;
    private int receivedCount;

    public FrameStreamReceiver(Listener listener) {
        super("MOSAIC-FrameStream");
        this.listener = listener;
    }

    @Override
    public void run() {
        try {
            socket = new DatagramSocket(PORT);
            socket.setReuseAddress(true);

            byte[] datagram = new byte[1200];
            while (running) {
                DatagramPacket packet = new DatagramPacket(datagram, datagram.length);
                socket.receive(packet);
                handlePacket(packet.getData(), packet.getLength());
            }
        } catch (SocketException e) {
            if (running) Log.e(TAG, "Frame stream socket failed", e);
        } catch (IOException e) {
            if (running) Log.e(TAG, "Frame stream receive failed", e);
        } finally {
            if (socket != null) {
                socket.close();
                socket = null;
            }
        }
    }

    public void shutdown() {
        running = false;
        if (socket != null) socket.close();
        interrupt();
    }

    private void handlePacket(byte[] packet, int length) {
        if (length < HEADER_SIZE) return;
        if (readU32(packet, 0) != MAGIC) return;
        if ((packet[4] & 0xff) != 1) return; // protocol version
        if ((packet[5] & 0xff) != 1) return; // codec: QOI RGBA

        int frameId = readU32(packet, 8);
        int width = readU16(packet, 12);
        int height = readU16(packet, 14);
        int chunkIndex = readU16(packet, 16);
        int chunkCount = readU16(packet, 18);
        int totalBytes = readU32(packet, 20);
        int payloadBytes = readU16(packet, 24);

        if (width < 1 || height < 1 || width > 1920 || height > 1080) return;
        if (chunkCount < 1 || chunkCount > 65535) return;
        if (chunkIndex >= chunkCount) return;
        if (totalBytes < 14 || totalBytes > MAX_FRAME_BYTES) return;
        if (payloadBytes < 0 || HEADER_SIZE + payloadBytes > length) return;

        if (frameId != currentFrameId) {
            beginFrame(frameId, width, height, chunkCount, totalBytes);
        }

        if (frameId != currentFrameId ||
                width != currentWidth ||
                height != currentHeight ||
                chunkCount != expectedChunks ||
                totalBytes != expectedBytes) {
            return;
        }

        if (!received[chunkIndex]) {
            chunks[chunkIndex] = Arrays.copyOfRange(packet, HEADER_SIZE, HEADER_SIZE + payloadBytes);
            received[chunkIndex] = true;
            receivedCount++;
        }

        if (receivedCount == expectedChunks) {
            byte[] qoi = assembleFrame();
            resetFrame();
            if (qoi == null) return;

            Bitmap bitmap = decodeQoi(qoi);
            if (bitmap != null && listener != null) {
                listener.onFrame(bitmap);
            }
        }
    }

    private void beginFrame(int frameId, int width, int height, int chunkCount, int totalBytes) {
        currentFrameId = frameId;
        currentWidth = width;
        currentHeight = height;
        expectedChunks = chunkCount;
        expectedBytes = totalBytes;
        chunks = new byte[chunkCount][];
        received = new boolean[chunkCount];
        receivedCount = 0;
    }

    private byte[] assembleFrame() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream(expectedBytes);
            for (byte[] chunk : chunks) {
                if (chunk == null) return null;
                out.write(chunk);
            }
            byte[] data = out.toByteArray();
            return data.length == expectedBytes ? data : null;
        } catch (IOException impossible) {
            return null;
        }
    }

    private void resetFrame() {
        currentFrameId = -1;
        chunks = null;
        received = null;
        receivedCount = 0;
    }

    private Bitmap decodeQoi(byte[] qoi) {
        if (qoi.length < 22) return null;
        if (qoi[0] != 'q' || qoi[1] != 'o' || qoi[2] != 'i' || qoi[3] != 'f') return null;

        int width = readU32(qoi, 4);
        int height = readU32(qoi, 8);
        int channels = qoi[12] & 0xff;
        if (width != currentWidth && currentWidth != 0) {
            // currentWidth is reset before decode; packet header already validated.
        }
        if (width < 1 || height < 1 || width > 1920 || height > 1080 || channels != 4) return null;

        int pixelCount = width * height;
        int[] pixels = new int[pixelCount];
        int[][] index = new int[64][4];

        int r = 0, g = 0, b = 0, a = 255;
        int run = 0;
        int p = 14;

        for (int i = 0; i < pixelCount; i++) {
            if (run > 0) {
                run--;
            } else {
                if (p >= qoi.length - 8) return null;
                int op = qoi[p++] & 0xff;

                if (op == 0xFE) {
                    if (p + 2 >= qoi.length) return null;
                    r = qoi[p++] & 0xff;
                    g = qoi[p++] & 0xff;
                    b = qoi[p++] & 0xff;
                } else if (op == 0xFF) {
                    if (p + 3 >= qoi.length) return null;
                    r = qoi[p++] & 0xff;
                    g = qoi[p++] & 0xff;
                    b = qoi[p++] & 0xff;
                    a = qoi[p++] & 0xff;
                } else {
                    switch (op & 0xC0) {
                        case 0x00: {
                            int[] px = index[op & 0x3F];
                            r = px[0]; g = px[1]; b = px[2]; a = px[3];
                            break;
                        }
                        case 0x40:
                            r = (r + ((op >> 4 & 0x03) - 2)) & 0xff;
                            g = (g + ((op >> 2 & 0x03) - 2)) & 0xff;
                            b = (b + ((op & 0x03) - 2)) & 0xff;
                            break;
                        case 0x80: {
                            if (p >= qoi.length) return null;
                            int b2 = qoi[p++] & 0xff;
                            int dg = (op & 0x3F) - 32;
                            int drdg = (b2 >> 4) - 8;
                            int dbdg = (b2 & 0x0F) - 8;
                            r = (r + dg + drdg) & 0xff;
                            g = (g + dg) & 0xff;
                            b = (b + dg + dbdg) & 0xff;
                            break;
                        }
                        case 0xC0:
                            run = op & 0x3F;
                            break;
                    }
                }

                int hash = (r * 3 + g * 5 + b * 7 + a * 11) & 63;
                index[hash][0] = r;
                index[hash][1] = g;
                index[hash][2] = b;
                index[hash][3] = a;
            }

            // glReadPixels is bottom-up; Bitmap is top-down.
            int srcY = i / width;
            int x = i - srcY * width;
            int dstY = height - 1 - srcY;
            pixels[dstY * width + x] =
                    ((a & 0xff) << 24) |
                    ((r & 0xff) << 16) |
                    ((g & 0xff) << 8) |
                    (b & 0xff);
        }

        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
    }

    private static int readU16(byte[] data, int off) {
        return ((data[off] & 0xff) << 8) | (data[off + 1] & 0xff);
    }

    private static int readU32(byte[] data, int off) {
        return ((data[off] & 0xff) << 24) |
                ((data[off + 1] & 0xff) << 16) |
                ((data[off + 2] & 0xff) << 8) |
                (data[off + 3] & 0xff);
    }
}
