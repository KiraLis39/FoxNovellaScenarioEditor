package config;

import iom.interfaces.JConfigurable;
import lombok.Data;

import java.nio.file.Path;
import java.nio.file.Paths;

@Data
public class Configuration implements JConfigurable {
    private Path source = Path.of("./config.dingo");

    private Path lastOpenPath;
    private Path screensPath;
    private Path backgsPath;
    private Path musicPath;
    private Path soundPath;
    private Path voicePath;
    private Path npcPath;
    private Path heroAvatarsPath = Paths.get("D:\\JavaProj\\MyWorkspace\\Dejavu_2019\\resources\\pictures\\npc");

    @Override
    public void setSource(Path path) {
        source = path;
    }

    @Override
    public Path getSource() {
        return source;
    }
}
