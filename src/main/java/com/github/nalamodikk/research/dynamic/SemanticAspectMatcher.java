package com.github.nalamodikk.research.dynamic;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.research.aspect.Aspect;
import com.github.nalamodikk.research.aspect.ModAspects;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Word-vector semantic fallback: infers aspects from an item/block name's meaning.
 *
 * Loads the precomputed table (built offline by tools/aspect_embedding, see the
 * aspect-embedding skill) from the jar once, then per id: tokenize -> average the
 * token vectors -> cosine vs aspect anchors -> top 1-2 above a confidence margin.
 * Used as a late layer in the resolver chain, before the generic hash fallback.
 *
 * The table is global/static (in the jar); results are cached per-world by the
 * callers. If the table is absent (fresh checkout before regenerate), this no-ops.
 */
public final class SemanticAspectMatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(SemanticAspectMatcher.class);
    private static final String DIR = "/koniava_data/aspect_semantic/";
    private static final float FLOOR = 0.18f;          // ignore weaker-than-this guesses
    private static final float SECOND_RATIO = 0.85f;   // keep a 2nd aspect only if this close to the 1st

    private static volatile boolean loaded = false;
    private static boolean available = false;

    private static int dim;
    private static float scale;
    private static Map<String, Integer> vocabIndex;
    private static byte[] vocabData;        // vocabN * dim, int8
    private static Aspect[] aspects;        // aspectN
    private static float[][] aspectVecs;    // aspectN * dim, normalized

    /** Top 1-2 aspects for this id by name semantics, or empty if unknown/unavailable. */
    public static List<Aspect> match(ResourceLocation id) {
        ensureLoaded();
        if (!available) return List.of();

        float[] q = new float[dim];
        int n = 0;
        for (String token : tokenize(id)) {
            Integer idx = vocabIndex.get(token);
            if (idx == null) continue;
            int base = idx * dim;
            for (int d = 0; d < dim; d++) q[d] += vocabData[base + d] * scale;
            n++;
        }
        if (n == 0 || !normalize(q)) return List.of();

        int best = -1, second = -1;
        float bestScore = -2f, secondScore = -2f;
        for (int a = 0; a < aspects.length; a++) {
            if (aspects[a] == null) continue;
            float s = dot(q, aspectVecs[a]);
            if (s > bestScore) {
                second = best; secondScore = bestScore;
                best = a; bestScore = s;
            } else if (s > secondScore) {
                second = a; secondScore = s;
            }
        }

        List<Aspect> out = new ArrayList<>(2);
        if (best >= 0 && bestScore >= FLOOR) {
            out.add(aspects[best]);
            if (second >= 0 && secondScore >= Math.max(FLOOR, SECOND_RATIO * bestScore)) {
                out.add(aspects[second]);
            }
        }
        return out;
    }

    // ── Loading ────────────────────────────────────────────────────────────

    private static synchronized void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        try {
            int vocabN, aspectN;
            try (InputStream in = res("meta.bin")) {
                ByteBuffer bb = ByteBuffer.wrap(in.readAllBytes()).order(ByteOrder.LITTLE_ENDIAN);
                vocabN = bb.getInt();
                aspectN = bb.getInt();
                dim = bb.getInt();
                scale = bb.getFloat();
            }

            vocabIndex = new HashMap<>(vocabN * 4 / 3 + 1);
            try (BufferedReader r = new BufferedReader(new InputStreamReader(res("vocab.txt"), StandardCharsets.UTF_8))) {
                String line; int i = 0;
                while ((line = r.readLine()) != null) vocabIndex.put(line, i++);
            }

            try (InputStream in = res("vocab_i8.bin")) {
                vocabData = in.readAllBytes();
            }

            List<String> aspectIds = new ArrayList<>(aspectN);
            try (BufferedReader r = new BufferedReader(new InputStreamReader(res("aspects.txt"), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) aspectIds.add(line);
            }

            aspects = new Aspect[aspectIds.size()];
            for (int a = 0; a < aspectIds.size(); a++) {
                aspects[a] = ModAspects.get(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, aspectIds.get(a)));
            }

            aspectVecs = new float[aspectIds.size()][dim];
            try (InputStream in = res("aspects_f32.bin")) {
                ByteBuffer bb = ByteBuffer.wrap(in.readAllBytes()).order(ByteOrder.LITTLE_ENDIAN);
                for (int a = 0; a < aspectVecs.length; a++)
                    for (int d = 0; d < dim; d++) aspectVecs[a][d] = bb.getFloat();
            }

            available = true;
            LOGGER.info("Aspect semantic table loaded: {} words, {} aspects, dim {}", vocabN, aspectN, dim);
        } catch (Exception e) {
            available = false;
            LOGGER.warn("Aspect semantic table not loaded ({}); semantic scanning disabled.", e.toString());
        }
    }

    private static InputStream res(String name) {
        InputStream in = SemanticAspectMatcher.class.getResourceAsStream(DIR + name);
        if (in == null) throw new IllegalStateException("missing resource " + DIR + name);
        return in;
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private static List<String> tokenize(ResourceLocation id) {
        String name = id.getPath().replaceAll("([a-z])([A-Z])", "$1 $2");
        List<String> out = new ArrayList<>();
        for (String p : name.split("[^A-Za-z]+")) {
            if (!p.isEmpty()) out.add(p.toLowerCase(Locale.ROOT));
        }
        return out;
    }

    private static boolean normalize(float[] v) {
        double sum = 0;
        for (float x : v) sum += x * x;
        if (sum <= 1e-12) return false;
        float inv = (float) (1.0 / Math.sqrt(sum));
        for (int i = 0; i < v.length; i++) v[i] *= inv;
        return true;
    }

    private static float dot(float[] a, float[] b) {
        float s = 0;
        for (int i = 0; i < a.length; i++) s += a[i] * b[i];
        return s;
    }

    private SemanticAspectMatcher() {}
}
