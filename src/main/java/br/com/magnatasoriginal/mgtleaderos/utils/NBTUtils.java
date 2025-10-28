package br.com.magnatasoriginal.mgtleaderos.utils;

import br.com.magnatasoriginal.mgtleaderos.MGTLeaderos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;

/**
 * Utilitário para manipulação de NBT
 * TODO: Implementar suporte a DataComponents do Minecraft 1.21.1
 * Por enquanto, NBT não é processado (o plugin original usa ItemStack simples)
 */
public class NBTUtils {

    /**
     * Converte uma string NBT em CompoundTag
     * TODO: Adaptar para DataComponents quando necessário
     */
    public static CompoundTag parseNBT(String nbtString) {
        try {
            return TagParser.parseTag(nbtString);
        } catch (Exception e) {
            MGTLeaderos.LOGGER.error("Erro ao parsear NBT: " + nbtString, e);
            return new CompoundTag();
        }
    }

    /**
     * Verifica se uma string é um NBT válido
     */
    public static boolean isValidNBT(String nbtString) {
        try {
            TagParser.parseTag(nbtString);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

