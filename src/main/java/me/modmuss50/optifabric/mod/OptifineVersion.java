package me.modmuss50.optifabric.mod;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileAlreadyExistsException;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipError;
import java.util.zip.ZipException;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

import net.fabricmc.loader.api.FabricLoader;

import me.modmuss50.optifabric.patcher.ASMUtils;
import me.modmuss50.optifabric.util.ZipUtils;

public class OptifineVersion {
	public static String version;
	public static String minecraftVersion;
	public static JarType jarType;

	public static File findOptifineJar() throws IOException {
		@SuppressWarnings("deprecation")
		File modsDir = new File(FabricLoader.getInstance().getGameDirectory(), "mods");
		File[] mods = modsDir.listFiles();

		if (mods != null) {
			File optifineJar = null;

			for (File file : mods) {
				if (file.isDirectory()) {
					continue;
				}

				if ("jar".equals(FilenameUtils.getExtension(file.getName()))) {
					JarType type = getJarType(file);
					if (type.error) {
						jarType = type;
						throw new RuntimeException("An error occurred when trying to find the optifine jar: " + type.name());
					}

					if (type == JarType.OPTIFINE_MOD || type == JarType.OPTIFINE_INSTALLER) {
						if (optifineJar != null) {
							jarType = JarType.DUPLICATED;
							OptifabricError.setError("Found more than one OptiFine jar, please ensure you only have 1 copy of optifine in the mods folder!");
							throw new FileAlreadyExistsException("Multiple optifine jars: " + file.getName() + " and " + optifineJar.getName());
						}

						jarType = type;
						optifineJar =  file;
					}
				}
			}

			if (optifineJar != null) {
				return optifineJar;
			}
		}

		jarType = JarType.MISSING;
		OptifabricError.setError("OptiFabric could not find the Optifine jar in the mods folder.");
		throw new FileNotFoundException("Could not find optifine jar");
	}

	private static JarType getJarType(File file) throws IOException {
		ClassNode classNode;
		try (JarFile jarFile = new JarFile(file)) {
			JarEntry jarEntry = jarFile.getJarEntry("net/optifine/Config.class"); // New 1.14.3 location
			if (jarEntry == null) {
				return JarType.SOMETHING_ELSE;
			}
			classNode = ASMUtils.asClassNode(jarEntry, jarFile);
		} catch (ZipException | ZipError e) {
			OptifabricError.setError("The zip at " + file + " is corrupt");
			return JarType.CORRUPT_ZIP;
		}

		for (FieldNode fieldNode : classNode.fields) {
			if (fieldNode.name.equals("VERSION")) {
				version = (String) fieldNode.value;
			}
			if (fieldNode.name.equals("MC_VERSION")) {
				minecraftVersion = (String) fieldNode.value;
			}
		}

		if (version == null || version.isEmpty() || minecraftVersion == null || minecraftVersion.isEmpty()) {
			OptifabricError.setError("Unable to find OptiFine version from OptiFine jar");
			return JarType.INCOMPATIBLE;
		}

		String currentMcVersion;
		try (InputStreamReader in = new InputStreamReader(OptifineVersion.class.getResourceAsStream("/version.json"))) {
			JsonObject jsonObject = new Gson().fromJson(in, JsonObject.class);
			currentMcVersion = jsonObject.get("name").getAsString();
		} catch (IOException | JsonParseException e) {
			OptifabricError.setError("Failed to find current minecraft version");
			e.printStackTrace();
			return JarType.INTERNAL_ERROR;
		}

		if (!currentMcVersion.equals(minecraftVersion)) {
			OptifabricError.setError(String.format("This version of optifine is not compatible with the current minecraft version\n\n Optifine requires %s you have %s", minecraftVersion, currentMcVersion));
			return JarType.INCOMPATIBLE;
		}

		MutableBoolean isInstaller = new MutableBoolean(false);
		ZipUtils.iterateContents(file, (zip, zipEntry) -> {
			if (zipEntry.getName().startsWith("patch/")) {
				isInstaller.setTrue();
				return false;
			} else {
				return true;
			}
		});

		if (isInstaller.isTrue()) {
			return JarType.OPTIFINE_INSTALLER;
		} else {
			return JarType.OPTIFINE_MOD;
		}
	}

	public enum JarType {
		MISSING(true),
		OPTIFINE_MOD(false),
		OPTIFINE_INSTALLER(false),
		INCOMPATIBLE(true),
		CORRUPT_ZIP(true),
		DUPLICATED(true),
		INTERNAL_ERROR(true),
		SOMETHING_ELSE(false);

		final boolean error;

		JarType(boolean error) {
			this.error = error;
		}

		public boolean isError() {
			return error;
		}
	}
}
