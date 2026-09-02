package net.shiroha233.roadweaver.config.structure;

import net.minecraft.core.RegistryAccess;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class StructureDiscoveryServiceTest {

    @Test
    void defersWhenTheCreateWorldContextHasNoDimensionRegistry() {
        assertDoesNotThrow(() -> StructureDiscoveryService.discoverFromRegistryAccess(RegistryAccess.EMPTY));
    }
}
