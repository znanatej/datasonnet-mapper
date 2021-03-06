package com.datasonnet.header;

import com.datasonnet.document.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class Header {
    public static final String DATASONNET_HEADER = "/** DataSonnet";
    public static final String DATASONNET_VERSION = "version";
    public static final String DATASONNET_INPUT = "input";
    public static final String DATASONNET_OUTPUT = "output";
    public static final String DATASONNET_PRESERVE_ORDER = "preseveOrder";
    public static final String DATAFORMAT_PREFIX = "dataformat";
    public static final String DATAFORMAT_DEFAULT = "*";
    private final String version;
    private final boolean preserveOrder;
    private final Map<String, MediaType> namedInputs;
    private final MediaType output;
    // using maps to facilitate only one per super/sub type
    private final Map<Integer, MediaType> allInputs;
    private final Map<Integer, MediaType> dataFormats;
    public Header(String version,
                  boolean preserveOrder,
                  Map<String, MediaType> namedInputs,
                  MediaType output,
                  Map<Integer, MediaType> allInputs,
                  Map<Integer, MediaType> dataFormats) {
        this.version = version;
        this.preserveOrder = preserveOrder;
        this.namedInputs = namedInputs;
        this.output = output;
        this.allInputs = allInputs;
        this.dataFormats = dataFormats;
    }

    private static final Header EMPTY =
            new Header("1.0", true, Collections.emptyMap(), MediaTypes.ALL, Collections.emptyMap(), Collections.emptyMap());

    public static Header parseHeader(String script) throws HeaderParseException {
        if (!script.trim().startsWith(DATASONNET_HEADER)) {
            return EMPTY;
        }

        try {
            String headerSection = script
                    .substring(0, script.indexOf("*/"))
                    .replace(DATASONNET_HEADER, "").replace("*/", "");

            AtomicReference<String> version = new AtomicReference<>("1.0");
            boolean preserve = true;
            AtomicReference<MediaType> output = new AtomicReference<>();
            Map<String, MediaType> inputs = new HashMap<>(4);
            Map<Integer, MediaType> allInputs = new HashMap<>(4);
            Map<Integer, MediaType> dataformat = new HashMap<>(4);

            for (String line : headerSection.split(System.lineSeparator())) {
                if (line.startsWith(DATASONNET_VERSION)) {
                    String[] tokens = line.split("=", 2);
                    version.set(tokens[1]);
                } else if (line.startsWith(DATASONNET_PRESERVE_ORDER)) {
                    String[] tokens = line.split("=", 2);
                    preserve = Boolean.parseBoolean(tokens[1]);
                } else if (line.startsWith(DATASONNET_INPUT)) {
                    String[] tokens = line.split(" ", 3);
                    if (DATAFORMAT_DEFAULT.equals(tokens[1])) {
                        MediaType toAdd = MediaType.valueOf(tokens[2]);
                        allInputs.put(toAdd.getType().hashCode() + toAdd.getSubtype().hashCode(),
                                toAdd);
                    } else {
                        inputs.put(tokens[1], MediaType.valueOf(tokens[2]));
                    }
                } else if (line.startsWith(DATASONNET_OUTPUT)) {
                    String[] tokens = line.split(" ", 2);
                    output.set(MediaType.valueOf(tokens[1]));
                } else if (line.startsWith(DATAFORMAT_PREFIX)) {
                    String[] tokens = line.split(" ", 2);
                    MediaType toAdd = MediaType.valueOf(tokens[1]);
                    dataformat.put(toAdd.getType().hashCode() + toAdd.getSubtype().hashCode(),
                            toAdd);
                }
            }

            return new Header(version.get(), preserve, inputs, output.get(), allInputs, dataformat);
        } catch (InvalidMediaTypeException exc) {
            // TODO: 8/3/20 capture the header line
            throw new HeaderParseException("Could not parse media type from header", exc);
        }
    }

    public String getVersion() {
        return version;
    }

    public Map<String, MediaType> getNamedInputs() {
        return Collections.unmodifiableMap(namedInputs);
    }

    public MediaType getOutput() {
        return output;
    }

    public Collection<MediaType> getAllInputs() {
        return Collections.unmodifiableCollection(allInputs.values());
    }

    public Collection<MediaType> getDataFormats() {
        return Collections.unmodifiableCollection(dataFormats.values());
    }

    public boolean isPreserveOrder() {
        return preserveOrder;
    }

    public <T> Document<T> combineInputParams(String inputName, Document<T> doc) {
        if (EMPTY == this) {
            return doc;
        }

        Map<String, String> params = new HashMap<>(4);
        MediaType mediaType = doc.getMediaType();
        Integer key = mediaType.getType().hashCode() + mediaType.getSubtype().hashCode();

        if (dataFormats.containsKey(key)) {
            params.putAll(dataFormats.get(key).getParameters());
        }

        if (allInputs.containsKey(key)) {
            params.putAll(allInputs.get(key).getParameters());
        }

        if (namedInputs.containsKey(inputName)) {
            MediaType inputType = namedInputs.get(inputName);
            if (inputType != null && inputType.equalsTypeAndSubtype(mediaType)) {
                params.putAll(inputType.getParameters());
            }
        }

        params.putAll(mediaType.getParameters());

        return ((DefaultDocument<T>) doc).withMediaType(new MediaType(mediaType, params));
    }

    public MediaType combineOutputParams(MediaType mediaType) {
        if (EMPTY == this) {
            return mediaType;
        }

        Map<String, String> params = new HashMap<>(4);
        Integer key = mediaType.getType().hashCode() + mediaType.getSubtype().hashCode();

        if (dataFormats.containsKey(key)) {
            params.putAll(dataFormats.get(key).getParameters());
        }

        if (output != null && output.equalsTypeAndSubtype(mediaType)) {
            params.putAll(output.getParameters());
        }

        params.putAll(mediaType.getParameters());

        return new MediaType(mediaType, params);
    }
}
