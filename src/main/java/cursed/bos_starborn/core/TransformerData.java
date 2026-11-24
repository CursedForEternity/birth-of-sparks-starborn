package cursed.bos_starborn.core;

import net.minecraft.resources.ResourceLocation;

public class TransformerData {
    private boolean morphed = false;
    private boolean altMode = false;

    private ResourceLocation torsoId = new ResourceLocation("bos_stardom", "testformer/torso");
    private ResourceLocation headId  = new ResourceLocation("bos_stardom", "testformer/head");
    private ResourceLocation armsId  = new ResourceLocation("bos_stardom", "testformer/arms");
    private ResourceLocation legsId  = new ResourceLocation("bos_stardom", "testformer/legs");
    private ResourceLocation altId   = new ResourceLocation("bos_stardom", "testformer/alt");

    private int primaryColor   = 0xD92B2B;
    private int secondaryColor = 0x2B59D9;
    private int accentColor    = 0xF2C94C;

    public boolean isMorphed() { return morphed; }
    public void setMorphed(boolean morphed) { this.morphed = morphed; }

    public boolean isAltMode() { return altMode; }
    public void setAltMode(boolean altMode) { this.altMode = altMode; }

    public ResourceLocation getTorsoId() { return torsoId; }
    public ResourceLocation getHeadId()  { return headId; }
    public ResourceLocation getArmsId()  { return armsId; }
    public ResourceLocation getLegsId()  { return legsId; }
    public ResourceLocation getAltId()   { return altId; }

    public int getPrimaryColor()   { return primaryColor; }
    public int getSecondaryColor() { return secondaryColor; }
    public int getAccentColor()    { return accentColor; }
}