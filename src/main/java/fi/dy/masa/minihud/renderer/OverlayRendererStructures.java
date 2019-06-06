package fi.dy.masa.minihud.renderer;

import java.util.Collection;
import org.lwjgl.opengl.GL11;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.util.Color4f;
import fi.dy.masa.minihud.config.RendererToggle;
import fi.dy.masa.minihud.util.DataStorage;
import fi.dy.masa.minihud.util.MiscUtils;
import fi.dy.masa.minihud.util.StructureData;
import fi.dy.masa.minihud.util.StructureType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MutableIntBoundingBox;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.dimension.OverworldDimension;

public class OverlayRendererStructures extends OverlayRendererBase
{
    @Override
    public boolean shouldRender(MinecraftClient mc)
    {
        if (RendererToggle.OVERLAY_STRUCTURE_MAIN_TOGGLE.getBooleanValue() == false)
        {
            return false;
        }

        if (mc.world.dimension instanceof OverworldDimension)
        {
            return StructureType.BURIED_TREASURE.isEnabled() ||
                   StructureType.DESERT_PYRAMID.isEnabled() ||
                   StructureType.IGLOO.isEnabled() ||
                   StructureType.JUNGLE_TEMPLE.isEnabled() ||
                   StructureType.MANSION.isEnabled() ||
                   StructureType.MINESHAFT.isEnabled() ||
                   StructureType.OCEAN_MONUMENT.isEnabled() ||
                   StructureType.OCEAN_RUIN.isEnabled() ||
                   StructureType.PILLAGER_OUTPOST.isEnabled() ||
                   StructureType.SHIPWRECK.isEnabled() ||
                   StructureType.STRONGHOLD.isEnabled() ||
                   StructureType.VILLAGE.isEnabled() ||
                   StructureType.WITCH_HUT.isEnabled();
        }
        else if (mc.world.dimension.isNether())
        {
            return StructureType.NETHER_FORTRESS.isEnabled();
        }
        else
        {
            return StructureType.END_CITY.isEnabled();
        }
    }

    @Override
    public boolean needsUpdate(Entity entity, MinecraftClient mc)
    {
        int hysteresis = 16;

        return DataStorage.getInstance().hasStructureDataChanged() ||
               Math.abs(entity.x - this.lastUpdatePos.getX()) > hysteresis ||
               Math.abs(entity.y - this.lastUpdatePos.getY()) > hysteresis ||
               Math.abs(entity.z - this.lastUpdatePos.getZ()) > hysteresis;
    }

    @Override
    public void update(Entity entity, MinecraftClient mc)
    {
        RenderObjectBase renderQuads = this.renderObjects.get(0);
        RenderObjectBase renderLines = this.renderObjects.get(1);
        BUFFER_1.begin(renderQuads.getGlMode(), VertexFormats.POSITION_COLOR);
        BUFFER_2.begin(renderLines.getGlMode(), VertexFormats.POSITION_COLOR);

        this.updateStructures(mc.world.dimension.getType(), this.lastUpdatePos, mc);

        BUFFER_1.end();
        BUFFER_2.end();

        renderQuads.uploadData(BUFFER_1);
        renderLines.uploadData(BUFFER_2);
    }

    @Override
    public void allocateGlResources()
    {
        this.allocateBuffer(GL11.GL_QUADS);
        this.allocateBuffer(GL11.GL_LINES);
    }

    private void updateStructures(DimensionType dimensionType, BlockPos playerPos, MinecraftClient mc)
    {
        ArrayListMultimap<StructureType, StructureData> structures = DataStorage.getInstance().getCopyOfStructureData();
        int maxRange = (mc.options.viewDistance + 4) * 16;

        for (StructureType type : StructureType.values())
        {
            if (type.isEnabled() && type.existsInDimension(dimensionType))
            {
                Collection<StructureData> structureData = structures.get(type);

                if (structureData.isEmpty() == false)
                {
                    this.renderStructuresWithinRange(type, structureData, playerPos, maxRange);
                }
            }
        }
    }

    private void renderStructuresWithinRange(StructureType type, Collection<StructureData> structureData, BlockPos playerPos, int maxRange)
    {
        for (StructureData structure : structureData)
        {
            if (MiscUtils.isStructureWithinRange(structure.getBoundingBox(), playerPos, maxRange))
            {
                this.renderStructure(type, structure);
            }
        }
    }

    private void renderStructure(StructureType type, StructureData structure)
    {
        Color4f color = type.getToggle().getColorMain().getColor();
        ImmutableList<MutableIntBoundingBox> components = structure.getComponents();

        fi.dy.masa.malilib.render.RenderUtils.drawBox(structure.getBoundingBox(), color, BUFFER_1, BUFFER_2);

        if (components.isEmpty() == false)
        {
            if (components.size() > 1 || MiscUtils.areBoxesEqual(components.get(0), structure.getBoundingBox()) == false)
            {
                color = type.getToggle().getColorComponents().getColor();

                for (MutableIntBoundingBox bb : components)
                {
                    fi.dy.masa.malilib.render.RenderUtils.drawBox(bb, color, BUFFER_1, BUFFER_2);
                }
            }
        }
    }
}
