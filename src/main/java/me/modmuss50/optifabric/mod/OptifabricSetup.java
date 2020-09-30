package me.modmuss50.optifabric.mod;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.apache.commons.lang3.tuple.Pair;

import org.objectweb.asm.tree.MethodNode;

import org.spongepowered.asm.mixin.Mixins;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.util.version.SemanticVersionImpl;
import net.fabricmc.loader.util.version.SemanticVersionPredicateParser;
import net.fabricmc.loader.util.version.VersionParsingException;

import com.chocohead.mm.api.ClassTinkerers;

import me.modmuss50.optifabric.patcher.ClassCache;
import me.modmuss50.optifabric.util.RemappingUtils;

public class OptifabricSetup implements Runnable {

	public static final String OPTIFABRIC_INCOMPATIBLE = "optifabric:incompatible";
	public static File optifineRuntimeJar = null;

	//This is called early on to allow us to get the transformers in beofore minecraft starts
	@Override
	public void run() {
		if(!validateLoaderVersion()) return;
		if(!validateMods()) return;

		OptifineInjector injector;
		try {
			Pair<File, ClassCache> runtime = OptifineSetup.getRuntime();
			optifineRuntimeJar = runtime.getLeft();

			//Add the optifine jar to the classpath, as
			ClassTinkerers.addURL(runtime.getLeft().toURI().toURL());

			injector = new OptifineInjector(runtime.getRight());
			injector.setup();
		} catch (Throwable e) {
			if(!OptifabricError.hasError()){
				OptifineVersion.jarType = OptifineVersion.JarType.INCOMPATIBE;
				OptifabricError.setError("Failed to load optifine, check the log for more info \n\n " + e.getMessage());
			}
			throw new RuntimeException("Failed to setup optifine", e);
		}

		if (FabricLoader.getInstance().isModLoaded("fabric-renderer-indigo")) {
			Mixins.addConfiguration("optifabric.compat.indigo.mixins.json");
		}

		if (isPresent("fabric-item-api-v1", ">=1.1.0")) {
			Mixins.addConfiguration("optifabric.compat.fabric-item-api.mixins.json");
		}

		Mixins.addConfiguration("optifabric.optifine.mixins.json");

		if(FabricLoader.getInstance().isModLoaded("cloth-client-events-v0")){
			Mixins.addConfiguration("optifabric.compat.cloth.mixins.json");
		}

		if (FabricLoader.getInstance().isModLoaded("clothesline")) {
			Mixins.addConfiguration("optifabric.compat.clothesline.mixins.json");
		}

		if (FabricLoader.getInstance().isModLoaded("trumpet-skeleton")) {
			Mixins.addConfiguration("optifabric.compat.trumpet-skeleton.mixins.json");
		}

		if (isPresent("multiconnect", ">1.3.14")) {
			Mixins.addConfiguration("optifabric.compat.multiconnect.mixins.json");
		}

		if (isPresent("now-playing", ">=1.1.0")) {
			Mixins.addConfiguration("optifabric.compat.now-playing.mixins.json");
		}

		if (isPresent("origins", ">=1.16-0.2.0")) {//ElytraFeatureRenderer
			injector.predictFuture(RemappingUtils.getClassName("class_979")).ifPresent(node -> {//ItemStack, LivingEntity
				String desc = RemappingUtils.mapMethodDescriptor("(Lnet/minecraft/class_1799;Lnet/minecraft/class_1309;)Z");

				for (MethodNode method : node.methods) {
					if ("shouldRender".equals(method.name) && desc.equals(method.desc)) {
						Mixins.addConfiguration("optifabric.compat.origins.mixins.json");
						break;
					}
				}
			});
		}

		if (FabricLoader.getInstance().isModLoaded("staffofbuilding")) {
			Mixins.addConfiguration("optifabric.compat.staffofbuilding.mixins.json");
		}

		if (FabricLoader.getInstance().isModLoaded("sandwichable")) {
			Mixins.addConfiguration("optifabric.compat.sandwichable.mixins.json");
		}

		if (isPresent("astromine", "<1.6")) {//Only needed for the 1.16.1 versions
			Mixins.addConfiguration("optifabric.compat.astromine.mixins.json");
		}

		if (FabricLoader.getInstance().isModLoaded("carpet")) {
			Mixins.addConfiguration("optifabric.compat.carpet.mixins.json");

			injector.predictFuture(RemappingUtils.getClassName("class_702")).ifPresent(node -> {//ParticleManager
				//(MatrixStack, VertexConsumerProvider$Immediate, LightmapTextureManager, Camera, Frustum)
				String desc = RemappingUtils.mapMethodDescriptor("(Lnet/minecraft/class_4587;Lnet/minecraft/class_4597$class_4598;"
																+ "Lnet/minecraft/class_765;Lnet/minecraft/class_4184;FLnet/minecraft/class_4604;)V");

				for (MethodNode method : node.methods) {
					if ("renderParticles".equals(method.name) && desc.equals(method.desc)) {
						Mixins.addConfiguration("optifabric.compat.carpet.extra-mixins.json");
						break;
					}
				}
			});
		}

		if (FabricLoader.getInstance().isModLoaded("hctm-base")) {
			Mixins.addConfiguration("optifabric.compat.hctm.mixins.json");
		}

		if (FabricLoader.getInstance().isModLoaded("mubble")) {
			Mixins.addConfiguration("optifabric.compat.mubble.mixins.json");
		}

		if (FabricLoader.getInstance().isModLoaded("phormat")) {
			Mixins.addConfiguration("optifabric.compat.phormat.mixins.json");
		}

		if (FabricLoader.getInstance().isModLoaded("chat_heads")) {
			Mixins.addConfiguration("optifabric.compat.chat-heads.mixins.json");
		}
	}

