package org.openhab.core.audio;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Mp4UdtaInjector {
    Map<String, String> textTags = Map.of("title", "©nam", "artist", "©ART", "album", "©alb", "albumArtist", "aART",
            "genre", "©gen", "composer", "©wrt", "comment", "©cmt", "year", "©day", "encoder", "©too", "lyrics",
            "©lyr");

    static class Atom {
        long size;
        String type;
        byte[] payload;

        List<Atom> children = new ArrayList<>();
        boolean container = false;
    }

    // --------------------------------------------------
    // UTILS
    // --------------------------------------------------
    static long readUInt32(byte[] b, int o) {
        return ((b[o] & 0xffL) << 24) | ((b[o + 1] & 0xffL) << 16) | ((b[o + 2] & 0xffL) << 8) | (b[o + 3] & 0xffL);
    }

    static void writeUInt32(OutputStream out, long v) throws IOException {
        out.write(new byte[] { (byte) (v >> 24), (byte) (v >> 16), (byte) (v >> 8), (byte) (v) });
    }

    static Atom readAtom(InputStream in) throws IOException {
        byte[] header = in.readNBytes(8);
        if (header.length == 0) {
            return null;
        }
        if (header.length < 8) {
            throw new IOException("Header atom tronqué");
        }

        long size = readUInt32(header, 0);
        String type = new String(header, 4, 4, StandardCharsets.US_ASCII);

        long payloadSize = size - 8;
        byte[] payload = in.readNBytes((int) payloadSize);
        if (payload.length < payloadSize) {
            throw new IOException("Atom tronqué : " + type);
        }

        Atom a = new Atom();
        a.size = size;
        a.type = type;
        a.payload = payload;

        if (type.equals("moov") || type.equals("trak")) {
            a.container = true;
            parseChildren(a);
        }
        return a;
    }

    static void parseChildren(Atom parent) throws IOException {
        ByteArrayInputStream bin = new ByteArrayInputStream(parent.payload);

        while (true) {
            byte[] header = bin.readNBytes(8);
            if (header.length == 0) {
                break;
            }
            if (header.length < 8) {
                throw new IOException("Sub atom tronqué");
            }

            long size = readUInt32(header, 0);
            String type = new String(header, 4, 4, StandardCharsets.US_ASCII);

            long payloadSize = size - 8;
            byte[] payload = bin.readNBytes((int) payloadSize);
            if (payload.length < payloadSize) {
                throw new IOException("Sub-atom tronqué : " + type);
            }

            Atom a = new Atom();
            a.size = size;
            a.type = type;
            a.payload = payload;
            a.container = type.equals("trak");

            if (a.container) {
                parseChildren(a);
            }

            parent.children.add(a);
        }
    }

    // --------------------------------------------------
    // BUILD APPLE METADATA
    // --------------------------------------------------

    static Atom buildData(String text) {
        byte[] utf8 = text.getBytes(StandardCharsets.UTF_8);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            out.write(new byte[] { 0, 0, 0, 1 }); // fullbox: version=0, flags=1
            out.write(new byte[] { 0, 0, 0, 0 }); // reserved
            out.write(utf8);
        } catch (IOException e) {
        }

        Atom a = new Atom();
        a.type = "data";
        a.payload = out.toByteArray();
        a.size = 8 + a.payload.length;
        return a;
    }

    static Atom buildItem(String type, String text) {
        Atom data = buildData(text);

        Atom item = new Atom();
        item.type = type;
        item.container = true;
        item.payload = new byte[0];
        item.children = List.of(data);

        return item;
    }

    static Atom buildIlst(Map<String, String> tags, byte[] coverBytes) {
        List<Atom> items = new ArrayList<>();

        // text tags
        Map<String, String> textAtoms = Map.of("title", "©nam", "artist", "©ART", "album", "©alb", "albumArtist",
                "aART", "genre", "©gen", "composer", "©wrt", "comment", "©cmt", "year", "©day", "encoder", "©too",
                "lyrics", "©lyr");

        for (var entry : tags.entrySet()) {
            if (textAtoms.containsKey(entry.getKey())) {
                items.add(buildTextItem(textAtoms.get(entry.getKey()), entry.getValue()));
            }
        }

        // cover
        if (coverBytes != null) {
            items.add(buildCoverItem(coverBytes, false));
        }

        // track number
        if (tags.containsKey("track") && tags.containsKey("trackTotal")) {
            items.add(buildTrackItem(Integer.parseInt("" + tags.get("track")),
                    Integer.parseInt("" + tags.get("trackTotal"))));
        }

        Atom ilst = new Atom();
        ilst.type = "ilst";
        ilst.container = true;
        ilst.payload = new byte[0];
        ilst.children = items;

        return ilst;
    }

    static Atom buildHdlr() {
        byte[] content = { 0, 0, 0, 0, // fullbox
                0, 0, 0, 0, // pre-defined
                'm', 'd', 'i', 'r', // handler type (OK pour metadata)
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // reserved
                0 // name null-terminated
        };

        Atom a = new Atom();
        a.type = "hdlr";
        a.payload = content;
        a.size = 8 + content.length;

        return a;
    }

    static Atom buildMeta(Map<String, String> tags, byte[] coverBytes) {
        Atom meta = new Atom();
        meta.type = "meta";
        meta.container = true;

        // fullbox header : version=0, flags=0
        meta.payload = new byte[] { 0, 0, 0, 0 };

        Atom hdlr = buildHdlr(); // handler = "mdta"
        Atom ilst = buildIlst(tags, coverBytes);

        meta.children = List.of(hdlr, ilst);

        return meta;
    }

    static Atom buildUdta(Map<String, String> tags, byte[] coverBytes) {
        Atom udta = new Atom();
        udta.type = "udta";
        udta.container = true;
        udta.payload = new byte[0];

        // meta box inside UDTA
        Atom meta = buildMeta(tags, coverBytes);

        udta.children = List.of(meta);

        return udta;
    }

    // --------------------------------------------------
    // INSERTION + SIZES
    // --------------------------------------------------

    static void insertUdtaAfterTkhd(Atom moov, Atom udta) {
        for (Atom trak : moov.children) {
            if (!trak.type.equals("trak")) {
                continue;
            }

            List<Atom> newList = new ArrayList<>();
            boolean inserted = false;

            for (Atom child : trak.children) {
                newList.add(child);
                if (!inserted && child.type.equals("tkhd")) {
                    newList.add(udta);
                    inserted = true;
                }
            }

            if (!inserted) {
                newList.add(udta);
            }

            trak.children = newList;
        }
    }

    static void updateSizes(Atom a) throws IOException {
        if (!a.container) {
            a.size = 8 + a.payload.length;
            return;
        }

        byte[] prefix = new byte[0];

        if (a.type.equals("meta")) {
            prefix = Arrays.copyOf(a.payload, 4); // keep fullbox header
        }

        int total = 8 + prefix.length;

        for (Atom c : a.children) {
            updateSizes(c);
            total += c.size;
        }

        a.size = total;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(prefix);

        for (Atom c : a.children) {
            writeAtom(out, c);
        }

        a.payload = out.toByteArray();
    }

    static void writeAtom(OutputStream out, Atom a) throws IOException {
        writeUInt32(out, a.size);
        // ENCODE atom type in ISO-8859-1 (required for ©nam, ©ART)
        out.write(a.type.getBytes("ISO-8859-1"));
        out.write(a.payload);
    }

    // --------------------------------------------------
    // API PUBLIQUE
    // --------------------------------------------------

    public static void inject(InputStream in, OutputStream out, Map<String, String> tags, byte[] coverBytes)
            throws IOException {

        List<Atom> atoms = new ArrayList<>();

        Atom a;
        while ((a = readAtom(in)) != null) {
            atoms.add(a);
        }

        Atom moov = atoms.stream().filter(x -> x.type.equals("moov")).findFirst()
                .orElseThrow(() -> new IOException("Segment init sans moov"));

        Atom udtaGlobal = buildUdta(tags, coverBytes);

        moov.children.add(udtaGlobal);

        Atom udtaTrack = cloneAtom(udtaGlobal);
        insertUdtaAfterTkhd(moov, udtaTrack);

        updateSizes(moov);

        for (Atom x : atoms) {
            writeAtom(out, x);
        }
    }

    static Atom buildTextItem(String atomType, String value) {
        Atom data = buildData(value);

        Atom item = new Atom();
        item.type = atomType;
        item.container = true;
        item.payload = new byte[0];
        item.children = List.of(data);

        return item;
    }

    static Atom buildCoverItem(byte[] imageBytes, boolean isPng) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            out.write(new byte[] { 0, 0, 0, 13 }); // fullbox, flags = 13 (cover art)
            out.write(new byte[] { 0, 0, 0, 0 }); // reserved
            out.write(imageBytes);
        } catch (Exception e) {
        }

        Atom data = new Atom();
        data.type = "data";
        data.payload = out.toByteArray();
        data.size = 8 + data.payload.length;

        Atom cover = new Atom();
        cover.type = "covr";
        cover.container = true;
        cover.payload = new byte[0];
        cover.children = List.of(data);

        return cover;
    }

    static Atom buildTrackItem(int track, int total) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            out.write(new byte[] { 0, 0, 0, 0 }); // fullbox
            out.write(new byte[2]); // 2 bytes reserved
            out.write((byte) 0); // padding
            out.write((byte) track);
            out.write((byte) 0);
            out.write((byte) total);
        } catch (Exception e) {
        }

        Atom data = new Atom();
        data.type = "data";
        data.payload = out.toByteArray();
        data.size = 8 + data.payload.length;

        Atom trkn = new Atom();
        trkn.type = "trkn";
        trkn.container = true;
        trkn.payload = new byte[0];
        trkn.children = List.of(data);

        return trkn;
    }

    static Atom cloneAtom(Atom a) {
        Atom n = new Atom();
        n.type = a.type;
        n.size = a.size;
        n.container = a.container;

        if (a.payload != null) {
            n.payload = a.payload.clone();
        } else {
            n.payload = new byte[0];
        }

        for (Atom c : a.children) {
            n.children.add(cloneAtom(c));
        }

        return n;
    }
}
