package net.shiroha233.roadweaver.config.sub;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StructurePredictionConfigTest {

    @Test
    void boundsOtherStructureRoadOffsetToTheConfigScreenRange() {
        StructurePredictionConfig config = new StructurePredictionConfig();

        config.setOtherStructureRoadOffset(128);
        config.sanitize();
        assertEquals(64, config.otherStructureRoadOffset());

        config.setOtherStructureRoadOffset(0);
        config.sanitize();
        assertEquals(0, config.otherStructureRoadOffset());
    }
}
