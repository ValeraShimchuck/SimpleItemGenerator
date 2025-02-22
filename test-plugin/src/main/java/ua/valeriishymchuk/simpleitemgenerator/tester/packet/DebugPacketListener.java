package ua.valeriishymchuk.simpleitemgenerator.tester.packet;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;

public class DebugPacketListener implements PacketListener {


    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        System.out.println("got packet: " + event.getPacketType().getName());
    }
}