	private boolean validateMods() {
		List<ModMetadata> incompatibleMods = new ArrayList<>();
		for (ModContainer container : FabricLoader.getInstance().getAllMods()) {
			ModMetadata metadata = container.getMetadata();
			if(metadata.containsCustomValue(OPTIFABRIC_INCOMPATIBLE)) {
				incompatibleMods.add(metadata);
			}
		}
		if (!incompatibleMods.isEmpty()) {
			OptifineVersion.jarType = OptifineVersion.JarType.INCOMPATIBE;
			StringBuilder errorMessage = new StringBuilder("One or more mods have stated they are incompatible with OptiFabric\nPlease remove OptiFabric or the following mods:\n");
			for (ModMetadata metadata : incompatibleMods) {
				errorMessage.append(metadata.getName())
						.append(" (")
						.append(metadata.getId())
						.append(")\n");
			}
			OptifabricError.setError(errorMessage.toString());
		}
		return incompatibleMods.isEmpty();
	}

	private boolean validateLoaderVersion() {
		try {
			if (!isVersionValid("fabricloader", ">=0.7.0")) {
				if(!OptifabricError.hasError()){
					OptifineVersion.jarType = OptifineVersion.JarType.INCOMPATIBE;
					OptifabricError.setError("You are using an outdated version of Fabric Loader, please update!\n\nRe-run the installer, or update via your launcher. See the link for help!", "https://fabricmc.net/wiki/install");
					OptifabricError.setHelpButtonText("Installation Instructions");
					return false;
				}
			}
		} catch (Throwable e){
			if(!OptifabricError.hasError()){
				OptifineVersion.jarType = OptifineVersion.JarType.INCOMPATIBE;
				OptifabricError.setError("Failed to load optifine, check the log for more info \n\n " + e.getMessage());
			}
			throw new RuntimeException("Failed to setup optifine", e);
		}
		return true;
	}

	private boolean isPresent(String modID, String versionRange) {
		try {
			return FabricLoader.getInstance().isModLoaded(modID) && isVersionValid(modID, versionRange);
		} catch (VersionParsingException e) {
			System.err.println("Error comparing the version for ".concat(modID));
			e.printStackTrace();
			return false; //Let's just gamble on the version not being valid so also not being a problem
		}
	}

	private boolean isVersionValid(String modID, String validVersion) throws VersionParsingException {
		ModMetadata modMetadata = getModMetaData(modID);
		if(modMetadata == null) {
			throw new RuntimeException(String.format("Failed to get mod container for %s, something has broke badly.", modID));
		}

		Predicate<SemanticVersionImpl> predicate = SemanticVersionPredicateParser.create(validVersion);
		SemanticVersionImpl version = new SemanticVersionImpl(modMetadata.getVersion().getFriendlyString(), false);
		return predicate.test(version);
	}

	private ModMetadata getModMetaData(String modId) {
		Optional<ModContainer> modContainer = FabricLoader.getInstance().getModContainer(modId);
		return modContainer.map(ModContainer::getMetadata).orElse(null);
	}

}
