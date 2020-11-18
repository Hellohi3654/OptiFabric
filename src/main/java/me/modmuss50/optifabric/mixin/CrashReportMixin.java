package me.modmuss50.optifabric.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;

import me.modmuss50.optifabric.mod.OptifabricError;
import me.modmuss50.optifabric.mod.OptifabricSetup;
import me.modmuss50.optifabric.mod.OptifineVersion;

@Mixin(CrashReport.class)
abstract class CrashReportMixin {
	@Unique
	private final CrashReportSection optifine = new CrashReportSection((CrashReport) (Object) this, "OptiFabric")
			.add("OptiFine jar designed for", OptifineVersion.minecraftVersion)
			.add("OptiFine jar version", OptifineVersion.version)
			.add("OptiFine jar status", () -> {
				if (OptifineVersion.jarType != null) {
					switch (OptifineVersion.jarType) {
					case MISSING:
						return "Not found";

					case CORRUPT_ZIP:
						return "Found corrupt jar whilst searching";

					case INCOMPATIBLE:
						return "Incompatible with the current OptiFabric";

					case INTERNAL_ERROR:
						return "Error whilst searching";

					case DUPLICATED:
						return "Multiple valid jars found";

					case OPTIFINE_INSTALLER:
						return "Valid OptiFine installer";

					case OPTIFINE_MOD:
						return "Valid OptiFine mod";

					case SOMETHING_ELSE:
					default:
						return "Unexpected state: " + OptifineVersion.jarType;
					}
				} else {
					return "Unsearched";
				}
			})
			.add("OptiFine remapped jar", OptifabricSetup.optifineRuntimeJar)
			.add("OptiFabric error", () -> {
				if (OptifabricError.hasError()) {
					return OptifabricError.getError();
				} else {
					return "<None>";
				}
			})
		;

	@Inject(method = "addStackTrace", at = @At("RETURN"))
	private void addStackTrace(StringBuilder builder, CallbackInfo info) {
		optifine.addStackTrace(builder.append("\n\n"));
	}
}