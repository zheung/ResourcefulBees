package com.resourcefulbees.resourcefulbees.compat.top;

import mcjty.theoneprobe.api.IBlockDisplayOverride;
import mcjty.theoneprobe.api.ITheOneProbe;

import javax.annotation.Nullable;
import java.util.function.Function;

public class TopCompat implements Function<ITheOneProbe, Void> {

    private static final IBlockDisplayOverride TIERED_BEEHIVE_DISPLAY_OVERRIDE = new TieredBeehiveDisplayOverride();
    private static final IBlockDisplayOverride CENTRIFUGE_DISPLAY_OVERRIDE = new CentrifugeDisplayOverride();

    @Nullable
    @Override
    public Void apply(ITheOneProbe theOneProbe) {
        theOneProbe.registerBlockDisplayOverride(TIERED_BEEHIVE_DISPLAY_OVERRIDE);
        theOneProbe.registerBlockDisplayOverride(CENTRIFUGE_DISPLAY_OVERRIDE);
        return null;
    }
}