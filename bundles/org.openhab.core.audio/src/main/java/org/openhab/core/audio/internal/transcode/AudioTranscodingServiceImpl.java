/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.core.audio.internal.transcode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioStream;
import org.openhab.core.audio.transcode.AudioTranscoder;
import org.openhab.core.audio.transcode.AudioTranscodingException;
import org.openhab.core.audio.transcode.AudioTranscodingService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link AudioTranscodingService} that dynamically tracks {@link AudioTranscoder}
 * components and resolves transcoding paths.
 *
 * @author Florian Hotze - Initial contribution
 */
@Component(service = AudioTranscodingService.class)
@NonNullByDefault
public class AudioTranscodingServiceImpl implements AudioTranscodingService {
    private static final int MAX_STEPS = 3;

    private final Logger logger = LoggerFactory.getLogger(AudioTranscodingServiceImpl.class);

    private final Set<AudioTranscoder> transcoders = new CopyOnWriteArraySet<>();

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addAudioTranscoder(AudioTranscoder transcoder) {
        logger.debug("Adding audio transcoder: {}", transcoder.getId());
        transcoders.add(transcoder);
    }

    public void removeAudioTranscoder(AudioTranscoder transcoder) {
        logger.debug("Removing audio transcoder: {}", transcoder.getId());
        transcoders.remove(transcoder);
    }

    @Override
    public boolean canTranscode(AudioFormat sourceFormat, AudioFormat targetFormat) {
        if (targetFormat.isCompatible(sourceFormat)) {
            return true;
        }

        return findTranscodingPath(sourceFormat, List.of(targetFormat)) != null;
    }

    @Override
    public AudioStream transcode(AudioStream source, AudioFormat targetFormat) throws AudioTranscodingException {
        if (targetFormat.isCompatible(source.getFormat())) {
            return source;
        }

        TranscodingPlan plan = findTranscodingPath(source.getFormat(), List.of(targetFormat));
        if (plan == null) {
            throw new AudioTranscodingException(
                    "No transcoding path available from " + source.getFormat() + " to " + targetFormat);
        }

        return transcode(source, plan);
    }

    @Override
    public AudioStream transcodeToSupported(AudioStream source, Set<AudioFormat> candidateFormats)
            throws AudioTranscodingException {
        for (AudioFormat candidate : candidateFormats) {
            if (candidate.isCompatible(source.getFormat())) {
                return source;
            }
        }

        TranscodingPlan plan = findTranscodingPath(source.getFormat(), candidateFormats);
        if (plan == null) {
            throw new AudioTranscodingException("No transcoding path available from format " + source.getFormat()
                    + " to any of " + candidateFormats);
        }

        return transcode(source, plan);
    }

    private AudioStream transcode(AudioStream source, TranscodingPlan plan) throws AudioTranscodingException {
        AudioStream currentStream = source;
        for (TranscodingStep step : plan.steps()) {
            logger.debug("Executing transcoding step with '{}' towards format '{}'", step.transcoder().getId(),
                    step.targetFormat());
            currentStream = step.transcoder().transcode(currentStream, step.targetFormat());
        }
        return currentStream;
    }

    /**
     * Finds a transcoding path from the given start format to the given goal formats using iterative depth-first search
     * (DFS) with an explicit stack.
     *
     * @param start
     * @param goals
     * @return
     */
    private @Nullable TranscodingPlan findTranscodingPath(AudioFormat start, Collection<AudioFormat> goals) {
        AudioTranscoder[] all = transcoders.toArray(new AudioTranscoder[0]);
        AudioFormat[] goalArr = goals.toArray(new AudioFormat[0]);
        AudioFormat[][] targets = new AudioFormat[all.length][];
        for (int i = 0; i < all.length; i++) {
            targets[i] = all[i].getSupportedTargetFormats().toArray(new AudioFormat[0]);
        }

        // per-depth search state (the explicit stack)
        int[] ti = new int[MAX_STEPS]; // current transcoder index
        int[] oi = new int[MAX_STEPS]; // current output index: declared targets first, then goals
        int[] costBefore = new int[MAX_STEPS];
        AudioFormat[] from = new AudioFormat[MAX_STEPS];
        // current path
        AudioTranscoder[] pathT = new AudioTranscoder[MAX_STEPS];
        AudioFormat[] pathF = new AudioFormat[MAX_STEPS];

        int bestCost = Integer.MAX_VALUE;
        int bestLength = Integer.MAX_VALUE;
        AudioFormat bestGoal = null;
        List<TranscodingStep> bestSteps = null;

        from[0] = start;
        int depth = 0;
        while (depth >= 0) {
            if (ti[depth] >= all.length) {
                depth--; // this level is exhausted, the parent resumes with its already advanced index
                continue;
            }
            AudioTranscoder transcoder = all[ti[depth]];
            AudioFormat[] declared = targets[ti[depth]];
            int next = costBefore[depth] + transcoder.getCost();

            if (next > bestCost || oi[depth] >= declared.length + goalArr.length) {
                ti[depth]++; // next transcoder
                oi[depth] = 0;
                continue;
            }

            int o = oi[depth]++;
            AudioFormat out = o < declared.length ? declared[o] : goalArr[o - declared.length];
            boolean createsCycle = false;
            for (int i = 0; i <= depth; i++) {
                if (from[i].equals(out)) {
                    createsCycle = true;
                    break;
                }
            }
            if (createsCycle) {
                continue;
            }
            if (!transcoder.canTranscode(from[depth], out)) {
                continue;
            }
            pathT[depth] = transcoder;
            pathF[depth] = out;

            AudioFormat hit = null;
            for (AudioFormat goal : goalArr) {
                if (goal.isCompatible(out)) {
                    hit = goal;
                    break;
                }
            }

            if (hit != null) {
                int length = depth + 1;
                if (next < bestCost || (next == bestCost && length < bestLength)) {
                    bestCost = next;
                    bestLength = length;
                    bestGoal = hit;
                    List<TranscodingStep> steps = new ArrayList<>(length);
                    for (int i = 0; i < length; i++) {
                        steps.add(new TranscodingStep(pathT[i], pathF[i]));
                    }
                    bestSteps = steps;
                }
            } else if (depth + 1 < MAX_STEPS) {
                depth++; // descend
                ti[depth] = 0;
                oi[depth] = 0;
                costBefore[depth] = next;
                from[depth] = out;
            }
        }

        return bestSteps == null || bestGoal == null ? null : new TranscodingPlan(bestGoal, bestSteps);
    }

    private record TranscodingStep(AudioTranscoder transcoder, AudioFormat targetFormat) {
    }

    private record TranscodingPlan(AudioFormat target, List<TranscodingStep> steps) {
    }
}
