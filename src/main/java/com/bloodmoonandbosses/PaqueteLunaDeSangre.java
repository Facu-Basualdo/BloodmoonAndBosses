package com.bloodmoonandbosses;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Le avisa a un cliente con el mod si ve una luna de sangre, para que tiña el cielo. Sólo se manda a quien puede
 * recibirlo (ServerPlayNetworking.canSend): un cliente vanilla no se entera y entra igual.
 */
public record PaqueteLunaDeSangre(boolean activa) implements CustomPacketPayload {
	public static final Type<PaqueteLunaDeSangre> TYPE = new Type<>(BloodmoonAndBosses.id("luna_de_sangre"));
	public static final StreamCodec<RegistryFriendlyByteBuf, PaqueteLunaDeSangre> CODEC =
			ByteBufCodecs.BOOL.map(PaqueteLunaDeSangre::new, PaqueteLunaDeSangre::activa).cast();

	@Override
	public Type<PaqueteLunaDeSangre> type() {
		return TYPE;
	}
}
