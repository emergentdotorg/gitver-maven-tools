package org.emergent.gittle.maven.extension;

import com.google.gson.JsonElement;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.emergent.gittle.core.Config;
import org.emergent.gittle.core.GittleException;
import org.emergent.gittle.core.PropCodec;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.emergent.gittle.core.gson.GsonUtil.OBJ_MAP_TT;

class ExtensionUtil {
    public static final String REVISION = "revision";
    public static final String $_REVISION = "${revision}";

    static Model readModelFromPom(Path pomPath) {
        try (InputStream inputStream = Files.newInputStream(pomPath)) {
            MavenXpp3Reader reader = new MavenXpp3Reader();
            return reader.read(inputStream);
        } catch (IOException | XmlPullParserException e) {
            throw new GittleException(e.getMessage(), e);
        }
    }

    static void writeModelToPom(Model projectModel, Path newPomPath) {
        try (Writer fileWriter = Files.newBufferedWriter(newPomPath, Charset.defaultCharset())) {
            MavenXpp3Writer writer = new MavenXpp3Writer();
            writer.write(fileWriter, projectModel);
        } catch (IOException e) {
            throw new GittleException(e.getMessage(), e);
        }
    }

    public static Xpp3Dom toXml(Config src) {
        JsonElement json = PropCodec.toJsonTree(src, Config.class);
        Map<String, Object> map = PropCodec.fromJsonTree(json, OBJ_MAP_TT.getType());
        return toXml("configuration", map);
    }

    private static Xpp3Dom toXml(String name, Object value) {
        Xpp3Dom dom = new Xpp3Dom(name);
        if (value instanceof Map<?, ?> m) {
            toXml(m).forEach(dom::addChild);
        } else if (value != null) {
            dom.setValue(String.valueOf(value));
        }
        return dom;
    }

    private static List<Xpp3Dom> toXml(Map<?, ?> map) {
        return map.entrySet().stream()
                .filter(e -> e.getKey() instanceof String)
                .filter(e -> Objects.nonNull(e.getValue()))
                .map(e -> toXml((String) e.getKey(), e.getValue()))
                .toList();
    }
}
