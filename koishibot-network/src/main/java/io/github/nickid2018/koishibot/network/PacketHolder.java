package io.github.nickid2018.koishibot.network;

import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Future;

public record PacketHolder(SerializableData packet, GenericFutureListener<? extends Future<? super Void>> listener) {
}
