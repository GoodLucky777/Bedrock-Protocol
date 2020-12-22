package com.nukkitx.protocol.bedrock.v422;

import com.nukkitx.nbt.NBTInputStream;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtUtils;
import com.nukkitx.network.VarInts;
import com.nukkitx.protocol.bedrock.BedrockSession;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import com.nukkitx.protocol.bedrock.v407.BedrockPacketHelper_v407;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;

import java.io.IOException;

/**
 * @author joserobjr
 * @since 2020-12-22
 */
public class BedrockPacketHelper_v422 extends BedrockPacketHelper_v407 {
    public static final BedrockPacketHelper_v422 INSTANCE = new BedrockPacketHelper_v422();
    @Override
    public ItemData readItem(ByteBuf buffer, BedrockSession session) {
        int id = VarInts.readInt(buffer);
        if (id == 0) {
            // We don't need to read anything extra.
            return ItemData.AIR;
        }
        int aux = VarInts.readInt(buffer);
        short damage = (short) (aux >> 8);
        if (damage == Short.MAX_VALUE) damage = -1;
        int count = aux & 0xff;
        int nbtSize = buffer.readShortLE();
        NbtMap compoundTag = null;
        if (nbtSize > 0) {
            try (NBTInputStream reader = NbtUtils.createReaderLE(new ByteBufInputStream(buffer.readSlice(nbtSize)))) {
                compoundTag = (NbtMap) reader.readTag();
            } catch (IOException e) {
                throw new IllegalStateException("Unable to load NBT data", e);
            }
        } else if (nbtSize == -1) {
            int tagCount = buffer.readUnsignedByte();
            if (tagCount != 1) throw new IllegalArgumentException("Expected 1 tag but got " + tagCount);
            try (NBTInputStream reader = NbtUtils.createNetworkReader(new ByteBufInputStream(buffer))) {
                compoundTag = (NbtMap) reader.readTag();
            } catch (IOException e) {
                throw new IllegalStateException("Unable to load NBT data", e);
            }
        }

        String[] canPlace = new String[VarInts.readInt(buffer)];
        for (int i = 0; i < canPlace.length; i++) {
            canPlace[i] = this.readString(buffer);
        }

        String[] canBreak = new String[VarInts.readInt(buffer)];
        for (int i = 0; i < canBreak.length; i++) {
            canBreak[i] = this.readString(buffer);
        }

        long blockingTicks = 0;
        if (this.isBlockingItem(id, session.getHardcodedBlockingId().get())) {
            blockingTicks = VarInts.readLong(buffer);
        }
        if (id == 355) { // shield runtime id
            buffer.skipBytes(1);
        }
        return ItemData.of(id, damage, count, compoundTag, canPlace, canBreak, blockingTicks);
    }
}
