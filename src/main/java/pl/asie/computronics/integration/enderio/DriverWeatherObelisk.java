package pl.asie.computronics.integration.enderio;

import java.util.LinkedHashMap;
import java.util.Locale;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import crazypants.enderio.machine.obelisk.weather.TileWeatherObelisk;
import crazypants.enderio.machine.obelisk.weather.TileWeatherObelisk.WeatherTask;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IComputerAccess;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverSidedTileEntity;
import pl.asie.computronics.integration.CCMultiPeripheral;
import pl.asie.computronics.integration.ManagedEnvironmentOCTile;
import pl.asie.computronics.reference.Names;

/**
 * @author Vexatos
 */
public class DriverWeatherObelisk {

    private static Object[] activate(TileWeatherObelisk tile) {
        if (tile.getActiveTask() == null) {
            tile.startTask();
            return new Object[] { tile.getActiveTask() != null };
        }
        return new Object[] { false };
    }

    private static Object[] canActivate(TileWeatherObelisk tile, int taskID) {
        final WeatherTask[] VALUES = WeatherTask.values();
        taskID--;
        if (taskID < 0 || taskID >= VALUES.length) {
            return new Object[] { false, "invalid weather mode. needs to be between 1 and " + VALUES.length };
        }
        return new Object[] { tile.canStartTask(VALUES[taskID]) };
    }

    private static Object[] weather_modes() {
        LinkedHashMap<String, Integer> weatherModes = new LinkedHashMap<>();
        final WeatherTask[] VALUES = WeatherTask.values();
        for (int i = 0; i < VALUES.length; i++) {
            weatherModes.put(VALUES[i].name().toLowerCase(Locale.ENGLISH), i + 1);
        }
        return new Object[] { weatherModes };
    }

    public static class OCDriver extends DriverSidedTileEntity {

        public class InternalManagedEnvironment extends ManagedEnvironmentOCTile<TileWeatherObelisk> {

            public InternalManagedEnvironment(TileWeatherObelisk tile) {
                super(tile, Names.EnderIO_WeatherObelisk);
            }

            @Override
            public int priority() {
                return 3;
            }

            @Callback(
                    doc = "function(task:number):boolean; Returns true if the specified mode can currently be activated.")
            public Object[] canActivate(Context c, Arguments a) {
                return DriverWeatherObelisk.canActivate(tile, a.checkInteger(0));
            }

            @Callback(doc = "function():boolean; Tries to change the weather; Returns true on success")
            public Object[] activate(Context c, Arguments a) {
                return DriverWeatherObelisk.activate(tile);
            }

            @Callback(doc = "This is a table of all the availabe weather modes", getter = true)
            public Object[] weather_modes(Context c, Arguments a) {
                return DriverWeatherObelisk.weather_modes();
            }
        }

        @Override
        public Class<?> getTileEntityClass() {
            return TileWeatherObelisk.class;
        }

        @Override
        public ManagedEnvironment createEnvironment(World world, int x, int y, int z, ForgeDirection side) {
            return new InternalManagedEnvironment(((TileWeatherObelisk) world.getTileEntity(x, y, z)));
        }
    }

    public static class CCDriver extends CCMultiPeripheral<TileWeatherObelisk> {

        public CCDriver() {}

        public CCDriver(TileWeatherObelisk tile, World world, int x, int y, int z) {
            super(tile, Names.EnderIO_WeatherObelisk, world, x, y, z);
        }

        @Override
        public int peripheralPriority() {
            return 3;
        }

        @Override
        public CCMultiPeripheral<TileWeatherObelisk> getPeripheral(World world, int x, int y, int z, int side) {
            TileEntity te = world.getTileEntity(x, y, z);
            if (te instanceof TileWeatherObelisk) {
                return new CCDriver((TileWeatherObelisk) te, world, x, y, z);
            }
            return null;
        }

        @Override
        public String[] getMethodNames() {
            return new String[] { "canActivate", "activate", "weather_modes" };
        }

        @Override
        public Object[] callMethod(IComputerAccess computer, ILuaContext context, int method, Object[] arguments)
                throws LuaException, InterruptedException {
            switch (method) {
                case 0: {
                    if (arguments.length < 1 || !(arguments[0] instanceof Double)) {
                        throw new LuaException("first argument needs to be a number");
                    }
                    return DriverWeatherObelisk.canActivate(tile, ((Double) arguments[0]).intValue());
                }
                case 1: {
                    return DriverWeatherObelisk.activate(tile);
                }
                case 2: {
                    return DriverWeatherObelisk.weather_modes();
                }
            }
            return null;
        }
    }
}
