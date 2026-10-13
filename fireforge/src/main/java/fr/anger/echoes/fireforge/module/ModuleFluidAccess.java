package fr.anger.echoes.fireforge.module;

import fr.anger.echoes.fireforge.forge.FireForgeBlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * Ce que les tuyaux d'autres mods peuvent faire sur un module : le remplir (eau dans la chaudière, lave dans le
 * réservoir). On ne vide pas une forge par ses modules.
 */
public final class ModuleFluidAccess implements IFluidHandler {

    private final FireForgeBlockEntity forge;
    private final Fluid fluid;

    public ModuleFluidAccess(FireForgeBlockEntity forge, Fluid fluid) {
        this.forge = forge;
        this.fluid = fluid;
    }

    private boolean isLava() {
        return fluid == Fluids.LAVA;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        int amount = isLava() ? forge.lava() : forge.water();
        return amount > 0 ? new FluidStack(fluid, amount) : FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank) {
        return isLava() ? forge.lavaCapacity() : forge.waterCapacity();
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return stack.getFluid().isSame(fluid);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !isFluidValid(0, resource)) return 0;
        return isLava() ? forge.fillLava(resource.getAmount(), action.simulate()) : forge.fillWater(resource.getAmount(), action.simulate());
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        return FluidStack.EMPTY;
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        return FluidStack.EMPTY;
    }
}
