package fr.anger.echoes.fireforge.workbench;

import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** Les tuyaux d'autres mods remplissent le bouilleur d'eau ; on ne le vide pas par là. */
public final class SteamBoilerFluidAccess implements IFluidHandler {

    private final SteamBoilerBlockEntity boiler;

    public SteamBoilerFluidAccess(SteamBoilerBlockEntity boiler) {
        this.boiler = boiler;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return boiler.water() > 0 ? new FluidStack(Fluids.WATER, boiler.water()) : FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank) {
        return Steam.TANK;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return stack.getFluid().isSame(Fluids.WATER);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !isFluidValid(0, resource)) return 0;
        return boiler.fillWater(resource.getAmount(), action.simulate());
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
